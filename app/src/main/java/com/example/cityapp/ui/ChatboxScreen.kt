package com.example.cityapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.models.Chat
import com.example.cityapp.models.Message
import com.example.cityapp.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ------------------- VIEWMODELS -------------------
class ChatViewModel(
    private val chatId: String,
    private val otherUserId: String
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private val messagesRef = db.collection("chats").document(chatId).collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    init {
        listenForMessages()
        markMessagesAsRead()
    }

    private fun markMessagesAsRead() {
        messagesRef
            .whereEqualTo("read", false)
            .whereEqualTo("senderId", otherUserId)
            .whereArrayContains("usersInChat", currentUserId)  // TOEGEVOEGD: scoped de query voor security rules
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener
                android.util.Log.d("ChatViewModel", "Marking ${snapshot.size()} messages as read.")
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }
                batch.commit().addOnFailureListener { e ->
                    android.util.Log.e("ChatViewModel", "Mark as read failed", e)
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("ChatViewModel", "Query for unread failed", e)
            }
    }

    private fun listenForMessages() {
        messagesRef
            .whereArrayContains("usersInChat", currentUserId)  // TOEGEVOEGD: scoped de query voor security rules
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _messages.value = snapshot.toObjects(Message::class.java)
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val newMessage = Message(
            senderId = currentUserId,
            text = text,
            timestamp = Timestamp.now(),
            read = false,
            usersInChat = listOf(currentUserId, otherUserId)
        )
        messagesRef.add(newMessage)
    }
}

class UnreadSendersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    private val _unreadSenders = MutableStateFlow<Set<String>>(emptySet())
    val unreadSenders = _unreadSenders.asStateFlow()

    init {
        listenForUnreadMessages()
    }

    private fun listenForUnreadMessages() {
        listener?.remove() // Stop oude listener
        val uid = auth.currentUser?.uid ?: return

        listener = db.collectionGroup("messages")
            .whereEqualTo("read", false)
            .whereArrayContains("usersInChat", uid)
            // VERWIJDERD: .whereNotEqualTo("senderId", uid) -- dit veroorzaakt issues met security validation
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("UnreadSendersViewModel", "Listen failed", e)
                    return@addSnapshotListener
                }
                val senders = snapshot?.documents?.mapNotNull { it.getString("senderId") }
                    ?.filter { it != uid }  // TOEGEVOEGD: filter eigen ID in code (vervangt de verwijderde whereNotEqualTo)
                    ?.toSet() ?: emptySet()
                _unreadSenders.value = senders
            }
    }

    fun refresh() {
        listenForUnreadMessages()
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}

// ------------------- COMPOSABLES -------------------
fun getChatId(user1: String, user2: String): String {
    return listOf(user1, user2).sorted().joinToString("_")
}

@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    startChatWithUserId: String? = null
) {
    var selectedChat by remember { mutableStateOf<Pair<String, String>?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val unreadViewModel: UnreadSendersViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Functie die een chat aanmaakt (of update) VOORDAT we navigeren
    fun selectAndCreateChat(chatId: String, otherId: String) {
        scope.launch {
            isLoading = true
            try {
                val chatRef = FirebaseFirestore.getInstance().collection("chats").document(chatId)
                val chat = Chat(id = chatId, users = listOf(currentUserId, otherId))
                // SetOptions.merge() maakt het document aan als het niet bestaat,
                // en overschrijft niets als het wel al bestaat.
                chatRef.set(chat, SetOptions.merge()).await()
                selectedChat = chatId to otherId
            } catch (e: Exception) {
                android.util.Log.e("ChatboxScreen", "Failed to create/ensure chat exists", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(startChatWithUserId) {
        if (startChatWithUserId != null && selectedChat == null) {
            val chatId = getChatId(currentUserId, startChatWithUserId)
            selectAndCreateChat(chatId, startChatWithUserId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedChat == null) {
            UserListScreen(
                currentUserId = currentUserId,
                unreadViewModel = unreadViewModel,
                onUserSelected = { chatId, otherUserId ->
                    selectAndCreateChat(chatId, otherUserId)
                }
            )
        } else {
            ChatScreen(
                chatId = selectedChat!!.first,
                otherUserId = selectedChat!!.second,
                onBack = {
                    unreadViewModel.refresh() // Essentieel: forceer update van de badges
                    selectedChat = null
                }
            )
        }
        // Toon een lader terwijl de chat wordt aangemaakt
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    currentUserId: String,
    unreadViewModel: UnreadSendersViewModel,
    onUserSelected: (chatId: String, otherUserId: String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var allUsers by remember { mutableStateOf<List<Pair<String, User>>>(emptyList()) }
    val unreadSenders by unreadViewModel.unreadSenders.collectAsState()

    LaunchedEffect(Unit) {
        db.collection("users").get().addOnSuccessListener { result ->
            allUsers = result.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                val userId = doc.id
                if (user != null && userId != currentUserId) userId to user else null
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allUsers, key = { it.first }) { (userId, user) ->
                val chatId = getChatId(currentUserId, userId)
                val hasUnread = unreadSenders.contains(userId)

                Card(
                    onClick = { onUserSelected(chatId, userId) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${user.firstName} ${user.lastName}".trim(),
                            fontSize = 16.sp,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                        )
                        if (hasUnread) {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberChatViewModel(chatId: String, otherUserId: String): ChatViewModel {
    return remember(chatId) {
        ChatViewModel(chatId, otherUserId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUserId: String,
    onBack: () -> Unit
) {
    val viewModel = rememberChatViewModel(chatId, otherUserId)
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var newMessage by remember { mutableStateOf("") }
    var otherUserName by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

    LaunchedEffect(otherUserId) {
        FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                otherUserName = "${doc.getString("firstName").orEmpty()} ${doc.getString("lastName").orEmpty()}".trim()
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.timestamp.toString() + it.senderId }) { message ->
                    val isCurrentUser = message.senderId == currentUserId
                    Box(modifier = Modifier.fillMaxWidth().padding(start = if (isCurrentUser) 64.dp else 0.dp, end = if (isCurrentUser) 0.dp else 64.dp)) {
                        Card(
                            modifier = Modifier.align(if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.LightGray)
                        ) {
                            Text(message.text, color = if (isCurrentUser) Color.White else Color.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Typ een bericht...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.sendMessage(newMessage); newMessage = "" }, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(24.dp), enabled = newMessage.isNotBlank()) {
                    Text("Verstuur")
                }
            }
        }
    }
}