# -*- coding: utf-8 -*-
from odoo import models, fields


class AccountPayment(models.Model):
    _inherit = 'account.payment'

    mobile_uid = fields.Char(
        string='Mobile UID',
        index=True,
        copy=False,
        help='Unique identifier for mobile app synchronization'
    )

    _sql_constraints = [
        ('mobile_uid_unique', 'UNIQUE(mobile_uid)',
         'Mobile UID must be unique!')
    ]
