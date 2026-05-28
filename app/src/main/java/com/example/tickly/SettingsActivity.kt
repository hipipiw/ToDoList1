package com.example.tickly

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// PENTING: Mengunci import R milik project Tickly agar tidak bentrok dengan sistem
import com.example.tickly.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvAllTasks: RecyclerView
    private lateinit var btnToggleSort: Button

    private var isSortAscending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dbHelper = DatabaseHelper(this)

        findViewById<ImageButton>(R.id.btnBackSettings).setOnClickListener { finish() }

        rvCategories = findViewById(R.id.rvManageCategories)
        rvAllTasks = findViewById(R.id.rvAllTasksSettings)
        btnToggleSort = findViewById(R.id.btnToggleSort)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvAllTasks.layoutManager = LinearLayoutManager(this)

        btnToggleSort.setOnClickListener {
            isSortAscending = !isSortAscending
            btnToggleSort.text = if (isSortAscending) "Sort: Terlama" else "Sort: Terbaru"
            loadAllTasksLog()
        }

        loadManageCategories()
        loadAllTasksLog()
    }

    private fun loadManageCategories() {
        val categoriesDetailed = dbHelper.getAllCategoriesDetailed()
        rvCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class CatViewHolder(v: View) : RecyclerView.ViewHolder(v) {
                val tvEmoji = v.findViewById<TextView>(R.id.tvManageCatEmoji)
                val tvName = v.findViewById<TextView>(R.id.tvManageCatName)
                val btnEdit = v.findViewById<TextView>(R.id.btnEditCat)
                val btnDelete = v.findViewById<TextView>(R.id.btnDeleteCat)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manage_category, parent, false)
                return CatViewHolder(view)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = categoriesDetailed[position]
                val h = holder as CatViewHolder
                h.tvName.text = item.first
                h.tvEmoji.text = item.second
                h.tvEmoji.backgroundTintList = ColorStateList.valueOf(Color.parseColor(item.third))

                h.btnDelete.setOnClickListener {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Hapus Kategori")
                        .setMessage("Hapus kategori '${item.first}'? Tugas dengan kategori ini tidak akan hilang.")
                        .setPositiveButton("Hapus") { d, _ ->
                            dbHelper.deleteCategory(item.first)
                            loadManageCategories()
                            d.dismiss()
                        }.setNegativeButton("Batal") { d, _ -> d.dismiss() }.show()
                }

                h.btnEdit.setOnClickListener { showEditCategoryDialog(item.first, item.second, item.third) }
            }
            override fun getItemCount(): Int = categoriesDetailed.size
        }
    }

    private fun loadAllTasksLog() {
        val allTasksRaw = dbHelper.getAllTasksRaw()
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.ENGLISH)

        val sortedList = if (isSortAscending) {
            allTasksRaw.sortedBy { try { sdf.parse(it.date) } catch (e: Exception) { Date(0) } }
        } else {
            allTasksRaw.sortedByDescending { try { sdf.parse(it.date) } catch (e: Exception) { Date(0) } }
        }

        val adapter = TaskAdapter(sortedList,
            onTaskChecked = { task, isChecked ->
                dbHelper.updateTaskStatus(task.id, isChecked)
                loadAllTasksLog()
            },
            onTaskLongClicked = { task ->
                dbHelper.deleteTask(task.id)
                loadAllTasksLog()
            }
        )
        rvAllTasks.adapter = adapter
    }

    private fun showEditCategoryDialog(oldName: String, oldEmoji: String, oldColor: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvPreview = dialogView.findViewById<TextView>(R.id.tvCategoryPreview)
        val etNewCategory = dialogView.findViewById<EditText>(R.id.etNewCategory)
        val gridEmojis = dialogView.findViewById<GridLayout>(R.id.gridEmojis)
        val layoutColorsRow = dialogView.findViewById<LinearLayout>(R.id.layoutColorsRow)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpanKategori)

        dialogView.findViewById<TextView>(R.id.tvCloseDialog).setOnClickListener { dialog.dismiss() }

        etNewCategory.setText(oldName)
        tvPreview.text = oldEmoji
        tvPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor(oldColor))
        btnSimpan.text = "Perbarui Kategori"

        var currentEmoji = oldEmoji
        var currentColorHex = oldColor

        val emojis = listOf("🎯", "🎨", "🏋️", "🍳", "🌿", "🐾", "✈️", "🎵", "📷", "💡", "🔧", "🧠", "🎮", "🌙", "⚡", "🦋", "🌺", "🌈")
        val colors = listOf("#D87093", "#9575CD", "#4DB6AC", "#FF8A65", "#64B5F6", "#D4AF37", "#E57373", "#4DD0E1")

        tvPreview.setOnClickListener {
            val inputEdit = EditText(this).apply {
                hint = "Ketik emoji..."
                filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                gravity = android.view.Gravity.CENTER
            }
            AlertDialog.Builder(this)
                .setTitle("Emoji Kustom ⌨️")
                .setView(inputEdit)
                .setPositiveButton("Pilih") { d, _ ->
                    val typed = inputEdit.text.toString().trim()
                    if (typed.isNotEmpty()) { currentEmoji = typed; tvPreview.text = typed }
                    d.dismiss()
                }.setNegativeButton("Batal") { d, _ -> d.dismiss() }.show()
        }

        for (emoji in emojis) {
            val tvEmoji = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = android.view.Gravity.CENTER
                setPadding(12, 12, 12, 12)
                setOnClickListener { currentEmoji = emoji; tvPreview.text = emoji }
            }
            gridEmojis.addView(tvEmoji)
        }

        val density = resources.displayMetrics.density
        val sizeInDp = (32 * density).toInt()
        val marginInDp = (6 * density).toInt()

        for (colorHex in colors) {
            val colorCircle = View(this).apply {
                val params = LinearLayout.LayoutParams(sizeInDp, sizeInDp).apply { setMargins(marginInDp, 0, marginInDp, 0) }
                layoutParams = params
                setBackgroundResource(R.drawable.bg_circle)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
                setOnClickListener {
                    currentColorHex = colorHex
                    tvPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
                }
            }
            layoutColorsRow.addView(colorCircle)
        }

        btnSimpan.setOnClickListener {
            val newName = etNewCategory.text.toString().trim()
            if (newName.isNotEmpty()) {
                dbHelper.updateCategoryDetails(oldName, newName, currentEmoji, currentColorHex)
                loadManageCategories()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}