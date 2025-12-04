package com.example.myapplication.db.dao

import androidx.room.*
import com.example.myapplication.db.entity.Contact
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象
 */
@Dao
interface ContactDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact>)
    
    @Update
    suspend fun update(contact: Contact)
    
    @Delete
    suspend fun delete(contact: Contact)
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Int): Contact?
    
    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: String): Contact?
    
    @Query("SELECT * FROM contacts ORDER BY createdTime DESC")
    fun getAllContacts(): Flow<List<Contact>>
    
    @Query("SELECT * FROM contacts ORDER BY lastSeenTime DESC")
    fun getContactsByLastSeen(): Flow<List<Contact>>
    
    @Query("UPDATE contacts SET status = :status, lastSeenTime = :lastSeenTime WHERE id = :contactId")
    suspend fun updateContactStatus(contactId: Int, status: Int, lastSeenTime: Long)
    
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}
