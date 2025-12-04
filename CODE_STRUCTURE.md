# 聊天器Android应用 - 项目代码结构

## 项目目录结构

```
MyApplication2/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── db/                          # 数据库层
│   │   │   │   │   ├── AppDatabase.kt           # Room数据库配置
│   │   │   │   │   ├── dao/                     # 数据访问对象
│   │   │   │   │   │   ├── ContactDao.kt       # 联系人DAO
│   │   │   │   │   │   ├── MessageDao.kt       # 消息DAO
│   │   │   │   │   │   └── ChatSessionDao.kt   # 会话DAO
│   │   │   │   │   └── entity/                 # 数据实体
│   │   │   │   │       ├── Contact.kt          # 联系人实体
│   │   │   │   │       ├── Message.kt          # 消息实体
│   │   │   │   │       ├── ChatSession.kt      # 会话实体
│   │   │   │   │       └── ViewModels.kt       # 视图模型类
│   │   │   │   ├── network/                    # 网络层
│   │   │   │   │   ├── protocol/               # 协议定义
│   │   │   │   │   │   └── ChatProtocol.kt    # 协议数据类和序列化
│   │   │   │   │   └── socket/                 # Socket客户端
│   │   │   │   │       └── ChatSocketClient.kt # Socket连接和通信
│   │   │   │   ├── repository/                 # 数据仓库层
│   │   │   │   │   └── ChatRepository.kt       # 统一数据访问接口
│   │   │   │   ├── server/                     # 聊天服务器(Java)
│   │   │   │   │   ├── ChatServer.java         # 服务器主类
│   │   │   │   │   └── ClientHandler.java      # 客户端处理器
│   │   │   │   └── ui/                         # UI层
│   │   │   │       ├── screen/
│   │   │   │       │   └── ChatScreen.kt       # 所有UI屏幕和组件
│   │   │   │       ├── theme/
│   │   │   │       │   └── Theme.kt            # 主题配置
│   │   │   │       └── viewmodel/              # ViewModel
│   │   │   │           ├── ChatViewModel.kt    # 聊天窗口ViewModel
│   │   │   │           └── SessionListViewModel.kt  # 会话列表ViewModel
│   │   │   └── res/                            # 资源文件
│   │   │       ├── values/
│   │   │       │   ├── strings.xml             # 字符串资源
│   │   │       │   └── colors.xml              # 颜色定义
│   │   │       └── drawable/                   # 图片资源
│   │   ├── test/                               # 单元测试
│   │   │   └── java/...
│   │   └── androidTest/                        # 集成测试
│   │       └── java/...
│   ├── build.gradle.kts                        # Gradle构建配置
│   └── proguard-rules.pro                      # 混淆规则
├── DESIGN_DOC.md                               # 设计文档(本文件)
├── CODE_STRUCTURE.md                           # 代码结构说明(本文件)
└── README.md                                   # 项目README
```

## 模块说明

### 1. 数据库模块 (`db/`)

#### AppDatabase.kt
- **作用**: Room数据库的配置和单例管理
- **关键内容**:
  ```kotlin
  @Database(entities = [Contact, Message, ChatSession], version = 1)
  abstract class AppDatabase : RoomDatabase
  ```
- **用途**: 提供所有DAO的访问接口

#### DAO接口

**ContactDao.kt**
- 方法: `insert`, `update`, `delete`, `getContactById`, `getAllContacts`, `updateContactStatus`
- 流式查询支持: `getAllContacts()`, `getContactsByLastSeen()`

**MessageDao.kt**
- 方法: `insert`, `update`, `getMessagesBySession`, `getUnreadMessages`, `markSessionAsRead`
- 关键查询: `getMessagesWithSender`(JOIN查询，获取发送者信息)

**ChatSessionDao.kt**
- 方法: 会话的CRUD操作
- 特殊操作: `setPinned`, `setArchived`, `updateLastMessage`

#### 数据实体

**Contact.kt**
```kotlin
@Entity(tableName = "contacts")
data class Contact(
    val userId: String,        // 网络唯一标识
    val nickname: String,      // 昵称
    val status: Int,           // 在线状态
    val ipAddress: String?     // IP地址
)
```

**Message.kt**
```kotlin
@Entity(tableName = "messages")
data class Message(
    val sessionId: Int,        // 所属会话
    val contactId: Int,        // 发送者
    val content: String,       // 消息内容
    val messageType: Int,      // 消息类型
    val timestamp: Long,       // 时间戳
    val isRead: Boolean        // 是否已读
)
```

**ChatSession.kt**
```kotlin
@Entity(tableName = "chat_sessions")
data class ChatSession(
    val contactId: Int,        // 关联联系人
    val lastMessage: String?,  // 最后消息预览
    val unreadCount: Int,      // 未读数
    val isPinned: Boolean,     // 是否置顶
    val isArchived: Boolean    // 是否归档
)
```

### 2. 网络通信模块 (`network/`)

