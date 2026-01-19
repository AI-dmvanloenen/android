# Quick Start Guide for Android Beginners

This guide will help you get the Odoo Field App running on your computer, even if you've never done Android development before.

## Part 1: Install Android Studio (One-time setup)

### For Windows:
1. Go to https://developer.android.com/studio
2. Click **Download Android Studio**
3. Run the downloaded `.exe` file
4. Follow the setup wizard:
   - Click **Next** through all screens
   - Keep all default options checked
   - Click **Install**
   - This will take 10-15 minutes
5. When complete, click **Finish** and launch Android Studio

### For Mac:
1. Go to https://developer.android.com/studio
2. Click **Download Android Studio**
3. Open the downloaded `.dmg` file
4. Drag Android Studio to Applications folder
5. Open Android Studio from Applications
6. If you see "Android Studio can't be opened", right-click and select **Open**

### For Linux:
1. Go to https://developer.android.com/studio
2. Download the `.tar.gz` file
3. Extract it: `tar -xzf android-studio-*.tar.gz`
4. Run: `cd android-studio/bin && ./studio.sh`

## Part 2: First Time Android Studio Setup

When you first open Android Studio:

1. **Welcome Screen** - Click **Next**
2. **Install Type** - Select **Standard**, click **Next**
3. **Theme** - Choose Light or Dark theme, click **Next**
4. **Verify Settings** - Click **Finish**
5. **Downloading Components** - Wait 10-20 minutes (downloads Android SDK, emulator, etc.)
6. Click **Finish** when done

## Part 3: Open the Project

1. On Android Studio welcome screen, click **Open**
2. Navigate to the `OdooFieldApp` folder (the one with this guide in it)
3. Click **OK**
4. Wait for "Gradle sync" to complete (bottom right of screen)
   - First time takes 5-10 minutes
   - You'll see "Gradle sync finished" when done

**If you see errors:**
- Click the **Try Again** button
- Or go to **File → Invalidate Caches / Restart**

## Part 4: Create a Virtual Device (Emulator)

You need a virtual Android phone to test the app:

1. Click **Tools → Device Manager** (or the phone icon in toolbar)
2. Click **Create Device**
3. Select **Phone → Pixel 5** (or any phone), click **Next**
4. Select **System Image**: Choose **S (API 31)** or higher
   - If it says **Download**, click it and wait
5. Click **Next**, then **Finish**

You now have a virtual Android phone!

## Part 5: Run the App

### Start the emulator:
1. In **Device Manager**, click the **Play** button (▶) next to your virtual device
2. Wait 1-2 minutes for it to boot up
3. You'll see an Android phone screen

### Run the app:
1. Click the **Run** button (green triangle ▶ in toolbar)
   - Or press **Shift + F10**
2. First time build takes 2-3 minutes
3. App will automatically install and open on the emulator

## Part 6: Use the App

### Setup API Key:
1. In the app, tap the **Settings** icon (⚙️) in top right
2. Enter your Odoo API key in the text field
3. Tap **Save API Key**
4. Tap the back arrow **←**

### Sync Customers:
1. Tap the **Refresh** icon (🔄) in top right
2. Wait a few seconds
3. You should see customers appear!

### Browse Customers:
1. Scroll through the customer list
2. Tap any customer to see details
3. Use the search bar to find specific customers

## Common First-Time Issues

### Issue: "Gradle sync failed"
**Fix:** 
1. Click **File → Invalidate Caches**
2. Check **Clear file system cache and Local History**
3. Click **Invalidate and Restart**

### Issue: Emulator won't start
**Fix:**
1. Go to **Tools → SDK Manager**
2. Click **SDK Tools** tab
3. Check **Android Emulator** and **Intel x86 Emulator Accelerator**
4. Click **Apply** and let it install

### Issue: "SDK location not found"
**Fix:**
1. Go to **File → Project Structure**
2. Under **SDK Location**, click the dropdown
3. Android Studio should auto-detect the SDK path
4. Click **OK**

### Issue: App crashes immediately
**Fix:**
1. At bottom of Android Studio, click **Logcat** tab
2. Look for red error messages
3. Usually means you need to sync Gradle again or clean project
4. Try **Build → Clean Project**, then **Build → Rebuild Project**

## Understanding the Code Structure

You don't need to understand everything, but here's what's where:

```
OdooFieldApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/odoo/fieldapp/    ← All Kotlin code here
│   │   │   ├── data/                  ← Database and API code
│   │   │   ├── domain/                ← Business logic
│   │   │   └── presentation/          ← UI screens
│   │   ├── res/                       ← Resources (strings, colors, icons)
│   │   └── AndroidManifest.xml        ← App configuration
│   └── build.gradle.kts               ← App dependencies
├── build.gradle.kts                   ← Project settings
└── README.md                          ← Detailed documentation
```

## Making Small Changes

### Change App Name:
1. Open `app/src/main/res/values/strings.xml`
2. Change `Odoo Field App` to your desired name
3. Run app to see change

### Change API URL:
1. Open `app/src/main/java/com/odoo/fieldapp/di/AppModule.kt`
2. Find `.baseUrl("https://test.moko.odoo.com/")`
3. Change to your URL
4. Run app

### Change Colors:
1. Open `app/src/main/res/values/colors.xml`
2. Modify color values
3. Run app

## Next Steps

Once you're comfortable:
1. Read the full **README.md** for detailed architecture info
2. Explore the code files mentioned in the structure above
3. Try making small changes and running the app
4. Check Logcat (bottom panel) when things go wrong

## Getting Help

When stuck:
1. **Check Logcat** - Shows error messages at bottom of Android Studio
2. **Read the error** - Often tells you exactly what's wrong
3. **Clean and Rebuild** - Fixes 50% of issues
4. **Invalidate Caches** - Fixes another 30% of issues
5. **Google the error** - Millions of Android developers have had your issue

## Keyboard Shortcuts (Save Time!)

- **Run app:** Shift + F10
- **Stop app:** Ctrl + F2 (Cmd + F2 on Mac)
- **Find anything:** Double press Shift
- **Find in files:** Ctrl + Shift + F (Cmd + Shift + F on Mac)
- **Auto-format code:** Ctrl + Alt + L (Cmd + Option + L on Mac)

## Understanding Gradle Sync

**What is it?** Gradle is the build system that:
- Downloads libraries (Retrofit, Room, etc.)
- Compiles your Kotlin code
- Packages everything into an APK

**When does it run?**
- When you first open the project
- When you change `build.gradle.kts` files
- When you click **Sync Now**

**How long does it take?**
- First time: 5-10 minutes
- Subsequent syncs: 30 seconds - 2 minutes

## You're Ready!

You now have:
- ✅ Android Studio installed
- ✅ Project opened and synced
- ✅ Virtual device created
- ✅ App running
- ✅ Basic understanding of the structure

**Congratulations!** You've successfully set up an Android development environment and run your first app. The learning curve is steep at first, but you're over the hardest part.

Remember: Every professional Android developer was once a beginner. Take it one step at a time, and don't hesitate to experiment - you can't break anything permanently!
