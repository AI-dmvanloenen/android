# -*- coding: utf-8 -*-
import json
from odoo.tests import HttpCase, tagged


@tagged('post_install', '-at_install')
class TestDeliveryAPI(HttpCase):
    """Test cases for Delivery API endpoints."""

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # Create API key for testing
        cls.user = cls.env.ref('base.user_admin')
        cls.api_key_value = cls.env['res.users.apikeys']._generate(
            scope='rpc',
            name='Test API Key',
            user_id=cls.user.id,
        )

        # Create test partner
        cls.test_partner = cls.env['res.partner'].create({
            'name': 'Test Customer for Delivery',
            'customer_rank': 1,
        })

    def _get_headers(self):
        return {
            'Authorization': f'Bearer {self.api_key_value}',
            'Content-Type': 'application/json',
        }

    def test_get_deliveries_unauthorized(self):
        """Test GET /deliveries without auth returns 401."""
        response = self.url_open('/deliveries')
        self.assertEqual(response.status_code, 401)

    def test_get_deliveries_success(self):
        """Test GET /deliveries returns deliveries."""
        response = self.url_open('/deliveries', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertIn('total', data)
        self.assertIn('data', data)
        self.assertIn('limit', data)
        self.assertIn('offset', data)

    def test_get_deliveries_pagination(self):
        """Test GET /deliveries pagination."""
        response = self.url_open('/deliveries?limit=5&offset=0', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['limit'], 5)
        self.assertEqual(data['offset'], 0)

    def test_get_deliveries_filter_state(self):
        """Test GET /deliveries with state filter."""
        response = self.url_open('/deliveries?state=assigned', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        for delivery in data['data']:
            self.assertEqual(delivery['state'], 'assigned')

    def test_get_deliveries_filter_partner(self):
        """Test GET /deliveries with partner_id filter."""
        response = self.url_open(
            f'/deliveries?partner_id={self.test_partner.id}',
            headers=self._get_headers()
        )
        self.assertEqual(response.status_code, 200)

    def test_get_deliveries_includes_lines(self):
        """Test GET /deliveries includes move lines."""
        response = self.url_open('/deliveries', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        for delivery in data['data']:
            self.assertIn('lines', delivery)
            self.assertIsInstance(delivery['lines'], list)
