package com.example.myapplication.db.dao

import androidx.room.*
import com.example.myapplication.db.entity.Message
import com.example.myapplication.db.entity.MessageWithSender
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)
    
    @Update
    suspend fun update(message: Message)
    
    @Delete
    suspend fun delete(message: Message)
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Int): Message?
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getMessagesBySession(sessionId: Int): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isRead = 0 ORDER BY timestamp ASC")
    suspend fun getUnreadMessages(sessionId: Int): List<Message>
    
    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isRead = 0")
    fun getUnreadCount(sessionId: Int): Flow<Int>
    
    @Query("UPDATE messages SET isRead = 1, readTime = :readTime WHERE sessionId = :sessionId")
    suspend fun markSessionAsRead(sessionId: Int, readTime: Long)
    
    @Query("UPDATE messages SET isRead = 1, readTime = :readTime WHERE id = :messageId")
    suspend fun markMessageAsRead(messageId: Int, readTime: Long)
    
    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteSessionMessages(sessionId: Int)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
    
    @Query("""
        SELECT m.*, c.* FROM messages m
        JOIN contacts c ON m.contactId = c.id
        WHERE m.sessionId = :sessionId
        ORDER BY m.timestamp DESC
    """)
    fun getMessagesWithSender(sessionId: Int): Flow<List<MessageWithSender>>
}
