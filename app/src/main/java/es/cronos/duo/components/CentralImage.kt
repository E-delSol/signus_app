package es.cronos.duo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import es.cronos.duo.R

@Composable
fun CentralImage(
    primaryColor: Color,
    centralImage: @Composable () -> Unit = {
        // Contenido por defecto (LogoImage interno)
        Box(modifier = Modifier.fillMaxSize()) {
            // Usamos el recurso mipmap que sabemos que existe para corregir el error de compilación
            Image(
                painter = painterResource(id = R.drawable.logo_signus),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 12.dp, shape = CircleShape)
            )
        }
    }
) {
    Box(
        modifier = Modifier
            .size(256.dp),
        contentAlignment = Alignment.Center
    ) {
        centralImage()
    }
}