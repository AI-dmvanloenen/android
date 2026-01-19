# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is an Odoo 18.0 custom module (`android_api`) that provides REST API endpoints for Android mobile app integration. It enables customer synchronization between an Android app and Odoo.

## Module Structure

```
android_api/
├── __manifest__.py          # Module metadata, version 18.0.5.0.0
├── controllers/
│   └── customer_controller.py   # REST API endpoints
├── models/
│   ├── res_partner.py       # Extends res.partner with mobile sync fields
│   └── sale_order.py        # Extends sale.order with mobile sync fields
└── views/
    └── res_partner_views.xml    # UI extensions for partner form/tree views
```

## API Endpoints

All endpoints require API key authentication via `Authorization` header (supports both `Bearer <key>` and raw key formats). Keys are validated against Odoo's `res.users.apikeys` with scope `rpc`.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/customer` | GET | Fetch all active customers (`customer_rank > 0`) |
| `/customer` | POST | Create/update customers in batch (upsert by `mobile_uid`) |

## Key Fields Added to Models

Both `res.partner` and `sale.order` are extended with:
- `mobile_uid` (Char, indexed, unique) - UUID for mobile app sync
- `mobile_sync_date` (Date) - Sync timestamp

## Development Commands

This is an Odoo module. Common operations:

```bash
# Restart Odoo and update module
./odoo-bin -c odoo.conf -u android_api

# Restart Odoo and install module
./odoo-bin -c odoo.conf -i android_api

# Run with debug logging
./odoo-bin -c odoo.conf --log-level=debug -u android_api
```

## API Data Format

Customer JSON structure used in API requests/responses:
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
  "date": "2024-01-15"
}
```

Note: `tax_id` maps to Odoo's `vat` field, `date` maps to `mobile_sync_date`.

## Dependencies

- `base` - Core Odoo module
- `contacts` - Contacts management module
