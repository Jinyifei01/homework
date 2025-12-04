package com.example.myapplication.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * 聊天服务器
 * 基于Java原始Socket API实现，支持多线程并发连接
 */
public class ChatServer {
    
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private Map<String, ClientHandler> connectedClients;
    private Gson gson;
    
    public ChatServer(int port) {
        this.port = port;
        this.connectedClients = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(50);
        this.gson = new Gson();
    }
    
    /**
     * 启动服务器
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[ChatServer] Started on port " + port);
            
            // 主线程循环接受客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                System.out.println("[ChatServer] New connection from " + clientIp);
                
                // 为每个客户端创建处理线程
                ClientHandler handler = new ClientHandler(this, clientSocket);
                executorService.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("[ChatServer] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdownNow();
            System.out.println("[ChatServer] Stopped");
        } catch (IOException e) {
            System.err.println("[ChatServer] Stop error: " + e.getMessage());
        }
    }
    
    /**
     * 客户端连接时注册
     */
    public void registerClient(String userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        System.out.println("[ChatServer] Client registered: " + userId);
        
        // 广播用户上线
        broadcastSystemMessage(userId + " has logged in");
    }
    
    /**
     * 客户端断开时注销
     */
    public void unregisterClient(String userId) {
        connectedClients.remove(userId);
        System.out.println("[ChatServer] Client unregistered: " + userId);
        
        // 广播用户离线
        broadcastSystemMessage(userId + " has logged out");
    }
    
    /**
     * 路由消息到目标用户
     */
    public void routeMessage(String fromUserId, String toUserId, String message) {
        ClientHandler targetHandler = connectedClients.get(toUserId);
        if (targetHandler != null) {
            targetHandler.sendMessage(message);
            System.out.println("[ChatServer] Message routed from " + fromUserId + " to " + toUserId);
        } else {
            System.out.println("[ChatServer] Target user offline: " + toUserId);
        }
    }
    
    /**
     * 广播消息到所有客户端
     */
    public void broadcastMessage(String message) {
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(message);
        }
    }
    
    /**
     * 广播系统消息
     */
    private void broadcastSystemMessage(String message) {
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("frameType", 2); // 控制帧
        systemMsg.put("action", "system");
        systemMsg.put("message", message);
        systemMsg.put("timestamp", System.currentTimeMillis());
        
        broadcastMessage(gson.toJson(systemMsg));
    }
    
    /**
     * 获取在线用户列表
     */
    public List<String> getOnlineUsers() {
        return new ArrayList<>(connectedClients.keySet());
    }
    
    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return connectedClients.size();
    }
    
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using default: " + port);
            }
        }
        
        ChatServer server = new ChatServer(port);
        server.start();
    }
}
