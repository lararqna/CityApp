package com.example.cityapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.R
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
import java.text.SimpleDateFormat
import java.util.*






class ChatListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var listener: ListenerRegistration? = null

    private val _chats = MutableStateFlow<List<Pair<Chat, User>>>(emptyList())
    val chats = _chats.asStateFlow()

    init {
        fetchChats()
    }

    private fun fetchChats() {
        if (currentUserId == null) return
        listener?.remove()

        listener = db.collection("chats")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("ChatListVM", "Listen failed", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    val chatUserPairs = snapshot.documents.mapNotNull { doc ->
                        val chat = doc.toObject(Chat::class.java) ?: return@mapNotNull null
                        val otherUserId = chat.users.firstOrNull { it != currentUserId } ?: return@mapNotNull null

                        val userDoc = db.collection("users").document(otherUserId).get().await()
                        val user = userDoc.toObject(User::class.java)?.copy(id = userDoc.id)

                        if (user != null) chat to user else null
                    }

                    val sortedList = chatUserPairs.sortedByDescending { (chat, _) ->
                        chat.lastMessageTimestamp ?: Timestamp.now()
                    }

                    _chats.value = sortedList
                }
            }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
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
        listener?.remove()
        val uid = auth.currentUser?.uid ?: return

        listener = db.collectionGroup("messages")
            .whereEqualTo("read", false)
            .whereArrayContains("usersInChat", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("UnreadSendersVM", "Listen failed", e)
                    return@addSnapshotListener
                }
                val senders = snapshot?.documents?.mapNotNull { it.getString("senderId") }
                    ?.filter { it != uid }
                    ?.toSet() ?: emptySet()
                _unreadSenders.value = senders
            }
    }

    // Forceer een update van de lijst met ongelezen berichten
    fun refresh() {
        listenForUnreadMessages()
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}


class ChatViewModel(
    private val chatId: String,
    private val otherUserId: String
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private val chatRef = db.collection("chats").document(chatId)
    private val messagesRef = chatRef.collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    init {
        listenForMessages()
        // MARKEREER BERICHTEN ALS GELEZEN BIJ HET OPENEN VAN DE CHAT
        markMessagesAsRead()
    }

    private fun listenForMessages() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                _messages.value = snapshot?.toObjects(Message::class.java) ?: emptyList()
            }
    }

    private fun markMessagesAsRead() {
        messagesRef
            .whereEqualTo("read", false)
            .whereEqualTo("senderId", otherUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }
                batch.commit().addOnFailureListener { e ->
                    android.util.Log.e("ChatViewModel", "Mark as read failed", e)
                }
            }
    }

    // In ChatViewModel:

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val newMessage = Message(
            senderId = currentUserId,
            text = text,
            timestamp = Timestamp.now(),
            usersInChat = listOf(currentUserId, otherUserId),
            read = false
        )
        messagesRef.add(newMessage)
        // Update de chat met de info van het laatste bericht
        chatRef.update(
            mapOf(
                "lastMessageText" to text,
                "lastMessageTimestamp" to newMessage.timestamp,
                // ESSENTIEEL: Stuur de sender ID mee
                "lastMessageSenderId" to currentUserId
            )
        )
    }

}


// ------------------- COMPOSABLES -------------------

