# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request

import logging

from .auth import (
    api_authenticate, get_pagination_params, json_error, paginated_response,
    json_response, validate_required_fields, check_rate_limit,
    format_date, format_datetime, parse_datetime, get_filter_params, build_domain
)

_logger = logging.getLogger(__name__)

# Allowed filters for GET endpoint
CUSTOMER_FILTERS = {
    'city': ('city', 'str'),
    'email': ('email', 'str'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class CustomerController(http.Controller):

    @http.route('/customer', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_customers(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(CUSTOMER_FILTERS)

            partners_env = request.env['res.partner'].sudo().with_context(active_test=False)

            # Base domain
            base_domain = [
                '|', '|',
                ('customer_rank', '>', 0),
                ('sale_order_ids', '!=', False),
                ('mobile_uid', '!=', False),
            ]

            # Apply filters (added with AND)
            domain = base_domain
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain = domain + [('write_date', '>=', filters.pop('write_date'))]
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = partners_env.search_count(domain)

            # Get paginated results
            customers = partners_env.search(domain, limit=limit, offset=offset, order='id')

            result = [self.__partner_to_dict(partner) for partner in customers]

            _logger.info(f'GET /customer - Returned {len(result)} of {total} customers')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    @http.route('/customer', type='http', auth='none', methods=['POST'], cors='*', csrf=False)
    def create_customers(self):
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

            partners_env = request.env['res.partner'].sudo().with_context(active_test=False)
            created_customers = []

            # Use savepoint for transaction handling
            with request.env.cr.savepoint():
                for idx, customer_data in enumerate(payload):
                    # Validate required fields
                    valid, error = validate_required_fields(customer_data, ['mobile_uid', 'name'])
                    if not valid:
                        error_data = {"error": "Validation failed", "index": idx, "details": customer_data.get('mobile_uid', f'item_{idx}')}
                        return json_error(f"Validation failed at index {idx}", details=error_data)

                    mobile_uid = customer_data.get('mobile_uid')
                    partner_vals = self.__dict_to_partner_vals(customer_data)

                    partner = partners_env.search([('mobile_uid', '=', mobile_uid)], limit=1)
                    if partner:
                        partner_vals.pop('mobile_uid', None)
                        partner.write(partner_vals)
                        _logger.info(f'Customer {partner.id} updated')
                    else:
                        partner = partners_env.create(partner_vals)
                        _logger.info(f'Customer {partner.id} created')

                    created_customers.append(self.__partner_to_dict(partner))

            return json_response({"count": len(created_customers), "data": created_customers})

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __partner_to_dict(self, partner):
        return {
            'id': partner.id,
            'mobile_uid': partner.mobile_uid or '',
            'name': partner.name or '',
            'city': partner.city or None,
            'tax_id': partner.vat or None,
            'email': partner.email or None,
            'phone': partner.phone or None,
            'website': partner.website or None,
            'partner_latitude': partner.partner_latitude or None,
            'partner_longitude': partner.partner_longitude or None,
            'mobile_sync_date': format_date(partner.mobile_sync_date),
            'write_date': format_datetime(partner.write_date) if partner.write_date else None,
        }

    def __dict_to_partner_vals(self, data):
        vals = {
            'name': data.get('name'),
            'mobile_uid': data.get('mobile_uid'),
            'city': data.get('city'),
            'vat': data.get('tax_id'),
            'email': data.get('email'),
            'phone': data.get('phone'),
            'website': data.get('website'),
            'partner_latitude': data.get('partner_latitude'),
            'partner_longitude': data.get('partner_longitude'),
            'is_company': True,
            'customer_rank': 1,
        }

        # Parse date (supports ISO 8601)
        date_str = data.get('mobile_sync_date') or data.get('date')  # Support both field names
        if date_str:
            dt, _ = parse_datetime(date_str)
            if dt:
                vals['mobile_sync_date'] = dt.date() if hasattr(dt, 'date') else dt

        return {k: v for k, v in vals.items() if v is not None}
