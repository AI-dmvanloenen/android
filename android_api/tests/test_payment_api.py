# -*- coding: utf-8 -*-
import json
from odoo.tests import HttpCase, tagged


@tagged('post_install', '-at_install')
class TestPaymentAPI(HttpCase):
    """Test cases for Payment API endpoints."""

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
            'name': 'Test Customer for Payment',
            'customer_rank': 1,
        })

        # Get a bank journal
        cls.bank_journal = cls.env['account.journal'].search([
            ('type', '=', 'bank'),
            ('company_id', '=', cls.env.company.id),
        ], limit=1)

    def _get_headers(self):
        return {
            'Authorization': f'Bearer {self.api_key_value}',
            'Content-Type': 'application/json',
        }

    def test_post_payment_unauthorized(self):
        """Test POST /payments without auth returns 401."""
        payload = json.dumps([{
            'mobile_uid': 'test',
            'partner_id': 1,
            'amount': 100,
        }])
        response = self.url_open('/payments', data=payload)
        self.assertEqual(response.status_code, 401)

    def test_post_payment_create(self):
        """Test POST /payments creates a new payment."""
        payload = json.dumps([{
            'mobile_uid': 'payment-test-001',
            'partner_id': self.test_partner.id,
            'amount': 500.00,
            'date': '2024-01-15',
            'memo': 'Test payment',
        }])

        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['count'], 1)
        self.assertEqual(data['data'][0]['mobile_uid'], 'payment-test-001')
        self.assertEqual(data['data'][0]['amount'], 500.00)

    def test_post_payment_update(self):
        """Test POST /payments updates existing payment."""
        # First create
        payload = json.dumps([{
            'mobile_uid': 'payment-update-test',
            'partner_id': self.test_partner.id,
            'amount': 100.00,
        }])
        self.url_open('/payments', data=payload, headers=self._get_headers())

        # Then update
        payload = json.dumps([{
            'mobile_uid': 'payment-update-test',
            'partner_id': self.test_partner.id,
            'amount': 200.00,
            'memo': 'Updated memo',
        }])
        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['data'][0]['amount'], 200.00)
        self.assertEqual(data['data'][0]['memo'], 'Updated memo')

    def test_post_payment_missing_amount(self):
        """Test POST /payments without amount returns error."""
        payload = json.dumps([{
            'mobile_uid': 'no-amount-test',
            'partner_id': self.test_partner.id,
        }])

        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_payment_invalid_partner(self):
        """Test POST /payments with invalid partner returns error."""
        payload = json.dumps([{
            'mobile_uid': 'invalid-partner-payment',
            'partner_id': 999999,
            'amount': 100,
        }])

        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 400)

    def test_post_payment_with_journal(self):
        """Test POST /payments with specific journal."""
        if not self.bank_journal:
            self.skipTest("No bank journal available")

        payload = json.dumps([{
            'mobile_uid': 'journal-test-payment',
            'partner_id': self.test_partner.id,
            'amount': 150.00,
            'journal_id': self.bank_journal.id,
        }])

        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['data'][0]['journal_id'], self.bank_journal.id)

    def test_post_payment_batch(self):
        """Test POST /payments with multiple payments."""
        payload = json.dumps([
            {
                'mobile_uid': 'batch-payment-001',
                'partner_id': self.test_partner.id,
                'amount': 100.00,
            },
            {
                'mobile_uid': 'batch-payment-002',
                'partner_id': self.test_partner.id,
                'amount': 200.00,
            },
        ])

        response = self.url_open(
            '/payments',
            data=payload,
            headers=self._get_headers(),
        )
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content)
        self.assertEqual(data['count'], 2)
