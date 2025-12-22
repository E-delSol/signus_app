package es.cronos.duo.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

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
    
    // Controlar si debemos mostrar el diálogo inicial (solo la primera vez o si se reinicia el flujo)
    var showPermissionRationale by remember { mutableStateOf(!hasCameraPermission) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            // IMPORTANTE: No volvemos a poner showPermissionRationale a true aquí automáticamente
            // para evitar bucles infinitos si el sistema deniega automáticamente.
            if (!granted) {
                showPermissionRationale = false 
            }
        }
    )

    if (hasCameraPermission) {
        CameraPreview(onCodeScanned)
    } else {
        // 1. Diálogo inicial explicativo (Pop-up)
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { 
                    // Si el usuario descarta el diálogo (click fuera), asumimos que no quiere dar permiso por ahora
                    showPermissionRationale = false 
                },
                title = { Text("Permiso de Cámara Requerido") },
                text = { Text("Para vincularte con tu pareja escaneando su código QR, necesitamos acceso a la cámara.") },
                confirmButton = {
                    Button(onClick = { 
                        // Lanzamos la petición del sistema y ocultamos nuestro diálogo
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
        
        // 2. Estado de "Permiso Denegado" o "Esperando" (Pantalla de fondo)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!showPermissionRationale) {
                // Si ya cerramos el diálogo y no tenemos permiso, mostramos opción manual
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requiere acceso a la cámara.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { 
                        // Intento manual de pedir permiso de nuevo (o mostrar diálogo)
                        showPermissionRationale = true 
                    }) {
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
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    processImageProxy(imageProxy, onCodeScanned)
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
                    e.printStackTrace()
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processImageProxy(imageProxy: ImageProxy, onCodeScanned: (String) -> Unit) {
    if (imageProxy.planes.isEmpty()) {
        imageProxy.close()
        return
    }
    
    val buffer = imageProxy.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    
    val height = imageProxy.height
    val width = imageProxy.width
    
    // Nota: Esta conversión asume formato YUV estándar.
    val source = PlanarYUVLuminanceSource(
        data, width, height, 0, 0, width, height, false
    )
    
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    
    try {
        val result = MultiFormatReader().decode(binaryBitmap)
        onCodeScanned(result.text)
    } catch (e: Exception) {
        // No QR found
    } finally {
        imageProxy.close()
    }
}