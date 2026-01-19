# -*- coding: utf-8 -*-
import hashlib
import secrets

from odoo import models, fields, api


class AndroidApiKey(models.Model):
    _name = 'android.api.key'
    _description = 'API Keys for Android App'

    name = fields.Char(string='Description', required=True)
    key = fields.Char(string='API Key', required=True, copy=False)
    key_hash = fields.Char(
        string='Key Hash',
        compute='_compute_key_hash',
        store=True
    )
    active = fields.Boolean(default=True)
    user_id = fields.Many2one('res.users', string='Associated User')
    last_used = fields.Datetime(string='Last Used', readonly=True)

    @api.depends('key')
    def _compute_key_hash(self):
        for record in self:
            if record.key:
                record.key_hash = hashlib.sha256(record.key.encode()).hexdigest()
            else:
                record.key_hash = False

    @api.model
    def generate_key(self):
        """Generate a secure random API key."""
        return secrets.token_hex(32)

    @api.model
    def validate_key(self, api_key):
        """
        Validate an API key and return the associated record if valid.
        Returns False if invalid.
        """
        if not api_key:
            return False
        key_hash = hashlib.sha256(api_key.encode()).hexdigest()
        api_key_record = self.search([
            ('key_hash', '=', key_hash),
            ('active', '=', True)
        ], limit=1)
        if api_key_record:
            api_key_record.sudo().write({'last_used': fields.Datetime.now()})
            return api_key_record
        return False
