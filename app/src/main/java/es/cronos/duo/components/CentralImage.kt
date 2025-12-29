package es.cronos.duo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            // Usamos el PNG directo de drawable para evitar problemas con iconos adaptativos XML en Compose
            Image(
                painter = painterResource(id = R.drawable.logo_signus),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().shadow(elevation = 12.dp, shape = CircleShape)

            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
) {
    Box(
        modifier = Modifier
            .size(256.dp),
//            .drawBehind {
//                drawCircle(
//                    color = primaryColor.copy(alpha = 0.1f),
//                    radius = size.minDimension / 2 + 16.dp.toPx(),
//                    center = Offset(size.width / 2, size.height / 2),
//                )
//            }
//            .clip(CircleShape)
//            .background(MaterialTheme.colorScheme.surface)
//            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        centralImage()
    }
}