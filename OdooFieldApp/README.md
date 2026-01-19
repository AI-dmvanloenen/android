# Odoo Field App

An offline-first Android application for field workers to access Odoo customer data while working in areas with limited connectivity.

## Project Overview

This app allows field workers in DRC (Congo) to:
- View customer information from Odoo
- Search and filter customers
- Work offline with locally cached data
- Sync data when connectivity is available

**Phase 1 (Current):** Customer data viewing and syncing
**Future Phases:** Sales, payments, deliveries, and two-way sync

## Architecture

### Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose (Modern declarative UI)
- **Database:** Room (SQLite wrapper)
- **Networking:** Retrofit + OkHttp
- **Dependency Injection:** Hilt
- **Async:** Coroutines + Flow
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture

### Project Structure
```
app/src/main/java/com/odoo/fieldapp/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Retrofit API, DTOs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Business models (Customer, SyncState, etc.)
│   └── repository/     # Repository interfaces
├── presentation/
│   ├── customer/       # Customer screens and ViewModel
│   ├── settings/       # Settings screen and ViewModel
│   └── navigation/     # Navigation setup
└── di/                 # Dependency injection modules
```

## Setup Instructions

### Prerequisites
1. **Android Studio** (Latest version - Hedgehog or later)
   - Download from: https://developer.android.com/studio
   
2. **Java Development Kit (JDK) 17**
   - Included with Android Studio

3. **Android Device or Emulator**
   - Min SDK: Android 7.0 (API 24)
   - Target SDK: Android 14 (API 34)

### Step 1: Open Project in Android Studio

1. Open Android Studio
2. Click **File → Open**
3. Navigate to the `OdooFieldApp` folder and click **OK**
4. Wait for Gradle sync to complete (this may take a few minutes on first run)

### Step 2: Resolve Any Gradle Issues

If you see errors:
1. Click **File → Invalidate Caches / Restart**
2. Let Android Studio download all dependencies
3. If you see "SDK not found", go to **File → Project Structure → SDK Location** and set Android SDK path

### Step 3: Configure API Endpoint (Optional)

By default, the app uses the test Odoo URL: `https://test.moko.odoo.com/`

To change this:
1. Open `app/src/main/java/com/odoo/fieldapp/di/AppModule.kt`
2. Find the `provideRetrofit()` function
3. Change the `baseUrl` parameter:
   ```kotlin
   .baseUrl("https://production.moko.odoo.com/")  // Your production URL
   ```

### Step 4: Run the App

#### Option A: On Emulator (Recommended for testing)
1. Click **Tools → Device Manager**
2. Create a new virtual device (e.g., Pixel 5, API 34)
3. Click the **Run** button (green triangle) or press **Shift+F10**
4. Select your emulator

#### Option B: On Physical Device
1. Enable **Developer Options** on your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
2. Enable **USB Debugging** in **Developer Options**
3. Connect your device via USB
4. Click **Run** and select your device

## Using the App

### First Time Setup

1. **Launch the app** - You'll see an empty customer list
2. **Open Settings** - Tap the gear icon (⚙️) in the top right
3. **Enter API Key:**
   - Paste your Odoo API key (e.g., `f0b89f06282f2b4f3b7759bd4d4afb913bc663c7`)
   - Tap **Save API Key**
4. **Go back** - Tap the back arrow
5. **Sync Data** - Tap the refresh icon (🔄) to pull customers from Odoo

### Main Features

#### Customer List
- **Search:** Type in the search bar to filter customers by name
- **Sync:** Tap refresh icon to pull latest data from Odoo
- **View Details:** Tap any customer to see full information

#### Customer Details
- View complete customer information including:
  - Contact details (phone, email, website)
  - Location (city)
  - Tax ID
  - Sync status
  - Odoo ID

#### Settings
- Configure API key
- Switch between test and production environments (future)

## Troubleshooting

### Issue: "API key not configured"
**Solution:** Go to Settings and enter your Odoo API key

### Issue: "Network error. Please check your internet connection"
**Solutions:**
- Check device internet connection
- Verify the base URL is correct
- Ensure the Odoo server is accessible from your network

### Issue: "Authentication failed. Please check your API key"
**Solutions:**
- Verify API key is correct (no extra spaces)
- Check API key hasn't expired in Odoo
- Ensure API user has proper permissions in Odoo

### Issue: "Endpoint not found"
**Solution:** Verify the `/customer` endpoint exists on your Odoo instance