#### ChatProtocol.kt

定义三个主要数据类:

**ChatProtocolMessage** - 聊天消息
```kotlin
data class ChatProtocolMessage(
    val version: String = "v1",
    val messageId: String,
    val messageType: Int,      // 1-文本, 2-图片, 3-表情
    val senderId: String,
    val content: String,
    val payload: Map<String, Any>  // 扩展字段
)
```

**ChatProtocolFrame** - 协议帧(传输单位)
```kotlin
data class ChatProtocolFrame(
    val frameType: Int,        // 1-数据, 2-控制, 3-心跳
    val frameId: String,
    val fromUser: String,
    val toUser: String?,       // null表示广播
    val data: String           // JSON格式的消息内容
)
```

**ControlMessage** - 控制消息
```kotlin
data class ControlMessage(
    val action: String,        // login, logout, heartbeat
    val userId: String,
    val extra: Map<String, Any>
)
```

#### ChatSocketClient.kt

- **职责**: 管理与服务器的Socket连接
- **关键方法**:
  ```kotlin
  suspend fun connect(userId: String, userName: String): Boolean
  suspend fun disconnect()
  suspend fun sendFrame(frame: ChatProtocolFrame): Boolean
  fun addMessageListener(listenerId: String, listener: (ChatProtocolFrame) -> Unit)
  ```
- **特点**: 
  - 基于Coroutines的异步实现
  - 自动心跳检测
  - 消息监听器模式

### 3. 服务器模块 (`server/`)

#### ChatServer.java

```java
public class ChatServer {
    private ServerSocket serverSocket;
    private ExecutorService executorService;    // 线程池
    private Map<String, ClientHandler> connectedClients; // 在线用户
    
    public void start()                         // 启动服务器
    public void registerClient(String, ...)     // 注册客户端
    public void routeMessage(String, String, String) // 单播消息
    public void broadcastMessage(String)        // 广播消息
}
```

**启动示例**:
```bash
java -cp . com.example.myapplication.server.ChatServer 8888
```

#### ClientHandler.java

- 为每个客户端创建一个处理线程
- 负责: 消息解析、分发、心跳处理
- 协议处理流程:
  ```
  receiveMessage() -> parseJSON() -> handleMessage() -> 
    ├─ handleDataFrame() -> routeMessage()
    ├─ handleControlFrame() -> handleLogin/Logout
    └─ handleHeartbeat()
  ```

### 4. 数据仓库模块 (`repository/`)

#### ChatRepository.kt

统一的数据访问层，封装所有DAO操作:

```kotlin
class ChatRepository(private val database: AppDatabase) {
    // Contact operations
    suspend fun addContact(contact: Contact)
    suspend fun updateContactStatus(...)
    
    // Message operations
    suspend fun addMessage(message: Message)
    suspend fun getMessagesInSession(sessionId: Int): Flow<List<Message>>
    suspend fun markSessionAsRead(sessionId: Int)
    
    // Session operations
    suspend fun createSession(session: ChatSession): Long
    fun getSessionsWithContacts(): Flow<List<SessionWithContact>>
    suspend fun setPinned(sessionId: Int, pinned: Boolean)
}
```

**优势**:
- 单一数据访问入口
- 便于测试和Mock
- 支持Flow异步流

### 5. UI/ViewModel模块

#### ChatViewModel.kt

```kotlin
class ChatViewModel(
    private val repository: ChatRepository,
    private val socketClient: ChatSocketClient
) : ViewModel() {
    val messages: Flow<List<Message>>
    val inputText: StateFlow<String>
    val showEmojiPicker: StateFlow<Boolean>
    
    fun sendMessage(content: String)
    fun toggleEmojiPicker()
    fun openChat(sessionId: Int, contact: Contact)
}
```

**职责**:
- 管理聊天窗口的UI状态
- 处理消息发送接收
- 数据库和Socket的桥接

#### SessionListViewModel.kt

```kotlin
class SessionListViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    val sessions: StateFlow<List<ChatSession>>
    val totalUnreadCount: StateFlow<Int>
    
    fun togglePinSession(sessionId: Int, ...)
    fun toggleArchiveSession(sessionId: Int, ...)
    fun deleteSession(sessionId: Int)
}
```

#### ChatScreen.kt

所有UI组件的集合:

| 组件 | 说明 |
|------|------|
| `ChatScreenContainer` | 路由容器，处理屏幕切换 |
| `SessionListScreen` | 会话列表页面 |
| `ChatScreen` | 聊天窗口页面 |
| `MessagesListPanel` | 消息列表区域 |
| `MessageBubble` | 单条消息气泡 |
| `MessageInputPanel` | 输入框区域 |
| `EmojiPickerPanel` | 表情选择器 |
| `SessionItemComposable` | 会话列表项 |

### 6. 应用入口

#### MainActivity.kt

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var socketClient: ChatSocketClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 初始化数据库、仓库、Socket客户端
        // 检查权限
        // 设置UI根组件
    }
}
```

## 数据流向

### 发送消息流程

```
User Input
  ↓
