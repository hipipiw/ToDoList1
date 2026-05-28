package com.example.tickly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CalendarAdapter(
    private val context: Context,
    private val daysList: List<String>,
    private val dbHelper: DatabaseHelper,
    private val selectedDate: String?
) : BaseAdapter() {

    override fun getCount(): Int = daysList.size
    override fun getItem(position: Int): Any = daysList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false)
        val tvDay = view.findViewById<TextView>(R.id.tvDayNumber)
        val viewDot = view.findViewById<View>(R.id.viewDot)

        val dateStr = daysList[position]
        if (dateStr.isNotEmpty()) {
            val dayNum = dateStr.split("/")[0]
            tvDay.text = dayNum

            // Logika sorotan (Highlight) jika tanggal ini sedang dipilih user
            if (dateStr == selectedDate) {
                tvDay.setBackgroundResource(R.drawable.bg_tab_indicator)
                tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                tvDay.background = null
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            }

            // HITUNG TUGAS DI SQLITE: Jika ada tugas belum selesai, tampilkan titik pink!
            val activeTasksForThisDay = dbHelper.getTasks(isCompleted = false, dateFilter = dateStr)
            if (activeTasksForThisDay.isNotEmpty()) {
                viewDot.visibility = View.VISIBLE
            } else {
                viewDot.visibility = View.INVISIBLE
            }
        } else {
            tvDay.text = ""
            viewDot.visibility = View.INVISIBLE
            tvDay.background = null
        }

        return view
    }
}