#!/bin/bash

# 聊天器项目 - 完整部署脚本

set -e

echo "=== 聊天器 Android 应用部署脚本 ==="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 函数定义
print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# 1. 环境检查
print_header "环境检查"

if ! command -v java &> /dev/null; then
    print_warning "Java未找到，请安装Java 11+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]*')
print_success "Java已安装: $JAVA_VERSION"

if ! command -v gradle &> /dev/null; then
    print_warning "Gradle未找到，使用gradlew"
fi

if ! command -v git &> /dev/null; then
    print_warning "Git未找到"
fi

print_success "环境检查完成"
echo ""

# 2. 构建客户端
print_header "构建Android客户端"

if [ -f "gradlew" ]; then
    ./gradlew clean assembleDebug
    print_success "客户端构建完成"
else
    print_warning "gradlew未找到，跳过Android构建"
fi
echo ""

# 3. 编译服务器
print_header "编译服务器"

SERVER_DIR="app/src/main/java/com/example/myapplication/server"
if [ -d "$SERVER_DIR" ]; then
    cd "$SERVER_DIR"
    javac *.java
    cd ../../../../../../../../../..
    print_success "服务器编译完成"
else
    print_warning "服务器源代码未找到"
fi
echo ""

# 4. 创建部署目录
print_header "创建部署目录"

mkdir -p deploy/client
mkdir -p deploy/server
print_success "部署目录创建完成"
echo ""

# 5. 复制文件
print_header "复制部署文件"

# 复制APK
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp app/build/outputs/apk/debug/app-debug.apk deploy/client/
    print_success "APK已复制"
fi

# 复制服务器编译文件
if [ -d "$SERVER_DIR" ]; then
    cp "$SERVER_DIR"/*.class deploy/server/
    print_success "服务器文件已复制"
fi

# 复制文档
cp DESIGN_DOC.md deploy/
cp CODE_STRUCTURE.md deploy/
cp README.md deploy/
print_success "文档已复制"
echo ""

# 6. 生成启动脚本
print_header "生成启动脚本"

cat > deploy/server/start_server.sh << 'EOF'
#!/bin/bash
echo "启动聊天服务器..."
java -cp . com.example.myapplication.server.ChatServer ${1:-8888}
EOF

chmod +x deploy/server/start_server.sh
print_success "服务器启动脚本已生成"

# 创建Windows启动脚本
cat > deploy/server/start_server.bat << 'EOF'
@echo off
echo 启动聊天服务器...
set PORT=%1
if "%PORT%"=="" set PORT=8888
java -cp . com.example.myapplication.server.ChatServer %PORT%
EOF

print_success "Windows启动脚本已生成"
echo ""

# 7. 生成部署说明
print_header "生成部署说明"

cat > deploy/DEPLOY_GUIDE.md << 'EOF'
# 聊天器 - 部署指南

## 项目文件说明

```
deploy/
├── client/
│   └── app-debug.apk          # Android客户端APK
├── server/
│   ├── *.class                 # 编译后的服务器类文件
│   ├── start_server.sh         # Linux/Mac启动脚本
│   └── start_server.bat        # Windows启动脚本
├── DESIGN_DOC.md               # 设计文档
├── CODE_STRUCTURE.md           # 代码结构说明
└── README.md                   # 项目说明
```

## 部署步骤

### 步骤1: 启动服务器

#### Linux/Mac
```bash
cd server
./start_server.sh 8888
```

#### Windows
```cmd
cd server
start_server.bat 8888
```

### 步骤2: 安装客户端

```bash
adb install client/app-debug.apk
```

### 步骤3: 配置连接地址

编辑客户端代码中的服务器地址：
```kotlin
socketClient = ChatSocketClient("YOUR_SERVER_IP", 8888)
```

### 步骤4: 运行应用

```bash
adb shell am start -n com.example.myapplication/.MainActivity
```

## 验证安装

### 验证服务器
```bash
telnet localhost 8888
```

### 验证客户端
- 检查应用是否正常启动
- 查看是否能成功连接到服务器
- 尝试发送消息

## 常见问题

### Q: 无法连接到服务器
- 检查服务器是否正在运行
- 检查IP地址和端口是否正确
- 检查防火墙设置
- 查看logcat日志：`adb logcat | grep ChatSocket`

### Q: APK安装失败
- 检查设备Android版本是否支持(API 24+)
- 清除旧版本应用：`adb uninstall com.example.myapplication`
- 检查存储空间是否充足

### Q: 服务器启动失败
- 检查端口是否被占用：`netstat -ano | findstr 8888`
- 检查Java版本：`java -version`
- 查看错误日志

## 性能测试

### 消息吞吐量测试

```bash
# 启动多个客户端测试
for i in {1..10}; do
    adb shell am start -n com.example.myapplication/.MainActivity &
done
```

### 连接稳定性测试

```bash
# 监控服务器连接
watch -n 1 'netstat -an | grep 8888'
```

## 后续优化

参见 DESIGN_DOC.md 中的"后期优化思路"部分

EOF

print_success "部署指南已生成"
echo ""

# 8. 总结
print_header "部署总结"

echo "✓ 构建完成"
echo "✓ 编译完成"
echo "✓ 文件已准备"
echo ""
echo "部署文件位置: $(pwd)/deploy"
echo ""
echo "后续步骤："
echo "1. 启动服务器: cd deploy/server && ./start_server.sh"
echo "2. 安装应用: adb install deploy/client/app-debug.apk"
echo "3. 配置服务器地址在MainActivity中"
echo "4. 运行应用: adb shell am start -n com.example.myapplication/.MainActivity"
echo ""
print_success "部署脚本执行完成！"
