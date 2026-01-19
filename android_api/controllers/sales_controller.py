# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request, Response
from odoo.exceptions import UserError

import json
import logging

_logger = logging.getLogger(__name__)


class SalesController(http.Controller):

    @http.route('/sales', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_sales(self):
        if not self.__api_authentication():
            return Response("Unauthorized", status=401)

        try:
            orders_env = request.env['sale.order'].sudo()
            orders = orders_env.search([])

            result = []
            for order in orders:
                result.append(self.__sale_order_to_dict(order))

            _logger.info(f'GET /sales - Returned {len(result)} sale orders')
            return Response(json.dumps(result, default=str), status=200, content_type='application/json')

        except (UserError, ValueError) as exp:
            _logger.exception('API exception')
            return Response(f"BadRequest - {exp}", status=400)

        except:
            _logger.exception('API exception')
            return Response("InternalServerError", status=500)

    def __sale_order_to_dict(self, order):
        date_str = None
        if order.date_order:
            if isinstance(order.date_order, str):
                date_str = order.date_order
            else:
                date_str = order.date_order.strftime('%Y-%m-%d %H:%M:%S')

        return {
            'id': order.id,
            'name': order.name or '',
            'date_order': date_str,
            'amount_total': order.amount_total,
            'partner_id': order.partner_id.id if order.partner_id else None,
        }

    def __api_authentication(self):
        api_key = request.httprequest.headers.get('Authorization')
        if not api_key:
            _logger.warning("Failed API authentication! No 'Authorization' header or empty!")
            return False

        # Support both "Bearer <key>" and raw key formats
        api_key = api_key.replace('Bearer ', '').strip()

        user_id = request.env['res.users.apikeys']._check_credentials(scope='rpc', key=api_key)

        if user_id:
            request.env.user = request.env['res.users'].browse(user_id)
            request.env.uid = user_id
            return True
        else:
            _logger.warning("Failed API authentication! Invalid API key")
            return False
