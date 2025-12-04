# 聊天器Android应用 - 设计思路文档

## 目录
1. [项目概述](#项目概述)
2. [架构设计](#架构设计)
3. [数据库设计](#数据库设计)
4. [通信协议设计](#通信协议设计)
5. [服务器设计](#服务器设计)
6. [UI/UX设计](#uiux设计)
7. [后期优化思路](#后期优化思路)

---

## 项目概述

### 功能需求
- **Android聊天客户端**
  - 消息列表页面（会话列表）
  - 未读消息统计与显示
  - 聊天窗口（消息输入、表情选择、消息列表、时间戳显示）
  - 实时消息接收与发送

- **局域网聊天服务器**
  - 基于Java原始Socket API实现
  - 支持多客户端并发连接
  - 消息路由（单播/广播）
  - 在线用户管理
  - 心跳检测

### 技术栈
- **客户端**: Kotlin + Jetpack Compose + Room Database + Coroutines
- **服务器**: Java + Socket + Thread Pool
- **通信**: 自定义Protocol (JSON over TCP)
- **数据存储**: Room SQLite Database

---

## 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                   Android客户端                          │
├──────────────────┬──────────────────┬──────────────────┤
│    UI层(Compose) │   ViewModel层    │   Repository层   │
│  - SessionList   │ - ChatViewModel  │ - ChatRepository │
│  - ChatScreen    │ - SessionListVM  │                  │
│  - Components    │                  │                  │
├──────────────────┴──────────────────┴──────────────────┤
│               数据层(Room Database)                     │
│  - Contact Entity  - Message Entity  - ChatSession     │
│  - ContactDao      - MessageDao      - ChatSessionDao  │
├──────────────────────────────────────────────────────────┤
│          网络层(Socket + Protocol Handler)              │
│         ChatSocketClient + ChatProtocol                 │
└──────────────────┬───────────────────────────────────────┘
                   │ TCP Socket
                   │ (自定义Protocol)
┌──────────────────▼───────────────────────────────────────┐
│              聊天服务器(Java)                            │
├──────────────────┬───────────────────────────────────────┤
│  ChatServer      │      ClientHandler                    │
│  - 接受连接      │  - 处理单个客户端                    │
│  - 管理在线用户  │  - 消息分发                          │
│  - 消息路由      │  - 协议解析                          │
└──────────────────┴───────────────────────────────────────┘
```

### 模块划分

1. **UI层** (`ui/screen/`)
   - `ChatScreen.kt`: 主聊天界面，包含所有UI组件
   - Compose Components: 可复用的UI组件

2. **ViewModel层** (`ui/viewmodel/`)
   - `ChatViewModel`: 聊天窗口逻辑
   - `SessionListViewModel`: 会话列表逻辑

3. **数据层** (`db/`)
   - `entity/`: 数据实体 (Contact, Message, ChatSession)
   - `dao/`: 数据访问对象
   - `AppDatabase.kt`: Room数据库配置

4. **Repository层** (`repository/`)
   - `ChatRepository`: 统一数据访问接口

5. **网络层** (`network/`)
   - `protocol/`: 协议定义和序列化
   - `socket/`: Socket客户端实现

6. **服务器** (`server/`)
   - `ChatServer.java`: 服务器主类
   - `ClientHandler.java`: 客户端处理器

---

## 数据库设计

### ER图

```
┌─────────────┐           ┌──────────────────┐
│  Contact    │ 1 ─── * │  ChatSession     │
├─────────────┤           ├──────────────────┤
│ id (PK)     │           │ id (PK)          │
│ userId      │           │ contactId (FK)   │
│ nickname    │           │ lastMessage      │
│ avatar      │           │ lastMessageTime  │
│ status      │           │ unreadCount      │
│ lastSeenTime│           │ isPinned         │
│ ipAddress   │           │ isArchived       │
└─────────────┘           └──────────────────┘
       ▲                           ▲
       │ 1                         │ 1
       │                           │
       │ *                         │ *
       │                    ┌──────────────┐
       │                    │   Message    │
       └────────────────────┤──────────────┤
              contactId     │ id (PK)      │
                            │ sessionId(FK)│
                            │ contactId(FK)│
                            │ content      │
                            │ messageType  │
                            │ timestamp    │
                            │ isRead       │
                            │ readTime     │
                            │ protocol     │
                            └──────────────┘
```

### 表设计

#### 1. Contact（联系人表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| userId | TEXT | 用户ID（网络标识） |
| nickname | TEXT | 昵称 |
| avatar | TEXT | 头像URL |
| status | INTEGER | 在线状态 (0-离线, 1-在线, 2-忙碌) |
| lastSeenTime | LONG | 最后在线时间 |
| ipAddress | TEXT | IP地址（用于直连） |
| createdTime | LONG | 创建时间 |

#### 2. ChatSession（会话表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| contactId | INTEGER FK | 联系人ID |
| lastMessage | TEXT | 最后消息预览 |
| lastMessageTime | LONG | 最后消息时间 |
| unreadCount | INTEGER | 未读消息数 |
| isPinned | BOOLEAN | 是否置顶 |
| isArchived | BOOLEAN | 是否归档 |
| createdTime | LONG | 创建时间 |

#### 3. Message（消息表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| sessionId | INTEGER FK | 所属会话ID |
| contactId | INTEGER FK | 发送者ID |
| content | TEXT | 消息内容 |
| messageType | INTEGER | 消息类型 (1-文本, 2-图片, 3-表情, 4-文件, 5-系统) |
| timestamp | LONG | 时间戳 |
| isRead | BOOLEAN | 是否已读 |
| readTime | LONG | 读取时间 |
| protocol | TEXT | 协议版本 |

---

## 通信协议设计

### 概述
采用JSON格式的自定义协议，基于TCP连接。支持版本控制和扩展性设计。

### 协议分层

```
┌─────────────────────────────────────┐
│   应用层: ChatProtocolMessage       │
│   (消息、表情、图片等)              │
├─────────────────────────────────────┤
│   传输层: ChatProtocolFrame         │
│   (消息头、路由、帧ID等)            │
├─────────────────────────────────────┤
│   网络层: TCP Socket                │
│   (Java Socket API)                 │
└─────────────────────────────────────┘
```

### 协议帧结构 (ChatProtocolFrame)

```json
{
  "version": "v1",
  "frameType": 1,           // 1-数据帧, 2-控制帧, 3-心跳帧
  "frameId": "frame_xxx",   // 帧唯一标识
  "fromUser": "user123",    // 发送用户ID
  "toUser": "user456",      // 接收用户ID (可null表示广播)
  "data": "{...}",          // 负载(JSON字符串)
  "checksum": "abc123"      // 可选的校验码
}
```

### 消息数据格式 (ChatProtocolMessage)

```json
{
  "version": "v1",
  "messageId": "msg_123",
  "msg_type": 1,            // 1-文本, 2-图片, 3-表情, 4-文件, 5-系统
  "sender_id": "user123",
  "sender_name": "Alice",
  "content": "Hello World",
  "timestamp": 1701609600000,
  "payload": {              // 扩展字段支持
    "custom_field": "value"
  }
}
```

### 控制消息格式 (ControlMessage)

```json
{
  "action": "login",        // login, logout, register, heartbeat, ack
  "userId": "user123",
  "userName": "Alice",
  "ipAddress": "192.168.1.100",
  "port": 8888,
  "extra": {}
}
```

### 消息流程

```
Client1                         Server                        Client2
  │                               │                              │
  ├─── ChatProtocolFrame ────────>│                              │
  │  (frameType: DATA)            │                              │
  │  (fromUser: user1)            │── 检查toUser ────────────────>│
  │  (toUser: user2)              │                              │
  │                               │   ChatProtocolFrame          │
  │                               │   (路由到user2)              │
  │                               │<─── 接收处理 ─────────────────│
  │                               │                              │
  │                          (可选ACK)                          │
```

### 协议版本控制与扩展

1. **版本号**: 每条消息都包含`version`字段，支持向后兼容
2. **自定义字段**: `payload`字段允许添加自定义数据
3. **消息类型扩展**: 新消息类型可通过添加`messageType`值实现
4. **升级策略**: 
   - 如果收到未知版本，记录警告但继续处理
   - 如果缺少必需字段，拒绝处理
   - 额外字段被忽略，保证向前兼容

---

## 服务器设计

### 架构

```
┌──────────────────────────────┐
│     ChatServer (Main)        │
│ - ServerSocket监听           │
│ - 接受客户端连接             │
│ - 线程池管理ClientHandler   │
└──────────────┬───────────────┘
               │
         ┌─────┴─────┐
         ▼           ▼
   ClientHandler1  ClientHandler2  ... ClientHandlerN
   - Socket通信    - Socket通信     - Socket通信
   - 协议解析      - 协议解析       - 协议解析
   - 消息分发      - 消息分发       - 消息分发
```

### 主要类

#### ChatServer
```java
public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;     // 线程池
    private Map<String, ClientHandler> connectedClients; // 在线用户
    
    public void start()              // 启动服务器
    public void registerClient()     // 注册客户端
    public void unregisterClient()   // 注销客户端
    public void routeMessage()       // 消息路由(单播)
    public void broadcastMessage()   // 广播消息
}
```

#### ClientHandler
```java
public class ClientHandler implements Runnable {
    private ChatServer server;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String userId;
    
    public void run()                // 处理客户端连接
    private void handleMessage()     // 分发消息
    private void handleLogin()       // 登录处理
    private void handleLogout()      // 登出处理
    public void sendMessage()        // 发送消息给客户端
}
```

### 消息处理流程

```
1. ClientHandler接收消息
   └─> 解析JSON
       └─> 识别frameType
           ├─> 数据帧(1): 路由消息
           │   ├─> 检查toUser
           │   │   ├─> 有toUser: 单播
           │   │   └─> 无toUser: 广播
           │   └─> 分发到ClientHandler
           │
           ├─> 控制帧(2): 控制操作
           │   ├─> login: 注册用户
           │   ├─> logout: 注销用户
           │   └─> heartbeat: 更新心跳
           │
           └─> 心跳帧(3): 保活
               └─> 记录时间戳
```

### 并发控制

- **线程安全**: 使用`ConcurrentHashMap`存储在线用户
- **消息同步**: 发送消息时使用`synchronized`保护writer
- **线程池**: `ExecutorService`管理客户端线程
  - 线程数: 50个
  - 每个客户端一个线程，长连接模式

### 扩展性设计

- **无状态服务器**: 支持多服务器部署(需要消息队列)
- **数据持久化**: 可扩展为将消息保存到数据库
- **认证机制**: 可添加token认证
- **限流控制**: 可添加消息频率限制

---

## UI/UX设计

### 屏幕设计

#### 1. 会话列表屏幕

```
┌────────────────────────────┐
│  Messages  [Unread: 5]  📋  │ ← TopAppBar
├────────────────────────────┤
│ 📌 Pinned (1)              │ ← 置顶会话
├────────────────────────────┤
│ ┌──────────────────────┐  │
│ │ Alice       10:30   5 │  │ ← 会话项
│ │ Hey there!          │  │
│ └──────────────────────┘  │
│ ┌──────────────────────┐  │
│ │ Bob          9:15      │
│ │ See you later        │
│ └──────────────────────┘  │
│                            │
│ All Messages               │ ← 分类标签
│ ┌──────────────────────┐  │
│ │ Charlie       8:00      │
│ │ Good morning!        │
│ └──────────────────────┘  │
└────────────────────────────┘
```

**特性**:
- 置顶消息分组显示
- 未读消息数Badge
- 最后消息预览
- 时间戳显示
- 可滑动列表

#### 2. 聊天窗口屏幕

```
┌────────────────────────────┐
│  Alice              [≡]    │ ← TopAppBar(联系人名称)
├────────────────────────────┤
│                            │
│ ┌──────────────────────┐  │ ← 消息气泡(接收)
│ │ Hey! How are you?    │  │   左对齐，灰色背景
│ │ 10:30              │  │
│ └──────────────────────┘  │
│                            │
│           ┌──────────────┐ │ ← 消息气泡(发送)
│           │ I'm good!    │ │  右对齐，蓝色背景
│           │ 10:31      │ │
│           └──────────────┘ │
│                            │
├────────────────────────────┤
│ 😀 │ [输入消息...] │ ➤      │ ← 输入框
├────────────────────────────┤
│ 😀 😂 😍 🥰 😘 😎 🤔 🥳  │ ← 表情选择器(可折叠)
│ 😢 😡 🤬 😱 👍 👎 🎉 🔥  │
└────────────────────────────┘
```

**特性**:
- 气泡式消息显示
- 左右对齐区分（接收/发送）
- 时间戳显示
- 表情选择器
- 实时输入显示
- 平滑动画

### 组件设计

| 组件 | 功能 | 状态管理 |
|------|------|--------|
| SessionListScreen | 会话列表页面 | SessionListViewModel |
| ChatScreen | 聊天窗口 | ChatViewModel |
| MessagesListPanel | 消息列表 | messages Flow |
| MessageBubble | 单条消息 | Message Entity |
| MessageInputPanel | 输入框 | inputText StateFlow |
| EmojiPickerPanel | 表情选择器 | showEmojiPicker StateFlow |

---

## 后期优化思路

### 1. 功能优化

#### 消息功能增强
- [ ] 图片/视频消息支持
- [ ] 文件传输功能
- [ ] 消息搜索功能
- [ ] 消息撤回/编辑
- [ ] 消息转发
- [ ] 群组聊天支持

#### 用户功能增强
- [ ] 用户注册/登录认证
- [ ] 用户资料编辑
- [ ] 在线状态更新
- [ ] 用户状态同步(在线/离线/忙碌)
- [ ] 消息已读状态同步
- [ ] 输入中...提示

#### 会话管理
- [ ] 会话加密
- [ ] 消息自动删除
- [ ] 会话备份导出
- [ ] 聊天历史导入

### 2. 性能优化

#### 网络层
- [ ] 消息压缩传输
- [ ] 协议二进制化(替代JSON)
- [ ] 连接池管理
- [ ] 消息队列(RabbitMQ/Kafka)
- [ ] CDN/边界节点部署

#### 数据库
- [ ] 消息分表存储(按时间分表)
- [ ] 数据库索引优化
- [ ] 缓存层(Redis)
- [ ] 消息去重处理

#### UI/UX
- [ ] 消息虚拟列表(只渲染可见项)
- [ ] 图片懒加载
- [ ] 消息预加载
- [ ] 分页加载历史消息

### 3. 可靠性优化

#### 消息可靠性
- [ ] 消息去重机制
- [ ] 消息确认(ACK)机制
- [ ] 断线重连策略
- [ ] 本地消息队列(离线缓存)
- [ ] 消息重试机制

#### 服务器可靠性
- [ ] 服务器集群化
- [ ] 负载均衡
- [ ] 故障转移
- [ ] 监控告警系统
- [ ] 日志系统

### 4. 安全性优化

#### 通信安全
- [ ] SSL/TLS加密
- [ ] 消息端到端加密
- [ ] Token认证
- [ ] API签名验证

#### 数据安全
- [ ] 本地数据加密
- [ ] 敏感字段脱敏
- [ ] SQL注入防护
- [ ] XSS防护

### 5. 可维护性提升

#### 代码质量
- [ ] 单元测试覆盖
- [ ] 集成测试框架
- [ ] CI/CD流程
- [ ] 代码规范检查(Lint)
- [ ] 文档完善

#### 服务器可扩展性
- [ ] 模块化架构改进
- [ ] 依赖注入框架(Spring)
- [ ] 配置管理系统
- [ ] 插件机制

### 6. 扩展应用场景

#### 企业功能
- [ ] 群组聊天
- [ ] 消息加密
- [ ] 审计日志
- [ ] 权限管理
- [ ] 消息撤回策略

#### 社交功能
- [ ] 朋友圈/动态
- [ ] 用户关注系统
- [ ] 消息已读显示
- [ ] 输入状态显示
- [ ] 消息反应(emoji反应)

#### 内容功能
- [ ] 消息分享
- [ ] 消息收藏
- [ ] 消息标签
- [ ] 话题标签支持
- [ ] 链接预览

### 7. 分析和监控

- [ ] 用户行为分析
- [ ] 性能监控面板
- [ ] 实时在线统计
- [ ] 消息延迟统计
- [ ] 错误率监控

### 8. 国际化和本地化

- [ ] 多语言支持
- [ ] 时区处理
- [ ] 货币支持(若需要)
- [ ] 文化适配

---

## 总结

本设计采用**分层架构**、**协议驱动**的方式实现了一个完整的聊天系统。在保证核心功能的同时，通过灵活的协议设计和扩展机制，为后续功能扩展预留了充分的空间。

服务器采用**原始Socket API**实现，具有轻量级、易理解的特点，为学习和演示目的理想。在生产环境中，可进一步演化为：
- 基于更高效的NIO框架(如Netty)
- 集成消息队列(RabbitMQ/Kafka)
- 使用微服务架构
- 添加WebSocket支持浏览器接入
