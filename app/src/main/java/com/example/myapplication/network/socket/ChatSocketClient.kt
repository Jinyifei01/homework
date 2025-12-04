package com.example.myapplication.network.socket

import android.util.Log
import com.example.myapplication.network.protocol.ChatProtocolFrame
import com.example.myapplication.network.protocol.ControlMessage
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * 聊天Socket客户端
 */
class ChatSocketClient(
    private val serverHost: String,
    private val serverPort: Int
) {
    
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var currentUserId: String? = null
    private val gson = Gson()
    
    // 消息监听器回调
    private val listeners = ConcurrentHashMap<String, (ChatProtocolFrame) -> Unit>()
    private var job: Job? = null
    
    // 连接状态
    var isConnected = false
        private set
    
    /**
     * 连接到服务器
     */
    suspend fun connect(userId: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket(serverHost, serverPort)
            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(socket!!.outputStream))
            currentUserId = userId
            isConnected = true
            
            // 发送登录控制消息
            val loginMsg = ControlMessage(
                action = ControlMessage.ACTION_LOGIN,
                userId = userId,
                userName = userName,
                ipAddress = getLocalIpAddress()
            )
            
            val frame = ChatProtocolFrame(
                frameType = ChatProtocolFrame.FRAME_TYPE_CONTROL,
                frameId = generateFrameId(),
                fromUser = userId,
                data = gson.toJson(loginMsg)
            )
            
            sendFrame(frame)
            
            // 启动接收线程
            startReceivingMessages()
            
            Log.d("ChatSocketClient", "Connected to $serverHost:$serverPort as $userId")
            true
        } catch (e: Exception) {
            Log.e("ChatSocketClient", "Connection failed", e)
            isConnected = false
            false
        }
    }
    
    /**
     * 断开连接
     */
    suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            if (isConnected && currentUserId != null) {
                val logoutMsg = ControlMessage(
                    action = ControlMessage.ACTION_LOGOUT,
                    userId = currentUserId!!
                )
                
                val frame = ChatProtocolFrame(
                    frameType = ChatProtocolFrame.FRAME_TYPE_CONTROL,
                    frameId = generateFrameId(),
                    fromUser = currentUserId!!,
                    data = gson.toJson(logoutMsg)
                )
                
                sendFrame(frame)
            }
            
            job?.cancel()
            reader?.close()
            writer?.close()
            socket?.close()
            isConnected = false
        } catch (e: Exception) {
            Log.e("ChatSocketClient", "Disconnect error", e)
        }
    }
    
    /**
     * 发送消息帧
     */
    suspend fun sendFrame(frame: ChatProtocolFrame): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(frame)
            writer?.write(json + "\n")
            writer?.flush()
            Log.d("ChatSocketClient", "Frame sent: ${frame.frameId}")
            true
        } catch (e: Exception) {
            Log.e("ChatSocketClient", "Send frame error", e)
            false
        }
    }
    
    /**
     * 注册消息监听器
     */
    fun addMessageListener(listenerId: String, listener: (ChatProtocolFrame) -> Unit) {
        listeners[listenerId] = listener
    }
    
    /**
     * 移除消息监听器
     */
    fun removeMessageListener(listenerId: String) {
        listeners.remove(listenerId)
    }
    
    /**
     * 启动接收消息线程
     */
    private fun startReceivingMessages() {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isConnected && reader != null) {
                    val line = reader?.readLine() ?: break
                    
                    try {
                        val frame = gson.fromJson(line, ChatProtocolFrame::class.java)
                        Log.d("ChatSocketClient", "Received frame: ${frame.frameId}")
                        
                        // 通知所有监听器
                        listeners.values.forEach { listener ->
                            try {
                                listener(frame)
                            } catch (e: Exception) {
                                Log.e("ChatSocketClient", "Listener error", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatSocketClient", "Parse frame error", e)
                    }
                }
            } finally {
                isConnected = false
            }
        }
    }
    
    /**
     * 发送心跳
     */
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || currentUserId == null) return@withContext false
            
            val heartbeatMsg = ControlMessage(
                action = ControlMessage.ACTION_HEARTBEAT,
                userId = currentUserId!!
            )
            
            val frame = ChatProtocolFrame(
                frameType = ChatProtocolFrame.FRAME_TYPE_HEARTBEAT,
                frameId = generateFrameId(),
                fromUser = currentUserId!!,
                data = gson.toJson(heartbeatMsg)
            )
            
            sendFrame(frame)
            true
        } catch (e: Exception) {
            Log.e("ChatSocketClient", "Heartbeat error", e)
            false
        }
    }
    
    companion object {
        private fun generateFrameId(): String {
            return "frame_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        }
        
        private fun getLocalIpAddress(): String {
            return try {
                val process = Runtime.getRuntime().exec("getprop net.hostname")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.readLine() ?: "127.0.0.1"
            } catch (e: Exception) {
                "127.0.0.1"
            }
        }
    }
}
