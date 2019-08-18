package com.smart.upolicia.data.model

data class User(
    val id_usuario: Int,
    val nombre1: String,
    val nombre2: String,
    val apellido1: String,
    val apellido2: String,
    val fecha_nacimiento: String,
    val cedula: String,
    val telefono: String,
    val correo: String,
    val imagen: String
)