# -*- coding: utf-8 -*-
import json
from odoo.tests import HttpCase, tagged


@tagged('post_install', '-at_install')
class TestSalesAPI(HttpCase):
    """Test cases for Sales Order API endpoints."""

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
            'name': 'Test Customer for Sales',
            'customer_rank': 1,
        })

        # Create test sale order
        cls.test_order = cls.env['sale.order'].create({
            'partner_id': cls.test_partner.id,
            'mobile_uid': 'test-sale-uid-001',
        })

    def _get_headers(self):
        return {
            'Authorization': f'Bearer {self.api_key_value}',
            'Content-Type': 'application/json',
        }

    def test_get_sales_unauthorized(self):
        """Test GET /sales without auth returns 401."""
        response = self.url_open('/sales')
        self.assertEqual(response.status_code, 401)

    def test_get_sales_success(self):
        """Test GET /sales returns sales orders."""
        response = self.url_open('/sales', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertIn('total', data)
        self.assertIn('data', data)

    def test_get_sales_filter_partner(self):
        """Test GET /sales with partner_id filter."""
        response = self.url_open(
            f'/sales?partner_id={self.test_partner.id}',
            headers=self._get_headers()
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        for order in data['data']:
            self.assertEqual(order['partner_id'], self.test_partner.id)

    def test_post_sales_create(self):
        """Test POST /sales creates a new sale order."""
        payload = json.dumps([{
            'mobile_uid': 'new-sale-uid-001',
            'partner_id': self.test_partner.id,
            'date_order': '2024-01-15T10:30:00',
        }])

        response = self.url_open(
            '/sales',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['count'], 1)
        self.assertEqual(data['data'][0]['mobile_uid'], 'new-sale-uid-001')

    def test_post_sales_invalid_partner(self):
        """Test POST /sales with invalid partner_id returns error."""
        payload = json.dumps([{
            'mobile_uid': 'invalid-partner-test',
            'partner_id': 999999,  # Non-existent
        }])

        response = self.url_open(
            '/sales',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_sales_missing_partner(self):
        """Test POST /sales without partner_id returns error."""
        payload = json.dumps([{
            'mobile_uid': 'missing-partner-test',
        }])

        response = self.url_open(
            '/sales',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)
