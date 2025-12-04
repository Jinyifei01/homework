package com.example.myapplication.db.dao

import androidx.room.*
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.SessionWithContact
import kotlinx.coroutines.flow.Flow

/**
 * 聊天会话数据访问对象
 */
@Dao
interface ChatSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSession): Long
    
    @Update
    suspend fun update(session: ChatSession)
    
    @Delete
    suspend fun delete(session: ChatSession)
    
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): ChatSession?
    
    @Query("SELECT * FROM chat_sessions WHERE contactId = :contactId")
    suspend fun getSessionByContactId(contactId: Int): ChatSession?
    
    @Query("SELECT * FROM chat_sessions ORDER BY lastMessageTime DESC")
    fun getAllSessions(): Flow<List<ChatSession>>
    
    @Query("""
        SELECT s.*, c.*, 
               (SELECT COUNT(*) FROM messages WHERE sessionId = s.id AND isRead = 0) as unreadCount
        FROM chat_sessions s
        JOIN contacts c ON s.contactId = c.id
        ORDER BY s.isPinned DESC, s.lastMessageTime DESC
    """)
    fun getSessionsWithContacts(): Flow<List<SessionWithContact>>
    
    @Query("SELECT * FROM chat_sessions WHERE isPinned = 1 ORDER BY lastMessageTime DESC")
    fun getPinnedSessions(): Flow<List<ChatSession>>
    
    @Query("SELECT * FROM chat_sessions WHERE isArchived = 0 ORDER BY lastMessageTime DESC")
    fun getActiveSessions(): Flow<List<ChatSession>>
    
    @Query("UPDATE chat_sessions SET unreadCount = :count WHERE id = :sessionId")
    suspend fun updateUnreadCount(sessionId: Int, count: Int)
    
    @Query("""
        UPDATE chat_sessions 
        SET lastMessage = :lastMessage, lastMessageTime = :time
        WHERE id = :sessionId
    """)
    suspend fun updateLastMessage(sessionId: Int, lastMessage: String, time: Long)
    
    @Query("UPDATE chat_sessions SET isPinned = :pinned WHERE id = :sessionId")
    suspend fun setPinned(sessionId: Int, pinned: Boolean)
    
    @Query("UPDATE chat_sessions SET isArchived = :archived WHERE id = :sessionId")
    suspend fun setArchived(sessionId: Int, archived: Boolean)
    
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)
}
