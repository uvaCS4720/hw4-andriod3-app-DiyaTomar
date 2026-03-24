package edu.nd.pmcburne.hello.data

data class CampusLocation(
    val id: Int,
    val name: String,
    val description: String,
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double
)
