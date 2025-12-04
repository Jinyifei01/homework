package com.example.myapplication.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.db.dao.ChatSessionDao
import com.example.myapplication.db.dao.ContactDao
import com.example.myapplication.db.dao.MessageDao
import com.example.myapplication.db.entity.ChatSession
import com.example.myapplication.db.entity.Contact
import com.example.myapplication.db.entity.Message

/**
 * 应用主数据库
 */
@Database(
    entities = [Contact::class, Message::class, ChatSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun chatSessionDao(): ChatSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
