package com.sinukeapps.chat.mydeepseekchat.model

enum class MessageType(val title: String) {
    ERROR("Error"),
    REQUEST("Me"),
    RESPONSE("ChatBot Response"),
    THINKING("Thinking...")
}
