package com.example.myapplication.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 聊天会话实体类
 */
@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val contactId: Int,                 // 联系人ID
    val lastMessage: String? = null,    // 最后一条消息预览
    val lastMessageTime: Long = 0,      // 最后消息时间
    val unreadCount: Int = 0,           // 未读消息数
    val isArchived: Boolean = false,    // 是否归档
    val isPinned: Boolean = false,      // 是否置顶
    val createdTime: Long = System.currentTimeMillis()  // 创建时间
)
