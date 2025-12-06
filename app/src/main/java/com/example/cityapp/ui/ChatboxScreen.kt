package com.example.cityapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.models.Chat
import com.example.cityapp.models.Message
import com.example.cityapp.models.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ------------------- VIEWMODEL -------------------
// Deze class bevat nu alle datalogica voor een specifieke chat
class ChatViewModel(
    private val chatId: String,
    private val currentUserId: String,
    private val otherUserId: String
) : ViewModel() {

    private val db = Firebase.firestore
    private val chatRef = db.collection("chats").document(chatId)
    private val messagesRef = chatRef.collection("messages")

    // StateFlow om de berichtenlijst vast te houden en te observeren
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    init {
        // Zodra de ViewModel wordt aangemaakt, start het luisteren naar berichten
        // en zorgt het ervoor dat de chat bestaat.
        ensureChatExists()
        listenForMessages()
    }

    private fun ensureChatExists() {
        chatRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                // Als de chat niet bestaat, maak hem dan aan.
                val chat = Chat(id = chatId, users = listOf(currentUserId, otherUserId))
                chatRef.set(chat)
            }
        }
    }

    private fun listenForMessages() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log de fout voor debugging
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Zet de documenten om naar Message-objecten
                    _messages.value = snapshot.toObjects(Message::class.java)
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val newMessage = Message(
            senderId = currentUserId,
            text = text,
            timestamp = Timestamp.now()
        )
        // Voeg het nieuwe bericht toe aan de database
        messagesRef.add(newMessage)
            .addOnFailureListener {
                // Log de fout als het versturen mislukt
            }
    }
}

// ------------------- COMPOSABLES -------------------

// Functie om chatId te genereren (onveranderd)
fun getChatId(user1: String, user2: String): String {
    return listOf(user1, user2).sorted().joinToString("_")
}

@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    currentUserId: String,
    startChatWithUserId: String? = null
) {
    var selectedChat by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(startChatWithUserId) {
        if (startChatWithUserId != null) {
            val chatId = getChatId(currentUserId, startChatWithUserId)
            selectedChat = chatId to startChatWithUserId
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedChat == null) {
            UserListScreen(currentUserId) { chatId, otherUserId ->
                selectedChat = chatId to otherUserId
            }
        } else {
            ChatScreen(
                chatId = selectedChat!!.first,
                currentUserId = currentUserId,
                otherUserId = selectedChat!!.second,
                onBack = { selectedChat = null }
            )
        }
    }
}

// UserListScreen code blijft hetzelfde (ik voeg het hier toe voor volledigheid)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    currentUserId: String,
    onUserSelected: (chatId: String, otherUserId: String) -> Unit
) {
    val db = Firebase.firestore
    var allUsers by remember { mutableStateOf<List<Pair<String, User>>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                allUsers = result.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    val userId = doc.id
                    if (user != null && userId != currentUserId) {
                        userId to user
                    } else null
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start een gesprek") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (allUsers.isEmpty()) {
                item { Text("Geen andere gebruikers gevonden.", modifier = Modifier.padding(16.dp)) }
            } else {
                item { Text("Selecteer een persoon om een chat te starten", fontSize = 16.sp) }

                items(allUsers) { (userId, user) ->
                    val chatId = getChatId(currentUserId, userId)
                    Card(
                        onClick = { onUserSelected(chatId, userId) },
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${user.firstName} ${user.lastName}".trim(), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// Helper om de ViewModel met argumenten aan te maken
@Composable
fun rememberChatViewModel(chatId: String, currentUserId: String, otherUserId: String): ChatViewModel {
    // Gebruik remember om de ViewModel slechts één keer per chat te maken
    return remember(chatId) {
        ChatViewModel(chatId, currentUserId, otherUserId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    currentUserId: String,
    otherUserId: String,
    onBack: () -> Unit
) {
    // 1. INITIALISEER DE VIEWMODEL EN HAAL DE STATE OP
    val viewModel = rememberChatViewModel(chatId, currentUserId, otherUserId)
    val messages by viewModel.messages.collectAsState()

    // 2. OUDE STATE VARIABELEN VERWIJDERD OF BEHOUDEN VOOR UI
    var otherUserName by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var newMessage by remember { mutableStateOf("") }

    // Deze logica blijft in de UI, omdat het puur om weergave gaat
    LaunchedEffect(otherUserId) {
        Firebase.firestore.collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    otherUserName = "${doc.getString("firstName").orEmpty()} ${doc.getString("lastName").orEmpty()}".trim()
                }
            }
    }

    // Scroll naar het nieuwste bericht bij update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (otherUserName.isNotBlank()) "Chat met $otherUserName" else "Laden...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false, // Zorgt dat de lijst bovenaan begint
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isCurrentUser = message.senderId == currentUserId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isCurrentUser) 64.dp else 0.dp,
                                end = if (isCurrentUser) 0.dp else 64.dp
                            )
                    ) {
                        Card(
                            modifier = Modifier.align(if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        ) {
                            Text(
                                message.text,
                                color = if (isCurrentUser) Color.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Inputveld en verzendknop
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Typ een bericht...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        // ROEP NU DE VIEWMODEL FUNCTIE AAN
                        viewModel.sendMessage(newMessage)
                        newMessage = ""
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    enabled = newMessage.isNotBlank()
                ) {
                    Text("Verstuur")
                }
            }
        }
    }
}
