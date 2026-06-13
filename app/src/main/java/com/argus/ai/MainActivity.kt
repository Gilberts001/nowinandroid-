package com.argus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArgusApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArgusApp() {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf("ARGUS: Online. How can I assist, Boss Gilbert?")) }
    var input by remember { mutableStateOf("") }
    
    val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("ARGUS V1") }) }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().padding(16.dp)
            ) {
                LazyColumn(Modifier.weight(1f)) {
                    items(messages) { Text(it, Modifier.padding(4.dp)) }
                }
                Row {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask ARGUS...") }
                    )
                    Button(onClick = {
                        if (input.isBlank()) return@Button
                        val userMsg = "YOU: $input"
                        messages = messages + userMsg
                        val prompt = input
                        input = ""
                        scope.launch {
                            try {
                                val response = askGroq(client, prompt)
                                messages = messages + "ARGUS: $response"
                            } catch (e: Exception) {
                                messages = messages + "ARGUS: Error - ${e.message}"
                            }
                        }
                    }) { Text("Send") }
                }
            }
        }
    }
}

suspend fun askGroq(client: HttpClient, prompt: String): String {
    val apiKey = BuildConfig.GROQ_API_KEY
    if (apiKey.isBlank()) return "Groq API key missing. Check Secrets."
    
    val response: GroqResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        contentType(ContentType.Application.Json)
        setBody(GroqRequest(
            model = "llama-3.1-8b-instant",
            messages = listOf(Message("user", prompt))
        ))
    }.body()
    
    return response.choices.firstOrNull()?.message?.content ?: "No response"
}

@Serializable
data class GroqRequest(val model: String, val messages: List<Message>)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class GroqResponse(val choices: List<Choice>)

@Serializable
data class Choice(val message: Message)
