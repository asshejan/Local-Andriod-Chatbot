package com.sinukeapps.chat.mydeepseekchat.api

import com.sinukeapps.chat.mydeepseekchat.model.ChatBotRequest
import com.sinukeapps.chat.mydeepseekchat.model.ChatBotResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatBotApi {

    @POST("api/generate")
    fun generate(@Body request: ChatBotRequest): Call<ChatBotResponse>

}
