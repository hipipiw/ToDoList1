package com.example.tickly

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTaskStats: TextView
    private lateinit var layoutCategoriesBar: LinearLayout
    private lateinit var tvPercentageBadge: TextView
    private lateinit var tvSelectedDateDisplay: TextView
    private lateinit var containerKalender: View
    private lateinit var scrollCategories: View

    // Komponen Custom Kalender
    private lateinit var gridViewCalendar: GridView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private var currentCalendar = Calendar.getInstance()

    private lateinit var btnTabTugas: androidx.appcompat.widget.AppCompatButton
    private lateinit var btnTabKalender: androidx.appcompat.widget.AppCompatButton
    private lateinit var btnTambahTugas: androidx.appcompat.widget.AppCompatButton

    // Fitur Hide/Clear Tugas Selesai
    private lateinit var btnClearSelesai: androidx.appcompat.widget.AppCompatButton
    private var isCompletedHidden = false

    private lateinit var rvActiveTasks: RecyclerView
    private lateinit var rvCompletedTasks: RecyclerView
    private lateinit var activeAdapter: TaskAdapter
    private lateinit var completedAdapter: TaskAdapter

    private var isCalendarMode = false
    private var selectedDateString: String? = null
    private var selectedCategoryFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        tvPercentageBadge = findViewById(R.id.tvPercentageBadge)
        btnTabTugas = findViewById(R.id.btnTabTugas)
        btnTabKalender = findViewById(R.id.btnTabKalender)
        btnTambahTugas = findViewById(R.id.btnTambahTugas)
        tvTaskStats = findViewById(R.id.tvTaskStats)
        layoutCategoriesBar = findViewById(R.id.layoutCategoriesBar)
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay)
        containerKalender = findViewById(R.id.containerKalender)
        scrollCategories = findViewById(R.id.scrollCategories)
        btnClearSelesai = findViewById(R.id.btnClearSelesai)

        gridViewCalendar = findViewById(R.id.calendarGridView)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        rvActiveTasks = findViewById(R.id.rvActiveTasks)
        rvCompletedTasks = findViewById(R.id.rvCompletedTasks)

        rvActiveTasks.layoutManager = LinearLayoutManager(this)
        activeAdapter = TaskAdapter(emptyList(),
            onTaskChecked = { task, isChecked -> handleTaskCheck(task, isChecked) },
            onTaskLongClicked = { task -> showDeleteDialog(task) }
        )
        rvActiveTasks.adapter = activeAdapter

        rvCompletedTasks.layoutManager = LinearLayoutManager(this)
        completedAdapter = TaskAdapter(emptyList(),
            onTaskChecked = { task, isChecked -> handleTaskCheck(task, isChecked) },
            onTaskLongClicked = { task -> showDeleteDialog(task) }
        )
        rvCompletedTasks.adapter = completedAdapter

        refreshCategoryBar()
        loadTasks()
        setupCustomCalendar()

        // Klik Logo untuk berpindah ke Halaman Settings
        val imgLogo = findViewById<ImageView>(R.id.logo)
        imgLogo.setOnClickListener {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Klik Button Clear/Show untuk menyembunyikan tugas selesai
        btnClearSelesai.setOnClickListener {
            isCompletedHidden = !isCompletedHidden
            if (isCompletedHidden) {
                btnClearSelesai.text = "Show"
                btnClearSelesai.setTextColor(ContextCompat.getColor(this, R.color.pink_primary))
            } else {
                btnClearSelesai.text = "Clear"
                btnClearSelesai.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
            loadTasks()
        }

        btnTabTugas.setOnClickListener {
            isCalendarMode = false
            selectedDateString = null
            containerKalender.visibility = View.GONE
            scrollCategories.visibility = View.VISIBLE
            tvSelectedDateDisplay.text = "Semua Tugas"

            btnTabTugas.setBackgroundResource(R.drawable.bg_tab_indicator)
            btnTabTugas.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabKalender.setBackgroundResource(android.R.color.transparent)
            btnTabKalender.setTextColor(ContextCompat.getColor(this, R.color.text_light))

            loadTasks()
        }

        btnTabKalender.setOnClickListener {
            isCalendarMode = true
            containerKalender.visibility = View.VISIBLE
            scrollCategories.visibility = View.GONE

            btnTabKalender.setBackgroundResource(R.drawable.bg_tab_indicator)
            btnTabKalender.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabTugas.setBackgroundResource(android.R.color.transparent)
            btnTabTugas.setTextColor(ContextCompat.getColor(this, R.color.text_light))

            val today = Calendar.getInstance()
            selectedDateString = "${today.get(Calendar.DAY_OF_MONTH)}/${today.get(Calendar.MONTH) + 1}/${today.get(Calendar.YEAR)}"
            val displaySdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            tvSelectedDateDisplay.text = displaySdf.format(today.time)

            setupCustomCalendar()
            loadTasks()
        }

        btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            setupCustomCalendar()
        }

        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            setupCustomCalendar()
        }

        btnTambahTugas.setOnClickListener { showAddTaskDialog() }
    }

    override fun onResume() {
        super.onResume()
        refreshCategoryBar()
        loadTasks()
        setupCustomCalendar()
    }

    private fun handleTaskCheck(task: Task, isChecked: Boolean) {
        dbHelper.updateTaskStatus(task.id, isChecked)
        loadTasks()
        setupCustomCalendar()
    }

    private fun loadTasks() {
        val allActive = dbHelper.getTasks(isCompleted = false, dateFilter = selectedDateString)
        val allCompleted = dbHelper.getTasks(isCompleted = true, dateFilter = selectedDateString)

        val filteredActive = if (selectedCategoryFilter == null) allActive else allActive.filter { it.category == selectedCategoryFilter }
        val filteredCompleted = if (selectedCategoryFilter == null) allCompleted else allCompleted.filter { it.category == selectedCategoryFilter }

        activeAdapter.updateData(filteredActive)

        // Menyembunyikan data jika status pengontrol aktif
        if (isCompletedHidden) {
            completedAdapter.updateData(emptyList())
        } else {
            completedAdapter.updateData(filteredCompleted)
        }

        tvTaskStats.text = "${filteredActive.size} tugas tersisa · ${filteredCompleted.size} selesai"

        val globalActive = dbHelper.getTasks(isCompleted = false).size
        val globalCompleted = dbHelper.getTasks(isCompleted = true).size
        val total = globalActive + globalCompleted
        val progressPercent = if (total > 0) (globalCompleted * 100) / total else 0
        tvPercentageBadge.text = "🌸 $progressPercent% done"
    }

    private fun setupCustomCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvMonthYear.text = sdf.format(currentCalendar.time)

        val daysList = ArrayList<String>()
        val monthCalendar = currentCalendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayIndex = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1
        for (i in 0 until firstDayIndex) {
            daysList.add("")
        }

        val maxDays = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthFix = monthCalendar.get(Calendar.MONTH) + 1
        val yearFix = monthCalendar.get(Calendar.YEAR)

        for (i in 1..maxDays) {
            daysList.add("$i/$monthFix/$yearFix")
        }

        val calendarAdapter = CalendarAdapter(this, daysList, dbHelper, selectedDateString)
        gridViewCalendar.adapter = calendarAdapter

        gridViewCalendar.setOnItemClickListener { _, _, position, _ ->
            val clickedDate = daysList[position]
            if (clickedDate.isNotEmpty()) {
                selectedDateString = clickedDate

                val parts = clickedDate.split("/")
                val clickCal = Calendar.getInstance()
                clickCal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                val displaySdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                tvSelectedDateDisplay.text = displaySdf.format(clickCal.time)

                setupCustomCalendar()
                loadTasks()
            }
        }
    }

    private fun refreshCategoryBar() {
        layoutCategoriesBar.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val allChip = inflater.inflate(R.layout.item_category_chip, layoutCategoriesBar, false) as TextView
        allChip.text = "🌟 Semua"
        if (selectedCategoryFilter == null) {
            allChip.setBackgroundResource(R.drawable.bg_rounded_button)
            allChip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        allChip.setOnClickListener {
            selectedCategoryFilter = null
            refreshCategoryBar()
            loadTasks()
        }
        layoutCategoriesBar.addView(allChip)

        val categoriesWithDetails = dbHelper.getAllCategoriesDetailed()
        for (cat in categoriesWithDetails) {
            val catName = cat.first
            val catEmoji = cat.second
            val catColor = cat.third

            val chip = inflater.inflate(R.layout.item_category_chip, layoutCategoriesBar, false) as TextView
            chip.text = "$catEmoji $catName"

            if (selectedCategoryFilter == catName) {
                chip.setBackgroundResource(R.drawable.bg_rounded_button)
                chip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(catColor))
                chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                chip.backgroundTintList = null
                chip.setTextColor(ContextCompat.getColor(this, R.color.category_text))
            }

            chip.setOnClickListener {
                selectedCategoryFilter = if (selectedCategoryFilter == catName) null else catName
                refreshCategoryBar()
                loadTasks()
            }
            layoutCategoriesBar.addView(chip)
        }

        val addChip = inflater.inflate(R.layout.item_category_chip, layoutCategoriesBar, false) as TextView
        addChip.text = "➕ Kategori"
        addChip.setTextColor(ContextCompat.getColor(this, R.color.pink_primary))
        addChip.setOnClickListener { showAddCategoryDialog() }
        layoutCategoriesBar.addView(addChip)
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvPreview = dialogView.findViewById<TextView>(R.id.tvCategoryPreview)
        val etNewCategory = dialogView.findViewById<EditText>(R.id.etNewCategory)
        val gridEmojis = dialogView.findViewById<GridLayout>(R.id.gridEmojis)
        val layoutColorsRow = dialogView.findViewById<LinearLayout>(R.id.layoutColorsRow)

        var currentEmoji = "🎯"
        var currentColorHex = "#D87093"

        val emojis = listOf(
            "🎯", "🎨", "🏋️", "🍳", "🌿", "🐾",
            "✈️", "🎵", "📷", "💡", "🔧", "🧠",
            "🎮", "🌙", "⚡", "🦋", "🌺", "🌈"
        )

        val colors = listOf(
            "#D87093", "#9575CD", "#4DB6AC", "#FF8A65",
            "#64B5F6", "#D4AF37", "#E57373", "#4DD0E1",
            "#BA68C8", "#9CCC65", "#FFD54F", "#FF7043"
        )

        tvPreview.setOnClickListener {
            val inputEdit = EditText(this).apply {
                hint = "Ketik emoji..."
                filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                gravity = android.view.Gravity.CENTER
            }
            AlertDialog.Builder(this)
                .setTitle("Emoji Kustom ⌨️")
                .setMessage("Ketik 1 emoji pilihanmu langsung dari keyboard HP:")
                .setView(inputEdit)
                .setPositiveButton("Pilih") { d, _ ->
                    val typed = inputEdit.text.toString().trim()
                    if (typed.isNotEmpty()) {
                        currentEmoji = typed
                        tvPreview.text = typed
                    }
                    d.dismiss()
                }
                .setNegativeButton("Batal") { d, _ -> d.dismiss() }
                .show()
        }

        for (emoji in emojis) {
            val tvEmoji = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = android.view.Gravity.CENTER
                setPadding(12, 12, 12, 12)
                isClickable = true
                setOnClickListener {
                    currentEmoji = emoji
                    tvPreview.text = emoji
                }
            }
            gridEmojis.addView(tvEmoji)
        }

        val density = resources.displayMetrics.density
        val sizeInDp = (32 * density).toInt()
        val marginInDp = (6 * density).toInt()

        for (colorHex in colors) {
            val colorCircle = View(this).apply {
                val params = LinearLayout.LayoutParams(sizeInDp, sizeInDp).apply {
                    setMargins(marginInDp, 0, marginInDp, 0)
                }
                layoutParams = params
                setBackgroundResource(R.drawable.bg_circle)
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorHex))

                setOnClickListener {
                    currentColorHex = colorHex
                    tvPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorHex))
                }
            }
            layoutColorsRow.addView(colorCircle)
        }

        dialogView.findViewById<TextView>(R.id.tvCloseDialog).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnSimpanKategori).setOnClickListener {
            val newCatName = etNewCategory.text.toString().trim()
            if (newCatName.isNotEmpty()) {
                dbHelper.addCategoryWithDetails(newCatName, currentEmoji, currentColorHex)
                Toast.makeText(this, "Kategori $currentEmoji $newCatName berhasil dibuat!", Toast.LENGTH_SHORT).show()
                refreshCategoryBar()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Nama kategori tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Tugas")
            .setMessage("Apakah Anda yakin ingin menghapus tugas '${task.title}'?")
            .setPositiveButton("Hapus") { dialog, _ ->
                dbHelper.deleteTask(task.id)
                loadTasks()
                setupCustomCalendar()
                Toast.makeText(this, "Tugas dihapus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val etDesc = dialogView.findViewById<EditText>(R.id.etTaskDesc)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvTaskDate)

        val categoriesDetailed = dbHelper.getAllCategoriesDetailed()
        val spinnerList = categoriesDetailed.map { "${it.second} ${it.first}" }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinnerList)
        spinnerCategory.adapter = adapter

        var finalSelectedDate = ""
        if (isCalendarMode && selectedDateString != null) {
            finalSelectedDate = selectedDateString!!
            tvDate.text = finalSelectedDate
        }

        tvDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val monthFix = selectedMonth + 1
                finalSelectedDate = "$selectedDay/$monthFix/$selectedYear"
                tvDate.text = finalSelectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.show()
        }

        dialogView.findViewById<TextView>(R.id.tvCloseTaskDialog).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnSimpan).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            val selectedFullString = spinnerCategory.selectedItem?.toString() ?: "🎯 Umum"
            val selectedCat = if (selectedFullString.contains(" ")) selectedFullString.split(" ", limit = 2)[1] else selectedFullString

            if (title.isEmpty() || finalSelectedDate.isEmpty()) {
                Toast.makeText(this, "Judul & Tanggal wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.addTask(title, desc, finalSelectedDate, selectedCat)
            loadTasks()
            setupCustomCalendar()
            dialog.dismiss()
        }
        dialog.show()
    }
}