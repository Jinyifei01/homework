package com.example.myapplication.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 聊天消息实体类
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,                 // 所属会话ID
    val contactId: Int,                 // 发送者联系人ID
    val content: String,                // 消息内容
    val messageType: Int = MSG_TYPE_TEXT, // 消息类型 (文本、图片、表情等)
    val timestamp: Long = System.currentTimeMillis(), // 消息时间戳
    val isRead: Boolean = false,        // 是否已读
    val readTime: Long = 0,             // 读取时间
    val protocol: String = "v1"         // 消息协议版本
) {
    companion object {
        const val MSG_TYPE_TEXT = 1      // 文本消息
        const val MSG_TYPE_IMAGE = 2     // 图片消息
        const val MSG_TYPE_EMOJI = 3     // 表情消息
        const val MSG_TYPE_FILE = 4      // 文件消息
        const val MSG_TYPE_SYSTEM = 5    // 系统消息
    }
}
