package com.example.myapplication.network.protocol

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 可扩展的聊天消息协议（v1）
 * 支持不同的消息类型和自定义扩展字段
 */
data class ChatProtocolMessage(
    val version: String = "v1",           // 协议版本
    val messageId: String,                 // 消息唯一ID
    @SerializedName("msg_type")
    val messageType: Int,                  // 消息类型
    @SerializedName("sender_id")
    val senderId: String,                  // 发送者ID
    @SerializedName("sender_name")
    val senderName: String,                // 发送者昵称
    val content: String,                   // 消息内容
    val timestamp: Long = System.currentTimeMillis(), // 时间戳
    val payload: Map<String, Any> = emptyMap() // 扩展字段支持
) {
    companion object {
        const val MSG_TYPE_TEXT = 1
        const val MSG_TYPE_IMAGE = 2
        const val MSG_TYPE_EMOJI = 3
        const val MSG_TYPE_FILE = 4
        const val MSG_TYPE_SYSTEM = 5
        
        fun toJson(message: ChatProtocolMessage): String {
            return Gson().toJson(message)
        }
        
        fun fromJson(json: String): ChatProtocolMessage {
            return Gson().fromJson(json, ChatProtocolMessage::class.java)
        }
    }
}

/**
 * 聊天协议帧（含消息头）
 */
data class ChatProtocolFrame(
    val frameType: Int,                    // 帧类型: 1-数据帧, 2-控制帧, 3-心跳帧
    val frameId: String,                   // 帧ID
    @SerializedName("from_user")
    val fromUser: String,                  // 发送用户
    @SerializedName("to_user")
    val toUser: String? = null,            // 接收用户（可为空表示广播）
    val data: String,                      // 负载数据（JSON格式的ChatProtocolMessage）
    val checksum: String? = null           // 校验码
) {
    companion object {
        const val FRAME_TYPE_DATA = 1
        const val FRAME_TYPE_CONTROL = 2
        const val FRAME_TYPE_HEARTBEAT = 3
        
        fun toJson(frame: ChatProtocolFrame): String {
            return Gson().toJson(frame)
        }
        
        fun fromJson(json: String): ChatProtocolFrame {
            return Gson().fromJson(json, ChatProtocolFrame::class.java)
        }
    }
}

/**
 * 控制消息（登录、注册、心跳等）
 */
data class ControlMessage(
    val action: String,                    // 操作类型: login, logout, register, heartbeat
    val userId: String,                    // 用户ID
    val userName: String? = null,          // 用户名
    val ipAddress: String? = null,         // IP地址
    val port: Int? = null,                 // 端口
    val extra: Map<String, Any> = emptyMap() // 扩展字段
) {
    companion object {
        const val ACTION_LOGIN = "login"
        const val ACTION_LOGOUT = "logout"
        const val ACTION_REGISTER = "register"
        const val ACTION_HEARTBEAT = "heartbeat"
        const val ACTION_ACK = "ack"
        
        fun toJson(message: ControlMessage): String {
            return Gson().toJson(message)
        }
        
        fun fromJson(json: String): ControlMessage {
            return Gson().fromJson(json, ControlMessage::class.java)
        }
    }
}
