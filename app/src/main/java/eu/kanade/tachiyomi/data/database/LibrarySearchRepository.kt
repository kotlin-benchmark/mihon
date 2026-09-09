package eu.kanade.tachiyomi.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class LibrarySearchRepository(private val context: Context) {

    fun searchByTitle(title: String) {
        val filter = cleanSql(title)
        val sql = "SELECT * FROM mangas WHERE title LIKE '%$filter%'"

        val db = SQLiteDatabase.openDatabase(
            context.getDatabasePath(DATABASE_NAME).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )

        //CWE-89
        //SINK
        db.rawQuery(sql, null).use { }
    }

    private fun cleanSql(input: String): String {
        return input.trim()
            .replace(";", "")
            .replace("--", "")
    }

    companion object {
        private const val DATABASE_NAME = "tachiyomi.db"
    }
}
