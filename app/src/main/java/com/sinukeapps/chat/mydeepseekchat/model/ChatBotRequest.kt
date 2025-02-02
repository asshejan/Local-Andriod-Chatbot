package com.sinukeapps.chat.mydeepseekchat.model

data class ChatBotRequest(val model: String, val stream: Boolean, val prompt: String)