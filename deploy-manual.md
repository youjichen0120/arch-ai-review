# 手动部署指南

## 1. 准备工作

### 在服务器上安装必要工具
```bash
# 连接到服务器
ssh root@175.24.61.92

# 安装Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
systemctl start docker
systemctl enable docker

# 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 安装MySQL客户端（用于初始化数据库）
yum install -y mysql
```

## 2. 上传文件到服务器

### 方法1：使用scp命令
```bash
# 在本地执行，上传后端文件
scp -r C:\Users\chen\IdeaProjects\arch-ai-review\* root@175.24.61.92:/opt/arch-ai-review/

# 上传前端文件
scp -r C:\Users\chen\IdeaProjects\arch-ai-review-web\* root@175.24.61.92:/opt/arch-ai-review-web/
```

### 方法2：使用WinSCP等图形化工具
- 连接到服务器：175.24.61.92，用户名：root，密码：zszq,666
- 创建目录：/opt/arch-ai-review 和 /opt/arch-ai-review-web
- 上传所有文件

## 3. 在服务器上执行部署

```bash
# 连接到服务器
ssh root@175.24.61.92

# 创建目录
mkdir -p /opt/arch-ai-review
mkdir -p /opt/arch-ai-review-web

# 进入后端目录
cd /opt/arch-ai-review

# 初始化数据库
mysql -h 175.24.61.92 -u root -p'zszq,666' < init-database.sql

# 启动服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

## 4. 访问应用

- 前端：http://175.24.61.92
- 后端API：http://175.24.61.92:8080/api/v1
- 默认管理员账号：admin / admin

## 5. 常用管理命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 进入容器
docker exec -it arch-ai-review-backend bash
docker exec -it arch-ai-review-frontend sh
```
