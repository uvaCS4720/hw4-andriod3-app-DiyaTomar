package edu.nd.pmcburne.hello.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CampusDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_TAGS TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        onCreate(db)
    }

    fun upsertLocations(locations: List<CampusLocation>) {
        writableDatabase.beginTransaction()
        try {
            locations.forEach { location ->
                val values = ContentValues().apply {
                    put(COLUMN_ID, location.id)
                    put(COLUMN_NAME, location.name)
                    put(COLUMN_DESCRIPTION, location.description)
                    put(COLUMN_LATITUDE, location.latitude)
                    put(COLUMN_LONGITUDE, location.longitude)
                    put(COLUMN_TAGS, location.tags.joinToString(TAG_SEPARATOR))
                }
                writableDatabase.insertWithOnConflict(
                    TABLE_LOCATIONS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getAllLocations(): List<CampusLocation> {
        val query = """
            SELECT $COLUMN_ID, $COLUMN_NAME, $COLUMN_DESCRIPTION, $COLUMN_LATITUDE, $COLUMN_LONGITUDE, $COLUMN_TAGS
            FROM $TABLE_LOCATIONS
            ORDER BY $COLUMN_NAME COLLATE NOCASE ASC
        """.trimIndent()

        return readableDatabase.rawQuery(query, null).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(COLUMN_NAME)
            val descriptionIndex = cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)
            val latitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)
            val longitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)
            val tagsIndex = cursor.getColumnIndexOrThrow(COLUMN_TAGS)

            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CampusLocation(
                            id = cursor.getInt(idIndex),
                            name = cursor.getString(nameIndex),
                            description = cursor.getString(descriptionIndex),
                            latitude = cursor.getDouble(latitudeIndex),
                            longitude = cursor.getDouble(longitudeIndex),
                            tags = cursor.getString(tagsIndex)
                                .split(TAG_SEPARATOR)
                                .filter(String::isNotBlank)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "campus_locations.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_LOCATIONS = "locations"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_TAGS = "tags"
        private const val TAG_SEPARATOR = "\u001F"
    }
}
