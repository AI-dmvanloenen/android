# Very Nice to Have Improvements

Medium priority improvements that would enhance the codebase quality, maintainability, and robustness.

## Android App

### 19. No Pagination Support
**File:** `VisitRepositoryImpl.kt:94`

**Issue:** Always uses `null` for `since` parameter. Large datasets (10,000+ visits) could fail.

**Recommendation:**
- Implement incremental sync using the `since` parameter
- Store last sync timestamp and only fetch records modified since then
- Add pagination support for initial full sync

---

### 20. No Offline Retry Logic
**File:** `VisitRepositoryImpl.kt:159-243`

**Issue:** Failed visit creates remain PENDING but no automatic retry mechanism.

**Recommendation:**
- Implement WorkManager for background sync retries
- Add exponential backoff for failed requests
- Show pending sync count to users with manual retry option

---

### 21. Cascading Delete Without Audit
**File:** `OdooDatabase.kt:288`

**Issue:** `ForeignKey.CASCADE` on VisitEntity deletes visits silently when customer deleted. No audit trail.

**Recommendation:**
- Consider using `ForeignKey.SET_NULL` instead of CASCADE
- Add soft delete support (isDeleted flag)
- Implement audit logging for data changes

---

### 22. Log Statements Leak Data
**File:** `VisitRepositoryImpl.kt:184`

**Issue:** UUIDs logged without `BuildConfig.DEBUG` guard.

**Recommendation:**
```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Visit saved locally with tempId=$tempId, mobileUid=$mobileUid")
}
```

---

### 23. Missing KDoc Documentation
**Files:** `VisitRepository.kt`, `Visit.kt`, `VisitMapper.kt`

**Issue:** New Visit classes lack documentation.

**Recommendation:**
- Add KDoc comments to public interfaces and classes
- Document parameters, return values, and exceptions
- Add usage examples for complex functions

---

### 24. Inconsistent Status Code Messages
**Files:** Multiple repositories

**Issue:** "Visits endpoint not found" vs "Endpoint not found" - inconsistent messaging.

**Recommendation:**
- Create a centralized error message constants file
- Standardize error message format across all repositories
- Consider localization support for user-facing messages

---

## Odoo Module

### 25. Unused mobile_sync_date Field
**File:** `res_partner_visit.py:38-41`

**Issue:** Field defined but never populated. Dead code or missing implementation.

**Recommendation:**
- Implement population logic in controller when records are synced
- Or remove the field if not needed
- Document intended use case

---

### 26. Non-Versioned API Routes
**File:** `visit_controller.py:26`

**Issue:** No `/api/v1/visits` route. Other controllers have versioned routes.

**Recommendation:**
- Add versioned route: `/api/v1/visits`
- Keep legacy `/visits` route for backward compatibility
- Add deprecation warning for non-versioned routes

---

### 27. Inconsistent Error Response Format
**File:** `visit_controller.py:98`

**Issue:** Different error format from customer_controller. Should standardize.

**Recommendation:**
- Create a consistent error response helper
- Include error code, message, and details in all responses
- Document error response format in API specification

---

### 28. Missing Batch Operation Logging
**Files:** All POST controllers

**Issue:** POST endpoints don't log created vs updated counts.

**Recommendation:**
```python
created_count = sum(1 for v in created_visits if v.get('created'))
updated_count = len(created_visits) - created_count
_logger.info(f'POST /visits - Created: {created_count}, Updated: {updated_count}')
```

---

### 29. Incomplete Manifest Documentation
**File:** `__manifest__.py:18-19`

**Issue:** Missing example payloads and field mappings.

**Recommendation:**
- Add detailed endpoint documentation in manifest
- Include example request/response payloads
- Document field mappings between mobile and Odoo

---

## Priority Order

1. **High Impact, Low Effort:**
   - #22 Log statements (security)
   - #26 Versioned routes (consistency)
   - #27 Error format (developer experience)

2. **Medium Impact, Medium Effort:**
   - #20 Offline retry logic (reliability)
   - #24 Error messages (user experience)
   - #28 Batch logging (debugging)

3. **Lower Priority:**
   - #19 Pagination (scalability - needed for large deployments)
   - #21 Cascading delete audit
   - #23 KDoc documentation
   - #25 Unused field cleanup
   - #29 Manifest documentation
