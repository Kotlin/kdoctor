package com.example.template

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform