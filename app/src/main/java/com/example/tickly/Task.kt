package com.example.tickly

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    var isCompleted: Boolean
)