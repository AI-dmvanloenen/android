# OdooFieldApp - Feature Implementation Plans

This folder contains detailed implementation plans for features needed to achieve parity with the legacy GoCongo apps.

## Current App Status

**Implemented:**
- Customer/Outlet Management (basic)
- Sales Order Management (in progress)
- Delivery Tracking
- Payment Collection
- Product Catalog
- Dashboard
- Offline-First Sync

## Feature Plans

| # | Feature | Complexity | File |
|---|---------|------------|------|
| 1 | [Visit Management](feature-01-visit-management.md) | Medium | Schedule and track sales visits |
| 2 | [GPS Location Tracking](feature-02-gps-location-tracking.md) | Low | Capture coordinates for outlets/visits |
| 3 | [Photo Capture](feature-03-photo-capture.md) | Medium | Shop photos and selfies |
| 4 | [Stock Movements](feature-04-stock-movements.md) | Medium | Track allocated/delivered/returned stock |
| 5 | [Promotions & Pricelists](feature-05-promotions-pricelists.md) | High | Customer-specific pricing and discounts |
| 6 | [Sales Targets](feature-06-sales-targets.md) | Medium | Zone-based performance tracking |
| 7 | [Delivery Rescheduling](feature-07-delivery-rescheduling.md) | Low | Reschedule deliveries with date tracking |
| 8 | [NFC/Barcode Scanning](feature-08-nfc-barcode-scanning.md) | Medium | Fast lookup via NFC cards or barcodes |

## Recommended Implementation Order

### Phase 1: Core Sales Workflow
1. **GPS Location Tracking** - Foundation for location-aware features
2. **Visit Management** - Central to sales operations

### Phase 2: Proof & Documentation
3. **Photo Capture** - Proof of visit and outlet registration

### Phase 3: Operations
4. **Delivery Rescheduling** - Quick win, low complexity
5. **Stock Movements** - Inventory visibility

### Phase 4: Business Logic
6. **Promotions & Pricelists** - Complex but high value
7. **Sales Targets** - Performance tracking

### Phase 5: Optimization
8. **NFC/Barcode Scanning** - Speed optimization for field work

## Each Plan Includes

- Data model definitions
- Files to create and modify
- API endpoints
- Database migrations
- DAO queries
- ViewModel integration
- UI components
- Verification steps

## Legacy App Reference

These plans are designed to achieve feature parity with:
- **Fresh Sales** / **GoCongo Sales** - Sales visit and order management
- **Fresh Delivery** / **GoCongo Delivery** - Delivery and payment tracking
- **TwigaPrime** - NFC-based fast delivery
