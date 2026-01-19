# -*- coding: utf-8 -*-
{
    'name': 'Android API',
    'version': '18.0.5.0.0',
    'category': 'Technical',
    'summary': 'REST API endpoints for Android app integration',
    'description': """
        Provides REST API endpoints for Android mobile apps:
        - GET /customer - Fetch all customers
        - POST /customer - Create customers in batch

        Features:
        - API key authentication
        - JSON request/response format
        - Custom mobile_uid field on res.partner for sync
    """,
    'author': 'Your Company',
    'website': 'https://www.yourcompany.com',
    'depends': ['base', 'contacts'],
    'data': [
        'views/res_partner_views.xml',
    ],
    'installable': True,
    'application': False,
    'auto_install': False,
    'license': 'LGPL-3',
}