### Issue: Gradle sync fails
**Solutions:**
1. Click **File → Invalidate Caches / Restart**
2. Delete `.gradle` folder in project root and sync again
3. Update Android Studio to latest version

### Issue: App crashes on startup
**Solutions:**
1. Check Logcat in Android Studio for error messages
2. Clean and rebuild: **Build → Clean Project**, then **Build → Rebuild Project**
3. Uninstall app from device/emulator and run again

## Testing Offline Mode

1. **Sync data** while online (refresh button)
2. **Enable Airplane Mode** on your device
3. **Navigate through customers** - Everything should work offline
4. **Disable Airplane Mode** and sync again to get latest data

## Database Schema

### Customer Table
```sql
CREATE TABLE customers (
    id TEXT PRIMARY KEY,           -- Local UUID
    cradleUid TEXT UNIQUE,         -- Odoo sync ID (prevents duplicates)
    name TEXT NOT NULL,
    city TEXT,
    taxId TEXT,
    email TEXT,
    phone TEXT,
    website TEXT,
    date INTEGER,                  -- Unix timestamp
    syncState TEXT,                -- SYNCED, PENDING, SYNCING, ERROR
    lastModified INTEGER,          -- Unix timestamp
    odooId INTEGER                 -- Odoo record ID
);
```

## API Integration

### Expected Odoo Endpoints

#### GET /customer
Fetch all customers
```bash
curl -X GET https://moko.odoo.com/customer \
  -H "Content-Type: application/json" \
  -H "Authorization: YOUR_API_KEY"
```

**Expected Response:**
```json
[
  {
    "id": 42,
    "name": "Advance Insight",
    "city": "Nairobi",
    "tax_id": "P800000447",
    "email": "info@advanceinsight.dev",
    "phone": "+254700000000",
    "website": "www.advanceinsight.dev",
    "date": "2025-04-11"
  }
]
```

#### POST /customer (Future)
Create new customers
```bash
curl -X POST https://moko.odoo.com/customer \
  -H "Content-Type: application/json" \
  -H "Authorization: YOUR_API_KEY" \
  -d '[{"name": "...", ...}]'
```

## Future Enhancements (Roadmap)

### Phase 2: Sales Module
- View sales orders
- Create new sales orders
- Link sales to customers
- Sync pending sales to Odoo

### Phase 3: Payments Module
- Record customer payments
- View payment history
- Sync payments to Odoo

### Phase 4: Deliveries Module
- Track deliveries
- Update delivery status
- Sync delivery confirmations

### Phase 5: Two-Way Sync
- Update existing customer data
- Conflict resolution
- Background sync with WorkManager

## Development Tips

### Key Files to Know

1. **CustomerRepository.kt** - Main business logic for data operations
2. **CustomerViewModel.kt** - UI state management
3. **CustomerListScreen.kt** - Main UI screen
4. **AppModule.kt** - Dependency injection configuration
5. **OdooApiService.kt** - API endpoint definitions

### Adding a New Feature

1. **Define domain model** in `domain/model/`
2. **Create Room entity** in `data/local/entity/`
3. **Add DAO methods** in `data/local/dao/`
4. **Define API DTOs** in `data/remote/dto/`
5. **Add API endpoints** in `data/remote/api/`
6. **Create repository** in `data/repository/`
7. **Build ViewModel** in `presentation/`
8. **Create UI screens** in `presentation/`

### Useful Android Studio Shortcuts

- **Run app:** Shift + F10
- **Debug app:** Shift + F9
- **Find in files:** Ctrl + Shift + F (Cmd + Shift + F on Mac)
- **Refactor/Rename:** Shift + F6
- **Format code:** Ctrl + Alt + L (Cmd + Option + L on Mac)

## Building APK for Distribution

### Debug APK (for testing)
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. APK location: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer to device and install

### Release APK (for production)
1. Generate signing key (one-time):
   ```bash
   keytool -genkey -v -keystore odoo-field-app.keystore \
     -alias odoo-field-app -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Configure signing in `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("path/to/odoo-field-app.keystore")
           storePassword = "your_password"
           keyAlias = "odoo-field-app"
           keyPassword = "your_password"
       }
   }
   ```

3. **Build → Generate Signed Bundle / APK → APK**
4. Select your keystore and build

## Support and Questions

For questions or issues:
1. Check the **Troubleshooting** section above
2. Review Android Studio Logcat for error messages
3. Check that Odoo endpoints are working with `curl` or Postman
4. Verify API key permissions in Odoo

## License

Proprietary - for internal use only
