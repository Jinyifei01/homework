package com.example.myapplication.db.entity

/**
 * 与联系人相关的会话和消息数据类（用于查询视图）
 */
data class SessionWithContact(
    val session: ChatSession,
    val contact: Contact,
    val unreadCount: Int
)

/**
 * 消息及其发送者的数据类（用于查询视图）
 */
data class MessageWithSender(
    val message: Message,
    val sender: Contact
)
