# -*- coding: utf-8 -*-
from odoo import models, fields


class ResPartnerVisit(models.Model):
    _name = 'res.partner.visit'
    _description = 'Customer Visit Log'
    _order = 'visit_datetime desc'

    partner_id = fields.Many2one(
        'res.partner',
        string='Customer',
        required=True,
        ondelete='cascade',
        index=True,
        help='Customer who was visited'
    )

    visit_datetime = fields.Datetime(
        string='Visit Date & Time',
        required=True,
        index=True,
        help='Date and time of the visit'
    )

    memo = fields.Text(
        string='Notes',
        help='Visit notes and discussion summary'
    )

    mobile_uid = fields.Char(
        string='Mobile UID',
        index=True,
        copy=False,
        help='Unique identifier for mobile app synchronization'
    )

    mobile_sync_date = fields.Date(
        string='Mobile Sync Date',
        help='Last synchronization date with mobile app'
    )

    _sql_constraints = [
        ('mobile_uid_unique', 'UNIQUE(mobile_uid)',
         'Mobile UID must be unique!')
    ]
