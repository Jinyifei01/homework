# 聊天器 - Android聊天应用

一个完整的Android聊天应用，包括客户端UI和Java实现的聊天服务器，支持实时消息、表情、联系人管理等功能。

## 📱 功能特性

### 客户端功能
- ✅ **会话列表** - 显示所有聊天会话，支持置顶和归档
- ✅ **实时消息** - Socket连接，实时收发消息
- ✅ **未读消息** - 未读消息计数和清除标记
- ✅ **聊天窗口** - 消息列表、输入框、表情选择器
- ✅ **时间戳** - 消息时间显示和智能格式化
- ✅ **联系人管理** - 联系人的添加、编辑、删除
- ✅ **本地存储** - Room数据库存储所有数据
- ✅ **消息类型** - 支持文本、表情、图片等多种消息类型

### 服务器功能
- ✅ **多客户端支持** - 使用线程池处理多个并发连接
- ✅ **消息路由** - 单播和广播消息分发
- ✅ **用户管理** - 登录、登出、在线状态管理
- ✅ **心跳检测** - 定期心跳保持连接活跃
- ✅ **可扩展协议** - JSON格式，支持版本控制和扩展字段

## 🏗️ 技术架构

### 客户端技术栈
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **数据库**: Room + SQLite
- **异步**: Coroutines + Flow
- **网络**: Socket + 自定义协议
- **架构**: MVVM + Repository Pattern

### 服务器技术栈
- **语言**: Java
- **网络**: ServerSocket + Thread
- **协议**: JSON over TCP
- **并发**: ExecutorService线程池
- **架构**: 单体服务

### 通信协议
- **格式**: JSON
- **传输**: TCP Socket
- **可扩展性**: 版本控制 + 自定义Payload字段
- **支持**: 文本、表情、图片、文件等多种消息类型

## 📁 项目结构

```
app/src/main/java/com/example/myapplication/
├── db/                          # 数据库层
│   ├── AppDatabase.kt
│   ├── dao/
│   │   ├── ContactDao.kt
│   │   ├── MessageDao.kt
│   │   └── ChatSessionDao.kt
│   └── entity/
│       ├── Contact.kt
│       ├── Message.kt
│       ├── ChatSession.kt
│       └── ViewModels.kt
├── network/                     # 网络通信层
│   ├── protocol/
│   │   └── ChatProtocol.kt
│   └── socket/
│       └── ChatSocketClient.kt
├── repository/                  # 数据仓库层
│   └── ChatRepository.kt
├── server/                      # 聊天服务器
│   ├── ChatServer.java
│   └── ClientHandler.java
└── ui/                          # UI层
    ├── screen/
    │   └── ChatScreen.kt
    ├── viewmodel/
    │   ├── ChatViewModel.kt
    │   └── SessionListViewModel.kt
    └── theme/
        └── Theme.kt
```

详见: [CODE_STRUCTURE.md](CODE_STRUCTURE.md)

## 🚀 快速开始

### 前置条件
- Android Studio (Koala或更新版本)
- Android SDK 24+
- Java 11+
- Gradle 8.0+

### 客户端运行

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd MyApplication2
   ```

2. **配置服务器地址**
   
   编辑 `app/src/main/java/com/example/myapplication/MainActivity.kt`:
   ```kotlin
   socketClient = ChatSocketClient("192.168.1.100", 8888) // 修改服务器地址
   ```

3. **构建和运行**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.example.myapplication/.MainActivity
   ```

### 服务器运行

1. **编译服务器**
   ```bash
   cd app/src/main/java/com/example/myapplication/server
   javac ChatServer.java ClientHandler.java
   ```

2. **启动服务器**
   ```bash
   # 使用默认端口 8888
   java ChatServer
   
   # 或指定端口
   java ChatServer 9999
   ```

3. **验证服务器**
   ```bash
   telnet localhost 8888
   ```

## 📖 使用说明

### 聊天客户端

1. **启动应用** - 应用自动连接到服务器
2. **查看会话** - 主屏幕显示所有聊天会话
3. **进入聊天** - 点击会话进入聊天窗口
4. **发送消息** - 输入内容，点击发送或添加表情
5. **消息标记** - 消息自动标记为已读
6. **管理会话** - 长按会话可置顶、归档或删除

### 服务器管理

1. **启动服务** - 执行启动命令
2. **监控连接** - 查看服务器日志
3. **消息路由** - 服务器自动处理消息分发
4. **停止服务** - Ctrl+C停止

## 🔧 配置说明

### 数据库配置

`AppDatabase.kt`:
```kotlin
@Database(entities = [Contact::class, Message::class, ChatSession::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    // 数据库配置
}
```

### Socket配置

`MainActivity.kt`:
```kotlin
socketClient = ChatSocketClient(
    serverHost = "192.168.1.100",  // 服务器IP
    serverPort = 8888               // 服务器端口
)
```

### 协议配置

