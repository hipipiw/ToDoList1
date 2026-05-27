package com.example.todolist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onUpdate: () -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtTask = view.findViewById<TextView>(R.id.txtTaskName)
        val txtDeadline = view.findViewById<TextView>(R.id.txtDeadline)
        val checkTask = view.findViewById<CheckBox>(R.id.checkTask)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)

        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {

        val task = taskList[position]

        holder.txtTask.text = task.title
        holder.txtDeadline.text = "Deadline: ${task.deadline}"

        holder.checkTask.setOnCheckedChangeListener(null)
        holder.checkTask.isChecked = task.isDone

        if (task.isDone) {
            holder.txtTask.paintFlags =
                holder.txtTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.txtTask.paintFlags =
                holder.txtTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.checkTask.setOnCheckedChangeListener { _, isChecked ->

            task.isDone = isChecked

            notifyItemChanged(position)

            onUpdate()
        }

        holder.btnDelete.setOnClickListener {

            taskList.removeAt(position)

            notifyDataSetChanged()

            onUpdate()
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }
}