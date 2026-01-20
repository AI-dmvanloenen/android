# Feature: NFC/Barcode Scanning

**Purpose:** Fast delivery and product lookup via NFC card or barcode scanning.

---

## Overview

This feature provides two scanning methods:
1. **NFC Card Scanning** - Read NFC cards to lookup deliveries by card UID
2. **Barcode/QR Scanning** - Use camera to scan product barcodes or delivery references

---

## Files to Create

| File | Purpose |
|------|---------|
| `util/NfcHelper.kt` | NFC tag reading utilities |
| `util/BarcodeScanner.kt` | ML Kit barcode scanning wrapper |
| `presentation/scan/ScanScreen.kt` | Camera viewfinder + NFC UI |
| `presentation/scan/ScanViewModel.kt` | Handle scan results and navigation |

---

## Files to Modify

| File | Change |
|------|--------|
| `AndroidManifest.xml` | Add NFC permission + intent filter |
| `data/local/dao/DeliveryDao.kt` | Add findByReference() query |
| `data/local/dao/ProductDao.kt` | findByBarcode() already exists |
| `presentation/navigation/Navigation.kt` | Add Scan route |
| `presentation/dashboard/DashboardScreen.kt` | Add scan FAB |
| `MainActivity.kt` | Handle NFC intent dispatch |

---

## Permissions & Dependencies

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature android:name="android.hardware.nfc" android:required="false" />
<uses-feature android:name="android.hardware.camera" android:required="false" />

<application>
    <activity
        android:name=".MainActivity"
        ...>
        <!-- NFC Intent Filter -->
        <intent-filter>
            <action android:name="android.nfc.action.NDEF_DISCOVERED" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="text/plain" />
        </intent-filter>
        <intent-filter>
            <action android:name="android.nfc.action.TAG_DISCOVERED" />
            <category android:name="android.intent.category.DEFAULT" />
        </intent-filter>
    </activity>
</application>
```

### build.gradle (app)
```kotlin
// ML Kit Barcode Scanning
implementation("com.google.mlkit:barcode-scanning:17.2.0")

// CameraX for barcode scanning
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

---

## NfcHelper Implementation

```kotlin
// util/NfcHelper.kt
class NfcHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isNfcAvailable(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun extractTagId(intent: Intent): String? {
        if (intent.action !in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )) {
            return null
        }

        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        return tag?.id?.toHexString()
    }

    fun extractNdefMessage(intent: Intent): String? {
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        val messages = rawMessages?.mapNotNull { it as? NdefMessage }
        return messages?.firstOrNull()?.records?.firstOrNull()?.let { record ->
            String(record.payload, Charsets.UTF_8).drop(3) // Skip language code prefix
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    fun enableForegroundDispatch(activity: Activity) {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
}
```

---

## BarcodeScanner Implementation

```kotlin
// util/BarcodeScanner.kt
class BarcodeScanner @Inject constructor() {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(
        imageProxy: ImageProxy,
        onBarcodeDetected: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    onComplete()
                }
        } else {
            imageProxy.close()
            onComplete()
        }
    }

    fun close() {
        scanner.close()
    }
}
```

---

## ScanViewModel

```kotlin
// presentation/scan/ScanViewModel.kt
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val deliveryDao: DeliveryDao,
    private val productDao: ProductDao,
    private val nfcHelper: NfcHelper
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun isNfcAvailable() = nfcHelper.isNfcAvailable()
    fun isNfcEnabled() = nfcHelper.isNfcEnabled()

    fun processNfcIntent(intent: Intent) {
        viewModelScope.launch {
            _isProcessing.value = true

            // Try to extract tag ID or NDEF message
            val tagId = nfcHelper.extractTagId(intent)
            val ndefMessage = nfcHelper.extractNdefMessage(intent)

            val lookupValue = ndefMessage ?: tagId

            if (lookupValue != null) {
                lookupByValue(lookupValue)
            } else {
                _scanResult.value = ScanResult.NotFound("Could not read NFC tag")
            }

            _isProcessing.value = false
        }
    }

    fun processBarcode(barcode: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            lookupByValue(barcode)
            _isProcessing.value = false
        }
    }

    private suspend fun lookupByValue(value: String) {
        // Try to find a delivery by reference
        val delivery = deliveryDao.findByReference(value)
        if (delivery != null) {
            _scanResult.value = ScanResult.DeliveryFound(delivery.id, delivery.name)
            return
        }

        // Try to find a product by barcode
        val product = productDao.findByBarcode(value)
        if (product != null) {
            _scanResult.value = ScanResult.ProductFound(product.id, product.name)
            return
        }

        // Try to find a product by SKU
        val productBySku = productDao.findBySku(value)
        if (productBySku != null) {
            _scanResult.value = ScanResult.ProductFound(productBySku.id, productBySku.name)
            return
        }

        _scanResult.value = ScanResult.NotFound("No match found for: $value")
    }

    fun clearResult() {
        _scanResult.value = null
    }
}

sealed class ScanResult {
    data class DeliveryFound(val deliveryId: Int, val deliveryName: String) : ScanResult()
    data class ProductFound(val productId: Int, val productName: String) : ScanResult()
    data class NotFound(val message: String) : ScanResult()
}
```

---

## ScanScreen

