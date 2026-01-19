# -*- coding: utf-8 -*-
from odoo import models, fields, api
import requests
import json
import logging
import hashlib
import hmac

_logger = logging.getLogger(__name__)


class AndroidApiWebhook(models.Model):
    _name = 'android.api.webhook'
    _description = 'Android API Webhook Configuration'

    name = fields.Char(string='Name', required=True)
    url = fields.Char(string='Webhook URL', required=True)
    secret = fields.Char(string='Secret Key', help='Used to sign webhook payloads')
    active = fields.Boolean(default=True)

    # Events to trigger
    on_customer_create = fields.Boolean(string='Customer Created', default=True)
    on_customer_update = fields.Boolean(string='Customer Updated', default=True)
    on_sale_create = fields.Boolean(string='Sale Order Created', default=True)
    on_sale_update = fields.Boolean(string='Sale Order Updated', default=True)
    on_delivery_create = fields.Boolean(string='Delivery Created', default=True)
    on_delivery_update = fields.Boolean(string='Delivery Updated', default=True)
    on_payment_create = fields.Boolean(string='Payment Created', default=True)
    on_payment_update = fields.Boolean(string='Payment Updated', default=True)

    # Logging
    last_triggered = fields.Datetime(string='Last Triggered', readonly=True)
    last_status = fields.Selection([
        ('success', 'Success'),
        ('failed', 'Failed'),
    ], string='Last Status', readonly=True)
    last_error = fields.Text(string='Last Error', readonly=True)

    def _compute_signature(self, payload):
        """Compute HMAC-SHA256 signature for the payload."""
        if not self.secret:
            return None
        return hmac.new(
            self.secret.encode('utf-8'),
            payload.encode('utf-8'),
            hashlib.sha256
        ).hexdigest()

    def test_webhook(self):
        """Test webhook with sample data (called from UI button)."""
        self.ensure_one()
        self.trigger(
            'test.webhook',
            'android.api.webhook',
            self.id,
            {'message': 'Test webhook notification', 'webhook_name': self.name}
        )

    def trigger(self, event, model, record_id, data):
        """Trigger webhook with event data."""
        self.ensure_one()

        payload = json.dumps({
            'event': event,
            'model': model,
            'record_id': record_id,
            'data': data,
        }, default=str)

        headers = {
            'Content-Type': 'application/json',
            'X-Webhook-Event': event,
        }

        signature = self._compute_signature(payload)
        if signature:
            headers['X-Webhook-Signature'] = signature

        try:
            response = requests.post(
                self.url,
                data=payload,
                headers=headers,
                timeout=10
            )
            response.raise_for_status()

            self.write({
                'last_triggered': fields.Datetime.now(),
                'last_status': 'success',
                'last_error': False,
            })
            _logger.info(f'Webhook {self.name} triggered successfully for {event}')

        except requests.exceptions.RequestException as e:
            self.write({
                'last_triggered': fields.Datetime.now(),
                'last_status': 'failed',
                'last_error': str(e),
            })
            _logger.error(f'Webhook {self.name} failed: {e}')

    @api.model
    def notify(self, event, model, record_id, data):
        """Send notifications to all matching active webhooks."""
        event_field_map = {
            'customer.created': 'on_customer_create',
            'customer.updated': 'on_customer_update',
            'sale.created': 'on_sale_create',
            'sale.updated': 'on_sale_update',
            'delivery.created': 'on_delivery_create',
            'delivery.updated': 'on_delivery_update',
            'payment.created': 'on_payment_create',
            'payment.updated': 'on_payment_update',
        }

        field_name = event_field_map.get(event)
        if not field_name:
            return

        webhooks = self.search([
            ('active', '=', True),
            (field_name, '=', True),
        ])

        for webhook in webhooks:
            # Run in a new transaction to not block the main operation
            webhook.with_delay().trigger(event, model, record_id, data) if hasattr(webhook, 'with_delay') else webhook.trigger(event, model, record_id, data)


class ResPartnerWebhook(models.Model):
    _inherit = 'res.partner'

    @api.model_create_multi
    def create(self, vals_list):
        records = super().create(vals_list)
        for record in records:
            if record.mobile_uid:  # Only notify for mobile-synced records
                self.env['android.api.webhook'].notify(
                    'customer.created',
                    'res.partner',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return records

    def write(self, vals):
        result = super().write(vals)
        for record in self:
            if record.mobile_uid:  # Only notify for mobile-synced records
                self.env['android.api.webhook'].notify(
                    'customer.updated',
                    'res.partner',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return result


class SaleOrderWebhook(models.Model):
    _inherit = 'sale.order'

    @api.model_create_multi
    def create(self, vals_list):
        records = super().create(vals_list)
        for record in records:
            if record.mobile_uid:
                self.env['android.api.webhook'].notify(
                    'sale.created',
                    'sale.order',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return records

    def write(self, vals):
        result = super().write(vals)
        for record in self:
            if record.mobile_uid:
                self.env['android.api.webhook'].notify(
                    'sale.updated',
                    'sale.order',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return result


class StockPickingWebhook(models.Model):
    _inherit = 'stock.picking'

    @api.model_create_multi
    def create(self, vals_list):
        records = super().create(vals_list)
        for record in records:
            if record.picking_type_code == 'outgoing':
                self.env['android.api.webhook'].notify(
                    'delivery.created',
                    'stock.picking',
                    record.id,
                    {'name': record.name, 'state': record.state}
                )
        return records

    def write(self, vals):
        result = super().write(vals)
        for record in self:
            if record.picking_type_code == 'outgoing':
                self.env['android.api.webhook'].notify(
                    'delivery.updated',
                    'stock.picking',
                    record.id,
                    {'name': record.name, 'state': record.state}
                )
        return result


class AccountPaymentWebhook(models.Model):
    _inherit = 'account.payment'

    @api.model_create_multi
    def create(self, vals_list):
        records = super().create(vals_list)
        for record in records:
            if record.mobile_uid:
                self.env['android.api.webhook'].notify(
                    'payment.created',
                    'account.payment',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return records

    def write(self, vals):
        result = super().write(vals)
        for record in self:
            if record.mobile_uid:
                self.env['android.api.webhook'].notify(
                    'payment.updated',
                    'account.payment',
                    record.id,
                    {'mobile_uid': record.mobile_uid, 'name': record.name}
                )
        return result
