#!/bin/bash

# 停止并删除现有容器
docker-compose down

# 构建后端
./mvnw clean package -DskipTests

# 构建并启动所有服务
docker-compose up -d --build

echo "部署完成！"
echo "前端访问地址: http://localhost"
echo "后端访问地址: http://localhost:8080/api/v1"

