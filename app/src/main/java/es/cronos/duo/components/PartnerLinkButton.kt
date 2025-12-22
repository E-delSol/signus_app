package es.cronos.duo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cronos.duo.R

/**
 * Un botón de acción primario (Material 3) con una flecha de navegación.
 *
 * @param text El texto principal que se muestra en el botón (e.g., "Vincular con mi pareja").
 * @param onClick La lambda que se ejecuta cuando se hace clic en el botón.
 * @param modifier Modificador para aplicar al botón.
 * @param containerColor El color de fondo del botón.
 * @param contentColor El color del texto e ícono.
 */
@Composable
fun PartnerLinkButton(
    title: String = stringResource(R.string.qr_code_title),
    description: String = stringResource(R.string.qr_code_description),
    icon: ImageVector = Icons.Filled.QrCode2,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Usamos el color primario de M3 por defecto, o puedes usar un color específico
    containerColor: Color = MaterialTheme.colorScheme.surface, // Azul primario para que coincida con la imagen original
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f), // Gris oscuro semi-transparente
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.tertiary,


) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
//            .height(56.dp), // Altura fija de 56dp para un botón principal prominente
        shape = RoundedCornerShape(28.dp), // Radio de 28dp para una forma de píldora
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, // Color de fondo del botón
            contentColor = contentColor // Color del texto e ícono
        ),
        // En M3, los botones primarios (FilledButton) no suelen tener elevación por defecto
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp
        )
    ) {

        Row(
            modifier = Modifier
//                .padding(6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Icono y Textos
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Contenedor del Icono (círculo oscuro)
                Box(
                    modifier = Modifier
                        .size(56.dp) // Tamaño similar al HTML h-14
                        .clip(CircleShape)
                        .background(iconBackgroundColor)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null, // Descarga visual, texto ya en la tarjeta
                        tint = titleColor.copy(alpha = 0.8f), // Tono ligeramente más claro para el icono
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Columna de Textos
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = titleColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = descriptionColor
                    )
                }
            }
        }


    }
}