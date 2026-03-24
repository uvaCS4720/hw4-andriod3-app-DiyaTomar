package edu.nd.pmcburne.hello.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class CampusRepository(context: Context) {
    private val databaseHelper = CampusDatabaseHelper(context.applicationContext)

    suspend fun synchronizeLocations() = withContext(Dispatchers.IO) {
        val remoteLocations = fetchRemoteLocations()
        databaseHelper.upsertLocations(remoteLocations)
    }

    suspend fun getStoredLocations(): List<CampusLocation> = withContext(Dispatchers.IO) {
        databaseHelper.getAllLocations()
    }

    private fun fetchRemoteLocations(): List<CampusLocation> {
        val connection = (URL(PLACEMARKS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseLocations(JSONArray(body))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLocations(array: JSONArray): List<CampusLocation> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val visualCenter = item.optJSONObject("visual_center") ?: continue
            val tagArray = item.optJSONArray("tag_list") ?: JSONArray()
            val tags = buildList {
                for (tagIndex in 0 until tagArray.length()) {
                    add(tagArray.getString(tagIndex))
                }
            }

            add(
                CampusLocation(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    description = item.optString("description"),
                    tags = tags,
                    latitude = visualCenter.getDouble("latitude"),
                    longitude = visualCenter.getDouble("longitude")
                )
            )
        }
    }

    companion object {
        private const val PLACEMARKS_URL = "https://www.cs.virginia.edu/~wxt4gm/placemarks.json"
    }
}
