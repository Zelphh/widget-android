package com.tartari.cajuwidget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Gasto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caju-widget.db",
                ).build().also { instancia = it }
            }
    }
}
