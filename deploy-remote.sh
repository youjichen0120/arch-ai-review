#!/bin/bash

# 服务器信息
SERVER_IP=175.24.61.92
SERVER_USER=root
SERVER_PASSWORD=zszq,666

# 本地目录
BACKEND_DIR="$(pwd)"
FRONTEND_DIR="$(pwd)/../arch-ai-review-web"

# 远程目录
REMOTE_DIR="/opt/arch-ai-review"

# 构建后端
echo "构建后端..."
cd "$BACKEND_DIR"
./mvnw clean package -DskipTests

# 构建前端
echo "构建前端..."
cd "$FRONTEND_DIR"
npm run build

# 创建临时部署目录
TEMP_DIR=$(mktemp -d)
mkdir -p "$TEMP_DIR/backend"
mkdir -p "$TEMP_DIR/frontend/dist"

# 复制文件到临时目录
cp "$BACKEND_DIR/target/"*.jar "$TEMP_DIR/backend/app.jar"
cp "$BACKEND_DIR/Dockerfile" "$TEMP_DIR/backend/"
cp "$BACKEND_DIR/docker-compose.yml" "$TEMP_DIR/"
cp -r "$FRONTEND_DIR/dist/"* "$TEMP_DIR/frontend/dist/"
cp "$FRONTEND_DIR/Dockerfile" "$TEMP_DIR/frontend/"
cp "$FRONTEND_DIR/nginx.conf" "$TEMP_DIR/frontend/"

# 创建远程部署脚本
cat > "$TEMP_DIR/remote-deploy.sh" << 'EOF'
#!/bin/bash

# 创建目录
mkdir -p /opt/arch-ai-review/backend
mkdir -p /opt/arch-ai-review/frontend

# 复制文件
cp -r backend/* /opt/arch-ai-review/backend/
cp -r frontend/* /opt/arch-ai-review/frontend/
cp docker-compose.yml /opt/arch-ai-review/

# 进入部署目录
cd /opt/arch-ai-review

# 停止并删除现有容器
docker-compose down

# 构建并启动所有服务
docker-compose up -d --build

echo "部署完成！"
echo "前端访问地址: http://$SERVER_IP"
echo "后端访问地址: http://$SERVER_IP:8080/api/v1"
EOF

# 使脚本可执行
chmod +x "$TEMP_DIR/remote-deploy.sh"

# 使用sshpass传输文件到服务器
echo "传输文件到服务器..."
sshpass -p "$SERVER_PASSWORD" ssh -o StrictHostKeyChecking=no "$SERVER_USER@$SERVER_IP" "mkdir -p $REMOTE_DIR"
sshpass -p "$SERVER_PASSWORD" scp -r "$TEMP_DIR/"* "$SERVER_USER@$SERVER_IP:$REMOTE_DIR"

# 执行远程部署脚本
echo "执行远程部署..."
sshpass -p "$SERVER_PASSWORD" ssh -o StrictHostKeyChecking=no "$SERVER_USER@$SERVER_IP" "cd $REMOTE_DIR && chmod +x remote-deploy.sh && ./remote-deploy.sh"

# 清理临时目录
rm -rf "$TEMP_DIR"

echo "部署过程完成！"

