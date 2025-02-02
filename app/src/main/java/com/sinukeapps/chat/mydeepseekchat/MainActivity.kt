package com.sinukeapps.chat.mydeepseekchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sinukeapps.chat.mydeepseekchat.api.ChatBotApi
import com.sinukeapps.chat.mydeepseekchat.model.ChatBotRequest
import com.sinukeapps.chat.mydeepseekchat.model.ChatBotResponse
import com.sinukeapps.chat.mydeepseekchat.model.ChatMessage
import com.sinukeapps.chat.mydeepseekchat.model.MessageType
import com.sinukeapps.chat.mydeepseekchat.ui.theme.MyDeepSeekChatTheme
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var chatBotApi: ChatBotApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.182:31028")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        chatBotApi = retrofit.create(ChatBotApi::class.java)

        enableEdgeToEdge()
        setContent {
            MyDeepSeekChatTheme {
                Scaffold(modifier = Modifier.imePadding()) { innerPadding ->
                    ChatScreen(
                        chatBotApi = chatBotApi,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(chatBotApi: ChatBotApi, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var chat by remember { mutableStateOf(listOf<ChatMessage>()) }
    val chatState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isThinking by remember { mutableStateOf(false) }

        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(
                state = chatState,
                modifier = Modifier.weight(1f)
            ) {
                items(chat.size) { index ->
                    MessageCard(chat[index])
                }

                if (isThinking) {
                    item {
                        MessageCard(ChatMessage(MessageType.THINKING, ""))
                    }
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Please, ask something") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = {
                        isThinking = true
                        val promptNormalized = prompt.trim()
                        val request = ChatBotRequest("deepseek-r1:8b", false, promptNormalized)
                        chat = chat + ChatMessage(MessageType.REQUEST, promptNormalized)
                        prompt = ""
                        chatBotApi.generate(request).enqueue(object : Callback<ChatBotResponse> {
                            override fun onResponse(
                                call: Call<ChatBotResponse>,
                                response: Response<ChatBotResponse>
                            ) {
                                chat = if (response.isSuccessful) {
                                    chat + ChatMessage(
                                        MessageType.RESPONSE,
                                        response.body()?.response ?: "No answer"
                                    )
                                } else {
                                    chat + ChatMessage(
                                        MessageType.ERROR,
                                        "Error code: ${response.code()}, Message: ${response.message()}"
                                    )
                                }
                                isThinking = false
                                coroutineScope.launch {
                                    chatState.animateScrollToItem(chat.size - 1)
                                }
                            }

                            override fun onFailure(call: Call<ChatBotResponse>, t: Throwable) {
                                chat =
                                    chat + ChatMessage(
                                        MessageType.ERROR,
                                        "${t.message}"
                                    )
                                isThinking = false
                                coroutineScope.launch {
                                    chatState.animateScrollToItem(chat.size - 1)
                                }
                            }
                        })
                    },
                    enabled = !isThinking && prompt.trim().isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(56.dp)
                        .width(56.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = "Send"
                    )
                }
            }
        }
}

@Composable
fun MessageCard(message: ChatMessage) {
    var color = MaterialTheme.colorScheme.surfaceVariant
    var titleColor = MaterialTheme.colorScheme.primary

    if (message.type == MessageType.ERROR) {
        color = MaterialTheme.colorScheme.errorContainer
        titleColor = MaterialTheme.colorScheme.error
    } else if (message.type == MessageType.REQUEST) color = MaterialTheme.colorScheme.primaryContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message.type.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (message.type == MessageType.THINKING) DotsLoadingIndicator()
            else Text(text = message.message)
        }
    }
}

@Composable
fun DotsLoadingIndicator() {
    val dotCount = 3
    val dotSize = 8.dp
    val dotSpacing = 4.dp
    val animationDuration = 300

    val infiniteTransition = rememberInfiniteTransition()
    val dotAlpha = (0 until dotCount).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = animationDuration,
                    delayMillis = index * animationDuration / dotCount,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        dotAlpha.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                        shape = CircleShape
                    )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MyDeepSeekChatTheme {
        ChatScreen(chatBotApi = object : ChatBotApi {
            override fun generate(request: ChatBotRequest): Call<ChatBotResponse> {
                return object : Call<ChatBotResponse> {
                    override fun enqueue(callback: Callback<ChatBotResponse>) {}
                    override fun isExecuted(): Boolean = false
                    override fun clone(): Call<ChatBotResponse> = this
                    override fun isCanceled(): Boolean = false
                    override fun cancel() {}
                    override fun execute(): Response<ChatBotResponse> =
                        Response.success(ChatBotResponse("Hi, Android!"))

                    override fun request(): okhttp3.Request = okhttp3.Request.Builder().build()
                    override fun timeout(): Timeout {
                        TODO("Not yet implemented")
                    }
                }
            }
        })
    }
}
