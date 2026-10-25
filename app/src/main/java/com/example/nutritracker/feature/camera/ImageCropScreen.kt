package com.example.nutritracker.feature.camera

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutritracker.util.ImageUtils
import com.example.nutritracker.util.tr
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onCropDone: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) { onBack() }

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = with(density) { configuration.screenWidthDp.dp.toPx().toInt() }.coerceAtLeast(720)
    val screenH = with(density) { configuration.screenHeightDp.dp.toPx().toInt() }.coerceAtLeast(1080)

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bmp = ImageUtils.loadBitmapSafe(context, imageUri, screenW * 2, screenH * 2)
            if (bmp != null) {
                imageBitmap = bmp.asImageBitmap()
            } else {
                imageLoadError = true
            }
        }
    }

    var crop by remember { mutableStateOf(false) }
    var isCropping by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("裁剪图片", "Crop Photo"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isCropping) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("返回", "Back"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isCropping,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text(tr("取消", "Cancel")) }
                    Button(
                        onClick = { if (!isCropping && imageBitmap != null) crop = true },
                        enabled = !isCropping && imageBitmap != null,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Crop, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("确认裁剪", "Apply Crop"))
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (imageLoadError) {
                Text(
                    tr("图片加载失败", "Failed to load image"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (imageBitmap == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                ImageCropper(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    imageBitmap = imageBitmap!!,
                    contentDescription = "Image Cropper",
                    cropStyle = CropDefaults.style(),
                    cropProperties = CropDefaults.properties(
                        cropOutlineProperty = CropOutlineProperty(
                            OutlineType.Rect,
                            RectCropShape(0, "Rect")
                        ),
                        handleSize = with(density) { 20.dp.toPx() }
                    ),
                    crop = crop,
                    onCropStart = { isCropping = true },
                    onCropSuccess = { resultBitmap ->
                        scope.launch(Dispatchers.IO) {
                            val bmp = resultBitmap.asAndroidBitmap()
                            val file = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                            }
                            val newUri = Uri.fromFile(file)
                            withContext(Dispatchers.Main) {
                                isCropping = false
                                crop = false
                                onCropDone(newUri)
                            }
                        }
                    }
                )

                if (isCropping) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
