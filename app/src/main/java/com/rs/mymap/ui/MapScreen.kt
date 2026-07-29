package com.rs.mymap.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.rs.mymap.data.api.RetrofitClient
import com.rs.mymap.data.model.Route
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initial camera position (Malang)
    val defaultPos = remember { LatLng(-7.9826092, 112.6282364) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 13f)
    }

    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    
    // Transport Mode: "driving" (Car), "two_wheeler" (Motorcycle), "walking" (Walking)
    var selectedMode by remember { mutableStateOf("driving") }
    
    // Multiple Routes State
    var allRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRouteIndex by remember { mutableStateOf(0) }
    
    var isLoading by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val apiKey = "AIzaSyAMxzfxQjAg9Jr-WE5EtBpAE7xCXwz2B1Q"

    // Computed states based on selection
    val currentRoute = allRoutes.getOrNull(selectedRouteIndex)
    val routePoints = remember(currentRoute) {
        currentRoute?.overviewPolyline?.points?.let { PolyUtil.decode(it) } ?: emptyList()
    }
    val distance = currentRoute?.legs?.getOrNull(0)?.distance?.text ?: ""
    val duration = currentRoute?.legs?.getOrNull(0)?.duration?.text ?: ""

    // Function to fetch routes
    val fetchRoutes = suspend {
        if (originText.isNotBlank() && destinationText.isNotBlank()) {
            isLoading = true
            try {
                val avoid = if (selectedMode == "two_wheeler") "tolls" else null
                
                val response = RetrofitClient.getDirectionsApiService(context).getDirections(
                    origin = originText,
                    destination = destinationText,
                    mode = selectedMode,
                    avoid = avoid,
                    alternatives = true,
                    apiKey = apiKey
                )
                if (response.routes.isNotEmpty()) {
                    allRoutes = response.routes
                    selectedRouteIndex = 0
                    
                    // Adjust camera to show the start of the route
                    val decodedPoints = PolyUtil.decode(response.routes[0].overviewPolyline.points)
                    val startLocation = decodedPoints.firstOrNull()
                    if (startLocation != null) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(startLocation, 14f)
                    }
                } else {
                    allRoutes = emptyList()
                    selectedRouteIndex = 0
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error fetching directions", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-update when mode changes
    LaunchedEffect(selectedMode) {
        if (allRoutes.isNotEmpty()) {
            fetchRoutes()
        }
    }

    // Main UI - Using Box to overlay everything on the map
    Box(modifier = modifier.fillMaxSize()) {
        // 1) Background Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (routePoints.isNotEmpty()) {
                val startMarkerState = remember(routePoints) { MarkerState(position = routePoints.first()) }
                val endMarkerState = remember(routePoints) { MarkerState(position = routePoints.last()) }

                Marker(state = startMarkerState, title = "Start")
                Marker(state = endMarkerState, title = "End")
                
                Polyline(
                    points = routePoints,
                    color = when(selectedMode) {
                        "driving" -> Color.Blue
                        "two_wheeler" -> Color(0xFF4CAF50)
                        else -> Color(0xFFFF9800)
                    },
                    width = 12f
                )
            }
        }

        // 2) Top Overlays (Input Area and Info Card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // User Input Area (Solid Background Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = originText,
                        onValueChange = { originText = it },
                        label = { Text("Origin (e.g. Unmer Malang)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it },
                        label = { Text("Destination (e.g. Alun-alun Malang)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Transport Mode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedMode == "driving",
                            onClick = { selectedMode = "driving" },
                            label = { Text("Mobil") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedMode == "two_wheeler",
                            onClick = { selectedMode = "two_wheeler" },
                            label = { Text("Motor") },
                            leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedMode == "walking",
                            onClick = { selectedMode = "walking" },
                            label = { Text("Jalan") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch { fetchRoutes() }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && originText.isNotBlank() && destinationText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLoading) "Loading..." else "Cari Rute")
                        }

                        Button(
                            onClick = {
                                originText = ""
                                destinationText = ""
                                allRoutes = emptyList()
                                selectedRouteIndex = 0
                                selectedMode = "driving"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reset")
                        }
                    }
                }
            }

            // Floating Information Display
            if (distance.isNotEmpty() && duration.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(0.9f)
                        .align(Alignment.CenterHorizontally)
                        .clickable { showBottomSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF003D51).copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Jarak", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(distance, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                        }
                        
                        VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                            Text("Waktu", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(duration, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                        }

                        VerticalDivider(modifier = Modifier.height(30.dp), thickness = 1.dp, color = Color.White.copy(alpha = 0.3f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Moda", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Icon(
                                imageVector = when(selectedMode) {
                                    "driving" -> Icons.Default.DirectionsCar
                                    "two_wheeler" -> Icons.Default.TwoWheeler
                                    else -> Icons.AutoMirrored.Filled.DirectionsWalk
                                },
                                contentDescription = selectedMode,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3) Bottom FAB
        if (allRoutes.size > 1) {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Pilihan Rute")
            }
        }
    }

    // 4) BottomSheet for Alternative Routes
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Pilihan Rute",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn {
                    itemsIndexed(allRoutes) { index, route ->
                        val routeDistance = route.legs.getOrNull(0)?.distance?.text ?: ""
                        val routeDuration = route.legs.getOrNull(0)?.duration?.text ?: ""
                        val via = if (route.summary.isNotEmpty()) "via ${route.summary}" else "Rute ${index + 1}"

                        ListItem(
                            modifier = Modifier.clickable {
                                selectedRouteIndex = index
                                showBottomSheet = false
                            },
                            headlineContent = { Text(via, fontWeight = if (index == selectedRouteIndex) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("$routeDuration • $routeDistance") },
                            leadingContent = {
                                Icon(
                                    imageVector = when(selectedMode) {
                                        "driving" -> Icons.Default.DirectionsCar
                                        "two_wheeler" -> Icons.Default.TwoWheeler
                                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                                    },
                                    contentDescription = null,
                                    tint = if (index == selectedRouteIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                if (index == selectedRouteIndex) {
                                    Icon(Icons.Default.Check, contentDescription = "Terpilih", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen()
}
