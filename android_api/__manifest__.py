# -*- coding: utf-8 -*-
{
    'name': 'Android API',
    'version': '18.0.17.0.0',
    'category': 'Technical',
    'summary': 'REST API endpoints for Android app integration',
    'description': """
        Provides REST API endpoints for Android mobile apps.

        ## Endpoints (all support /api/v1/ prefix)
        - GET /customer - Fetch customers with filtering and pagination
        - POST /customer - Create/update customers in batch (upsert by mobile_uid)
        - GET /sales - Fetch sale orders with filtering and pagination
        - POST /sales - Create/update sale orders in batch (upsert by mobile_uid)
        - GET /deliveries - Fetch outgoing deliveries with lines
        - POST /payments - Create/update customer payments (upsert by mobile_uid)
        - GET /products - Fetch saleable products with filtering and pagination
        - GET /visits - Fetch customer visits with filtering and pagination
        - POST /visits - Create/update customer visits (upsert by mobile_uid)

        ## Features
        - API key authentication (Bearer token or raw key)
        - JSON request/response format
        - Pagination with total count metadata
        - Filtering support (city, email, partner_id, state, since)
        - Rate limiting (100 requests/minute)
        - Transaction handling for batch operations
        - Foreign key validation
        - ISO 8601 datetime format
        - Webhook notifications for record changes
        - OpenAPI/Swagger specification included

        ## Documentation
        OpenAPI spec available at: /android_api/static/api/openapi.yaml
    """,
    'author': 'Your Company',
    'website': 'https://www.yourcompany.com',
    'depends': ['base', 'contacts', 'sale', 'stock', 'account'],
    'data': [
        'security/ir.model.access.csv',
        'views/res_partner_views.xml',
        'views/webhook_views.xml',
    ],
    'installable': True,
    'application': True,
    'auto_install': False,
    'license': 'LGPL-3',
}
