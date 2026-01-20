# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request
from psycopg2 import IntegrityError

import logging
import re

from .auth import (
    api_authenticate, json_error, json_response, validate_required_fields,
    validate_foreign_key, check_rate_limit, get_pagination_params,
    get_filter_params, build_domain, paginated_response, format_datetime,
    parse_datetime
)

_logger = logging.getLogger(__name__)

# UUID v4 format regex
UUID_REGEX = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', re.IGNORECASE)

# Maximum memo length (5000 characters)
MAX_MEMO_LENGTH = 5000

# Allowed filters for GET endpoint
VISIT_FILTERS = {
    'partner_id': ('partner_id', 'int'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class VisitController(http.Controller):

    @http.route('/visits', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_visits(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(VISIT_FILTERS)

            visits_env = request.env['res.partner.visit'].sudo()

            # Base domain (empty = all visits)
            domain = []

            # Apply filters
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain.append(('write_date', '>=', filters.pop('write_date')))
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = visits_env.search_count(domain)

            # Get paginated results
            visits = visits_env.search(domain, limit=limit, offset=offset, order='visit_datetime desc')

            result = [self.__visit_to_dict(visit) for visit in visits]

            _logger.info(f'GET /visits - Returned {len(result)} of {total} visits')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    @http.route('/visits', type='http', auth='none', methods=['POST'], cors='*', csrf=False)
    def create_visits(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            payload = request.httprequest.json

            if not isinstance(payload, list):
                return json_error("Request body must be a JSON array")

            if len(payload) == 0:
                return json_error("Request body cannot be empty")

            if len(payload) > 100:
                return json_error("Batch size cannot exceed 100 records")

            visits_env = request.env['res.partner.visit'].sudo()
            created_visits = []

            # Use savepoint for transaction handling
            with request.env.cr.savepoint():
                for idx, visit_data in enumerate(payload):
                    # Validate required fields
                    valid, error = validate_required_fields(visit_data, ['mobile_uid', 'partner_id', 'visit_datetime'])
                    if not valid:
                        return json_error(f"Validation failed at index {idx}: missing required fields", details={"mobile_uid": visit_data.get('mobile_uid', f'item_{idx}')})

                    mobile_uid = visit_data.get('mobile_uid')

                    # Validate mobile_uid format (UUID v4)
                    if not UUID_REGEX.match(str(mobile_uid)):
                        return json_error(
                            f"Validation failed at index {idx}: invalid mobile_uid format",
                            details={"mobile_uid": mobile_uid, "expected": "UUID v4 format (xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx)"}
                        )

                    # Validate and parse visit_datetime
                    datetime_str = visit_data.get('visit_datetime')
                    parsed_dt, parse_error = parse_datetime(datetime_str)
                    if not parsed_dt:
                        return json_error(
                            f"Validation failed at index {idx}: invalid visit_datetime format",
                            details={"mobile_uid": mobile_uid, "visit_datetime": datetime_str, "error": parse_error or "Unable to parse datetime"}
                        )

                    # Validate memo length
                    memo = visit_data.get('memo')
                    if memo and len(str(memo)) > MAX_MEMO_LENGTH:
                        return json_error(
                            f"Validation failed at index {idx}: memo exceeds maximum length",
                            details={"mobile_uid": mobile_uid, "max_length": MAX_MEMO_LENGTH, "actual_length": len(str(memo))}
                        )

                    # Validate foreign key
                    valid, error = validate_foreign_key('res.partner', visit_data.get('partner_id'), 'partner_id')
                    if not valid:
                        return error

                    visit_vals = self.__dict_to_visit_vals(visit_data, parsed_dt)

                    # Handle upsert with race condition protection
                    try:
                        visit = visits_env.search([('mobile_uid', '=', mobile_uid)], limit=1)
                        if visit:
                            visit_vals.pop('mobile_uid', None)
                            visit.write(visit_vals)
                            _logger.info(f'Visit {visit.id} updated')
                        else:
                            visit = visits_env.create(visit_vals)
                            _logger.info(f'Visit {visit.id} created')
                    except IntegrityError:
                        # Race condition: another request created the record, retry as update
                        request.env.cr.rollback()
                        visit = visits_env.search([('mobile_uid', '=', mobile_uid)], limit=1)
                        if visit:
                            visit_vals.pop('mobile_uid', None)
                            visit.write(visit_vals)
                            _logger.info(f'Visit {visit.id} updated (after race condition)')
                        else:
                            raise  # Re-raise if still not found (shouldn't happen)

                    created_visits.append(self.__visit_to_dict(visit))

            return json_response({"count": len(created_visits), "data": created_visits})

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __visit_to_dict(self, visit):
        return {
            'id': visit.id,
            'mobile_uid': visit.mobile_uid or '',
            'partner_id': visit.partner_id.id if visit.partner_id else None,
            'partner_name': visit.partner_id.name if visit.partner_id else '',
            'visit_datetime': format_datetime(visit.visit_datetime) if visit.visit_datetime else None,
            'memo': visit.memo or '',
            'write_date': format_datetime(visit.write_date) if visit.write_date else None,
        }

    def __dict_to_visit_vals(self, data, parsed_datetime=None):
        """Convert request data to Odoo field values.

        Args:
            data: The request data dictionary
            parsed_datetime: Pre-parsed datetime object (avoids re-parsing)
        """
        vals = {
            'mobile_uid': data.get('mobile_uid'),
            'partner_id': data.get('partner_id'),
        }

        # Use pre-parsed datetime if provided, otherwise parse
        if parsed_datetime:
            vals['visit_datetime'] = parsed_datetime
        else:
            datetime_str = data.get('visit_datetime')
            if datetime_str:
                dt, _ = parse_datetime(datetime_str)
                if dt:
                    vals['visit_datetime'] = dt

        memo = data.get('memo')
        if memo:
            # Truncate memo to max length as safety measure
            vals['memo'] = str(memo)[:MAX_MEMO_LENGTH]

        return {k: v for k, v in vals.items() if v is not None}
