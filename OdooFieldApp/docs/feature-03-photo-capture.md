# Feature: Photo Capture

**Purpose:** Capture shop photos and selfies as proof of visit/outlet registration.

---

## Data Model Changes

### Customer Model Additions
```kotlin
// Add to domain/model/Customer.kt
val image1: String?,  // Local file URI or base64 for sync
val image2: String?,
val image3: String?,
val image4: String?
```

### Visit Model Additions
```kotlin
// Add to domain/model/Visit.kt
val shopImg1: String?,
val shopImg2: String?,
val shopImg3: String?,
val shopImg4: String?,
val shopSelfieImg: String?
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `util/CameraHelper.kt` | Camera intent handling |
| `util/ImageCompressor.kt` | Compress images before upload |
| `presentation/components/ImageCaptureButton.kt` | Reusable capture button |
| `presentation/components/ImagePreviewGrid.kt` | Display captured images grid |

---

## Files to Modify

| File | Change |
|------|--------|
| `domain/model/Customer.kt` | Add image1-4 fields |
| `domain/model/Visit.kt` | Add shopImg1-4, shopSelfieImg fields |
| `data/local/entity/CustomerEntity.kt` | Add image columns + migration |
| `data/local/entity/VisitEntity.kt` | Add image columns |
| `data/local/OdooDatabase.kt` | Add migration |
| `data/remote/dto/CustomerDto.kt` | Add base64 image fields |
| `data/remote/dto/VisitDto.kt` | Add base64 image fields |
| `AndroidManifest.xml` | Add camera permission + FileProvider |
| `presentation/customer/CustomerCreateScreen.kt` | Add image capture UI |
| `presentation/customer/CustomerDetailScreen.kt` | Display images |
| `presentation/visit/VisitDetailScreen.kt` | Add photo capture during visit |

---

## Permissions

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />

<application>
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```

### res/xml/file_paths.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="images" path="images/" />
</paths>
```

---

## Dependencies

### build.gradle (app)
```kotlin
// Image loading
implementation("io.coil-kt:coil-compose:2.5.0")
```

---

## CameraHelper Implementation

```kotlin
// util/CameraHelper.kt
class CameraHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.cacheDir.resolve("images").also { it.mkdirs() }
        return File.createTempFile("IMG_${timestamp}_", ".jpg", storageDir)
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
```

---

## ImageCompressor Implementation

```kotlin
// util/ImageCompressor.kt
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun compressImage(uri: Uri, maxWidth: Int = 800, maxHeight: Int = 600): ByteArray {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Calculate scale
            val scale = minOf(
                maxWidth.toFloat() / original.width,
                maxHeight.toFloat() / original.height,
                1f
            )

            val scaledWidth = (original.width * scale).toInt()
            val scaledHeight = (original.height * scale).toInt()

            val scaled = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            if (scaled != original) scaled.recycle()
            original.recycle()

            outputStream.toByteArray()
        }
    }

    suspend fun toBase64(uri: Uri): String {
        val bytes = compressImage(uri)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
```

---

## UI Components

### ImageCaptureButton
```kotlin
@Composable
fun ImageCaptureButton(
    imageUri: String?,
    onCaptureClick: () -> Unit,
    onRemoveClick: () -> Unit,
    label: String = "Add Photo",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCaptureClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

### ImagePreviewGrid
```kotlin
@Composable
fun ImagePreviewGrid(
    images: List<String?>,
    maxImages: Int = 4,
    onCaptureClick: (index: Int) -> Unit,
    onRemoveClick: (index: Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(maxImages) { index ->
            ImageCaptureButton(
                imageUri = images.getOrNull(index),
                onCaptureClick = { onCaptureClick(index) },
                onRemoveClick = { onRemoveClick(index) },
                label = "Photo ${index + 1}"
            )
        }
    }
}
```

---

## Camera Launch Integration

```kotlin
@Composable
fun CustomerCreateScreen(viewModel: CustomerViewModel) {
    var currentImageIndex by remember { mutableStateOf(0) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            viewModel.setImage(currentImageIndex, tempImageUri.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    fun launchCamera(index: Int) {
        currentImageIndex = index
        val file = cameraHelper.createImageFile()
        tempImageUri = cameraHelper.getUriForFile(file)
        cameraLauncher.launch(tempImageUri)
    }

    // UI
    ImagePreviewGrid(
        images = uiState.images,
        onCaptureClick = { index ->
            if (hasCameraPermission) {
                launchCamera(index)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onRemoveClick = { index ->
            viewModel.removeImage(index)
        }
    )
}
```

---

## ViewModel Integration

```kotlin
// In CustomerViewModel
data class CustomerCreateState(
    val name: String = "",
    // ... other fields
    val image1: String? = null,
    val image2: String? = null,
    val image3: String? = null,
    val image4: String? = null
) {
    val images: List<String?> get() = listOf(image1, image2, image3, image4)
}

fun setImage(index: Int, uri: String) {
    _createState.update { state ->
        when (index) {
            0 -> state.copy(image1 = uri)
            1 -> state.copy(image2 = uri)
            2 -> state.copy(image3 = uri)
            3 -> state.copy(image4 = uri)
            else -> state
        }
    }
}

fun removeImage(index: Int) {
    setImage(index, null)
}
```

---

## Sync Handling

When syncing to Odoo, convert local file URIs to base64:

```kotlin
// In repository, before API call
suspend fun prepareForSync(customer: Customer): CustomerCreateRequest {
    return CustomerCreateRequest(
        name = customer.name,
        // ... other fields
        citGcImage1 = customer.image1?.let { imageCompressor.toBase64(Uri.parse(it)) },
        citGcImage2 = customer.image2?.let { imageCompressor.toBase64(Uri.parse(it)) },
        citGcImage3 = customer.image3?.let { imageCompressor.toBase64(Uri.parse(it)) },
        citGcImage4 = customer.image4?.let { imageCompressor.toBase64(Uri.parse(it)) }
    )
}
```

---

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE customers ADD COLUMN image1 TEXT")
        database.execSQL("ALTER TABLE customers ADD COLUMN image2 TEXT")
        database.execSQL("ALTER TABLE customers ADD COLUMN image3 TEXT")
        database.execSQL("ALTER TABLE customers ADD COLUMN image4 TEXT")
    }
}
```

---

## Verification Steps

1. Open customer create screen
2. Tap on photo placeholder - verify camera permission requested
3. Grant permission, take photo
4. Verify photo appears in the grid
5. Add up to 4 photos
6. Remove a photo and verify it's cleared
7. Save customer and verify images persist locally
8. Sync to Odoo and verify base64 images appear in partner record
9. Repeat for Visit photos and selfie

---

## Notes

- Store local file URIs in database (not base64) to save space
- Only convert to base64 when syncing to Odoo
- Compress images to ~800x600 before encoding (reduces payload size)
- Use Coil for efficient image loading with caching
- Clean up temp files periodically
- For selfie, consider using front camera: `cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA`