fun getChatId(user1: String, user2: String): String {
    return listOf(user1, user2).sorted().joinToString("_")
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

@Composable
fun ChatboxScreen(
    modifier: Modifier = Modifier,
    startChatWithUserId: String? = null
) {
    val scope = rememberCoroutineScope()
    var selectedChat by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showUserSearch by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var isLoading by remember { mutableStateOf(false) }
    val unreadViewModel: UnreadSendersViewModel = viewModel()

    // Functie om een chat aan te maken of te zorgen dat deze bestaat, en dan te navigeren
    fun selectAndCreateChat(otherId: String) {
        if (isLoading) return

        scope.launch {
            isLoading = true
            try {
                val chatId = getChatId(currentUserId, otherId)
                val chatRef = FirebaseFirestore.getInstance().collection("chats").document(chatId)

                val chat = Chat(
                    id = chatId,
                    users = listOf(currentUserId, otherId),
                    lastMessageTimestamp = Timestamp.now()
                )

                chatRef.set(chat, SetOptions.merge()).await()

                selectedChat = chatId to otherId
                showUserSearch = false

            } catch (e: Exception) {
                android.util.Log.e("ChatboxScreen", "Failed to create/merge chat", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(startChatWithUserId) {
        if (startChatWithUserId != null && selectedChat == null) {
            selectAndCreateChat(startChatWithUserId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            selectedChat != null -> {
                ChatScreen(
                    chatId = selectedChat!!.first,
                    otherUserId = selectedChat!!.second,
                    onBack = {
                        // Ververs de badges wanneer je terugkomt uit de chat
                        unreadViewModel.refresh()
                        selectedChat = null
                    }
                )
            }
            showUserSearch -> {
                UserSearchScreen(
                    currentUserId = currentUserId,
                    onUserSelected = { otherUserId -> selectAndCreateChat(otherUserId) },
                    onBack = { showUserSearch = false }
                )
            }
            else -> {
                ChatListScreen(
                    unreadViewModel = unreadViewModel,
                    onChatSelected = { chatId, otherUserId ->
                        selectedChat = chatId to otherUserId
                    },
                    onSearchClick = { showUserSearch = true }
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (chatId: String, otherUserId: String) -> Unit,
    onSearchClick: () -> Unit,
    unreadViewModel: UnreadSendersViewModel = viewModel(),
    viewModel: ChatListViewModel = viewModel()
) {
    val chatsWithUsers by viewModel.chats.collectAsState()
    val unreadSenders by unreadViewModel.unreadSenders.collectAsState()
    var showLoading by remember { mutableStateOf(true) }

    LaunchedEffect(chatsWithUsers) {
        if (chatsWithUsers.isNotEmpty()) {
            showLoading = false
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Berichten") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Maakt de achtergrond onzichtbaar
                    titleContentColor = MaterialTheme.colorScheme.onSurface // Tekstkleur aanpassen (bijv. zwart)
                )
            )
        },floatingActionButton = {
            FloatingActionButton(        onClick = onSearchClick,
                shape = CircleShape, // <-- VOEG DEZE REGEL TOE
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nieuw gesprek", tint = Color.White)
            }
        }

    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showLoading && chatsWithUsers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (chatsWithUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Klik op '+' om een gesprek te starten.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatsWithUsers, key = { it.first.id }) { (chat, user) ->
                        ChatItem(
                            user = user,
                            chat = chat,
                            // Gebruik de status van de UnreadSendersViewModel
                            hasUnread = unreadSenders.contains(user.id),
                            onClick = { onChatSelected(chat.id, user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(user: User, chat: Chat, hasUnread: Boolean, onClick: () -> Unit) {
    // 1. Haal de ID van de huidige gebruiker op om de afzender te bepalen
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 2. Bepaal de prefix voor het laatste bericht
    val lastMessagePrefix = if (chat.lastMessageSenderId == currentUserId) {
        "Jij: "
    } else {
        "" // Geen prefix als het van de andere persoon is
    }

    // 3. Bouw de uiteindelijke tekst voor het laatste bericht
    val lastMessageDisplay = if (chat.lastMessageText.isNullOrBlank()) {
        "Nog geen berichten"
    } else {
        lastMessagePrefix + chat.lastMessageText
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(if (hasUnread) 2.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = if (hasUnread) Color(0xFFF0F8FF) else Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --- LOGICA VOOR PROFIELFOTO/ICOON ---
            val imageModifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.LightGray)

            if (user.profilePictureUrl.isNullOrBlank()) {
                // Toon het standaard Persoon-icoon (wanneer geen URL)
                Box(
                    modifier = imageModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Standaard profielfoto",
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        tint = Color.Gray
                    )
                }
            } else {
                // Probeer de Coil afbeelding te laden (wanneer URL aanwezig)
                Image(
                    painter = rememberAsyncImagePainter(
                        model = user.profilePictureUrl,
                        error = painterResource(id = R.drawable.ic_location_pin)
                    ),
                    contentDescription = "Profielfoto van ${user.firstName}",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            }
            // --- EINDE LOGICA PROFIELFOTO/ICOON ---

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}".trim(),
                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 17.sp
                )
                // TOON HET AANGEPASTE LAATSTE BERICHT
                Text(
                    text = lastMessageDisplay,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                chat.lastMessageTimestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                if (hasUnread) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    currentUserId: String,
    onUserSelected: (otherUserId: String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<Pair<String, User>>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("users").get().addOnSuccessListener { result ->
            allUsers = result.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                val userId = doc.id
                if (user != null && userId != currentUserId) userId to user else null
            }
        }
    }

    val filteredUsers = allUsers.filter {
        "${it.second.firstName} ${it.second.lastName}".contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start een gesprek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Terug")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )


        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Zoek op naam...") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredUsers, key = { it.first }) { (userId, user) ->
                    Card(
                        onClick = { onUserSelected(userId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Zelfde profielfoto-stijl als Inbox
                            val imageModifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)

                            if (user.profilePictureUrl.isNullOrBlank()) {
                                Box(
                                    modifier = imageModifier,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profielfoto",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        tint = Color.Gray
                                    )
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = user.profilePictureUrl,
                                        error = painterResource(id = R.drawable.ic_location_pin)
                                    ),
                                    contentDescription = "Profielfoto",
                                    modifier = imageModifier,
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            // Zelfde tekststijl als Inbox
                            Text(
                                text = "${user.firstName} ${user.lastName}".trim(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }

                }
            }
        }
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
    var newMessage by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<User?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

    LaunchedEffect(otherUserId) {
        FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                otherUser = doc.toObject(User::class.java)
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Laden...") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Terug") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                Button(
                    onClick = { viewModel.sendMessage(newMessage); newMessage = "" },
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

// Helper Composable
@Composable
fun rememberChatViewModel(chatId: String, otherUserId: String): ChatViewModel {
    return remember(chatId) {
        ChatViewModel(chatId, otherUserId)
    }
}