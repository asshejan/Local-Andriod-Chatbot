package com.sinukeapps.chat.mydeepseekchat.model

data class ChatBotRequest(
    val model: String, 
    val prompt: String, 
    val stream: Boolean = false
)