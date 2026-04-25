package com.wanderlog.android.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.wanderlog.android.core.ui.component.WanderTopBar

@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        val firstItem = state.items.firstOrNull()
        if (firstItem?.place?.latitude != null) {
            position = CameraPosition.fromLatLngZoom(
                LatLng(firstItem.place.latitude, firstItem.place.longitude!!), 12f
            )
        }
    }

    LaunchedEffect(state.items) {
        val firstItem = state.items.firstOrNull() ?: return@LaunchedEffect
        val lat = firstItem.place?.latitude ?: return@LaunchedEffect
        val lng = firstItem.place?.longitude ?: return@LaunchedEffect
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 12f)
    }

    val polylinePoints = remember(state.items) {
        state.items.mapNotNull { item ->
            val lat = item.place?.latitude ?: return@mapNotNull null
            val lng = item.place.longitude ?: return@mapNotNull null
            LatLng(lat, lng)
        }
    }

    Scaffold(topBar = { WanderTopBar(title = "Map", onBack = onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.mapError != null -> {
                    val mapError = state.mapError.orEmpty()
                    Text(
                        text = mapError,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                state.items.isEmpty() -> {
                    Text(
                        "No items with resolved map coordinates on this trip yet. Imported flights may need a better place match or a valid Maps key.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { viewModel.selectItem(null) }
                    ) {
                        state.items.forEach { item ->
                            val lat = item.place?.latitude ?: return@forEach
                            val lng = item.place.longitude ?: return@forEach
                            Marker(
                                state = MarkerState(position = LatLng(lat, lng)),
                                title = item.title,
                                snippet = item.place.address,
                                onClick = { viewModel.selectItem(item.id); false }
                            )
                        }
                        if (polylinePoints.size >= 2) {
                            Polyline(points = polylinePoints)
                        }
                    }
                }
            }
        }
    }
}
