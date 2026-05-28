package com.example.tickly

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var taskList: List<Task>,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskLongClicked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvTaskDesc)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvTaskItemDate) // Daftarkan ID tanggal baru
        val cbStatus: CheckBox = itemView.findViewById(R.id.cbTaskStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.tvTitle.text = task.title
        holder.tvDesc.text = task.description
        holder.tvCategory.text = task.category
        holder.tvDate.text = "🗓️ ${task.date}" // Masukkan data tanggal dari SQLite ke UI

        holder.cbStatus.setOnCheckedChangeListener(null)
        holder.cbStatus.isChecked = task.isCompleted

        // Logika coret teks jika status selesai
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(0xFF9B9B9B.toInt())
            holder.tvDate.setTextColor(0xFF9B9B9B.toInt()) // Tanggal ikut samar saat selesai
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(0xFF4A4A4A.toInt())
            holder.tvDate.setTextColor(0xFF9B9B9B.toInt())
        }

        holder.cbStatus.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task, isChecked)
        }

        holder.itemView.setOnLongClickListener {
            onTaskLongClicked(task)
            true
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newTasks: List<Task>) {
        taskList = newTasks
        notifyDataSetChanged()
    }
}