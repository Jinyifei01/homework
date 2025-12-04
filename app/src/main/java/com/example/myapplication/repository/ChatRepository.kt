package com.example.myapplication.repository

import com.example.myapplication.db.AppDatabase
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.Contact
import com.example.myapplication.db.entity.Message
import kotlinx.coroutines.flow.Flow

/**
 * 聊天数据仓库，封装所有数据访问操作
 */
class ChatRepository(private val database: AppDatabase) {
    
    // Contact操作
    suspend fun addContact(contact: Contact) = database.contactDao().insert(contact)
    
    suspend fun updateContact(contact: Contact) = database.contactDao().update(contact)
    
    suspend fun deleteContact(contact: Contact) = database.contactDao().delete(contact)
    
    suspend fun getContact(id: Int) = database.contactDao().getContactById(id)
    
    fun getAllContacts(): Flow<List<Contact>> = database.contactDao().getAllContacts()
    
    fun getContactsByLastSeen(): Flow<List<Contact>> = database.contactDao().getContactsByLastSeen()
    
    suspend fun updateContactStatus(contactId: Int, status: Int) {
        database.contactDao().updateContactStatus(contactId, status, System.currentTimeMillis())
    }
    
    // Message操作
    suspend fun addMessage(message: Message) = database.messageDao().insert(message)
    
    suspend fun addMessages(messages: List<Message>) = database.messageDao().insertAll(messages)
    
    suspend fun updateMessage(message: Message) = database.messageDao().update(message)
    
    fun getMessagesInSession(sessionId: Int): Flow<List<Message>> = 
        database.messageDao().getMessagesBySession(sessionId)
    
    suspend fun getUnreadMessages(sessionId: Int) = 
        database.messageDao().getUnreadMessages(sessionId)
    
    fun getUnreadCount(sessionId: Int): Flow<Int> = 
        database.messageDao().getUnreadCount(sessionId)
    
    suspend fun markSessionAsRead(sessionId: Int) {
        database.messageDao().markSessionAsRead(sessionId, System.currentTimeMillis())
    }
    
    suspend fun markMessageAsRead(messageId: Int) {
        database.messageDao().markMessageAsRead(messageId, System.currentTimeMillis())
    }
    
    fun getMessagesWithSender(sessionId: Int) = 
        database.messageDao().getMessagesWithSender(sessionId)
    
    // ChatSession操作
    suspend fun createSession(session: ChatSession): Long = 
        database.chatSessionDao().insert(session)
    
    suspend fun updateSession(session: ChatSession) = 
        database.chatSessionDao().update(session)
    
    suspend fun deleteSession(sessionId: Int) = 
        database.chatSessionDao().deleteSession(sessionId)
    
    suspend fun getSession(sessionId: Int) = 
        database.chatSessionDao().getSessionById(sessionId)
    
    suspend fun getSessionByContact(contactId: Int) = 
        database.chatSessionDao().getSessionByContactId(contactId)
    
    fun getAllSessions(): Flow<List<ChatSession>> = 
        database.chatSessionDao().getAllSessions()
    
    fun getSessionsWithContacts() = 
        database.chatSessionDao().getSessionsWithContacts()
    
    fun getPinnedSessions(): Flow<List<ChatSession>> = 
        database.chatSessionDao().getPinnedSessions()
    
    fun getActiveSessions(): Flow<List<ChatSession>> = 
        database.chatSessionDao().getActiveSessions()
    
    suspend fun updateLastMessage(sessionId: Int, lastMessage: String, time: Long) {
        database.chatSessionDao().updateLastMessage(sessionId, lastMessage, time)
    }
    
    suspend fun setPinned(sessionId: Int, pinned: Boolean) {
        database.chatSessionDao().setPinned(sessionId, pinned)
    }
    
    suspend fun setArchived(sessionId: Int, archived: Boolean) {
        database.chatSessionDao().setArchived(sessionId, archived)
    }
}
