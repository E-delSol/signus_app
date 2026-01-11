package es.cronos.duo.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerPickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    var selectedHours by remember { mutableIntStateOf(0) }
    var selectedMinutes by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Temporizar estado",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Selecciona la duración del nuevo estado",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours
                    TimeWheel(
                        range = 0..23,
                        unit = "h",
                        initialValue = 0,
                        onValueChange = { selectedHours = it }
                    )
                    
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes
                    TimeWheel(
                        range = 0..59,
                        unit = "m",
                        initialValue = 0,
                        onValueChange = { selectedMinutes = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedHours, selectedMinutes) }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeWheel(
    range: IntRange,
    unit: String,
    initialValue: Int = range.first,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val visibleItems = 3
    val itemCount = range.count()
    
    // Start in the middle of Int.MAX_VALUE to allow scrolling both ways
    val initialOffset = range.indexOf(initialValue).coerceAtLeast(0)
    val midPoint = Int.MAX_VALUE / 2
    val startIndex = midPoint - (midPoint % itemCount) + initialOffset
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Use derivedStateOf to minimize recompositions
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    // Notify change when scroll settles or changes significantly
    LaunchedEffect(firstVisibleIndex) {
        val currentIndex = firstVisibleIndex % itemCount
        val value = range.elementAt(currentIndex)
        onValueChange(value)
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(itemHeight * visibleItems)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight), // Center the item
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Int.MAX_VALUE) { index ->
                val rangeIndex = index % itemCount
                val value = range.elementAt(rangeIndex)
                
                // Determine if this specific item instance is the selected one
                val isSelected = index == firstVisibleIndex
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'),
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isSelected) 1f else 0.5f
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}