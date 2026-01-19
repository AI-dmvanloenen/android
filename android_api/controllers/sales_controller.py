# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request

import logging

from .auth import (
    api_authenticate, get_pagination_params, json_error, paginated_response,
    json_response, validate_required_fields, validate_foreign_key, check_rate_limit,
    format_datetime, parse_datetime, get_filter_params, build_domain
)

_logger = logging.getLogger(__name__)

# Allowed filters for GET endpoint
SALES_FILTERS = {
    'partner_id': ('partner_id', 'int'),
    'state': ('state', 'str'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class SalesController(http.Controller):

    @http.route('/sales', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_sales(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(SALES_FILTERS)

            orders_env = request.env['sale.order'].sudo()

            # Base domain (empty = all orders)
            domain = []

            # Apply filters
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain.append(('write_date', '>=', filters.pop('write_date')))
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = orders_env.search_count(domain)

            # Get paginated results
            orders = orders_env.search(domain, limit=limit, offset=offset, order='id')

            result = [self.__sale_order_to_dict(order) for order in orders]

            _logger.info(f'GET /sales - Returned {len(result)} of {total} sale orders')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    @http.route('/sales', type='http', auth='none', methods=['POST'], cors='*', csrf=False)
    def create_sales(self):
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

            orders_env = request.env['sale.order'].sudo()
            created_orders = []

            # Use savepoint for transaction handling
            with request.env.cr.savepoint():
                for idx, order_data in enumerate(payload):
                    # Validate required fields
                    valid, error = validate_required_fields(order_data, ['mobile_uid', 'partner_id'])
                    if not valid:
                        return json_error(f"Validation failed at index {idx}", details={"mobile_uid": order_data.get('mobile_uid', f'item_{idx}')})

                    # Validate foreign key
                    valid, error = validate_foreign_key('res.partner', order_data.get('partner_id'), 'partner_id')
                    if not valid:
                        return error

                    mobile_uid = order_data.get('mobile_uid')
                    order_vals = self.__dict_to_sale_order_vals(order_data)

                    order = orders_env.search([('mobile_uid', '=', mobile_uid)], limit=1)
                    if order:
                        order_vals.pop('mobile_uid', None)
                        order.write(order_vals)
                        _logger.info(f'Sale order {order.id} updated')
                    else:
                        order = orders_env.create(order_vals)
                        _logger.info(f'Sale order {order.id} created')

                    created_orders.append(self.__sale_order_to_dict(order))

            return json_response({"count": len(created_orders), "data": created_orders})

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __sale_order_to_dict(self, order):
        return {
            'id': order.id,
            'mobile_uid': order.mobile_uid or '',
            'name': order.name or '',
            'date_order': format_datetime(order.date_order),
            'amount_total': order.amount_total,
            'state': order.state or '',
            'partner_id': order.partner_id.id if order.partner_id else None,
            'write_date': format_datetime(order.write_date) if order.write_date else None,
        }

    def __dict_to_sale_order_vals(self, data):
        vals = {
            'mobile_uid': data.get('mobile_uid'),
            'partner_id': data.get('partner_id'),
        }

        # Parse datetime (supports ISO 8601)
        date_str = data.get('date_order')
        if date_str:
            dt, _ = parse_datetime(date_str)
            if dt:
                vals['date_order'] = dt

        return {k: v for k, v in vals.items() if v is not None}
