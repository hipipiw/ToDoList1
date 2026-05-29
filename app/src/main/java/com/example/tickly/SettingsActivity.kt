package com.example.tickly

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream
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

        val btnExportDB = findViewById<Button>(R.id.btnExportDB)
        btnExportDB.setOnClickListener {
            exportDatabaseToPDF()
        }
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

    private fun exportDatabaseToPDF() {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val tableHeaderPaint = Paint()
        val linePaint = Paint()

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 1f
        linePaint.color = Color.BLACK

        tableHeaderPaint.textSize = 10f
        tableHeaderPaint.isFakeBoldText = true

        paint.textSize = 10f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header Utama
        titlePaint.textSize = 18f
        titlePaint.isFakeBoldText = true
        canvas.drawText("DATABASE REPORT: TICKLY", 50f, 50f, titlePaint)
        
        paint.color = Color.GRAY
        val dateString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateString", 50f, 70f, paint)
        paint.color = Color.BLACK

        var yPos = 110f
        
        // --- TABEL KATEGORI ---
        titlePaint.textSize = 14f
        canvas.drawText("Table: categories", 50f, yPos, titlePaint)
        yPos += 15f

        val catCols = arrayOf("ID", "NAME", "EMOJI", "COLOR")
        val catWidths = floatArrayOf(40f, 150f, 60f, 100f)
        var xPos = 50f

        // Header Tabel
        canvas.drawRect(50f, yPos, 50f + catWidths.sum(), yPos + 20f, linePaint)
        for (i in catCols.indices) {
            canvas.drawText(catCols[i], xPos + 5f, yPos + 14f, tableHeaderPaint)
            xPos += catWidths[i]
        }
        yPos += 20f

        val categories = dbHelper.getAllCategoriesFull()
        for (cat in categories) {
            xPos = 50f
            canvas.drawRect(50f, yPos, 50f + catWidths.sum(), yPos + 20f, linePaint)
            
            canvas.drawText(cat["id"] ?: "", xPos + 5f, yPos + 14f, paint)
            xPos += catWidths[0]
            canvas.drawText(cat["name"] ?: "", xPos + 5f, yPos + 14f, paint)
            xPos += catWidths[1]
            canvas.drawText(cat["emoji"] ?: "", xPos + 5f, yPos + 14f, paint)
            xPos += catWidths[2]
            canvas.drawText(cat["color"] ?: "", xPos + 5f, yPos + 14f, paint)
            
            yPos += 20f
        }

        yPos += 40f

        // --- TABEL TUGAS ---
        if (yPos > 750f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
        }

        titlePaint.textSize = 14f
        canvas.drawText("Table: tasks", 50f, yPos, titlePaint)
        yPos += 15f

        val taskCols = arrayOf("ID", "TITLE", "DATE", "CAT", "STATUS")
        val taskWidths = floatArrayOf(30f, 180f, 80f, 100f, 80f)
        
        // Header Tabel
        xPos = 50f
        canvas.drawRect(50f, yPos, 50f + taskWidths.sum(), yPos + 20f, linePaint)
        for (i in taskCols.indices) {
            canvas.drawText(taskCols[i], xPos + 5f, yPos + 14f, tableHeaderPaint)
            xPos += taskWidths[i]
        }
        yPos += 20f

        val tasks = dbHelper.getAllTasksRaw()
        for (task in tasks) {
            if (yPos > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
                
                // Redraw Header on new page
                xPos = 50f
                canvas.drawRect(50f, yPos, 50f + taskWidths.sum(), yPos + 20f, linePaint)
                for (i in taskCols.indices) {
                    canvas.drawText(taskCols[i], xPos + 5f, yPos + 14f, tableHeaderPaint)
                    xPos += taskWidths[i]
                }
                yPos += 20f
            }

            xPos = 50f
            canvas.drawRect(50f, yPos, 50f + taskWidths.sum(), yPos + 20f, linePaint)
            
            canvas.drawText(task.id.toString(), xPos + 5f, yPos + 14f, paint)
            xPos += taskWidths[0]
            
            val titleTrim = if (task.title.length > 30) task.title.substring(0, 27) + "..." else task.title
            canvas.drawText(titleTrim, xPos + 5f, yPos + 14f, paint)
            xPos += taskWidths[1]
            
            canvas.drawText(task.date, xPos + 5f, yPos + 14f, paint)
            xPos += taskWidths[2]
            
            canvas.drawText(task.category, xPos + 5f, yPos + 14f, paint)
            xPos += taskWidths[3]
            
            canvas.drawText(if (task.isCompleted) "Done" else "Pending", xPos + 5f, yPos + 14f, paint)
            
            yPos += 20f
        }

        pdfDocument.finishPage(page)
        savePdfToDownloads(pdfDocument)
    }

    private fun savePdfToDownloads(pdfDocument: PdfDocument) {
        val fileName = "Tickly_Export_${System.currentTimeMillis()}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val outputStream: OutputStream? = resolver.openOutputStream(it)
                    outputStream?.let { os ->
                        pdfDocument.writeTo(os)
                        os.close()
                        Toast.makeText(this, "PDF berhasil disimpan di folder Download", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val targetFile = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                val os = java.io.FileOutputStream(targetFile)
                pdfDocument.writeTo(os)
                os.close()
                Toast.makeText(this, "PDF berhasil disimpan: ${targetFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
