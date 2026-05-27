package com.example.todolist

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    private lateinit var txtTotal: TextView
    private lateinit var txtPending: TextView
    private lateinit var txtDone: TextView

    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerTask)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        txtTotal = findViewById(R.id.txtTotal)
        txtPending = findViewById(R.id.txtPending)
        txtDone = findViewById(R.id.txtDone)

        adapter = TaskAdapter(taskList) {
            updateStats()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            showAddDialog()
        }

        updateStats()
    }

    private fun showAddDialog() {

        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)

        val edtTask = view.findViewById<EditText>(R.id.edtTask)
        val edtDeadline = view.findViewById<EditText>(R.id.edtDeadline)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val calendar = Calendar.getInstance()

        edtDeadline.setOnClickListener {

            DatePickerDialog(
                this,
                { _, year, month, day ->

                    calendar.set(year, month, day)

                    TimePickerDialog(
                        this,
                        { _, hour, minute ->

                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)

                            val format = SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                Locale.getDefault()
                            )

                            edtDeadline.setText(format.format(calendar.time))

                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        btnSave.setOnClickListener {

            val title = edtTask.text.toString()
            val deadline = edtDeadline.text.toString()

            if (title.isNotEmpty()) {

                taskList.add(
                    Task(title, deadline, false)
                )

                adapter.notifyDataSetChanged()

                updateStats()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateStats() {

        val total = taskList.size
        val done = taskList.count { it.isDone }
        val pending = total - done

        txtTotal.text = total.toString()
        txtDone.text = done.toString()
        txtPending.text = pending.toString()
    }
}