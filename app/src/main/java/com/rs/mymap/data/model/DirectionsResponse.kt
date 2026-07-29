package com.rs.mymap.data.model

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes")
    val routes: List<Route>
)

data class Route(
    @SerializedName("summary")
    val summary: String,
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline,
    @SerializedName("legs")
    val legs: List<Leg>
)

data class Leg(
    @SerializedName("distance")
    val distance: TextValue,
    @SerializedName("duration")
    val duration: TextValue
)

data class TextValue(
    @SerializedName("text")
    val text: String,
    @SerializedName("value")
    val value: Int
)

data class OverviewPolyline(
    @SerializedName("points")
    val points: String
)
