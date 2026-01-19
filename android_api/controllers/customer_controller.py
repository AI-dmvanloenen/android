# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request, Response
from odoo.exceptions import UserError

from datetime import datetime
import json
import logging

_logger = logging.getLogger(__name__)


class CustomerController(http.Controller):

    @http.route('/customer', type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_customers(self):
        if not self.__api_authentication():
            return Response("Unauthorized", status=401)

        try:
            partners_env = request.env['res.partner'].sudo()
            customers = partners_env.search([
                ('customer_rank', '>', 0),
                ('active', '=', True),
            ])

            result = []
            for partner in customers:
                result.append(self.__partner_to_dict(partner))

            _logger.info(f'GET /customer - Returned {len(result)} customers')
            return Response(json.dumps(result, default=str), status=200, content_type='application/json')

        except (UserError, ValueError) as exp:
            _logger.exception('API exception')
            return Response(f"BadRequest - {exp}", status=400)

        except:
            _logger.exception('API exception')
            return Response("InternalServerError", status=500)

    @http.route('/customer', type='http', auth='none', methods=['POST'], cors='*', csrf=False)
    def create_customers(self):
        if not self.__api_authentication():
            return Response("Unauthorized", status=401)

        try:
            payload = request.httprequest.json

            if not isinstance(payload, list):
                return Response("Request body must be a JSON array", status=400)

            partners_env = request.env['res.partner'].sudo()
            created_customers = []

            for customer_data in payload:
                mobile_uid = customer_data.get('mobile_uid')
                if not mobile_uid:
                    return Response("mobile_uid missing", status=400)

                name = customer_data.get('name')
                if not name:
                    return Response("name missing", status=400)

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

            return Response(json.dumps(created_customers, default=str), status=200, content_type='application/json')

        except (UserError, ValueError) as exp:
            _logger.exception('API exception')
            return Response(f"BadRequest - {exp}", status=400)

        except:
            _logger.exception('API exception')
            return Response("InternalServerError", status=500)

    def __partner_to_dict(self, partner):
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

    def __dict_to_partner_vals(self, data):
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
