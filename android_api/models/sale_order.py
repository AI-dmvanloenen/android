# -*- coding: utf-8 -*-
from odoo import models, fields


class SaleOrder(models.Model):
    _inherit = 'sale.order'

    mobile_uid = fields.Char(
        string='Mobile UID',
        index=True,
        copy=False,
        help='Unique identifier for mobile app synchronization'
    )
    mobile_sync_date = fields.Date(
        string='Mobile Sync Date',
        help='Date field for mobile app synchronization'
    )

    _sql_constraints = [
        ('mobile_uid_unique', 'UNIQUE(mobile_uid)',
         'Mobile UID must be unique!')
    ]
