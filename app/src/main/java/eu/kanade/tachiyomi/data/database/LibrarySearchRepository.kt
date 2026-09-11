package eu.kanade.tachiyomi.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

class LibrarySearchRepository(private val context: Context) {

    fun searchByTitle(title: String) {
        val filter = cleanSql(title)
        val sql = "SELECT * FROM mangas WHERE title LIKE '%$filter%'"

        val db = openDatabase()

        //CWE-89
        //SINK
        db.query(sql).use { }
    }

    private fun openDatabase(): SupportSQLiteDatabase {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DATABASE_NAME)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).readableDatabase
    }

    private fun cleanSql(input: String): String {
        return input.trim()
            .replace(";", "")
            .replace("--", "")
    }

    companion object {
        private const val DATABASE_NAME = "tachiyomi.db"
        private const val DATABASE_VERSION = 1
    }
}