```kotlin
// presentation/scan/ScanScreen.kt
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onDeliveryFound: (Int) -> Unit,
    onProductFound: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scanResult by viewModel.scanResult.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Handle scan results
    LaunchedEffect(scanResult) {
        when (val result = scanResult) {
            is ScanResult.DeliveryFound -> {
                onDeliveryFound(result.deliveryId)
                viewModel.clearResult()
            }
            is ScanResult.ProductFound -> {
                onProductFound(result.productId)
                viewModel.clearResult()
            }
            is ScanResult.NotFound -> {
                // Show toast or snackbar
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // NFC status
            if (viewModel.isNfcAvailable()) {
                NfcStatusCard(
                    isEnabled = viewModel.isNfcEnabled(),
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Camera viewfinder
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraPreview(
                        onBarcodeScanned = { barcode ->
                            if (!isProcessing) {
                                viewModel.processBarcode(barcode)
                            }
                        }
                    )

                    // Scanning overlay
                    ScanningOverlay()

                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                // Request permission
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission required for barcode scanning")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Grant Permission")
                    }
                }
            }

            // Instructions
            Text(
                text = "Point camera at barcode or tap NFC card",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NfcStatusCard(isEnabled: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEnabled) "NFC Ready - Tap card to scan" else "NFC Disabled",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeScanner = remember { BarcodeScanner() }

    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                barcodeScanner.processImageProxy(
                                    imageProxy,
                                    onBarcodeDetected = { barcode ->
                                        val now = System.currentTimeMillis()
                                        // Debounce: only process if different barcode or 2 seconds passed
                                        if (barcode != lastScannedBarcode || now - lastScanTime > 2000) {
                                            lastScannedBarcode = barcode
                                            lastScanTime = now
                                            onBarcodeScanned(barcode)
                                        }
                                    },
                                    onComplete = {}
                                )
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        // Handle error
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScanningOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaSize = 250.dp.toPx()
        val left = (size.width - scanAreaSize) / 2
        val top = (size.height - scanAreaSize) / 2

        // Semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset.Zero,
            size = Size(size.width, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top + scanAreaSize),
            size = Size(size.width, size.height - top - scanAreaSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top),
            size = Size(left, scanAreaSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(left + scanAreaSize, top),
            size = Size(size.width - left - scanAreaSize, scanAreaSize)
        )

        // Scan area border
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanAreaSize, scanAreaSize),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
```

---

## MainActivity NFC Integration

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var nfcHelper: NfcHelper

    private var nfcIntentCallback: ((Intent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OdooFieldAppTheme {
                val navController = rememberNavController()

                // Provide NFC callback to scan screen
                CompositionLocalProvider(
                    LocalNfcCallback provides { callback ->
                        nfcIntentCallback = callback
                    }
                ) {
                    Navigation(navController = navController)
                }
            }
        }

        // Handle NFC intent that launched the app
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (intent.action in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )) {
            nfcIntentCallback?.invoke(intent)
        }
    }
}

val LocalNfcCallback = staticCompositionLocalOf<((Intent) -> Unit) -> Unit> { {} }
```

---

## DAO Updates

```kotlin
// data/local/dao/DeliveryDao.kt
@Dao
interface DeliveryDao {
    // ... existing methods

    @Query("SELECT * FROM deliveries WHERE name = :reference LIMIT 1")
    suspend fun findByReference(reference: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE name LIKE '%' || :reference || '%' LIMIT 1")
    suspend fun findByPartialReference(reference: String): DeliveryEntity?
}

// data/local/dao/ProductDao.kt - already has findByBarcode
@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE defaultCode = :sku LIMIT 1")
    suspend fun findBySku(sku: String): ProductEntity?
}
```

---

## Navigation Updates

```kotlin
// presentation/navigation/Navigation.kt
sealed class Screen(val route: String) {
    // ... existing screens
    object Scan : Screen("scan")
}

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        // ... existing routes

        composable(Screen.Scan.route) {
            ScanScreen(
                onDeliveryFound = { deliveryId ->
                    navController.navigate(Screen.DeliveryDetail.createRoute(deliveryId))
                },
                onProductFound = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## Dashboard FAB

```kotlin
// Add to DashboardScreen
Scaffold(
    floatingActionButton = {
        FloatingActionButton(
            onClick = { navController.navigate(Screen.Scan.route) }
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
        }
    }
) {
    // ... dashboard content
}
```

---

## Verification Steps

1. Open app and navigate to Scan screen
2. Verify camera permission is requested
3. Grant permission and verify camera preview shows
4. Scan a product barcode - verify navigates to product detail
5. Scan a delivery reference (if printed as barcode) - verify navigates to delivery
6. Test NFC: tap an NFC card - verify tag is read
7. Verify NFC lookup finds matching delivery or shows "not found"
8. Test with NFC disabled - verify appropriate message shown
9. Test scanning unknown barcode - verify "not found" message
10. Test debouncing - scan same barcode twice quickly, should only trigger once

---

## Notes

- NFC foreground dispatch ensures app receives NFC events when in foreground
- Barcode scanning uses ML Kit for on-device processing (no network needed)
- Debounce barcode scans to prevent duplicate lookups
- Support common barcode formats: QR, Code128, Code39, EAN-13, UPC
- Consider adding manual entry fallback for damaged barcodes
- NFC cards in TwigaPrime store delivery reference in NDEF message
