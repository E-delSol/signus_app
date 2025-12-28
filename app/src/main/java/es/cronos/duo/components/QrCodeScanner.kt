package es.cronos.duo.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

@Composable
fun QrCodeScanner(
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var showPermissionRationale by remember { mutableStateOf(!hasCameraPermission) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                showPermissionRationale = false 
            }
        }
    )

    if (hasCameraPermission) {
        CameraPreview(onCodeScanned)
    } else {
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Permiso de Cámara Requerido") },
                text = { Text("Para escanear el código QR necesitamos acceso a la cámara.") },
                confirmButton = {
                    Button(onClick = { 
                        showPermissionRationale = false
                        launcher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Continuar")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPermissionRationale = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!showPermissionRationale) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requiere acceso a la cámara.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showPermissionRationale = true }) {
                        Text("Solicitar permiso")
                    }
                }
            } else {
                Text("Esperando permiso...")
            }
        }
    }
}

@Composable
fun CameraPreview(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Usamos LifecycleCameraController que maneja automáticamente el ciclo de vida,
    // y lo más importante: ENFOQUE AUTOMÁTICO y ZOOM.
    val cameraController = remember { 
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Solo habilitamos el análisis de imagen. 
            // La vista previa (Preview) se gestiona automáticamente al enlazar con PreviewView.
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    // Configuración optimizada para QR
    val reader = remember { 
        MultiFormatReader().apply {
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
            setHints(hints)
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = {
            // Establecer el analizador cada vez que se recompone o actualiza
            cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context)
            ) { imageProxy ->
                processImageProxy(imageProxy, reader, onCodeScanned)
            }
        }
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy, 
    reader: MultiFormatReader, 
    onCodeScanned: (String) -> Unit
) {
    try {
        if (imageProxy.planes.isEmpty()) {
            return
        }
        
        // 1. Obtener datos YUV limpios
        val buffer = imageProxy.planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = imageProxy.planes[0].rowStride
        val pixelStride = imageProxy.planes[0].pixelStride
        
        val data = ByteArray(width * height)
        
        if (pixelStride == 1 && rowStride == width) {
            buffer.get(data)
        } else {
            for (y in 0 until height) {
                val pos = y * rowStride
                buffer.position(pos)
                buffer.get(data, y * width, width)
            }
        }
        
        // 2. Manejar Rotación
        // CameraX nos dice cuántos grados hay que rotar la imagen para que esté derecha.
        // Para ZXing, necesitamos rotar los bytes.
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        
        val (rotatedData, finalWidth, finalHeight) = when (rotationDegrees) {
            90 -> rotate90(data, width, height)
            180 -> Triple(data.reversedArray(), width, height)
            270 -> rotate270(data, width, height)
            else -> Triple(data, width, height)
        }

        // 3. Decodificar
        val source = PlanarYUVLuminanceSource(
            rotatedData, finalWidth, finalHeight, 0, 0, finalWidth, finalHeight, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        
        val result = reader.decode(binaryBitmap)
        onCodeScanned(result.text)
        
    } catch (e: Exception) {
        // No QR found
    } finally {
        imageProxy.close()
    }
}

// Función auxiliar para rotar 90 grados en el sentido de las agujas del reloj
private fun rotate90(data: ByteArray, width: Int, height: Int): Triple<ByteArray, Int, Int> {
    val rotatedData = ByteArray(data.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            // (x, y) -> (y, height - 1 - x)
            rotatedData[x * height + (height - 1 - y)] = data[y * width + x]
        }
    }
    return Triple(rotatedData, height, width)
}

private fun rotate270(data: ByteArray, width: Int, height: Int): Triple<ByteArray, Int, Int> {
    val rotatedData = ByteArray(data.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            // (x, y) -> (width - 1 - x, y)
            rotatedData[(width - 1 - x) * height + y] = data[y * width + x]
        }
    }
    return Triple(rotatedData, height, width)
}