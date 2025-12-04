package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.Contact
import com.example.myapplication.db.entity.Message
import com.example.myapplication.network.socket.ChatSocketClient
import com.example.myapplication.repository.ChatRepository
import com.example.myapplication.ui.viewmodel.SessionListViewModel
import com.example.myapplication.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主聊天屏幕容器
 */
@Composable
fun ChatScreenContainer(
    repository: ChatRepository,
    socketClient: ChatSocketClient
) {
    var currentScreen by remember { mutableStateOf<String?>(null) }
    var selectedSession by remember { mutableStateOf<Pair<Int, Contact>?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            selectedSession != null -> {
                ChatScreen(
                    sessionId = selectedSession!!.first,
                    contact = selectedSession!!.second,
                    repository = repository,
                    socketClient = socketClient,
                    onBackClick = { selectedSession = null }
                )
            }
            else -> {
                SessionListScreen(
                    repository = repository,
                    onSessionClick = { sessionId, contact ->
                        selectedSession = Pair(sessionId, contact)
                    }
                )
            }
        }
    }
}

/**
 * 会话列表屏幕
 */
@Composable
fun SessionListScreen(
    repository: ChatRepository,
    onSessionClick: (Int, Contact) -> Unit
) {
    val sessionListVm: SessionListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionListViewModel(repository) as T
            }
        }
    )
    
    val activeSessions by sessionListVm.activeSessions.collectAsState()
    val pinnedSessions by sessionListVm.pinnedSessions.collectAsState()
    val totalUnread by sessionListVm.totalUnreadCount.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Messages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (totalUnread > 0) {
                        Badge(
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(totalUnread.toString())
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pinnedSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Pinned",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                items(pinnedSessions) { session ->
                    SessionItemComposable(
                        session = session,
                        onSessionClick = onSessionClick
                    )
                }
                item {
                    Divider()
                }
            }
            
            if (activeSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "All Messages",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                items(activeSessions) { session ->
                    SessionItemComposable(
                        session = session,
                        onSessionClick = onSessionClick
                    )
                }
            }
        }
    }
}

/**
 * 聊天会话项
 */
@Composable
fun SessionItemComposable(
    session: ChatSession,
    onSessionClick: (Int, Contact) -> Unit
) {
    var contact by remember { mutableStateOf<Contact?>(null) }
    
    // 这里需要获取联系人信息，实际实现中应该通过Flow
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                contact?.let { onSessionClick(session.id, it) }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contact Name",  // Replace with actual contact name
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = session.lastMessage ?: "No messages yet",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(session.lastMessageTime),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (session.unreadCount > 0) {
                    Badge(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(session.unreadCount.toString())
                    }
                }
            }
        }
    }
}

/**
 * 聊天屏幕
 */
@Composable
fun ChatScreen(
    sessionId: Int,
    contact: Contact,
    repository: ChatRepository,
    socketClient: ChatSocketClient,
    onBackClick: () -> Unit
) {
    val chatVm: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(repository, socketClient) as T
            }
        }
    )
    
    val messages by chatVm.messages.collectAsState(initial = emptyList())
    val inputText by chatVm.inputText.collectAsState()
    val showEmojiPicker by chatVm.showEmojiPicker.collectAsState()
    
    LaunchedEffect(sessionId, contact) {
        chatVm.openChat(sessionId, contact)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.nickname) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (showEmojiPicker) {
                    EmojiPickerPanel(onEmojiSelected = { emoji ->
                        chatVm.updateInputText(inputText + emoji)
                    })
                }
                
                MessageInputPanel(
                    inputText = inputText,
                    onTextChanged = { chatVm.updateInputText(it) },
                    onEmojiClick = { chatVm.toggleEmojiPicker() },
                    onSendClick = { chatVm.sendMessage(inputText) }
                )
            }
        }
    ) { innerPadding ->
        MessagesListPanel(
            messages = messages,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * 消息列表面板
 */
@Composable
fun MessagesListPanel(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        reverseLayout = true
    ) {
        items(messages.reversed()) { message ->
            MessageBubble(message = message)
        }
    }
}

/**
 * 消息气泡
 */
@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isRead) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isRead) Color(0xFF0084FF) else Color(0xFFE0E0E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (message.isRead) Color.White else Color.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 12.sp,
                    color = if (message.isRead) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * 消息输入面板
 */
@Composable
fun MessageInputPanel(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFFF5F5F5)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEmojiClick) {
            Text("😀", fontSize = 24.sp)
        }
        
        TextField(
            value = inputText,
            onValueChange = onTextChanged,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 100.dp),
            placeholder = { Text("Message...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = false
        )
        
        IconButton(
            onClick = onSendClick,
            enabled = inputText.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}

/**
 * 表情选择器面板
 */
@Composable
fun EmojiPickerPanel(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf(
        "😀", "😂", "😍", "🥰", "😘", "😎", "🤔", "🥳",
        "😢", "😡", "🤬", "😱", "👍", "👎", "🎉", "🔥"
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(Color(0xFFF5F5F5))
            .padding(8.dp)
    ) {
        items(emojis.chunked(8)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .clickable { onEmojiSelected(emoji) }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
fun formatTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    
    val today = Calendar.getInstance()
    return when {
        isSameDay(calendar, today) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        isYesterday(calendar, today) -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    cal2.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, cal2)
}
