package com.wanderlog.android.presentation.tripPhotos

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.wanderlog.android.core.util.generateDateRange
import com.wanderlog.android.core.util.toShortDisplay
import com.wanderlog.android.core.ui.component.ConfirmDialog
import com.wanderlog.android.core.ui.component.WanderTopBar
import com.wanderlog.android.domain.model.TripPhoto
import com.wanderlog.android.domain.repository.LocalPhotoDeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class PendingPhotoDeleteRequest(
    val intentSender: IntentSender,
    val pendingPhotos: List<TripPhoto>,
    val retryAfterConfirmation: Boolean
)

private enum class TripPhotosViewMode {
    GALLERY,
    MANAGE
}

private enum class TripPhotoLocationFilter {
    ALL,
    WITH_LOCATION
}

private sealed interface PhotoLocationLabelState {
    data object None : PhotoLocationLabelState
    data object Loading : PhotoLocationLabelState
    data object Available : PhotoLocationLabelState
    data class Named(val label: String) : PhotoLocationLabelState
}

private data class TripPhotoDayOption(
    val key: String?,
    val chipLabel: String,
    val summaryLabel: String,
    val count: Int
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun TripPhotosScreen(
    onBack: () -> Unit,
    viewModel: TripPhotosViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val permissionState = rememberMultiplePermissionsState(photoPermissions())
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var preferredViewMode by rememberSaveable { mutableStateOf(TripPhotosViewMode.GALLERY) }
    var isGalleryAutoCycling by rememberSaveable { mutableStateOf(false) }
    var galleryLocationFilter by rememberSaveable { mutableStateOf(TripPhotoLocationFilter.ALL) }
    var selectedGalleryDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<TripPhoto?>(null) }
    var currentGalleryPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showFullScreenSlideshow by rememberSaveable { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var photosPendingDeleteConfirmation by remember { mutableStateOf<List<TripPhoto>?>(null) }
    var pendingSystemDeleteRequest by remember { mutableStateOf<PendingPhotoDeleteRequest?>(null) }
    val selectedPhotos = remember(state.photos, selectedPhotoIds) {
        state.photos.filter { it.id in selectedPhotoIds }
    }
    val isSelectionMode = selectedPhotoIds.isNotEmpty()
    val activeViewMode = if (isSelectionMode) TripPhotosViewMode.MANAGE else preferredViewMode
    val galleryLocationFilteredPhotos = remember(state.photos, galleryLocationFilter) {
        when (galleryLocationFilter) {
            TripPhotoLocationFilter.ALL -> state.photos
            TripPhotoLocationFilter.WITH_LOCATION -> state.photos.filter(TripPhoto::hasLocation)
        }
    }
    val galleryDayOptions = remember(galleryLocationFilteredPhotos, state.tripStartDate, state.tripEndDate) {
        buildTripPhotoDayOptions(
            photos = galleryLocationFilteredPhotos,
            tripStartDate = state.tripStartDate,
            tripEndDate = state.tripEndDate
        )
    }
    val selectedGalleryDayOption = remember(galleryDayOptions, selectedGalleryDayKey) {
        galleryDayOptions.firstOrNull { it.key == selectedGalleryDayKey }
            ?: galleryDayOptions.firstOrNull()
    }
    val filteredGalleryPhotos = remember(galleryLocationFilteredPhotos, selectedGalleryDayKey) {
        val selectedDay = selectedGalleryDayKey.toLocalDateOrNull()
        if (selectedDay == null) {
            galleryLocationFilteredPhotos
        } else {
            galleryLocationFilteredPhotos.filter { it.capturedLocalDate() == selectedDay }
        }
    }

    KeepScreenOnEffect(enabled = isGalleryAutoCycling || showFullScreenSlideshow)

    LaunchedEffect(activeViewMode, filteredGalleryPhotos.size) {
        if (activeViewMode != TripPhotosViewMode.GALLERY || filteredGalleryPhotos.size < 2) {
            isGalleryAutoCycling = false
        }
    }

    LaunchedEffect(galleryDayOptions) {
        if (selectedGalleryDayKey != null && galleryDayOptions.none { it.key == selectedGalleryDayKey }) {
            selectedGalleryDayKey = null
        }
    }

    LaunchedEffect(state.photos) {
        val availableIds = state.photos.mapTo(mutableSetOf()) { it.id }
        selectedPhotoIds = selectedPhotoIds.filterTo(mutableSetOf()) { it in availableIds }
        if (selectedPhoto?.id !in availableIds) {
            selectedPhoto = null
        }
    }

    LaunchedEffect(filteredGalleryPhotos) {
        val availableIds = filteredGalleryPhotos.mapTo(mutableSetOf()) { it.id }
        if (currentGalleryPhotoId !in availableIds) {
            currentGalleryPhotoId = filteredGalleryPhotos.firstOrNull()?.id
        }
        if (showFullScreenSlideshow && filteredGalleryPhotos.isEmpty()) {
            showFullScreenSlideshow = false
        }
    }

    fun showDeleteError(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun finalizeDeletedPhotos(photos: List<TripPhoto>, refresh: Boolean = true) {
        if (photos.isNotEmpty()) {
            val deletedIds = photos.mapTo(mutableSetOf()) { it.id }
            if (selectedPhoto?.id in deletedIds) {
                selectedPhoto = null
            }
            selectedPhotoIds = selectedPhotoIds - deletedIds
        }
        if (refresh) {
            viewModel.refresh()
        }
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingDelete = pendingSystemDeleteRequest ?: return@rememberLauncherForActivityResult
        pendingSystemDeleteRequest = null

        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            runCatching {
                if (pendingDelete.retryAfterConfirmation) {
                    viewModel.deletePhotos(pendingDelete.pendingPhotos)
                } else {
                    LocalPhotoDeleteResult.Deleted(pendingDelete.pendingPhotos)
                }
            }.onSuccess { deleteResult ->
                when (deleteResult) {
                    is LocalPhotoDeleteResult.Deleted -> {
                        finalizeDeletedPhotos(deleteResult.deletedPhotos.ifEmpty { pendingDelete.pendingPhotos })
                        snackbarHostState.showSnackbar(deleteResultMessage(pendingDelete.pendingPhotos.size))
                    }

                    is LocalPhotoDeleteResult.RequiresUserConfirmation -> {
                        if (deleteResult.deletedPhotos.isNotEmpty()) {
                            finalizeDeletedPhotos(deleteResult.deletedPhotos)
                        }
                        pendingSystemDeleteRequest = PendingPhotoDeleteRequest(
                            intentSender = deleteResult.intentSender,
                            pendingPhotos = deleteResult.pendingPhotos,
                            retryAfterConfirmation = deleteResult.retryAfterConfirmation
                        )
                    }
                }
            }.onFailure { error ->
                showDeleteError(error.message ?: "Failed to delete photo")
            }
        }
    }

    LaunchedEffect(pendingSystemDeleteRequest) {
        pendingSystemDeleteRequest?.let { pendingDelete ->
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(pendingDelete.intentSender).build()
            )
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted, state.isTripReady) {
        if (permissionState.allPermissionsGranted && state.isTripReady) {
            viewModel.loadPhotos()
        }
    }

    Scaffold(
        topBar = {
            WanderTopBar(
                title = when {
                    isSelectionMode -> "${selectedPhotoIds.size} selected"
                    state.tripName.isBlank() -> "Trip Photos"
                    else -> "${state.tripName} Photos"
                },
                onBack = onBack,
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            photosPendingDeleteConfirmation = selectedPhotos.takeIf { it.isNotEmpty() }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected photos")
                        }
                        IconButton(onClick = { selectedPhotoIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    } else if (permissionState.allPermissionsGranted && state.isTripReady) {
                        if (activeViewMode == TripPhotosViewMode.GALLERY) {
                            if (filteredGalleryPhotos.isNotEmpty()) {
                                IconButton(onClick = { showFullScreenSlideshow = true }) {
                                    Icon(Icons.Default.OpenInFull, contentDescription = "Open full-screen slideshow")
                                }
                            }
                            IconButton(
                                onClick = { isGalleryAutoCycling = !isGalleryAutoCycling },
                                enabled = filteredGalleryPhotos.size > 1
                            ) {
                                Icon(
                                    imageVector = if (isGalleryAutoCycling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isGalleryAutoCycling) {
                                        "Pause slideshow"
                                    } else {
                                        "Start slideshow"
                                    }
                                )
                            }
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh photos")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            !permissionState.allPermissionsGranted -> {
                PermissionRequiredState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
                )
            }

            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading local photos...")
                    }
                }
            }

            state.error != null -> {
                ErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    message = state.error,
                    onRetry = viewModel::refresh
                )
            }

            state.photos.isEmpty() -> {
                EmptyPhotosState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    tripDateLabel = state.tripDateLabel
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Showing local photos from ${state.tripDateLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (activeViewMode) {
                                TripPhotosViewMode.GALLERY -> if (filteredGalleryPhotos.size > 1) {
                                    "Browse by trip day, or turn on the location filter to only show photos with GPS metadata. Use the play button to auto-cycle the current gallery selection."
                                } else {
                                    "Browse by trip day, or turn on the location filter to only show photos with GPS metadata. Switch to Manage when you want to clean up or delete items."
                                }
                                TripPhotosViewMode.MANAGE -> "These are references to photos already on the device. Deleting them here deletes the original gallery photos. Long press a photo to select multiple. Photos without date metadata will not appear here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isSelectionMode) {
                        TripPhotosModeSwitcher(
                            activeViewMode = activeViewMode,
                            onModeChange = { preferredViewMode = it },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    when (activeViewMode) {
                        TripPhotosViewMode.GALLERY -> {
                            TripPhotoGalleryFilters(
                                locationFilter = galleryLocationFilter,
                                onLocationFilterChange = { galleryLocationFilter = it },
                                totalPhotoCount = state.photos.size,
                                locationTaggedPhotoCount = state.photos.count(TripPhoto::hasLocation),
                                dayOptions = galleryDayOptions,
                                selectedDayKey = selectedGalleryDayKey,
                                onDaySelect = { selectedGalleryDayKey = it },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            if (filteredGalleryPhotos.isEmpty()) {
                                FilteredGalleryEmptyState(
                                    selectedDayOption = selectedGalleryDayOption,
                                    locationFilter = galleryLocationFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                TripPhotoGallery(
                                    photos = filteredGalleryPhotos,
                                    tripStartDate = state.tripStartDate,
                                    currentPhotoId = currentGalleryPhotoId,
                                    isAutoCycling = isGalleryAutoCycling,
                                    onAutoCycleChange = { isGalleryAutoCycling = it },
                                    onCurrentPhotoChanged = { currentGalleryPhotoId = it.id },
                                    onOpenPhoto = { selectedPhoto = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        TripPhotosViewMode.MANAGE -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.photos, key = { it.id }) { photo ->
                                    TripPhotoCard(
                                        photo = photo,
                                        isSelected = photo.id in selectedPhotoIds,
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedPhotoIds = selectedPhotoIds.toggle(photo.id)
                                            } else {
                                                selectedPhoto = photo
                                            }
                                        },
                                        onLongClick = {
                                            selectedPhotoIds = selectedPhotoIds + photo.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        TripPhotoPreviewDialog(
            photo = photo,
            tripStartDate = state.tripStartDate,
            onDelete = { photosPendingDeleteConfirmation = listOf(photo) },
            onDismiss = { selectedPhoto = null }
        )
    }

    if (showFullScreenSlideshow && filteredGalleryPhotos.isNotEmpty()) {
        FullScreenTripPhotoSlideshowDialog(
            photos = filteredGalleryPhotos,
            tripStartDate = state.tripStartDate,
            initialPhotoId = currentGalleryPhotoId,
            isAutoCycling = isGalleryAutoCycling,
            onAutoCycleChange = { isGalleryAutoCycling = it },
            onCurrentPhotoChanged = { currentGalleryPhotoId = it.id },
            onDismiss = { showFullScreenSlideshow = false }
        )
    }

    photosPendingDeleteConfirmation?.let { photos ->
        ConfirmDialog(
            title = if (photos.size == 1) "Delete photo" else "Delete photos",
            message = if (photos.size == 1) {
                "Delete \"${photos.first().displayName}\" from this device? This photo is only referenced by Wandercraft, so deleting it here removes the original photo from the gallery as well."
            } else {
                "Delete ${photos.size} photos from this device? These photos are only referenced by Wandercraft, so deleting them here removes the original photos from the gallery as well."
            },
            onConfirm = {
                photosPendingDeleteConfirmation = null
                coroutineScope.launch {
                    runCatching { viewModel.deletePhotos(photos) }
                        .onSuccess { deleteResult ->
                            when (deleteResult) {
                                is LocalPhotoDeleteResult.Deleted -> {
                                    finalizeDeletedPhotos(deleteResult.deletedPhotos.ifEmpty { photos })
                                    snackbarHostState.showSnackbar(deleteResultMessage(photos.size))
                                }

                                is LocalPhotoDeleteResult.RequiresUserConfirmation -> {
                                    if (deleteResult.deletedPhotos.isNotEmpty()) {
                                        finalizeDeletedPhotos(deleteResult.deletedPhotos)
                                    }
                                    pendingSystemDeleteRequest = PendingPhotoDeleteRequest(
                                        intentSender = deleteResult.intentSender,
                                        pendingPhotos = deleteResult.pendingPhotos,
                                        retryAfterConfirmation = deleteResult.retryAfterConfirmation
                                    )
                                }
                            }
                        }
                        .onFailure { error ->
                            showDeleteError(error.message ?: "Failed to delete photo")
                        }
                }
            },
            onDismiss = { photosPendingDeleteConfirmation = null }
        )
    }
}

@Composable
private fun TripPhotoGalleryFilters(
    locationFilter: TripPhotoLocationFilter,
    onLocationFilterChange: (TripPhotoLocationFilter) -> Unit,
    totalPhotoCount: Int,
    locationTaggedPhotoCount: Int,
    dayOptions: List<TripPhotoDayOption>,
    selectedDayKey: String?,
    onDaySelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = locationFilter == TripPhotoLocationFilter.ALL,
                onClick = { onLocationFilterChange(TripPhotoLocationFilter.ALL) },
                label = { Text("All photos ($totalPhotoCount)") }
            )
            FilterChip(
                selected = locationFilter == TripPhotoLocationFilter.WITH_LOCATION,
                onClick = { onLocationFilterChange(TripPhotoLocationFilter.WITH_LOCATION) },
                label = { Text("With location ($locationTaggedPhotoCount)") }
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayOptions, key = { it.key ?: "all-days" }) { option ->
                FilterChip(
                    selected = option.key == selectedDayKey || (option.key == null && selectedDayKey == null),
                    onClick = { onDaySelect(option.key) },
                    label = { Text(option.chipLabel) }
                )
            }
        }
    }
}

@Composable
private fun TripPhotosModeSwitcher(
    activeViewMode: TripPhotosViewMode,
    onModeChange: (TripPhotosViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeViewMode == TripPhotosViewMode.GALLERY,
            onClick = { onModeChange(TripPhotosViewMode.GALLERY) },
            label = { Text("Gallery") }
        )
        FilterChip(
            selected = activeViewMode == TripPhotosViewMode.MANAGE,
            onClick = { onModeChange(TripPhotosViewMode.MANAGE) },
            label = { Text("Manage") }
        )
    }
}

@Composable
private fun PermissionRequiredState(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Allow local photo access", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Wandercraft can show device photos that fall within the trip dates. This stays on the phone and does not require Google Photos or any internet connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRequestPermission) {
                    Text("Allow access")
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Could not load local photos", style = MaterialTheme.typography.titleMedium)
                Text(
                    message ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun EmptyPhotosState(
    modifier: Modifier = Modifier,
    tripDateLabel: String
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No local photos found", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No device photos matched ${tripDateLabel.ifBlank { "this trip's dates" }}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "If your gallery has photos from this trip, check that they still exist on the device and have capture dates saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilteredGalleryEmptyState(
    selectedDayOption: TripPhotoDayOption?,
    locationFilter: TripPhotoLocationFilter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No photos match this gallery filter", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when {
                        locationFilter == TripPhotoLocationFilter.WITH_LOCATION && selectedDayOption?.key != null -> {
                            "No photos on ${selectedDayOption.summaryLabel} have saved GPS metadata."
                        }

                        locationFilter == TripPhotoLocationFilter.WITH_LOCATION -> {
                            "No photos in this trip range have saved GPS metadata."
                        }

                        selectedDayOption?.key != null -> {
                            "No photos were found for ${selectedDayOption.summaryLabel}."
                        }

                        else -> {
                            "No photos are available for the current gallery filters."
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Try another trip day or turn off the location filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripPhotoGallery(
    photos: List<TripPhoto>,
    tripStartDate: LocalDate?,
    currentPhotoId: Long?,
    isAutoCycling: Boolean,
    onAutoCycleChange: (Boolean) -> Unit,
    onCurrentPhotoChanged: (TripPhoto) -> Unit,
    onOpenPhoto: (TripPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = remember(photos, currentPhotoId) {
        photos.indexOfFirst { it.id == currentPhotoId }
            .takeIf { it >= 0 }
            ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPageIndex = pagerState.settledPage.coerceIn(0, photos.lastIndex)
    val currentPhoto = photos[currentPageIndex]
    val currentLocationLabel = rememberPhotoLocationLabel(currentPhoto)

    LaunchedEffect(photos.size) {
        val lastIndex = photos.lastIndex
        if (lastIndex >= 0 && pagerState.settledPage > lastIndex) {
            pagerState.scrollToPage(lastIndex)
        }
        if (photos.size < 2 && isAutoCycling) {
            onAutoCycleChange(false)
        }
    }

    LaunchedEffect(isAutoCycling, photos.size, pagerState.settledPage) {
        if (!isAutoCycling || photos.size < 2) {
            return@LaunchedEffect
        }

        delay(3500)
        val nextPage = if (pagerState.settledPage >= photos.lastIndex) 0 else pagerState.settledPage + 1
        pagerState.animateScrollToPage(nextPage, animationSpec = SlideshowScrollAnimationSpec)
    }

    LaunchedEffect(currentPageIndex, photos) {
        onCurrentPhotoChanged(photos[currentPageIndex])
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f),
            shape = RoundedCornerShape(28.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onOpenPhoto(photo) }
                ) {
                    AsyncImage(
                        model = photo.contentUri,
                        contentDescription = photo.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.18f),
                                        Color.Black.copy(alpha = 0.62f)
                                    )
                                )
                            )
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "${page + 1} / ${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = photo.daySummary(tripStartDate),
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = currentPhoto.daySummary(tripStartDate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentPhoto.capturedAtLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentLocationLabel.asText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (photos.size > 1) {
                    if (isAutoCycling) {
                        "Slideshow is playing. Tap the pause button in the top bar or any thumbnail below to take control."
                    } else {
                        "Swipe the photo or tap a thumbnail below to browse the trip like a gallery. Use the play button in the top bar to auto-cycle." 
                    }
                } else {
                    "Tap the photo to open it full screen."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                Card(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onAutoCycleChange(false)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = SlideshowScrollAnimationSpec)
                            }
                        },
                    border = if (index == currentPageIndex) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    AsyncImage(
                        model = photo.contentUri,
                        contentDescription = photo.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenTripPhotoSlideshowDialog(
    photos: List<TripPhoto>,
    tripStartDate: LocalDate?,
    initialPhotoId: Long?,
    isAutoCycling: Boolean,
    onAutoCycleChange: (Boolean) -> Unit,
    onCurrentPhotoChanged: (TripPhoto) -> Unit,
    onDismiss: () -> Unit
) {
    val initialPage = remember(photos, initialPhotoId) {
        photos.indexOfFirst { it.id == initialPhotoId }
            .takeIf { it >= 0 }
            ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    val currentPageIndex = pagerState.settledPage.coerceIn(0, photos.lastIndex)
    val currentPhoto = photos[currentPageIndex]
    val currentLocationLabel = rememberPhotoLocationLabel(currentPhoto)

    KeepScreenOnEffect(enabled = true)

    LaunchedEffect(photos.size) {
        val lastIndex = photos.lastIndex
        if (lastIndex >= 0 && pagerState.settledPage > lastIndex) {
            pagerState.scrollToPage(lastIndex)
        }
        if (photos.size < 2 && isAutoCycling) {
            onAutoCycleChange(false)
        }
    }

    LaunchedEffect(isAutoCycling, photos.size, pagerState.settledPage) {
        if (!isAutoCycling || photos.size < 2) {
            return@LaunchedEffect
        }

        delay(4200)
        val nextPage = if (pagerState.settledPage >= photos.lastIndex) 0 else pagerState.settledPage + 1
        pagerState.animateScrollToPage(nextPage, animationSpec = SlideshowScrollAnimationSpec)
    }

    LaunchedEffect(currentPageIndex, photos) {
        onCurrentPhotoChanged(photos[currentPageIndex])
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val photo = photos[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = photo.contentUri,
                            contentDescription = photo.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewActionButton(
                        icon = if (isAutoCycling) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoCycling) "Pause full-screen slideshow" else "Start full-screen slideshow",
                        onClick = { onAutoCycleChange(!isAutoCycling) },
                        containerColor = Color.Black.copy(alpha = 0.72f)
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "${currentPageIndex + 1} / ${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    PreviewActionButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close full-screen slideshow",
                        onClick = onDismiss,
                        containerColor = Color.Black.copy(alpha = 0.72f)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentPhoto.daySummary(tripStartDate),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentPhoto.capturedAtLabel(),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = currentLocationLabel.asText(),
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripPhotoCard(
    photo: TripPhoto,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column {
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            ) {
                AsyncImage(
                    model = photo.contentUri,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                    SelectionBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = photo.capturedAtLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TripPhotoPreviewDialog(
    photo: TripPhoto,
    tripStartDate: LocalDate?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val locationLabel = rememberPhotoLocationLabel(photo)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photo.contentUri,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                PreviewActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete photo",
                    onClick = onDelete,
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                PreviewActionButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close photo",
                    onClick = onDismiss,
                    containerColor = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = photo.daySummary(tripStartDate),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = photo.capturedAtLabel(),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = locationLabel.asText(),
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
private fun PreviewActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current

    DisposableEffect(view, enabled) {
        val previousKeepScreenOn = view.keepScreenOn
        if (enabled) {
            view.keepScreenOn = true
        }

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }
}

private fun photoPermissions(): List<String> =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        }

        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        else -> {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

private fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { runCatching(LocalDate::parse).getOrNull() }

private fun deleteResultMessage(count: Int): String =
    if (count == 1) "Photo deleted from this device" else "$count photos deleted from this device"

private fun buildTripPhotoDayOptions(
    photos: List<TripPhoto>,
    tripStartDate: LocalDate?,
    tripEndDate: LocalDate?
): List<TripPhotoDayOption> {
    val countsByDate = photos.groupingBy(TripPhoto::capturedLocalDate).eachCount()
    val dayDates = when {
        tripStartDate != null && tripEndDate != null -> generateDateRange(tripStartDate, tripEndDate)
        countsByDate.isNotEmpty() -> countsByDate.keys.sorted()
        else -> emptyList()
    }

    return buildList {
        add(
            TripPhotoDayOption(
                key = null,
                chipLabel = "All days (${photos.size})",
                summaryLabel = "all days",
                count = photos.size
            )
        )

        if (tripStartDate != null && tripEndDate != null) {
            dayDates.forEachIndexed { index, date ->
                val count = countsByDate[date] ?: 0
                val summaryLabel = "Day ${index + 1} • ${date.toShortDisplay()}"
                add(
                    TripPhotoDayOption(
                        key = date.toString(),
                        chipLabel = "$summaryLabel ($count)",
                        summaryLabel = summaryLabel,
                        count = count
                    )
                )
            }
        } else {
            dayDates.forEach { date ->
                val count = countsByDate[date] ?: 0
                val summaryLabel = date.toShortDisplay()
                add(
                    TripPhotoDayOption(
                        key = date.toString(),
                        chipLabel = "$summaryLabel ($count)",
                        summaryLabel = summaryLabel,
                        count = count
                    )
                )
            }
        }
    }
}

private fun TripPhoto.capturedAtLabel(): String =
    Instant.ofEpochMilli(capturedAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(PhotoTimestampFormatter)

private fun TripPhoto.capturedLocalDate(): LocalDate =
    Instant.ofEpochMilli(capturedAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun TripPhoto.daySummary(tripStartDate: LocalDate?): String {
    val date = capturedLocalDate()
    return if (tripStartDate != null) {
        val dayNumber = ChronoUnit.DAYS.between(tripStartDate, date).toInt() + 1
        "Day $dayNumber • ${date.toShortDisplay()}"
    } else {
        date.toShortDisplay()
    }
}

private fun TripPhoto.locationSummary(): String =
    if (hasLocation) "Location metadata available" else "No saved GPS coordinates"

@Composable
private fun rememberPhotoLocationLabel(photo: TripPhoto): PhotoLocationLabelState {
    val context = LocalContext.current
    val state by produceState<PhotoLocationLabelState>(
        initialValue = if (photo.hasLocation) PhotoLocationLabelState.Loading else PhotoLocationLabelState.None,
        key1 = photo.id,
        key2 = photo.latitude,
        key3 = photo.longitude
    ) {
        if (!photo.hasLocation) {
            value = PhotoLocationLabelState.None
            return@produceState
        }

        value = withContext(Dispatchers.IO) {
            resolvePhotoLocationName(context, photo)
                ?.let(PhotoLocationLabelState::Named)
                ?: PhotoLocationLabelState.Available
        }
    }

    return state
}

private fun PhotoLocationLabelState.asText(): String = when (this) {
    PhotoLocationLabelState.None -> "No saved GPS coordinates"
    PhotoLocationLabelState.Loading -> "Looking up nearby place..."
    PhotoLocationLabelState.Available -> "Location metadata available"
    is PhotoLocationLabelState.Named -> label
}

private fun resolvePhotoLocationName(context: android.content.Context, photo: TripPhoto): String? {
    if (!photo.hasLocation || !Geocoder.isPresent()) {
        return null
    }

    val latitude = photo.latitude ?: return null
    val longitude = photo.longitude ?: return null

    return runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.getDefault())
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()
            ?.toPhotoLocationLabel()
    }.getOrNull()
}

private fun Address.toPhotoLocationLabel(): String? {
    val parts = listOfNotNull(subLocality, locality, subAdminArea, adminArea, countryName)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()

    return when {
        parts.isEmpty() -> null
        parts.size == 1 -> parts.first()
        else -> parts.take(2).joinToString(", ")
    }
}

private val SlideshowScrollAnimationSpec = tween<Float>(durationMillis = 1050, easing = FastOutSlowInEasing)

private val PhotoTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
