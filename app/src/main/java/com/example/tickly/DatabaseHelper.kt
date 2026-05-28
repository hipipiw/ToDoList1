package com.example.tickly

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "tickly_db"
        private const val DATABASE_VERSION = 3 // Naik ke versi 3 untuk mendukung struktur Emoji dan Warna kustom

        // Tabel Tugas
        const val TABLE_TASKS = "tasks"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_DESC = "description"
        const val COL_DATE = "task_date"
        const val COL_CATEGORY = "category"
        const val COL_STATUS = "status"

        // Tabel Kategori
        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "cat_id"
        const val COL_CAT_NAME = "cat_name"
        const val COL_CAT_EMOJI = "cat_emoji" // Kolom baru untuk ikon emoji
        const val COL_CAT_COLOR = "cat_color" // Kolom baru untuk kode warna hex
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Buat tabel tugas
        val createTableTasks = ("CREATE TABLE $TABLE_TASKS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_TITLE TEXT,"
                + "$COL_DESC TEXT,"
                + "$COL_DATE TEXT,"
                + "$COL_CATEGORY TEXT,"
                + "$COL_STATUS INTEGER DEFAULT 0)")
        db.execSQL(createTableTasks)

        // Buat tabel kategori komplit dengan penampung emoji & warna kustom
        val createTableCategories = ("CREATE TABLE $TABLE_CATEGORIES ("
                + "$COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_CAT_NAME TEXT,"
                + "$COL_CAT_EMOJI TEXT,"
                + "$COL_CAT_COLOR TEXT)")
        db.execSQL(createTableCategories)

        // Masukkan kategori default awal bawaan aplikasi lengkap dengan warna pastelnya
        db.execSQL("INSERT INTO $TABLE_CATEGORIES ($COL_CAT_NAME, $COL_CAT_EMOJI, $COL_CAT_COLOR) VALUES ('Kerjaan', '💼', '#9575CD')")
        db.execSQL("INSERT INTO $TABLE_CATEGORIES ($COL_CAT_NAME, $COL_CAT_EMOJI, $COL_CAT_COLOR) VALUES ('Belajar', '📚', '#4DB6AC')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }

    // --- FUNGSI MANAJEMEN TUGAS ---
    fun addTask(title: String, desc: String, date: String, category: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_TITLE, title)
        values.put(COL_DESC, desc)
        values.put(COL_DATE, date)
        values.put(COL_CATEGORY, category)
        values.put(COL_STATUS, 0)
        val success = db.insert(TABLE_TASKS, null, values)
        db.close()
        return success
    }

    fun updateTaskStatus(id: Int, isCompleted: Boolean): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_STATUS, if (isCompleted) 1 else 0)
        val success = db.update(TABLE_TASKS, values, "$COL_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    fun getTasks(isCompleted: Boolean, dateFilter: String? = null): List<Task> {
        val taskList = ArrayList<Task>()
        val db = this.readableDatabase
        var selection = "$COL_STATUS = ?"
        var selectionArgs = arrayOf(if (isCompleted) "1" else "0")

        if (dateFilter != null) {
            selection += " AND $COL_DATE = ?"
            selectionArgs = arrayOf(if (isCompleted) "1" else "0", dateFilter)
        }

        val cursor = db.query(TABLE_TASKS, null, selection, selectionArgs, null, null, "$COL_ID DESC")
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY))
                taskList.add(Task(id, title, desc, date, category, isCompleted))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return taskList
    }

    fun deleteTask(id: Int): Int {
        val db = this.writableDatabase
        val success = db.delete(TABLE_TASKS, "$COL_ID=?", arrayOf(id.toString()))
        db.close()
        return success
    }

    // --- FUNGSI MANAJEMEN KATEGORI BARU DENGAN EMOJI & WARNA HEX ---
    fun addCategoryWithDetails(name: String, emoji: String, colorHex: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CAT_NAME, name)
        values.put(COL_CAT_EMOJI, emoji)
        values.put(COL_CAT_COLOR, colorHex)
        val id = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return id
    }

    fun getAllCategoriesDetailed(): List<Triple<String, String, String>> {
        val list = ArrayList<Triple<String, String, String>>() // Menyimpan data bentukan: Triple <Nama, Emoji, WarnaHex>
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CATEGORIES", null)
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME))
                val emoji = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_EMOJI)) ?: "🎯"
                val color = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_COLOR)) ?: "#D87093"
                list.add(Triple(name, emoji, color))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    // --- TAMBAHAN BARU UNTUK HALAMAN SETTINGS (CRUD KATEGORI & AUDIT LOG) ---

    // 1. Fungsi Menghapus Kategori
    fun deleteCategory(name: String): Int {
        val db = this.writableDatabase
        val success = db.delete(TABLE_CATEGORIES, "$COL_CAT_NAME=?", arrayOf(name))
        db.close()
        return success
    }

    // 2. Fungsi Memperbarui/Edit Detail Kategori
    fun updateCategoryDetails(oldName: String, newName: String, emoji: String, colorHex: String): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CAT_NAME, newName)
        values.put(COL_CAT_EMOJI, emoji)
        values.put(COL_CAT_COLOR, colorHex)
        val success = db.update(TABLE_CATEGORIES, values, "$COL_CAT_NAME=?", arrayOf(oldName))
        db.close()
        return success
    }

    // 3. Fungsi Mengambil Seluruh Tugas Tanpa Batasan Tanggal/Status untuk Audit Log
    fun getAllTasksRaw(): List<Task> {
        val taskList = ArrayList<Task>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_TASKS, null, null, null, null, null, "$COL_ID DESC")
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS)) == 1
                taskList.add(Task(id, title, desc, date, category, status))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return taskList
    }
}