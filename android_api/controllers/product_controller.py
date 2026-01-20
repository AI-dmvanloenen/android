# -*- coding: utf-8 -*-

from odoo import http
from odoo.http import request

import logging

from .auth import (
    api_authenticate, get_pagination_params, json_error, paginated_response,
    check_rate_limit, get_filter_params, build_domain
)

_logger = logging.getLogger(__name__)

# Allowed filters for GET endpoint
PRODUCT_FILTERS = {
    'category_id': ('categ_id', 'int'),
    'type': ('type', 'str'),
    'active': ('active', 'bool'),
    'since': ('write_date', 'datetime'),  # Records modified since
}


class ProductController(http.Controller):

    @http.route(['/products', '/api/v1/products'], type='http', auth='none', methods=['GET'], cors='*', csrf=False)
    def get_products(self):
        # Rate limiting
        allowed, error = check_rate_limit()
        if not allowed:
            return error

        if not api_authenticate():
            return json_error("Unauthorized", status=401)

        try:
            limit, offset = get_pagination_params()
            filters = get_filter_params(PRODUCT_FILTERS)

            products_env = request.env['product.product'].sudo()

            # Base domain - only saleable products
            domain = [('sale_ok', '=', True)]

            # Apply filters
            if filters:
                # Handle 'since' filter specially (>=)
                if 'write_date' in filters:
                    domain.append(('write_date', '>=', filters.pop('write_date')))
                domain = build_domain(domain, filters)

            # Get total count for pagination
            total = products_env.search_count(domain)

            # Get paginated results
            products = products_env.search(domain, limit=limit, offset=offset, order='id')

            result = [self.__product_to_dict(product) for product in products]

            _logger.info(f'GET /products - Returned {len(result)} of {total} products')
            return paginated_response(result, total, limit, offset)

        except Exception as exp:
            _logger.exception('API exception')
            return json_error("Internal server error", status=500, details=str(exp))

    def __product_to_dict(self, product):
        return {
            'id': product.id,
            'name': product.name or '',
            'default_code': product.default_code or None,  # SKU
            'barcode': product.barcode or None,
            'list_price': product.list_price,
            'uom_id': product.uom_id.id if product.uom_id else None,
            'uom_name': product.uom_id.name if product.uom_id else None,
            'categ_id': product.categ_id.id if product.categ_id else None,
            'categ_name': product.categ_id.name if product.categ_id else None,
            'type': product.type or 'consu',  # consu, service, product
            'active': product.active,
        }
