package com.example.myapplication.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 联系人实体类
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,           // 用户ID（用于网络标识）
    val nickname: String,          // 昵称
    val avatar: String? = null,    // 头像URL
    val status: Int = 0,           // 状态: 0-离线, 1-在线, 2-忙碌
    val lastSeenTime: Long = 0,    // 最后见面时间
    val createdTime: Long = System.currentTimeMillis(),  // 创建时间
    val ipAddress: String? = null  // IP地址（用于直连）
)
