# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is an Odoo 18.0 custom module (`android_api`) that provides REST API endpoints for Android mobile app integration. It enables synchronization of customers, sales orders, deliveries, and payments between an Android app and Odoo.

## Module Structure

```
android_api/
├── __manifest__.py                  # Module metadata, version 18.0.10.0.0
├── controllers/
│   ├── auth.py                      # Shared auth, pagination, validation utilities
│   ├── customer_controller.py       # Customer endpoints
│   ├── sales_controller.py          # Sales order endpoints
│   ├── delivery_controller.py       # Delivery endpoints
│   └── payment_controller.py        # Payment endpoints
├── models/
│   ├── res_partner.py               # Extends res.partner with mobile sync fields
│   ├── sale_order.py                # Extends sale.order with mobile sync fields
│   ├── account_payment.py           # Extends account.payment with mobile sync fields
│   └── webhook.py                   # Webhook configuration and triggers
├── views/
│   ├── res_partner_views.xml        # UI extensions for partner form/tree views
│   └── webhook_views.xml            # Webhook configuration UI
├── security/
│   └── ir.model.access.csv          # Access rights
├── static/api/
│   └── openapi.yaml                 # OpenAPI/Swagger specification
└── tests/
    ├── test_customer_api.py
    ├── test_sales_api.py
    ├── test_delivery_api.py
    └── test_payment_api.py
```

## API Endpoints

All endpoints require API key authentication via `Authorization` header (supports both `Bearer <key>` and raw key formats). Keys are validated against Odoo's `res.users.apikeys` with scope `rpc`.

### Base URLs
- Legacy: `/customer`, `/sales`, `/deliveries`, `/payments`
- Versioned: `/api/v1/customer`, `/api/v1/sales`, `/api/v1/deliveries`, `/api/v1/payments`

### Rate Limiting
- 100 requests per 60 seconds per API key
- Returns HTTP 429 when exceeded

### Pagination (GET endpoints)
All GET endpoints return paginated responses with metadata:
```json
{
  "total": 1523,
  "limit": 100,
  "offset": 0,
  "count": 100,
  "data": [...]
}
```

Query parameters:
- `limit`: max records to return (default: 100, max: 1000)
- `offset`: number of records to skip (default: 0)

### Filtering (GET endpoints)
Common filters:
- `since`: ISO 8601 datetime - get records modified since (uses `write_date >= value`)

Endpoint-specific filters:
- `/customer`: `city`, `email`
- `/sales`: `partner_id`, `state`
- `/deliveries`: `partner_id`, `sale_id`, `state`

Example: `GET /api/v1/customer?city=Amsterdam&since=2024-01-01T00:00:00`

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/customer` | GET | Fetch customers (customer_rank > 0, has sale orders, or has mobile_uid) |
| `/api/v1/customer` | POST | Create/update customers in batch (upsert by `mobile_uid`, max 100) |
| `/api/v1/sales` | GET | Fetch all sale orders |
| `/api/v1/sales` | POST | Create/update sale orders in batch (upsert by `mobile_uid`, max 100) |
| `/api/v1/deliveries` | GET | Fetch outgoing deliveries with move lines |
| `/api/v1/payments` | POST | Create/update customer payments (upsert by `mobile_uid`, max 100) |

## Key Features

### JSON Error Responses
All errors return JSON with consistent format:
```json
{
  "error": "Error message",
  "details": { ... }
}
```

### Transaction Handling
POST endpoints use database savepoints for atomic batch operations. If any record fails validation, the entire batch is rolled back.

### Foreign Key Validation
POST endpoints validate that referenced records exist (partner_id, journal_id) before creating/updating.

### Webhooks
Configure webhooks in Odoo (Settings > Android API > Webhooks) to receive HTTP notifications when records are created/updated. Supports:
- HMAC-SHA256 signature verification
- Configurable events per webhook
- Event types: `customer.created`, `customer.updated`, `sale.created`, `sale.updated`, `delivery.created`, `delivery.updated`, `payment.created`, `payment.updated`

## Key Fields Added to Models

`res.partner` and `sale.order` are extended with:
- `mobile_uid` (Char, indexed, unique) - UUID for mobile app sync
- `mobile_sync_date` (Date) - Sync timestamp

`account.payment` is extended with:
- `mobile_uid` (Char, indexed, unique) - UUID for mobile app sync

## Development Commands

```bash
# Restart Odoo and update module
./odoo-bin -c odoo.conf -u android_api

# Restart Odoo and install module
./odoo-bin -c odoo.conf -i android_api

# Run with debug logging
./odoo-bin -c odoo.conf --log-level=debug -u android_api

# Run tests
./odoo-bin -c odoo.conf -d test_db --test-enable --stop-after-init -u android_api
```

## API Data Formats

All datetimes use ISO 8601 format: `YYYY-MM-DDTHH:MM:SS`

### Customer (POST /api/v1/customer, GET /api/v1/customer)
```json
{
  "id": 1,
  "mobile_uid": "uuid-string",
  "name": "Customer Name",
  "city": "City",
  "tax_id": "VAT number",
  "email": "email@example.com",
  "phone": "+1234567890",
  "website": "https://example.com",
  "mobile_sync_date": "2024-01-15",
  "write_date": "2024-01-15"
}
```
Note: `tax_id` maps to Odoo's `vat` field.

### Sale Order (POST /api/v1/sales, GET /api/v1/sales)
```json
{
  "id": 1,
  "mobile_uid": "uuid-string",
  "name": "SO001",
  "date_order": "2024-01-15T10:30:00",
  "amount_total": 1500.00,
  "state": "sale",
  "partner_id": 123,
  "write_date": "2024-01-15T10:30:00"
}
```

### Delivery (GET /api/v1/deliveries)
```json
{
  "id": 1,
  "name": "WH/OUT/00001",
  "partner_id": 123,
  "scheduled_date": "2024-01-15T10:30:00",
  "state": "assigned",
  "sale_id": 456,
  "write_date": "2024-01-15T10:30:00",
  "lines": [
    {
      "id": 1,
      "product_id": 10,
      "product_name": "Product A",
      "quantity": 10.0,
      "quantity_done": 0.0,
      "uom": "Units"
    }
  ]
}
```

### Payment (POST /api/v1/payments)
```json
{
  "id": 1,
  "mobile_uid": "uuid-string",
  "name": "CUST.IN/2024/00001",
  "partner_id": 123,
  "amount": 500.00,
  "date": "2024-01-15",
  "memo": "Invoice INV/2024/001 payment",
  "journal_id": 7,
  "state": "draft"
}
```
Note: `memo` maps to Odoo's `ref` field. Payments are created as inbound customer payments. If `journal_id` is not provided, the first bank journal is used automatically.

### Webhook Payload
```json
{
  "event": "customer.created",
  "model": "res.partner",
  "record_id": 123,
  "data": {
    "mobile_uid": "uuid-string",
    "name": "Customer Name"
  }
}
```
Headers:
- `X-Webhook-Event`: Event type
- `X-Webhook-Signature`: HMAC-SHA256 signature (if secret configured)

## Dependencies

- `base` - Core Odoo module
- `contacts` - Contacts management module
- `sale` - Sales module
- `stock` - Inventory module
- `account` - Accounting module

## OpenAPI Documentation

OpenAPI 3.0 specification available at: `/android_api/static/api/openapi.yaml`
