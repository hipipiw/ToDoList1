package com.example.todolist

data class Task(
    val title: String,
    val deadline: String,
    var isDone: Boolean
)