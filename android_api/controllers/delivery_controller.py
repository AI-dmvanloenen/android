# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request

import logging

from .auth import (
    api_authenticate, get_pagination_params, json_error, paginated_response,
    check_rate_limit, format_datetime, get_filter_params, build_domain
)

_logger = logging.getLogger(__name__)

# Allowed filters for GET endpoint
DELIVERY_FILTERS = {
    'partner_id': ('partner_id', 'int'),
    'sale_id': ('sale_id', 'int'),
    'state': ('state', 'str'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class DeliveryController(http.Controller):

    @http.route('/deliveries', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_deliveries(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(DELIVERY_FILTERS)

            pickings_env = request.env['stock.picking'].sudo()

            # Base domain - only outgoing deliveries
            domain = [('picking_type_code', '=', 'outgoing')]

            # Apply filters
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain.append(('write_date', '>=', filters.pop('write_date')))
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = pickings_env.search_count(domain)

            # Get paginated results
            pickings = pickings_env.search(domain, limit=limit, offset=offset, order='id')

            result = [self.__picking_to_dict(picking) for picking in pickings]

            _logger.info(f'GET /deliveries - Returned {len(result)} of {total} deliveries')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __picking_to_dict(self, picking):
        lines = []
        for move in picking.move_ids_without_package:
            lines.append({
                'id': move.id,
                'product_id': move.product_id.id if move.product_id else None,
                'product_name': move.product_id.name if move.product_id else '',
                'quantity': move.product_uom_qty,
                'quantity_done': move.quantity,
                'uom': move.product_uom.name if move.product_uom else '',
            })

        return {
            'id': picking.id,
            'name': picking.name or '',
            'partner_id': picking.partner_id.id if picking.partner_id else None,
            'scheduled_date': format_datetime(picking.scheduled_date),
            'state': picking.state or '',
            'sale_id': picking.sale_id.id if picking.sale_id else None,
            'write_date': format_datetime(picking.write_date) if picking.write_date else None,
            'lines': lines,
        }
