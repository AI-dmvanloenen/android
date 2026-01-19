# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request

import logging

from .auth import (
    api_authenticate, json_error, json_response, validate_required_fields,
    validate_foreign_key, check_rate_limit, format_date, parse_datetime,
    get_pagination_params, get_filter_params, build_domain, paginated_response,
    format_datetime
)

_logger = logging.getLogger(__name__)

# Allowed filters for GET endpoint
PAYMENT_FILTERS = {
    'partner_id': ('partner_id', 'int'),
    'state': ('state', 'str'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class PaymentController(http.Controller):

    @http.route('/payments', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_payments(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(PAYMENT_FILTERS)

            payments_env = request.env['account.payment'].sudo()

            # Base domain - only inbound customer payments
            domain = [('payment_type', '=', 'inbound'), ('partner_type', '=', 'customer')]

            # Apply filters
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain.append(('write_date', '>=', filters.pop('write_date')))
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = payments_env.search_count(domain)

            # Get paginated results
            payments = payments_env.search(domain, limit=limit, offset=offset, order='id')

            result = [self.__payment_to_dict(payment) for payment in payments]

            _logger.info(f'GET /payments - Returned {len(result)} of {total} payments')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    @http.route('/payments', type='http', auth='none', methods=['POST'], cors='*', csrf=False)
    def create_payments(self):
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

            payments_env = request.env['account.payment'].sudo()
            created_payments = []

            # Use savepoint for transaction handling
            with request.env.cr.savepoint():
                for idx, payment_data in enumerate(payload):
                    # Validate required fields
                    valid, error = validate_required_fields(payment_data, ['mobile_uid', 'partner_id', 'amount'])
                    if not valid:
                        return json_error(f"Validation failed at index {idx}", details={"mobile_uid": payment_data.get('mobile_uid', f'item_{idx}')})

                    # Validate foreign keys
                    valid, error = validate_foreign_key('res.partner', payment_data.get('partner_id'), 'partner_id')
                    if not valid:
                        return error

                    if payment_data.get('journal_id'):
                        valid, error = validate_foreign_key('account.journal', payment_data.get('journal_id'), 'journal_id')
                        if not valid:
                            return error

                    mobile_uid = payment_data.get('mobile_uid')
                    payment_vals = self.__dict_to_payment_vals(payment_data)

                    payment = payments_env.search([('mobile_uid', '=', mobile_uid)], limit=1)
                    if payment:
                        payment_vals.pop('mobile_uid', None)
                        payment.write(payment_vals)
                        _logger.info(f'Payment {payment.id} updated')
                    else:
                        payment = payments_env.create(payment_vals)
                        _logger.info(f'Payment {payment.id} created')

                    created_payments.append(self.__payment_to_dict(payment))

            return json_response({"count": len(created_payments), "data": created_payments})

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __payment_to_dict(self, payment):
        return {
            'id': payment.id,
            'mobile_uid': payment.mobile_uid or '',
            'name': payment.name or '',
            'partner_id': payment.partner_id.id if payment.partner_id else None,
            'amount': payment.amount,
            'date': format_date(payment.date),
            'memo': payment.ref or '',
            'journal_id': payment.journal_id.id if payment.journal_id else None,
            'state': payment.state or '',
            'write_date': format_datetime(payment.write_date) if payment.write_date else None,
        }

    def __dict_to_payment_vals(self, data):
        vals = {
            'mobile_uid': data.get('mobile_uid'),
            'partner_id': data.get('partner_id'),
            'amount': data.get('amount'),
            'payment_type': 'inbound',
            'partner_type': 'customer',
        }

        # Use provided journal_id or find default bank journal
        journal_id = data.get('journal_id')
        if journal_id:
            vals['journal_id'] = journal_id
        else:
            # Find a bank journal that accepts inbound payments
            bank_journal = request.env['account.journal'].sudo().search([
                ('type', '=', 'bank'),
                ('company_id', '=', request.env.company.id),
            ], limit=1)
            if bank_journal:
                vals['journal_id'] = bank_journal.id

        # Parse date (supports ISO 8601)
        date_str = data.get('date')
        if date_str:
            dt, _ = parse_datetime(date_str)
            if dt:
                vals['date'] = dt.date() if hasattr(dt, 'date') else dt

        memo = data.get('memo')
        if memo:
            vals['ref'] = memo

        return {k: v for k, v in vals.items() if v is not None}
