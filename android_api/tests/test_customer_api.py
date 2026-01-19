# -*- coding: utf-8 -*-
import json
from odoo.tests import HttpCase, tagged


@tagged('post_install', '-at_install')
class TestCustomerAPI(HttpCase):
    """Test cases for Customer API endpoints."""

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # Create test customer
        cls.test_partner = cls.env['res.partner'].create({
            'name': 'Test Customer',
            'mobile_uid': 'test-uid-001',
            'customer_rank': 1,
            'email': 'test@example.com',
            'city': 'Test City',
        })

        # Create API key for testing
        cls.user = cls.env.ref('base.user_admin')
        cls.api_key_value = cls.env['res.users.apikeys']._generate(
            scope='rpc',
            name='Test API Key',
            user_id=cls.user.id,
        )

    def _get_headers(self):
        return {
            'Authorization': f'Bearer {self.api_key_value}',
            'Content-Type': 'application/json',
        }

    def test_get_customers_unauthorized(self):
        """Test GET /customer without auth returns 401."""
        response = self.url_open('/customer')
        self.assertEqual(response.status_code, 401)

    def test_get_customers_success(self):
        """Test GET /customer returns customers."""
        response = self.url_open('/customer', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertIn('total', data)
        self.assertIn('data', data)
        self.assertIn('limit', data)
        self.assertIn('offset', data)

    def test_get_customers_pagination(self):
        """Test GET /customer pagination."""
        response = self.url_open('/customer?limit=10&offset=0', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['limit'], 10)
        self.assertEqual(data['offset'], 0)

    def test_get_customers_filter_city(self):
        """Test GET /customer with city filter."""
        response = self.url_open('/customer?city=Test%20City', headers=self._get_headers())
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        for customer in data['data']:
            if customer['city']:
                self.assertEqual(customer['city'], 'Test City')

    def test_post_customer_create(self):
        """Test POST /customer creates a new customer."""
        payload = json.dumps([{
            'mobile_uid': 'new-test-uid-001',
            'name': 'New Test Customer',
            'email': 'newtest@example.com',
            'city': 'New City',
        }])

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['count'], 1)
        self.assertEqual(data['data'][0]['name'], 'New Test Customer')
        self.assertEqual(data['data'][0]['mobile_uid'], 'new-test-uid-001')

    def test_post_customer_update(self):
        """Test POST /customer updates existing customer."""
        payload = json.dumps([{
            'mobile_uid': 'test-uid-001',  # Existing
            'name': 'Updated Test Customer',
            'city': 'Updated City',
        }])

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['data'][0]['name'], 'Updated Test Customer')
        self.assertEqual(data['data'][0]['city'], 'Updated City')

    def test_post_customer_missing_mobile_uid(self):
        """Test POST /customer without mobile_uid returns error."""
        payload = json.dumps([{
            'name': 'Test Customer Without UID',
        }])

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_customer_missing_name(self):
        """Test POST /customer without name returns error."""
        payload = json.dumps([{
            'mobile_uid': 'test-no-name',
        }])

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_customer_invalid_body(self):
        """Test POST /customer with non-array body returns error."""
        payload = json.dumps({
            'mobile_uid': 'test',
            'name': 'Test',
        })

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_customer_batch_limit(self):
        """Test POST /customer batch size limit."""
        # Create 101 items (exceeds limit of 100)
        payload = json.dumps([
            {'mobile_uid': f'batch-test-{i}', 'name': f'Batch Test {i}'}
            for i in range(101)
        ])

        response = self.url_open(
            '/customer',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)
