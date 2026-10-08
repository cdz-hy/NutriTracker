package com.example.nutritracker.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.nutritracker.util.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    onImageSelected: (List<Uri>, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }

    val uriListSaver = remember {
        listSaver<List<Uri>, String>(
            save = { list -> list.map { it.toString() } },
            restore = { strings -> strings.map { Uri.parse(it) } }
        )
    }
    var selectedUris by rememberSaveable(stateSaver = uriListSaver) { mutableStateOf(emptyList()) }
    val maxImages = 4
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSpace = maxImages - selectedUris.size
            if (remainingSpace <= 0) {
                errorMessage = "最多只能选择 $maxImages 张图片"
            } else {
                val added = uris.take(remainingSpace)
                selectedUris = selectedUris + added
                if (uris.size > remainingSpace) {
                    errorMessage = "已达到上限 $maxImages 张，仅保留前 $remainingSpace 张"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "拍照识别食物",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        ImageUtils.cleanupTempFiles(context, selectedUris)
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "需要相机权限",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "请授予相机权限以拍照识别，也可直接点击下方相册按钮选图",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Text("授予权限")
                            }
                            OutlinedButton(onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                runCatching { context.startActivity(intent) }
                            }) {
                                Text("系统设置")
                            }
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("关闭", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                ) {
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.TextSnippet,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (notes.isEmpty()) {
                            Text(
                                text = "描述做法、分量、食材等...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (notes.isNotEmpty()) {
                        IconButton(
                            onClick = { notes = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "清除",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (selectedUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(selectedUris.size) { index ->
                            val uri = selectedUris[index]
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "已选图片 ${index + 1}，点击裁剪",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { croppingUri = uri }
                                )
                                IconButton(
                                    onClick = {
                                        ImageUtils.cleanupTempFiles(context, listOf(uri))
                                        selectedUris = selectedUris.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedUris.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { onImageSelected(selectedUris, notes.trim()) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "完成")
                                Text("完成 (${selectedUris.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (selectedUris.size >= maxImages) {
                                errorMessage = "最多只能选择 $maxImages 张图片"
                            } else {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = "从相册选择",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    LargeFloatingActionButton(
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                return@LargeFloatingActionButton
                            }
                            if (selectedUris.size >= maxImages) {
                                errorMessage = "最多只能选择 $maxImages 张图片"
                                return@LargeFloatingActionButton
                            }
                            val photoFile = File(context.cacheDir, "food_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        selectedUris = selectedUris + Uri.fromFile(photoFile)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        errorMessage = "拍照失败: ${exception.message}"
                                    }
                                }
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "拍照",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    var lastCroppingUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(croppingUri) {
        if (croppingUri != null) lastCroppingUri = croppingUri
    }

    AnimatedVisibility(
        visible = croppingUri != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        lastCroppingUri?.let { uri ->
            ImageCropScreen(
                imageUri = uri,
                onCropDone = { croppedUri ->
                    val idx = selectedUris.indexOf(uri)
                    if (idx >= 0) {
                        ImageUtils.cleanupTempFiles(context, listOf(uri))
                        selectedUris = selectedUris.toMutableList().apply { set(idx, croppedUri) }
                    }
                    croppingUri = null
                },
                onBack = { croppingUri = null }
            )
        }
    }
}