`ChatProtocol.kt`:
```kotlin
const val MSG_TYPE_TEXT = 1      // 文本消息
const val MSG_TYPE_IMAGE = 2     // 图片消息
const val MSG_TYPE_EMOJI = 3     // 表情消息
const val MSG_TYPE_FILE = 4      // 文件消息
const val MSG_TYPE_SYSTEM = 5    // 系统消息
```

## 📊 数据库设计

### 表结构

**contacts** - 联系人表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| userId | TEXT | 用户ID |
| nickname | TEXT | 昵称 |
| status | INTEGER | 在线状态 |
| lastSeenTime | LONG | 最后在线时间 |
| ipAddress | TEXT | IP地址 |

**messages** - 消息表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| sessionId | INTEGER | 会话ID |
| contactId | INTEGER | 发送者ID |
| content | TEXT | 消息内容 |
| messageType | INTEGER | 消息类型 |
| timestamp | LONG | 时间戳 |
| isRead | BOOLEAN | 是否已读 |

**chat_sessions** - 会话表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| contactId | INTEGER | 联系人ID |
| lastMessage | TEXT | 最后消息 |
| unreadCount | INTEGER | 未读数 |
| isPinned | BOOLEAN | 是否置顶 |
| isArchived | BOOLEAN | 是否归档 |

## 🔌 协议详解

### 消息帧结构

```json
{
  "frameType": 1,
  "frameId": "frame_123",
  "fromUser": "user1",
  "toUser": "user2",
  "data": "{message_content}",
  "version": "v1"
}
```

### 消息内容格式

```json
{
  "version": "v1",
  "messageId": "msg_123",
  "msg_type": 1,
  "sender_id": "user1",
  "sender_name": "Alice",
  "content": "Hello World",
  "timestamp": 1701609600000,
  "payload": {}
}
```

详见: [DESIGN_DOC.md](DESIGN_DOC.md#通信协议设计)

## 🧪 测试

### 单元测试

```bash
./gradlew testDebug
```

### 集成测试

```bash
./gradlew connectedAndroidTest
```

### 手动测试场景

1. **消息发送接收**
   - 启动服务器和2个客户端
   - 从客户端1发送消息
   - 验证客户端2接收到消息

2. **连接管理**
   - 关闭客户端连接
   - 验证服务器日志显示连接关闭
   - 重新启动客户端，验证重连

3. **消息持久化**
   - 发送消息
   - 关闭应用
   - 重启应用
   - 验证消息仍在数据库中

## 🐛 常见问题

**Q: 无法连接到服务器?**
- A: 检查服务器IP地址和端口配置
- 确保服务器正在运行
- 检查防火墙设置
- 使用 `adb logcat` 查看日志

**Q: 消息无法发送?**
- A: 检查Socket连接状态
- 确保输入框不为空
- 查看服务器日志

**Q: 应用崩溃?**
- A: 检查 `adb logcat` 中的异常信息
- 确保权限正确配置
- 尝试清除应用数据重启

**Q: 服务器启动失败?**
- A: 检查端口是否被占用
- 确保Java版本11+
- 查看Java错误日志

## 📈 性能优化建议

### 短期优化
- [ ] 消息虚拟列表（只渲染可见项）
- [ ] 图片懒加载
- [ ] 数据库查询优化（添加索引）
- [ ] 消息批量发送

### 中期优化
- [ ] 本地消息队列（离线缓存）
- [ ] 消息压缩传输
- [ ] 连接池管理
- [ ] 后台心跳线程

### 长期优化
- [ ] 服务器集群化
- [ ] 消息队列集成（RabbitMQ/Kafka）
- [ ] NIO框架升级（Netty）
- [ ] 消息历史分表存储

详见: [DESIGN_DOC.md#后期优化思路](DESIGN_DOC.md#后期优化思路)

## 🔐 安全性说明

当前版本：
- ❌ 暂无端到端加密
- ❌ 暂无用户认证
- ❌ 暂无消息签名验证

后续改进：
- [ ] SSL/TLS加密传输
- [ ] Token认证机制
- [ ] 消息数字签名
- [ ] 敏感字段脱敏

## 📝 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启Pull Request

## 📄 文档索引

- [设计思路文档](DESIGN_DOC.md) - 完整的架构和设计说明
- [代码结构说明](CODE_STRUCTURE.md) - 详细的模块和代码解释
- [快速开始](#快速开始) - 本README中的快速开始部分
- [API文档](#) - （待完善）

## 📞 联系方式

- 📧 Email: [yifei_jin001@163.com]
- 💬 Issues: [[GitHub Issues](https://github.com/Jinyifei01/homework)]


## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可。

## 🙏 致谢

感谢以下开源项目的支持：
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/jetpack/androidx/releases/room)
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)

---

**最后更新**: 2025年12月3日

**项目版本**: 1.0.0

**开发人员**: [Your Name]

**状态**: ✅ 核心功能完成 | 🔄 优化进行中 | ⏳ 文档完善中
# homework