ChatScreen.MessageInputPanel
  ↓ (updateInputText)
ChatViewModel.inputText (StateFlow)
  ↓ (User clicks Send)
ChatViewModel.sendMessage()
  ├─ 保存到数据库
  │   ↓
  │   Repository.addMessage()
  │   ↓
  │   MessageDao.insert()
  │   ↓
  │   AppDatabase
  │
  └─ 通过Socket发送
      ↓
      ChatSocketClient.sendFrame()
      ↓
      TCP Socket → Server
```

### 接收消息流程

```
Server → TCP Socket
  ↓
ChatSocketClient (接收线程)
  ↓ (notifyListener)
ChatViewModel (监听器回调)
  ├─ 解析消息
  ├─ 保存到数据库
  │  ↓
  │  Repository.addMessage()
  │  ↓
  │  MessageDao.insert()
  │
  └─ UI自动更新
     ↓
     messages Flow
     ↓
     ChatScreen.MessagesListPanel
     ↓
     MessageBubble (Recompose)
```

## 构建和运行

### 客户端

1. **构建**:
   ```bash
   ./gradlew assembleDebug
   ```

2. **运行**:
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.example.myapplication/.MainActivity
   ```

### 服务器

1. **编译**:
   ```bash
   javac -d bin src/main/java/com/example/myapplication/server/*.java
   ```

2. **运行**:
   ```bash
   java -cp bin com.example.myapplication.server.ChatServer 8888
   ```

3. **验证**:
   ```bash
   telnet localhost 8888
   ```

## 关键设计决策

### 1. 为什么使用Room而不是SQLite?
- 编译时SQL检查，发现错误更早
- 自动线程管理
- 简化了DAO的编写
- Flow/Coroutines集成

### 2. 为什么使用Compose而不是XML布局?
- 响应式UI，状态变化自动更新
- 代码复用性更好
- 类型安全
- 开发效率更高

### 3. 为什么用Java实现服务器而不是Kotlin?
- 兼容性更好
- 相对学生来说更熟悉
- 性能等价，便于理解Thread和Socket原理
- 可直接作为参考实现

### 4. 为什么选择JSON而不是二进制协议?
- 可读性好，便于调试
- 易于扩展和维护
- 协议版本控制容易
- 后续可演化为Protocol Buffers等

### 5. 为什么用单线程接收而不是多线程?
- 消息顺序得到保证
- 实现简单，易于理解
- 足以应对大多数场景
- 可通过Coroutines实现高效并发

## 扩展指南

### 添加新消息类型

1. 在 `Message.kt` 中添加常量:
   ```kotlin
   companion object {
       const val MSG_TYPE_VOICE = 6
   }
   ```

2. 在 `ChatProtocolMessage.kt` 中添加:
   ```kotlin
   const val MSG_TYPE_VOICE = 6
   ```

3. 在 `ChatViewModel.kt` 中添加处理:
   ```kotlin
   fun sendVoiceMessage(audioBytes: ByteArray) {
       sendMessage(String(audioBytes), Message.MSG_TYPE_VOICE)
   }
   ```

### 添加消息加密

1. 创建加密工具:
   ```kotlin
   object MessageEncryption {
       fun encrypt(msg: String): String { ... }
       fun decrypt(msg: String): String { ... }
   }
   ```

2. 在协议中添加encrypt字段:
   ```kotlin
   data class ChatProtocolMessage(
       ...,
       val encrypted: Boolean = false
   )
   ```

3. 发送时加密，接收时解密

### 添加消息队列备份

1. 在ChatViewModel中:
   ```kotlin
   private val messageQueue = mutableListOf<Message>()
   
   fun trySendMessage() {
       if (socketClient.isConnected) {
           messageQueue.forEach { sendMessage(it) }
           messageQueue.clear()
       }
   }
   ```

## 性能优化建议

### 内存优化
- 实现消息虚拟列表(LazyColumn已支持)
- 定期清理过期消息
- 图片压缩存储

### 网络优化
- 启用消息批量发送
- 实现消息队列机制
- 添加连接池

### 数据库优化
- 添加索引(userId, sessionId)
- 实现消息分页加载
- 定期数据库清理

## 常见问题

**Q: Socket连接断开如何处理?**
A: ChatSocketClient在disconnect()时关闭连接，可在MainActivity中实现重连逻辑。

**Q: 如何支持离线消息?**
A: 在ChatViewModel中添加本地消息队列，连接恢复时批量同步。

**Q: 如何实现消息加密?**
A: 在payload中添加encrypted字段，使用AES等加密算法。

**Q: 服务器如何持久化消息?**
A: ClientHandler可添加数据库操作，在handleDataFrame中保存消息。

**Q: 如何支持多用户登录?**
A: 修改ChatSocketClient为单例或通过Dependency Injection注入多个实例。
