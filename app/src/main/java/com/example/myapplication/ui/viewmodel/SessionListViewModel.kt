package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.Contact
import com.example.myapplication.network.protocol.ChatProtocolMessage
import com.example.myapplication.network.socket.ChatSocketClient
import com.example.myapplication.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 会话列表ViewModel
 */
class SessionListViewModel(private val repository: ChatRepository) : ViewModel() {
    
    // 会话列表
    val sessions: StateFlow<List<ChatSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // 激活的会话（未归档）
    val activeSessions: StateFlow<List<ChatSession>> = repository.getActiveSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // 置顶会话
    val pinnedSessions: StateFlow<List<ChatSession>> = repository.getPinnedSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // 总未读消息数
    val totalUnreadCount: StateFlow<Int> = sessions
        .map { list -> list.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    /**
     * 置顶/取消置顶会话
     */
    fun togglePinSession(sessionId: Int, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.setPinned(sessionId, !currentPinned)
        }
    }
    
    /**
     * 归档/取消归档会话
     */
    fun toggleArchiveSession(sessionId: Int, currentArchived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(sessionId, !currentArchived)
        }
    }
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
