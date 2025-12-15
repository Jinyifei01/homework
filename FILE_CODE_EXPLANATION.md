# 📖 项目文件代码详细解释

本文档详细介绍项目中每个源代码文件的具体实现和代码说明。

---

## 目录

1. [应用入口](#1-应用入口)
2. [数据库层](#2-数据库层)
3. [网络通信层](#3-网络通信层)
4. [数据仓库层](#4-数据仓库层)
5. [服务器层](#5-服务器层)
6. [UI层](#6-ui层)

---

## 1. 应用入口

### MainActivity.kt

**文件路径**: `app/src/main/java/com/example/myapplication/MainActivity.kt`

**作用**: Android应用的主入口，负责初始化核心组件并设置UI。

#### 代码详解

```kotlin
class MainActivity : ComponentActivity() {
```
- 继承自`ComponentActivity`，这是支持Jetpack Compose的Activity基类
- 提供了对Compose生命周期的完整支持

```kotlin
private lateinit var database: AppDatabase
private lateinit var repository: ChatRepository
private lateinit var socketClient: ChatSocketClient
```
- **database**: Room数据库实例，管理本地数据存储
- **repository**: 数据仓库，封装所有数据访问操作
- **socketClient**: Socket客户端，处理与服务器的网络通信
- 使用`lateinit`延迟初始化，在`onCreate`中实际创建

```kotlin
private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    // Handle permissions result
}
```
- 权限请求启动器，使用新的Activity Result API
- 可以同时请求多个权限
- 回调函数处理权限授予结果

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
```
- `enableEdgeToEdge()`: 启用全屏显示，内容延伸到状态栏和导航栏下方
- 提供更现代的沉浸式UI体验

```kotlin
database = AppDatabase.getInstance(this)
repository = ChatRepository(database)
socketClient = ChatSocketClient("192.168.1.100", 8888)
```
- 初始化三大核心组件
- 数据库使用单例模式，确保全局唯一
- Socket客户端配置服务器地址和端口（需要根据实际情况修改）

```kotlin
requestPermissions()
```
- 请求应用所需的运行时权限
- 包括网络访问、存储访问等

```kotlin
setContent {
    MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
            ChatScreenContainer(
                repository = repository,
                socketClient = socketClient
            )
        }
    }
}
```
- `setContent`: Compose的内容设置方法
- `MyApplicationTheme`: 应用主题，定义颜色、字体等样式
- `Scaffold`: Material Design的脚手架组件，提供基本布局结构
- `ChatScreenContainer`: 聊天应用的主容器，传入repository和socketClient

```kotlin
private fun requestPermissions() {
    val permissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
```
- 定义需要的权限列表
- `INTERNET`: 网络访问权限
- `ACCESS_NETWORK_STATE`: 查询网络状态
- `READ_EXTERNAL_STORAGE`: 读取外部存储（用于图片等）
- `WRITE_EXTERNAL_STORAGE`: 写入外部存储

```kotlin
val permissionsToRequest = permissions.filter {
    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
}.toTypedArray()

if (permissionsToRequest.isNotEmpty()) {
    requestPermissionLauncher.launch(permissionsToRequest)
}
```
- 筛选出尚未授予的权限
- 只请求需要的权限，避免重复请求

```kotlin
override fun onDestroy() {
    super.onDestroy()
    Thread {
        runBlocking {
            socketClient.disconnect()
        }
    }.start()
}
```
- Activity销毁时清理Socket连接
- 使用新线程执行断开操作，避免阻塞主线程
- `runBlocking`: 在协程中执行挂起函数

---

## 2. 数据库层

### 2.1 AppDatabase.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/AppDatabase.kt`

**作用**: Room数据库配置类，定义数据库版本和实体。

#### 代码详解

```kotlin
@Database(
    entities = [Contact::class, Message::class, ChatSession::class],
    version = 1,
    exportSchema = false
)
```
- `@Database`: Room数据库注解
- `entities`: 数据库包含的实体类列表
- `version = 1`: 数据库版本号，用于数据库迁移
- `exportSchema = false`: 不导出数据库模式（开发时可设为true）

```kotlin
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun chatSessionDao(): ChatSessionDao
```
- 抽象类继承`RoomDatabase`
- 定义抽象方法返回各个DAO接口
- Room在编译时自动生成实现

```kotlin
companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null
```
- 单例模式的静态实例
- `@Volatile`: 确保多线程环境下的可见性
- 防止创建多个数据库实例

```kotlin
fun getInstance(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "chat_app_db"
        ).build()
        INSTANCE = instance
        instance
    }
}
```
- 双重检查锁定（DCL）实现单例
- `INSTANCE ?:`: 如果实例为空则创建
- `synchronized`: 同步块防止并发创建
- `Room.databaseBuilder`: 构建数据库
  - `context.applicationContext`: 使用应用上下文，避免内存泄漏
  - `AppDatabase::class.java`: 数据库类
  - `"chat_app_db"`: 数据库文件名

### 2.2 数据实体

#### 2.2.1 Contact.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/entity/Contact.kt`

**作用**: 联系人数据实体，存储用户信息。

##### 代码详解

```kotlin
@Entity(tableName = "contacts")
data class Contact(
```
- `@Entity`: Room实体注解
- `tableName = "contacts"`: 指定数据库表名
- `data class`: Kotlin数据类，自动生成equals、hashCode、toString等方法

```kotlin
@PrimaryKey(autoGenerate = true)
val id: Int = 0,
```
- `@PrimaryKey`: 主键注解
- `autoGenerate = true`: 自动生成递增的主键值
- 默认值0表示新创建的联系人

```kotlin
val userId: String,           // 用户ID（用于网络标识）
```
- 网络通信时使用的唯一标识
- 与本地数据库ID分离，支持跨设备同步

```kotlin
val nickname: String,          // 昵称
val avatar: String? = null,    // 头像URL
```
- `nickname`: 用户显示名称
- `avatar`: 可选的头像URL，`?`表示可为空

```kotlin
val status: Int = 0,           // 状态: 0-离线, 1-在线, 2-忙碌
```
- 用户在线状态
- 使用整数枚举：0=离线，1=在线，2=忙碌

```kotlin
val lastSeenTime: Long = 0,    // 最后见面时间
val createdTime: Long = System.currentTimeMillis(),
```
- `lastSeenTime`: 最后活跃时间戳
- `createdTime`: 联系人创建时间，默认当前时间

```kotlin
val ipAddress: String? = null  // IP地址（用于直连）
```
- 可选的IP地址
- 支持P2P直连功能（预留）

#### 2.2.2 Message.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/entity/Message.kt`

**作用**: 消息数据实体，存储聊天消息。

##### 代码详解

```kotlin
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        ),
```
- 外键关联到Contact表
- `parentColumns`: 父表（Contact）的列
- `childColumns`: 子表（Message）的列
- `onDelete = CASCADE`: 删除联系人时级联删除其消息

```kotlin
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
```
- 外键关联到ChatSession表
- 级联删除：删除会话时自动删除相关消息

```kotlin
val sessionId: Int,                 // 所属会话ID
val contactId: Int,                 // 发送者联系人ID
```
- `sessionId`: 消息所属的聊天会话
- `contactId`: 消息发送者的ID

```kotlin
val content: String,                // 消息内容
val messageType: Int = MSG_TYPE_TEXT,
```
- `content`: 消息正文
- `messageType`: 消息类型，默认为文本消息

```kotlin
val timestamp: Long = System.currentTimeMillis(),
val isRead: Boolean = false,
val readTime: Long = 0,
```
- `timestamp`: 消息发送时间
- `isRead`: 是否已读标记
- `readTime`: 读取时间（0表示未读）

```kotlin
val protocol: String = "v1"
```
- 协议版本号，支持协议升级

```kotlin
companion object {
    const val MSG_TYPE_TEXT = 1      // 文本消息
    const val MSG_TYPE_IMAGE = 2     // 图片消息
    const val MSG_TYPE_EMOJI = 3     // 表情消息
    const val MSG_TYPE_FILE = 4      // 文件消息
    const val MSG_TYPE_SYSTEM = 5    // 系统消息
}
```
- 消息类型常量定义
- 使用伴生对象（companion object）存放静态常量

#### 2.2.3 ChatSession.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/entity/ChatSession.kt`

**作用**: 聊天会话实体，管理会话状态。

##### 代码详解

```kotlin
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
```
- 外键关联到Contact表
- 每个会话对应一个联系人

```kotlin
val contactId: Int,                 // 联系人ID
val lastMessage: String? = null,    // 最后一条消息预览
val lastMessageTime: Long = 0,      // 最后消息时间
```
- `contactId`: 关联的联系人
- `lastMessage`: 会话列表显示的最后消息预览
- `lastMessageTime`: 用于排序会话列表

```kotlin
val unreadCount: Int = 0,           // 未读消息数
```
- 未读消息计数
- 在会话列表中显示红点或数字

```kotlin
val isArchived: Boolean = false,    // 是否归档
val isPinned: Boolean = false,      // 是否置顶
```
- `isArchived`: 归档状态，归档的会话不在主列表显示
- `isPinned`: 置顶状态，置顶会话显示在列表顶部

```kotlin
val createdTime: Long = System.currentTimeMillis()
```
- 会话创建时间

#### 2.2.4 ViewModels.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/entity/ViewModels.kt`

**作用**: 定义查询视图的数据模型。

##### 代码详解

```kotlin
data class SessionWithContact(
    val session: ChatSession,
    val contact: Contact,
    val unreadCount: Int
)
```
- 会话与联系人的组合数据
- 用于会话列表显示
- 包含会话信息、联系人信息和未读数

```kotlin
data class MessageWithSender(
    val message: Message,
    val sender: Contact
)
```
- 消息与发送者的组合数据
- 用于聊天窗口显示
- JOIN查询的结果类型

### 2.3 数据访问对象（DAO）

#### 2.3.1 ContactDao.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/dao/ContactDao.kt`

**作用**: 联系人数据访问接口。

##### 代码详解

```kotlin
@Dao
interface ContactDao {
```
- `@Dao`: Room数据访问对象注解
- 接口定义，Room自动生成实现

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(contact: Contact)
```
- `@Insert`: 插入操作注解
- `onConflict = REPLACE`: 冲突时替换（根据主键判断）
- `suspend`: 挂起函数，在协程中调用

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(contacts: List<Contact>)
```
- 批量插入联系人
- 提高批量操作性能

```kotlin
@Update
suspend fun update(contact: Contact)
```
- 更新联系人信息
- 根据主键匹配更新

```kotlin
@Delete
suspend fun delete(contact: Contact)
```
- 删除联系人
- 根据主键匹配删除

```kotlin
@Query("SELECT * FROM contacts WHERE id = :id")
suspend fun getContactById(id: Int): Contact?
```
- `@Query`: 自定义SQL查询
- 根据ID查询单个联系人
- 返回`Contact?`表示可能为空

```kotlin
@Query("SELECT * FROM contacts WHERE userId = :userId")
suspend fun getContactByUserId(userId: String): Contact?
```
- 根据网络用户ID查询
- 用于接收消息时查找发送者

```kotlin
@Query("SELECT * FROM contacts ORDER BY createdTime DESC")
fun getAllContacts(): Flow<List<Contact>>
```
- 查询所有联系人
- 按创建时间降序排序
- 返回`Flow`实现响应式数据流

```kotlin
@Query("SELECT * FROM contacts ORDER BY lastSeenTime DESC")
fun getContactsByLastSeen(): Flow<List<Contact>>
```
- 按最后活跃时间排序
- 显示最近联系人

```kotlin
@Query("UPDATE contacts SET status = :status, lastSeenTime = :lastSeenTime WHERE id = :contactId")
suspend fun updateContactStatus(contactId: Int, status: Int, lastSeenTime: Long)
```
- 更新联系人状态
- 同时更新最后活跃时间

```kotlin
@Query("DELETE FROM contacts")
suspend fun deleteAllContacts()
```
- 清空所有联系人
- 用于测试或重置

#### 2.3.2 MessageDao.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/dao/MessageDao.kt`

**作用**: 消息数据访问接口。

##### 代码详解

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(message: Message)
```
- 插入单条消息
- 冲突时替换（用于消息重发）

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(messages: List<Message>)
```
- 批量插入消息
- 用于历史消息同步

```kotlin
@Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC")
fun getMessagesBySession(sessionId: Int): Flow<List<Message>>
```
- 查询指定会话的所有消息
- 按时间戳降序排序（最新的在前）
- 返回Flow，UI自动更新

```kotlin
@Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isRead = 0 ORDER BY timestamp ASC")
suspend fun getUnreadMessages(sessionId: Int): List<Message>
```
- 查询未读消息
- 按时间升序排序（从旧到新）
- 返回List，一次性获取

```kotlin
@Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isRead = 0")
fun getUnreadCount(sessionId: Int): Flow<Int>
```
- 查询未读消息数量
- 返回Flow实时更新未读数

```kotlin
@Query("UPDATE messages SET isRead = 1, readTime = :readTime WHERE sessionId = :sessionId")
suspend fun markSessionAsRead(sessionId: Int, readTime: Long)
```
- 标记会话所有消息为已读
- 进入聊天窗口时调用

```kotlin
@Query("UPDATE messages SET isRead = 1, readTime = :readTime WHERE id = :messageId")
suspend fun markMessageAsRead(messageId: Int, readTime: Long)
```
- 标记单条消息为已读
- 支持精确控制

```kotlin
@Query("DELETE FROM messages WHERE sessionId = :sessionId")
suspend fun deleteSessionMessages(sessionId: Int)
```
- 删除会话的所有消息
- 清空聊天记录

```kotlin
@Query("""
    SELECT m.*, c.* FROM messages m
    JOIN contacts c ON m.contactId = c.id
    WHERE m.sessionId = :sessionId
    ORDER BY m.timestamp DESC
""")
fun getMessagesWithSender(sessionId: Int): Flow<List<MessageWithSender>>
```
- JOIN查询消息和发送者信息
- 返回组合数据，避免多次查询
- 用于聊天窗口显示发送者头像和昵称

#### 2.3.3 ChatSessionDao.kt

**文件路径**: `app/src/main/java/com/example/myapplication/db/dao/ChatSessionDao.kt`

**作用**: 聊天会话数据访问接口。

##### 代码详解

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(session: ChatSession): Long
```
- 插入会话
- 返回`Long`类型的生成ID
- 用于后续关联消息

```kotlin
@Query("SELECT * FROM chat_sessions WHERE contactId = :contactId")
suspend fun getSessionByContactId(contactId: Int): ChatSession?
```
- 根据联系人ID查找会话
- 用于判断会话是否已存在

```kotlin
@Query("""
    SELECT s.*, c.*, 
           (SELECT COUNT(*) FROM messages WHERE sessionId = s.id AND isRead = 0) as unreadCount
    FROM chat_sessions s
    JOIN contacts c ON s.contactId = c.id
    ORDER BY s.isPinned DESC, s.lastMessageTime DESC
""")
fun getSessionsWithContacts(): Flow<List<SessionWithContact>>
```
- 复杂的JOIN查询
- 关联会话和联系人
- 子查询计算未读数
- 排序：置顶优先，然后按时间

```kotlin
@Query("SELECT * FROM chat_sessions WHERE isPinned = 1 ORDER BY lastMessageTime DESC")
fun getPinnedSessions(): Flow<List<ChatSession>>
```
- 查询置顶会话
- 用于特殊显示

```kotlin
@Query("SELECT * FROM chat_sessions WHERE isArchived = 0 ORDER BY lastMessageTime DESC")
fun getActiveSessions(): Flow<List<ChatSession>>
```
- 查询活跃（未归档）会话
- 主会话列表使用

```kotlin
@Query("""
    UPDATE chat_sessions 
    SET lastMessage = :lastMessage, lastMessageTime = :time
    WHERE id = :sessionId
""")
suspend fun updateLastMessage(sessionId: Int, lastMessage: String, time: Long)
```
- 更新最后一条消息
- 接收新消息时调用

```kotlin
@Query("UPDATE chat_sessions SET isPinned = :pinned WHERE id = :sessionId")
suspend fun setPinned(sessionId: Int, pinned: Boolean)
```
- 设置置顶状态
- 用户操作触发

```kotlin
@Query("UPDATE chat_sessions SET isArchived = :archived WHERE id = :sessionId")
suspend fun setArchived(sessionId: Int, archived: Boolean)
```
- 设置归档状态
- 隐藏不常用会话

---

## 3. 网络通信层

### 3.1 ChatProtocol.kt

**文件路径**: `app/src/main/java/com/example/myapplication/network/protocol/ChatProtocol.kt`

**作用**: 定义聊天通信协议的数据结构。

#### 代码详解

##### ChatProtocolMessage - 聊天消息

```kotlin
data class ChatProtocolMessage(
    val version: String = "v1",
```
- 协议版本号
- 支持协议演化和向后兼容

```kotlin
    val messageId: String,
```
- 消息唯一标识
- 用于去重和确认

```kotlin
    @SerializedName("msg_type")
    val messageType: Int,
```
- `@SerializedName`: Gson序列化时使用的JSON字段名
- 消息类型：文本、图片、表情等

```kotlin
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_name")
    val senderName: String,
```
- 发送者ID和昵称
- JSON字段使用下划线命名

```kotlin
    val content: String,
```
- 消息内容
- 文本消息直接存储，其他类型存储URL或路径

```kotlin
    val timestamp: Long = System.currentTimeMillis(),
```
- 发送时间戳
- 默认当前时间

```kotlin
    val payload: Map<String, Any> = emptyMap()
```
- 扩展字段
- 支持自定义数据，不影响协议兼容性

```kotlin
companion object {
    const val MSG_TYPE_TEXT = 1
    const val MSG_TYPE_IMAGE = 2
    const val MSG_TYPE_EMOJI = 3
    const val MSG_TYPE_FILE = 4
    const val MSG_TYPE_SYSTEM = 5
```
- 消息类型常量

```kotlin
    fun toJson(message: ChatProtocolMessage): String {
        return Gson().toJson(message)
    }
    
    fun fromJson(json: String): ChatProtocolMessage {
        return Gson().fromJson(json, ChatProtocolMessage::class.java)
    }
}
```
- JSON序列化和反序列化辅助方法

##### ChatProtocolFrame - 协议帧

```kotlin
data class ChatProtocolFrame(
    val frameType: Int,
```
- 帧类型：数据帧、控制帧、心跳帧

```kotlin
    val frameId: String,
```
- 帧唯一标识

```kotlin
    @SerializedName("from_user")
    val fromUser: String,
    @SerializedName("to_user")
    val toUser: String? = null,
```
- `fromUser`: 发送用户
- `toUser`: 接收用户，null表示广播

```kotlin
    val data: String,
```
- 负载数据
- 通常是JSON格式的ChatProtocolMessage

```kotlin
    val checksum: String? = null
```
- 可选的校验码
- 用于验证数据完整性

```kotlin
companion object {
    const val FRAME_TYPE_DATA = 1
    const val FRAME_TYPE_CONTROL = 2
    const val FRAME_TYPE_HEARTBEAT = 3
```
- 帧类型常量

##### ControlMessage - 控制消息

```kotlin
data class ControlMessage(
    val action: String,
```
- 控制操作类型

```kotlin
    val userId: String,
    val userName: String? = null,
```
- 用户标识和名称

```kotlin
    val ipAddress: String? = null,
    val port: Int? = null,
```
- 网络信息（用于P2P等功能）

```kotlin
    val extra: Map<String, Any> = emptyMap()
```
- 扩展字段

```kotlin
companion object {
    const val ACTION_LOGIN = "login"
    const val ACTION_LOGOUT = "logout"
    const val ACTION_REGISTER = "register"
    const val ACTION_HEARTBEAT = "heartbeat"
    const val ACTION_ACK = "ack"
```
- 控制操作类型常量

### 3.2 ChatSocketClient.kt

**文件路径**: `app/src/main/java/com/example/myapplication/network/socket/ChatSocketClient.kt`

**作用**: Socket客户端，管理与服务器的连接和通信。

#### 代码详解

```kotlin
class ChatSocketClient(
    private val serverHost: String,
    private val serverPort: Int
) {
```
- 构造函数接收服务器地址和端口

```kotlin
private var socket: Socket? = null
private var reader: BufferedReader? = null
private var writer: BufferedWriter? = null
```
- Socket连接和IO流
- 使用可空类型，连接前为null

```kotlin
private var currentUserId: String? = null
```
- 当前登录用户ID

```kotlin
private val gson = Gson()
```
- JSON解析器

```kotlin
private val listeners = ConcurrentHashMap<String, (ChatProtocolFrame) -> Unit>()
```
- 消息监听器集合
- 使用`ConcurrentHashMap`保证线程安全
- 键为监听器ID，值为回调函数

```kotlin
private var job: Job? = null
```
- 接收消息的协程Job
- 用于取消协程

```kotlin
var isConnected = false
    private set
```
- 连接状态
- `private set`：外部只读，内部可写

```kotlin
suspend fun connect(userId: String, userName: String): Boolean = withContext(Dispatchers.IO) {
```
- `suspend`: 挂起函数，不阻塞调用线程
- `withContext(Dispatchers.IO)`: 在IO调度器上执行
- 返回连接是否成功

```kotlin
try {
    socket = Socket(serverHost, serverPort)
    reader = BufferedReader(InputStreamReader(socket!!.inputStream))
    writer = BufferedWriter(OutputStreamWriter(socket!!.outputStream))
```
- 创建Socket连接
- 包装输入输出流为BufferedReader/Writer

```kotlin
    currentUserId = userId
    isConnected = true
```
- 记录状态

```kotlin
    val loginMsg = ControlMessage(
        action = ControlMessage.ACTION_LOGIN,
        userId = userId,
        userName = userName,
        ipAddress = getLocalIpAddress()
    )
```
- 创建登录控制消息

```kotlin
    val frame = ChatProtocolFrame(
        frameType = ChatProtocolFrame.FRAME_TYPE_CONTROL,
        frameId = generateFrameId(),
        fromUser = userId,
        data = gson.toJson(loginMsg)
    )
```
- 包装为协议帧

```kotlin
    sendFrame(frame)
```
- 发送登录帧

```kotlin
    startReceivingMessages()
```
- 启动接收消息的协程

```kotlin
    Log.d("ChatSocketClient", "Connected to $serverHost:$serverPort as $userId")
    true
```
- 记录日志，返回成功

```kotlin
} catch (e: Exception) {
    Log.e("ChatSocketClient", "Connection failed", e)
    isConnected = false
    false
}
```
- 异常处理，返回失败

```kotlin
suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
```
- 断开连接

```kotlin
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
```
- 发送登出消息

```kotlin
    job?.cancel()
    reader?.close()
    writer?.close()
    socket?.close()
    isConnected = false
```
- 取消协程
- 关闭所有资源
- 更新状态

```kotlin
suspend fun sendFrame(frame: ChatProtocolFrame): Boolean = withContext(Dispatchers.IO) {
    try {
        val json = gson.toJson(frame)
        writer?.write(json + "\n")
        writer?.flush()
```
- 序列化帧为JSON
- 添加换行符作为消息分隔符
- 刷新缓冲区确保发送

```kotlin
        Log.d("ChatSocketClient", "Frame sent: ${frame.frameId}")
        true
    } catch (e: Exception) {
        Log.e("ChatSocketClient", "Send frame error", e)
        false
    }
}
```
- 记录日志，返回结果

```kotlin
fun addMessageListener(listenerId: String, listener: (ChatProtocolFrame) -> Unit) {
    listeners[listenerId] = listener
}
```
- 注册消息监听器
- ViewModel通过此方法接收消息

```kotlin
fun removeMessageListener(listenerId: String) {
    listeners.remove(listenerId)
}
```
- 移除监听器

```kotlin
private fun startReceivingMessages() {
    job = CoroutineScope(Dispatchers.IO).launch {
```
- 在IO调度器上启动新协程

```kotlin
        try {
            while (isConnected && reader != null) {
                val line = reader?.readLine() ?: break
```
- 循环读取消息行
- readLine阻塞直到有数据或连接关闭

```kotlin
                try {
                    val frame = gson.fromJson(line, ChatProtocolFrame::class.java)
                    Log.d("ChatSocketClient", "Received frame: ${frame.frameId}")
```
- 解析JSON为协议帧

```kotlin
                    listeners.values.forEach { listener ->
                        try {
                            listener(frame)
                        } catch (e: Exception) {
                            Log.e("ChatSocketClient", "Listener error", e)
                        }
                    }
```
- 通知所有监听器
- 每个监听器异常不影响其他

```kotlin
                } catch (e: Exception) {
                    Log.e("ChatSocketClient", "Parse frame error", e)
                }
            }
        } finally {
            isConnected = false
        }
    }
}
```
- finally确保更新状态

```kotlin
suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
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
}
```
- 发送心跳保持连接

```kotlin
companion object {
    private fun generateFrameId(): String {
        return "frame_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
```
- 生成唯一帧ID
- 时间戳+随机数

```kotlin
    private fun getLocalIpAddress(): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop net.hostname")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
```
- 获取本地IP地址
- 使用Android系统属性

---

## 4. 数据仓库层

### ChatRepository.kt

**文件路径**: `app/src/main/java/com/example/myapplication/repository/ChatRepository.kt`

**作用**: 数据仓库，封装所有数据访问操作，提供统一接口。

#### 代码详解

```kotlin
class ChatRepository(private val database: AppDatabase) {
```
- 构造函数接收数据库实例
- 通过数据库访问所有DAO

#### Contact操作

```kotlin
suspend fun addContact(contact: Contact) = database.contactDao().insert(contact)
```
- 添加联系人
- 直接委托给DAO

```kotlin
suspend fun updateContact(contact: Contact) = database.contactDao().update(contact)
```
- 更新联系人

```kotlin
suspend fun deleteContact(contact: Contact) = database.contactDao().delete(contact)
```
- 删除联系人

```kotlin
suspend fun getContact(id: Int) = database.contactDao().getContactById(id)
```
- 根据ID获取联系人

```kotlin
fun getAllContacts(): Flow<List<Contact>> = database.contactDao().getAllContacts()
```
- 获取所有联系人
- 返回Flow，响应式更新

```kotlin
fun getContactsByLastSeen(): Flow<List<Contact>> = database.contactDao().getContactsByLastSeen()
```
- 按最后活跃时间排序

```kotlin
suspend fun updateContactStatus(contactId: Int, status: Int) {
    database.contactDao().updateContactStatus(contactId, status, System.currentTimeMillis())
}
```
- 更新联系人状态
- 自动更新时间戳

#### Message操作

```kotlin
suspend fun addMessage(message: Message) = database.messageDao().insert(message)
```
- 添加消息

```kotlin
suspend fun addMessages(messages: List<Message>) = database.messageDao().insertAll(messages)
```
- 批量添加消息

```kotlin
suspend fun updateMessage(message: Message) = database.messageDao().update(message)
```
- 更新消息

```kotlin
fun getMessagesInSession(sessionId: Int): Flow<List<Message>> = 
    database.messageDao().getMessagesBySession(sessionId)
```
- 获取会话消息

```kotlin
suspend fun getUnreadMessages(sessionId: Int) = 
    database.messageDao().getUnreadMessages(sessionId)
```
- 获取未读消息

```kotlin
fun getUnreadCount(sessionId: Int): Flow<Int> = 
    database.messageDao().getUnreadCount(sessionId)
```
- 获取未读数量

```kotlin
suspend fun markSessionAsRead(sessionId: Int) {
    database.messageDao().markSessionAsRead(sessionId, System.currentTimeMillis())
}
```
- 标记会话已读
- 自动设置读取时间

```kotlin
suspend fun markMessageAsRead(messageId: Int) {
    database.messageDao().markMessageAsRead(messageId, System.currentTimeMillis())
}
```
- 标记单条消息已读

```kotlin
fun getMessagesWithSender(sessionId: Int) = 
    database.messageDao().getMessagesWithSender(sessionId)
```
- 获取消息和发送者信息

#### ChatSession操作

```kotlin
suspend fun createSession(session: ChatSession): Long = 
    database.chatSessionDao().insert(session)
```
- 创建会话
- 返回生成的ID

```kotlin
suspend fun updateSession(session: ChatSession) = 
    database.chatSessionDao().update(session)
```
- 更新会话

```kotlin
suspend fun deleteSession(sessionId: Int) = 
    database.chatSessionDao().deleteSession(sessionId)
```
- 删除会话

```kotlin
suspend fun getSession(sessionId: Int) = 
    database.chatSessionDao().getSessionById(sessionId)
```
- 获取单个会话

```kotlin
suspend fun getSessionByContact(contactId: Int) = 
    database.chatSessionDao().getSessionByContactId(contactId)
```
- 根据联系人查找会话

```kotlin
fun getAllSessions(): Flow<List<ChatSession>> = 
    database.chatSessionDao().getAllSessions()
```
- 获取所有会话

```kotlin
fun getSessionsWithContacts() = 
    database.chatSessionDao().getSessionsWithContacts()
```
- 获取会话和联系人组合数据

```kotlin
fun getPinnedSessions(): Flow<List<ChatSession>> = 
    database.chatSessionDao().getPinnedSessions()
```
- 获取置顶会话

```kotlin
fun getActiveSessions(): Flow<List<ChatSession>> = 
    database.chatSessionDao().getActiveSessions()
```
- 获取活跃会话

```kotlin
suspend fun updateLastMessage(sessionId: Int, lastMessage: String, time: Long) {
    database.chatSessionDao().updateLastMessage(sessionId, lastMessage, time)
}
```
- 更新最后一条消息

```kotlin
suspend fun setPinned(sessionId: Int, pinned: Boolean) {
    database.chatSessionDao().setPinned(sessionId, pinned)
}
```
- 设置置顶

```kotlin
suspend fun setArchived(sessionId: Int, archived: Boolean) {
    database.chatSessionDao().setArchived(sessionId, archived)
}
```
- 设置归档

**Repository模式的优势**:
1. 单一数据访问入口
2. 便于单元测试（可以Mock Repository）
3. 封装复杂逻辑
4. 支持多数据源（本地+远程）

---

## 5. 服务器层

### 5.1 ChatServer.java

**文件路径**: `app/src/main/java/com/example/myapplication/server/ChatServer.java`

**作用**: 聊天服务器主类，管理客户端连接和消息路由。

#### 代码详解

```java
public class ChatServer {
```
- 服务器主类

```java
private int port;
private ServerSocket serverSocket;
private ExecutorService executorService;
private Map<String, ClientHandler> connectedClients;
private Gson gson;
```
- `port`: 服务器监听端口
- `serverSocket`: 服务器Socket
- `executorService`: 线程池，处理并发连接
- `connectedClients`: 在线客户端映射表
- `gson`: JSON解析器

```java
public ChatServer(int port) {
    this.port = port;
    this.connectedClients = new ConcurrentHashMap<>();
    this.executorService = Executors.newFixedThreadPool(50);
    this.gson = new Gson();
}
```
- 构造函数
- `ConcurrentHashMap`: 线程安全的Map
- 固定大小线程池，最多50个并发连接

```java
public void start() {
    try {
        serverSocket = new ServerSocket(port);
        System.out.println("[ChatServer] Started on port " + port);
```
- 创建服务器Socket
- 绑定到指定端口

```java
        while (true) {
            Socket clientSocket = serverSocket.accept();
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            System.out.println("[ChatServer] New connection from " + clientIp);
```
- 主循环接受客户端连接
- `accept()`阻塞直到有新连接

```java
            ClientHandler handler = new ClientHandler(this, clientSocket);
            executorService.execute(handler);
        }
```
- 为每个客户端创建处理器
- 提交到线程池执行

```java
    } catch (IOException e) {
        System.err.println("[ChatServer] Error: " + e.getMessage());
        e.printStackTrace();
    } finally {
        stop();
    }
}
```
- 异常处理
- finally确保关闭服务器

```java
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
```
- 停止服务器
- 关闭ServerSocket
- 停止线程池

```java
public void registerClient(String userId, ClientHandler handler) {
    connectedClients.put(userId, handler);
    System.out.println("[ChatServer] Client registered: " + userId);
    
    broadcastSystemMessage(userId + " has logged in");
}
```
- 注册新客户端
- 添加到在线用户映射
- 广播用户上线消息

```java
public void unregisterClient(String userId) {
    connectedClients.remove(userId);
    System.out.println("[ChatServer] Client unregistered: " + userId);
    
    broadcastSystemMessage(userId + " has logged out");
}
```
- 注销客户端
- 从在线用户映射中移除
- 广播用户离线消息

```java
public void routeMessage(String fromUserId, String toUserId, String message) {
    ClientHandler targetHandler = connectedClients.get(toUserId);
    if (targetHandler != null) {
        targetHandler.sendMessage(message);
        System.out.println("[ChatServer] Message routed from " + fromUserId + " to " + toUserId);
    } else {
        System.out.println("[ChatServer] Target user offline: " + toUserId);
    }
}
```
- 单播消息路由
- 查找目标用户的处理器
- 如果在线则发送，否则记录日志

```java
public void broadcastMessage(String message) {
    for (ClientHandler handler : connectedClients.values()) {
        handler.sendMessage(message);
    }
}
```
- 广播消息给所有在线用户
- 遍历所有处理器

```java
private void broadcastSystemMessage(String message) {
    Map<String, Object> systemMsg = new HashMap<>();
    systemMsg.put("frameType", 2); // 控制帧
    systemMsg.put("action", "system");
    systemMsg.put("message", message);
    systemMsg.put("timestamp", System.currentTimeMillis());
    
    broadcastMessage(gson.toJson(systemMsg));
}
```
- 广播系统消息
- 构造控制帧
- JSON序列化后广播

```java
public List<String> getOnlineUsers() {
    return new ArrayList<>(connectedClients.keySet());
}

public int getOnlineCount() {
    return connectedClients.size();
}
```
- 获取在线用户信息
- 用于统计和监控

```java
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
```
- main方法，服务器启动入口
- 支持命令行参数指定端口
- 默认端口8888

### 5.2 ClientHandler.java

**文件路径**: `app/src/main/java/com/example/myapplication/server/ClientHandler.java`

**作用**: 客户端处理器，为每个连接维护一个处理线程。

#### 代码详解

```java
public class ClientHandler implements Runnable {
```
- 实现Runnable接口，在线程池中运行

```java
private ChatServer server;
private Socket socket;
private BufferedReader reader;
private BufferedWriter writer;
private String userId;
private Gson gson;
```
- `server`: 服务器引用，用于消息路由
- `socket`: 客户端连接
- `reader/writer`: IO流
- `userId`: 客户端用户ID
- `gson`: JSON解析器

```java
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
```
- 构造函数
- 包装Socket的输入输出流

```java
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
```
- 线程运行方法
- 循环读取客户端消息
- readLine阻塞直到有数据
- finally确保清理资源

```java
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
```
- 处理接收到的消息
- 解析JSON
- 根据帧类型分发处理

```java
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
```
- 处理数据帧
- 提取发送者和接收者
- 判断单播还是广播

```java
        System.out.println("[ClientHandler] Data frame from " + fromUser + " to " + (toUser == null ? "broadcast" : toUser));
    } catch (Exception e) {
        System.err.println("[ClientHandler] Data frame error: " + e.getMessage());
    }
}
```
- 记录日志
- 异常处理

```java
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
```
- 处理控制帧
- 解析控制消息
- 根据action分发处理

```java
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
```
- 处理登录
- 记录用户ID
- 注册到服务器
- 发送确认响应

```java
private void handleRegister(String userId, JsonObject controlMsg) {
    System.out.println("[ClientHandler] Register request from " + userId);
    
    JsonObject response = new JsonObject();
    response.addProperty("action", "ack");
    response.addProperty("status", "success");
    response.addProperty("message", "Register successful");
    response.addProperty("timestamp", System.currentTimeMillis());
    
    sendMessage(response.toString());
}
```
- 处理注册
- 简化实现，实际应用需要连接数据库

```java
private void handleLogout(String userId) {
    System.out.println("[ClientHandler] Logout request from " + userId);
    server.unregisterClient(userId);
    
    try {
        socket.close();
    } catch (IOException e) {
        System.err.println("[ClientHandler] Close socket error: " + e.getMessage());
    }
}
```
- 处理登出
- 从服务器注销
- 关闭连接

```java
private void handleHeartbeat(JsonObject frame) {
    System.out.println("[ClientHandler] Heartbeat from " + frame.get("fromUser").getAsString());
}
```
- 处理心跳
- 记录日志保持连接活跃

```java
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
```
- 发送消息给客户端
- `synchronized`: 保证线程安全
- 添加换行符作为消息分隔符
- flush确保立即发送

```java
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
```
- 清理资源
- 注销用户
- 关闭所有IO资源

---

## 6. UI层

### 6.1 ViewModel层

#### 6.1.1 ChatViewModel.kt

**文件路径**: `app/src/main/java/com/example/myapplication/ui/viewmodel/ChatViewModel.kt`

**作用**: 聊天窗口的业务逻辑控制器，管理消息发送接收和UI状态。

##### 代码详解

```kotlin
class ChatViewModel(
    private val repository: ChatRepository,
    private val socketClient: ChatSocketClient
) : ViewModel() {
```
- 继承ViewModel，生命周期感知
- 依赖注入Repository和SocketClient

```kotlin
private val gson = Gson()
```
- JSON解析器

```kotlin
private val _currentSessionId = MutableStateFlow<Int?>(null)
val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()
```
- 当前打开的会话ID
- `MutableStateFlow`: 可变状态流
- `asStateFlow()`: 转换为只读的StateFlow

```kotlin
private val _currentContact = MutableStateFlow<Contact?>(null)
val currentContact: StateFlow<Contact?> = _currentContact.asStateFlow()
```
- 当前聊天的联系人信息

```kotlin
val messages: Flow<List<Message>> = currentSessionId
    .filterNotNull()
    .flatMapLatest { sessionId -> repository.getMessagesInSession(sessionId) }
```
- 消息列表Flow
- `filterNotNull()`: 过滤null值
- `flatMapLatest`: 当sessionId变化时切换到新的Flow
- 自动从数据库获取并更新消息

```kotlin
private val _inputText = MutableStateFlow("")
val inputText: StateFlow<String> = _inputText.asStateFlow()
```
- 输入框文本状态

```kotlin
private val _showEmojiPicker = MutableStateFlow(false)
val showEmojiPicker: StateFlow<Boolean> = _showEmojiPicker.asStateFlow()
```
- 表情选择器显示状态

```kotlin
val isConnected: StateFlow<Boolean> = MutableStateFlow(socketClient.isConnected).asStateFlow()
```
- 网络连接状态

```kotlin
var currentUserId: String? = null
```
- 当前登录用户ID

```kotlin
init {
    socketClient.addMessageListener("chatViewModel") { frame ->
        handleSocketMessage(frame)
    }
}
```
- 初始化块
- 注册Socket消息监听器
- 接收到消息时调用handleSocketMessage

```kotlin
fun openChat(sessionId: Int, contact: Contact) {
    _currentSessionId.value = sessionId
    _currentContact.value = contact
    
    viewModelScope.launch {
        repository.markSessionAsRead(sessionId)
    }
}
```
- 打开聊天窗口
- 设置当前会话和联系人
- 标记消息为已读
- `viewModelScope.launch`: 在ViewModel的协程作用域中执行

```kotlin
fun updateInputText(text: String) {
    _inputText.value = text
}
```
- 更新输入框内容

**主要功能方法**（未完全展示）：
- `sendMessage()`: 发送消息到服务器和数据库
- `toggleEmojiPicker()`: 切换表情选择器显示
- `handleSocketMessage()`: 处理接收到的Socket消息
- `insertEmoji()`: 插入表情到输入框

**MVVM架构优势**：
- UI和业务逻辑分离
- 数据持有者，生命周期感知
- 自动UI更新（通过StateFlow）

#### 6.1.2 SessionListViewModel.kt

**文件路径**: `app/src/main/java/com/example/myapplication/ui/viewmodel/SessionListViewModel.kt`

**作用**: 会话列表的业务逻辑控制器。

##### 代码详解

```kotlin
class SessionListViewModel(private val repository: ChatRepository) : ViewModel() {
```
- 只依赖Repository，不需要网络层

```kotlin
val sessions: StateFlow<List<ChatSession>> = repository.getAllSessions()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```
- 所有会话列表
- `stateIn`: 将Flow转换为StateFlow
- `SharingStarted.Lazily`: 第一个订阅者出现时开始
- `emptyList()`: 初始值

```kotlin
val activeSessions: StateFlow<List<ChatSession>> = repository.getActiveSessions()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```
- 活跃（未归档）会话

```kotlin
val pinnedSessions: StateFlow<List<ChatSession>> = repository.getPinnedSessions()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```
- 置顶会话

```kotlin
val totalUnreadCount: StateFlow<Int> = sessions
    .map { list -> list.sumOf { it.unreadCount } }
    .stateIn(viewModelScope, SharingStarted.Lazily, 0)
```
- 总未读消息数
- `map`: 转换Flow数据
- `sumOf`: 计算总和

```kotlin
fun togglePinSession(sessionId: Int, currentPinned: Boolean) {
    viewModelScope.launch {
        repository.setPinned(sessionId, !currentPinned)
    }
}
```
- 切换置顶状态
- 在协程中调用Repository

```kotlin
fun toggleArchiveSession(sessionId: Int, currentArchived: Boolean) {
    viewModelScope.launch {
        repository.setArchived(sessionId, !currentArchived)
    }
}
```
- 切换归档状态

**其他方法**（未完全展示）：
- `deleteSession()`: 删除会话
- `createNewSession()`: 创建新会话
- `refreshSessions()`: 刷新会话列表

### 6.2 UI组件层

#### ChatScreen.kt

**文件路径**: `app/src/main/java/com/example/myapplication/ui/screen/ChatScreen.kt`

**作用**: 包含所有UI Composable组件。

由于UI代码较长（约350行），这里列出主要组件说明：

##### 主要Composable组件

**1. ChatScreenContainer**
```kotlin
@Composable
fun ChatScreenContainer(
    repository: ChatRepository,
    socketClient: ChatSocketClient
)
```
- 根容器组件
- 管理ViewModel实例
- 处理屏幕路由和导航

**2. SessionListScreen**
```kotlin
@Composable
fun SessionListScreen(
    sessions: List<ChatSession>,
    onSessionClick: (Int, Contact) -> Unit
)
```
- 会话列表页面
- 使用LazyColumn显示列表
- 支持点击进入聊天

**3. ChatScreen**
```kotlin
@Composable
fun ChatScreen(
    messages: List<Message>,
    currentContact: Contact?,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit
)
```
- 聊天窗口主界面
- 包含顶部栏、消息列表、输入框
- 处理消息发送和返回

**4. MessagesListPanel**
```kotlin
@Composable
fun MessagesListPanel(messages: List<Message>)
```
- 消息列表显示区域
- 使用LazyColumn实现虚拟滚动
- 自动滚动到最新消息

**5. MessageBubble**
```kotlin
@Composable
fun MessageBubble(
    message: Message,
    isSentByMe: Boolean
)
```
- 单条消息气泡
- 区分发送/接收样式
- 显示消息内容、时间、已读状态

**6. MessageInputPanel**
```kotlin
@Composable
fun MessageInputPanel(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onEmojiClick: () -> Unit
)
```
- 消息输入区域
- 文本输入框
- 发送按钮和表情按钮

**7. EmojiPickerPanel**
```kotlin
@Composable
fun EmojiPickerPanel(
    onEmojiSelected: (String) -> Unit
)
```
- 表情选择器
- 网格布局显示表情
- 点击插入表情到输入框

**8. SessionItemComposable**
```kotlin
@Composable
fun SessionItemComposable(
    session: ChatSession,
    contact: Contact,
    onClick: () -> Unit
)
```
- 会话列表项
- 显示头像、昵称、最后消息、未读数
- 支持长按操作（置顶、归档）

**Compose架构特点**：
- 声明式UI
- 状态驱动更新
- 组件化和复用
- 类型安全

### 6.3 主题层

#### Theme.kt

**文件路径**: `app/src/main/java/com/example/myapplication/ui/theme/Theme.kt`

**作用**: 定义应用的Material Design主题。

##### 主要内容

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)
```
- 深色和浅色配色方案
- 定义主色、次色、强调色等

```kotlin
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)
```
- 主题Composable函数
- 支持深色模式
- 支持动态颜色（Android 12+）
- 应用Material Design样式

**其他主题文件**:
- `Color.kt`: 颜色常量定义
- `Type.kt`: 字体排版定义

---

## 总结

本文档详细介绍了项目中每个源代码文件的实现细节：

1. **应用入口**: MainActivity初始化核心组件
2. **数据库层**: Room实现数据持久化
3. **网络层**: Socket客户端和协议定义
4. **仓库层**: 统一数据访问接口
5. **服务器层**: Java实现的聊天服务器
6. **UI层**: Compose实现的响应式UI

每个模块都遵循单一职责原则，层次清晰，易于维护和扩展。

---

**文档版本**: 1.0.0  
**最后更新**: 2025年12月15日  
**作者**: 聊天器项目开发团队
