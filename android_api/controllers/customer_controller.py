# -*- coding: utf-8 -*-
import json
import logging
from datetime import datetime

from odoo import http, fields
from odoo.http import request, Response

_logger = logging.getLogger(__name__)


class CustomerController(http.Controller):
    """
    REST API Controller for Customer (res.partner) operations.

    Endpoints:
        GET  /customer - Fetch all customers
        POST /customer - Create customers in batch

    Authentication:
        All endpoints require Authorization header with valid API key
    """

    def _validate_api_key(self):
        """
        Validate the API key from Authorization header.
        Returns tuple: (api_key_record, error_response)
        """
        auth_header = request.httprequest.headers.get('Authorization', '')

        if not auth_header:
            return None, self._error_response(401, 'Missing Authorization header')

        # Support both "Bearer <key>" and raw key formats
        api_key = auth_header.replace('Bearer ', '').strip()

        api_key_model = request.env['android.api.key'].sudo()
        api_key_record = api_key_model.validate_key(api_key)

        if not api_key_record:
            return None, self._error_response(401, 'Invalid API key')

        return api_key_record, None

    def _error_response(self, status_code, message, details=None):
        """Create standardized error response."""
        error_body = {
            'error': message,
            'status': status_code
        }
        if details:
            error_body['details'] = details

        return Response(
            json.dumps(error_body),
            status=status_code,
            content_type='application/json'
        )

    def _success_response(self, data, status_code=200):
        """Create standardized success response."""
        return Response(
            json.dumps(data, default=str),
            status=status_code,
            content_type='application/json'
        )

    def _partner_to_dict(self, partner):
        """
        Convert res.partner record to API response dictionary.
        Maps Odoo fields to the expected JSON structure.
        """
        date_str = None
        if partner.mobile_sync_date:
            if isinstance(partner.mobile_sync_date, str):
                date_str = partner.mobile_sync_date
            else:
                date_str = partner.mobile_sync_date.strftime('%Y-%m-%d')

        return {
            'id': partner.id,
            'mobile_uid': partner.mobile_uid or '',
            'name': partner.name or '',
            'city': partner.city or None,
            'tax_id': partner.vat or None,
            'email': partner.email or None,
            'phone': partner.phone or None,
            'website': partner.website or None,
            'date': date_str,
        }

    def _dict_to_partner_vals(self, data):
        """
        Convert API request dictionary to res.partner field values.
        Maps JSON fields to Odoo fields.
        """
        vals = {
            'name': data.get('name'),
            'mobile_uid': data.get('mobile_uid'),
            'city': data.get('city'),
            'vat': data.get('tax_id'),
            'email': data.get('email'),
            'phone': data.get('phone'),
            'website': data.get('website'),
            'is_company': True,
            'customer_rank': 1,
        }

        date_str = data.get('date')
        if date_str:
            try:
                vals['mobile_sync_date'] = datetime.strptime(date_str, '%Y-%m-%d').date()
            except ValueError:
                _logger.warning(f"Invalid date format: {date_str}")

        return {k: v for k, v in vals.items() if v is not None}

    @http.route(
        '/customer',
        type='http',
        auth='none',
        methods=['GET'],
        csrf=False,
        cors='*'
    )
    def get_customers(self, **kwargs):
        """
        GET /customer

        Fetch all customers from res.partner.

        Headers:
            Authorization: <API_KEY>

        Returns:
            200: JSON array of customer objects
            401: Authentication failed
            500: Server error
        """
        try:
            api_key_record, error = self._validate_api_key()
            if error:
                return error

            Partner = request.env['res.partner'].sudo()
            customers = Partner.search([
                ('customer_rank', '>', 0),
                ('active', '=', True),
            ])

            result = [self._partner_to_dict(c) for c in customers]

            _logger.info(f"GET /customer - Returned {len(result)} customers")
            return self._success_response(result)

        except Exception as e:
            _logger.exception("Error in GET /customer")
            return self._error_response(500, 'Internal server error', str(e))

    @http.route(
        '/customer',
        type='http',
        auth='none',
        methods=['POST'],
        csrf=False,
        cors='*'
    )
    def create_customers(self, **kwargs):
        """
        POST /customer

        Create customers in batch.

        Headers:
            Authorization: <API_KEY>
            Content-Type: application/json

        Body:
            JSON array of customer objects

        Returns:
            200: JSON array of created customer objects (with Odoo IDs)
            400: Invalid request body
            401: Authentication failed
            500: Server error
        """
        try:
            api_key_record, error = self._validate_api_key()
            if error:
                return error

            try:
                body = request.httprequest.get_data(as_text=True)
                customers_data = json.loads(body)
            except json.JSONDecodeError as e:
                return self._error_response(400, 'Invalid JSON body', str(e))

            if not isinstance(customers_data, list):
                return self._error_response(400, 'Request body must be a JSON array')

            Partner = request.env['res.partner'].sudo()
            created_customers = []
            errors = []

            for idx, customer_data in enumerate(customers_data):
                try:
                    if not customer_data.get('name'):
                        errors.append({
                            'index': idx,
                            'error': 'Missing required field: name'
                        })
                        continue

                    if not customer_data.get('mobile_uid'):
                        errors.append({
                            'index': idx,
                            'error': 'Missing required field: mobile_uid'
                        })
                        continue

                    existing = Partner.search([
                        ('mobile_uid', '=', customer_data['mobile_uid'])
                    ], limit=1)

                    if existing:
                        vals = self._dict_to_partner_vals(customer_data)
                        vals.pop('mobile_uid', None)
                        existing.write(vals)
                        created_customers.append(self._partner_to_dict(existing))
                        _logger.info(
                            f"Updated customer with mobile_uid: {customer_data['mobile_uid']}"
                        )
                    else:
                        vals = self._dict_to_partner_vals(customer_data)
                        new_partner = Partner.create(vals)
                        created_customers.append(self._partner_to_dict(new_partner))
                        _logger.info(
                            f"Created customer with mobile_uid: {customer_data['mobile_uid']}"
                        )

                except Exception as e:
                    errors.append({
                        'index': idx,
                        'error': str(e),
                        'mobile_uid': customer_data.get('mobile_uid', 'unknown')
                    })
                    _logger.exception(f"Error creating customer at index {idx}")

            if errors:
                _logger.warning(f"POST /customer completed with {len(errors)} errors")

            _logger.info(
                f"POST /customer - Created/updated {len(created_customers)} customers"
            )
            return self._success_response(created_customers)

        except Exception as e:
            _logger.exception("Error in POST /customer")
            return self._error_response(500, 'Internal server error', str(e))
