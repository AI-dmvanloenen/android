# UI Improvements

Recommendations for improving user experience and accessibility in the OdooFieldApp.

---

## High Priority UI Changes

### 1. Touch Targets Too Small
**File:** `DetailRow.kt:23-82`

**Issue:** Clickable items (phone, email, website) need minimum 48dp touch targets for accessibility.

**Solution:**
```kotlin
modifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 48.dp)  // Minimum touch target
    .clickable(onClick = onClick)
    .padding(vertical = 8.dp)
```

---

### 2. Generic Loading State
**File:** `CustomerDetailScreen.kt:151-152`

**Issue:** Replace "Loading..." text with shimmer skeleton or CircularProgressIndicator.

**Solution:**
```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    CircularProgressIndicator()
}
```

---

### 3. No Sync Error Retry
**File:** `CustomerDetailScreen.kt:56-75`

**Issue:** Add "Retry" button to snackbar for sync errors - currently dead-end.

**Solution:**
```kotlin
snackbarHostState.showSnackbar(
    message = "Sync failed",
    actionLabel = "Retry",
    duration = SnackbarDuration.Long
).let { result ->
    if (result == SnackbarResult.ActionPerformed) {
        onRetrySync()
    }
}
```

---

### 4. Visit Dialog No Retry
**File:** `VisitDialog.kt:94-101`

**Issue:** Add "Retry" button next to error message in dialog.

**Solution:**
Add a Row with error text and retry button when `createState is Resource.Error`.

---

### 5. Duplicate Save Buttons
**File:** `CustomerCreateScreen.kt:59-78, 260-279`

**Issue:** Save in both TopAppBar AND bottom - keep only bottom for thumb reach.

**Recommendation:** Remove the TopAppBar save button, keep only the bottom one for better thumb accessibility on large phones.

---

### 6. Add Product Button Hidden
**File:** `SaleCreateScreen.kt:232-243`

**Issue:** TextButton not prominent - convert to FAB or filled button.

**Solution:**
```kotlin
FilledTonalButton(
    onClick = onShowProductPicker,
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Add, contentDescription = null)
    Spacer(modifier = Modifier.width(8.dp))
    Text("Add Product")
}
```

---

### 7. No Character Counter
**File:** `VisitDialog.kt:80-91`

**Issue:** Memo field needs max length indicator (e.g., "0/500").

**Solution:**
```kotlin
OutlinedTextField(
    // ... existing props
    supportingText = {
        Text("${visitMemo.length}/500")
    }
)
```

---

### 8. No Undo for Deletions
**File:** `SaleCreateScreen.kt:408-418`

**Issue:** Deleting line item has no undo - add snackbar with "Undo" for 3 seconds.

**Solution:** Store deleted item temporarily and show snackbar with undo action.

---

## Medium Priority UI Changes

### 9. Phone/Email Not Clearly Tappable
**File:** `CustomerDetailScreen.kt`

**Recommendation:** Add visual cue (underline or icon change) showing these are actionable.

---

### 10. Visit Memo Truncation
**File:** `CustomerDetailScreen.kt`

**Recommendation:** Show first 3 lines with "Read more" toggle for long memos.

---

### 11. No Item Added Feedback
**File:** `SaleCreateScreen.kt`

**Recommendation:** Show toast "Product added to order" after product selection.

---

### 12. No Live Location Preview
**File:** `CustomerDetailScreen.kt`

**Recommendation:** Show lat/long updating in real-time during location capture.

---

### 13. Empty Customer List Message
**File:** `SaleCreateScreen.kt`

**Recommendation:** Add icon and message: "No customers. Sync from Dashboard first."

---

### 14. No Sale Sorting
**File:** `CustomerDetailScreen.kt`

**Recommendation:** Sort sales by date descending; show "Newest first" label.

---

### 15. Delivery State Badge Colors
**File:** `CustomerDetailScreen.kt`

**Recommendation:** Move hardcoded colors to theme or create `DeliveryStateBadge` component.

---

### 16. Inconsistent Button Styling
**Files:** Multiple

**Recommendation:** Create consistent button patterns - filled vs outlined vs text usage. Document in a style guide.

---

## Low Priority UI Changes

### 17. No Haptic Feedback

**Recommendation:** Add vibration on confirmations and important button clicks.

```kotlin
val haptic = LocalHapticFeedback.current
Button(onClick = {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    onConfirm()
})
```

---

### 18. Quantity +/- No Animation

**Recommendation:** Add brief scale animation on increment/decrement.

---

### 19. Success Auto-Dismiss Too Fast

**Recommendation:** Consider showing "Success!" for 2 seconds (currently 3 may be too long).

---

### 20. CardDefaults Elevation Varies

**Recommendation:** Standardize to single elevation value (4.dp) across all screens.

---

### 21. Loading Spinner Colors Differ

**Recommendation:** Create `LoadingButton` composable for consistent styling.

---

### 22. No Screen Transition Animations

**Recommendation:** Add `enterTransition`/`exitTransition` in Navigation.

```kotlin
composable(
    route = Screen.CustomerDetail.route,
    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
) {
    // ...
}
```

---

## Implementation Priority

### Sprint 1 (Accessibility & Critical UX)
- #1 Touch Targets (accessibility compliance)
- #3 Sync Error Retry (dead-end fix)
- #4 Visit Dialog Retry (dead-end fix)

### Sprint 2 (Polish)
- #2 Loading States
- #5 Remove duplicate save
- #7 Character Counter
- #8 Undo for Deletions

### Sprint 3 (Enhancement)
- #6 Add Product Button prominence
- #9-16 Medium priority items

### Backlog
- #17-22 Low priority nice-to-haves

---

## Design System Recommendations

Consider creating these reusable components:
1. `LoadingButton` - Button with integrated loading state
2. `DetailRow` - Standardized clickable detail row with 48dp min height
3. `StateBadge` - Colored badge for states (delivery, sync, etc.)
4. `EmptyState` - Consistent empty state with icon and message
5. `ErrorSnackbar` - Snackbar with retry action
