package com.example.myapplication.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;

/**
 * 客户端处理器
 * 为每个连接的客户端维护一个处理线程
 */
public class ClientHandler implements Runnable {
    
    private ChatServer server;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String userId;
    private Gson gson;
    
    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.gson = new Gson();
        
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.err.println("[ClientHandler] Init error: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] " + userId + " read error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String json) {
        try {
            JsonObject frame = JsonParser.parseString(json).getAsJsonObject();
            int frameType = frame.get("frameType").getAsInt();
            
            switch (frameType) {
                case 1: // 数据帧
                    handleDataFrame(frame);
                    break;
                case 2: // 控制帧
                    handleControlFrame(frame);
                    break;
                case 3: // 心跳帧
                    handleHeartbeat(frame);
                    break;
                default:
                    System.out.println("[ClientHandler] Unknown frame type: " + frameType);
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler] Parse error: " + e.getMessage());
        }
    }
    
    /**
     * 处理数据帧（实际消息）
     */
    private void handleDataFrame(JsonObject frame) {
        try {
            String fromUser = frame.get("fromUser").getAsString();
            String toUser = frame.has("toUser") ? frame.get("toUser").getAsString() : null;
            String data = frame.get("data").getAsString();
            
            if (toUser != null && !toUser.isEmpty()) {
                // 单播：发送给特定用户
                server.routeMessage(fromUser, toUser, frame.toString());
            } else {
                // 广播：发送给所有用户
                server.broadcastMessage(frame.toString());
            }
            
            System.out.println("[ClientHandler] Data frame from " + fromUser + " to " + (toUser == null ? "broadcast" : toUser));
        } catch (Exception e) {
            System.err.println("[ClientHandler] Data frame error: " + e.getMessage());
        }
    }
    
    /**
     * 处理控制帧（登录、注册等）
     */
    private void handleControlFrame(JsonObject frame) {
        try {
            String fromUser = frame.get("fromUser").getAsString();
            String data = frame.get("data").getAsString();
            
            JsonObject controlMsg = JsonParser.parseString(data).getAsJsonObject();
            String action = controlMsg.get("action").getAsString();
            
            switch (action) {
                case "login":
                    handleLogin(fromUser, controlMsg);
                    break;
                case "logout":
                    handleLogout(fromUser);
                    break;
                case "register":
                    handleRegister(fromUser, controlMsg);
                    break;
                case "heartbeat":
                    handleHeartbeat(frame);
                    break;
                default:
                    System.out.println("[ClientHandler] Unknown control action: " + action);
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler] Control frame error: " + e.getMessage());
        }
    }
    
    /**
     * 处理登录
     */
    private void handleLogin(String userId, JsonObject controlMsg) {
        this.userId = userId;
        server.registerClient(userId, this);
        
        // 发送登录确认
        JsonObject response = new JsonObject();
        response.addProperty("action", "ack");
        response.addProperty("status", "success");
        response.addProperty("message", "Login successful");
        response.addProperty("timestamp", System.currentTimeMillis());
        
        sendMessage(response.toString());
    }
    
    /**
     * 处理注册
     */
    private void handleRegister(String userId, JsonObject controlMsg) {
        // 这里可以添加实际的用户注册逻辑
        // 目前只是简单地记录注册请求
        System.out.println("[ClientHandler] Register request from " + userId);
        
        JsonObject response = new JsonObject();
        response.addProperty("action", "ack");
        response.addProperty("status", "success");
        response.addProperty("message", "Register successful");
        response.addProperty("timestamp", System.currentTimeMillis());
        
        sendMessage(response.toString());
    }
    
    /**
     * 处理登出
     */
    private void handleLogout(String userId) {
        System.out.println("[ClientHandler] Logout request from " + userId);
        server.unregisterClient(userId);
        
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Close socket error: " + e.getMessage());
        }
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat(JsonObject frame) {
        // 记录心跳，保持连接活跃
        System.out.println("[ClientHandler] Heartbeat from " + frame.get("fromUser").getAsString());
    }
    
    /**
     * 发送消息给客户端
     */
    public void sendMessage(String message) {
        try {
            synchronized (writer) {
                writer.write(message + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Send error: " + e.getMessage());
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            if (userId != null) {
                server.unregisterClient(userId);
            }
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Cleanup error: " + e.getMessage());
        }
    }
}
