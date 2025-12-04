package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.Contact
import com.example.myapplication.db.entity.Message
import com.example.myapplication.network.protocol.ChatProtocolFrame
import com.example.myapplication.network.protocol.ChatProtocolMessage
import com.example.myapplication.network.protocol.ControlMessage
import com.example.myapplication.network.socket.ChatSocketClient
import com.example.myapplication.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * 聊天窗口ViewModel
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val socketClient: ChatSocketClient
) : ViewModel() {
    
    private val gson = Gson()
    
    // 当前会话ID
    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()
    
    // 当前会话的联系人信息
    private val _currentContact = MutableStateFlow<Contact?>(null)
    val currentContact: StateFlow<Contact?> = _currentContact.asStateFlow()
    
    // 消息列表
    val messages: Flow<List<Message>> = currentSessionId
        .filterNotNull()
        .flatMapLatest { sessionId -> repository.getMessagesInSession(sessionId) }
    
    // 输入框内容
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    // 表情选择器是否显示
    private val _showEmojiPicker = MutableStateFlow(false)
    val showEmojiPicker: StateFlow<Boolean> = _showEmojiPicker.asStateFlow()
    
    // 网络连接状态
    val isConnected: StateFlow<Boolean> = MutableStateFlow(socketClient.isConnected).asStateFlow()
    
    // 当前用户ID
    var currentUserId: String? = null
    
    init {
        // 注册Socket消息监听器
        socketClient.addMessageListener("chatViewModel") { frame ->
            handleSocketMessage(frame)
        }
    }
    
    /**
     * 打开聊天窗口
     */
    fun openChat(sessionId: Int, contact: Contact) {
        _currentSessionId.value = sessionId
        _currentContact.value = contact
        
        viewModelScope.launch {
            // 标记消息为已读
            repository.markSessionAsRead(sessionId)
        }
    }
    
    /**
     * 更新输入框内容
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    /**
     * 切换表情选择器
     */
    fun toggleEmojiPicker() {
        _showEmojiPicker.value = !_showEmojiPicker.value
    }
    
    /**
     * 发送消息
     */
    fun sendMessage(content: String, messageType: Int = Message.MSG_TYPE_TEXT) {
        val sessionId = _currentSessionId.value ?: return
        val contact = _currentContact.value ?: return
        
        viewModelScope.launch {
            try {
                // 1. 保存到本地数据库
                val message = Message(
                    sessionId = sessionId,
                    contactId = contact.id,
                    content = content,
                    messageType = messageType,
                    timestamp = System.currentTimeMillis(),
                    isRead = true
                )
                repository.addMessage(message)
                
                // 2. 通过Socket发送给服务器
                sendMessageViaSocket(contact.userId, content, messageType)
                
                // 3. 更新会话的最后消息
                repository.updateLastMessage(
                    sessionId,
                    content.take(50),
                    System.currentTimeMillis()
                )
                
                // 4. 清空输入框
                _inputText.value = ""
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 通过Socket发送消息
     */
    private suspend fun sendMessageViaSocket(
        toUserId: String,
        content: String,
        messageType: Int
    ) {
        if (!socketClient.isConnected || currentUserId == null) return
        
        val messageId = UUID.randomUUID().toString()
        val chatMsg = ChatProtocolMessage(
            messageId = messageId,
            messageType = messageType,
            senderId = currentUserId!!,
            senderName = _currentContact.value?.nickname ?: "Unknown",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        
        val frame = ChatProtocolFrame(
            frameType = ChatProtocolFrame.FRAME_TYPE_DATA,
            frameId = "frame_${System.currentTimeMillis()}_${Random().nextInt(10000)}",
            fromUser = currentUserId!!,
            toUser = toUserId,
            data = gson.toJson(chatMsg)
        )
        
        socketClient.sendFrame(frame)
    }
    
    /**
     * 处理来自Socket的消息
     */
    private fun handleSocketMessage(frame: ChatProtocolFrame) {
        try {
            when (frame.frameType) {
                ChatProtocolFrame.FRAME_TYPE_DATA -> {
                    val chatMsg = gson.fromJson(frame.data, ChatProtocolMessage::class.java)
                    handleIncomingMessage(chatMsg, frame.fromUser)
                }
                ChatProtocolFrame.FRAME_TYPE_CONTROL -> {
                    // 处理控制消息（可选）
                }
                ChatProtocolFrame.FRAME_TYPE_HEARTBEAT -> {
                    // 处理心跳
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private fun handleIncomingMessage(message: ChatProtocolMessage, fromUserId: String) {
        viewModelScope.launch {
            try {
                // 这里应该从Socket消息中获取对应的Contact
                // 简化实现：创建临时Contact对象
                var contact = Contact(
                    userId = fromUserId,
                    nickname = message.senderName,
                    status = 1
                )
                repository.addContact(contact)
                
                // 查找或创建会话
                var session = repository.getSessionByContact(contact.id)
                if (session == null) {
                    session = ChatSession(contactId = contact.id)
                    val sessionId = repository.createSession(session).toInt()
                    session = repository.getSession(sessionId)!!
                }
                
                // 保存消息
                val dbMessage = Message(
                    sessionId = session.id,
                    contactId = contact.id,
                    content = message.content,
                    messageType = message.messageType,
                    timestamp = message.timestamp,
                    isRead = false
                )
                repository.addMessage(dbMessage)
                
                // 更新会话信息
                repository.updateLastMessage(
                    session.id,
                    message.content.take(50),
                    message.timestamp
                )
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        socketClient.removeMessageListener("chatViewModel")
    }
}
