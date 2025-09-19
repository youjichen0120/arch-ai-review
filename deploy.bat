@echo off
echo 开始部署到服务器...

REM 服务器信息
set SERVER_IP=175.24.61.92
set SERVER_USER=root
set SERVER_PASSWORD=zszq,666

REM 本地路径
set BACKEND_DIR=C:\Users\chen\IdeaProjects\arch-ai-review
set FRONTEND_DIR=C:\Users\chen\IdeaProjects\arch-ai-review-web

REM 远程路径
set REMOTE_DIR=/opt/arch-ai-review

echo 1. 构建后端应用...
cd /d "%BACKEND_DIR%"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 后端构建失败！
    pause
    exit /b 1
)

echo 2. 构建前端应用...
cd /d "%FRONTEND_DIR%"
call npm run build
if %errorlevel% neq 0 (
    echo 前端构建失败！
    pause
    exit /b 1
)

echo 3. 创建部署包...
set TEMP_DIR=%TEMP%\arch-ai-review-deploy
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
mkdir "%TEMP_DIR%"
mkdir "%TEMP_DIR%\backend"
mkdir "%TEMP_DIR%\frontend"

REM 复制后端文件
copy "%BACKEND_DIR%\target\*.jar" "%TEMP_DIR%\backend\app.jar"
copy "%BACKEND_DIR%\Dockerfile" "%TEMP_DIR%\backend\"
copy "%BACKEND_DIR%\docker-compose.yml" "%TEMP_DIR%\"
copy "%BACKEND_DIR%\init-database.sql" "%TEMP_DIR%\"

REM 复制前端文件
xcopy "%FRONTEND_DIR%\dist\*" "%TEMP_DIR%\frontend\" /E /I
copy "%FRONTEND_DIR%\Dockerfile" "%TEMP_DIR%\frontend\"
copy "%FRONTEND_DIR%\nginx.conf" "%TEMP_DIR%\frontend\"

echo 4. 创建远程部署脚本...
echo #!/bin/bash > "%TEMP_DIR%\remote-deploy.sh"
echo echo "开始服务器端部署..." >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 创建目录 >> "%TEMP_DIR%\remote-deploy.sh"
echo mkdir -p /opt/arch-ai-review/backend >> "%TEMP_DIR%\remote-deploy.sh"
echo mkdir -p /opt/arch-ai-review/frontend >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 复制文件 >> "%TEMP_DIR%\remote-deploy.sh"
echo cp -r backend/* /opt/arch-ai-review/backend/ >> "%TEMP_DIR%\remote-deploy.sh"
echo cp -r frontend/* /opt/arch-ai-review/frontend/ >> "%TEMP_DIR%\remote-deploy.sh"
echo cp docker-compose.yml /opt/arch-ai-review/ >> "%TEMP_DIR%\remote-deploy.sh"
echo cp init-database.sql /opt/arch-ai-review/ >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 进入部署目录 >> "%TEMP_DIR%\remote-deploy.sh"
echo cd /opt/arch-ai-review >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 初始化数据库 >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "初始化数据库..." >> "%TEMP_DIR%\remote-deploy.sh"
echo mysql -h 175.24.61.92 -u root -p'zszq,666' ^< init-database.sql >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 停止并删除现有容器 >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "停止现有容器..." >> "%TEMP_DIR%\remote-deploy.sh"
echo docker-compose down >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 构建并启动所有服务 >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "启动服务..." >> "%TEMP_DIR%\remote-deploy.sh"
echo docker-compose up -d --build >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 等待服务启动 >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "等待服务启动..." >> "%TEMP_DIR%\remote-deploy.sh"
echo sleep 30 >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo # 检查服务状态 >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "检查服务状态..." >> "%TEMP_DIR%\remote-deploy.sh"
echo docker-compose ps >> "%TEMP_DIR%\remote-deploy.sh"
echo. >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "部署完成！" >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "前端访问地址: http://%SERVER_IP%" >> "%TEMP_DIR%\remote-deploy.sh"
echo echo "后端访问地址: http://%SERVER_IP%:8080/api/v1" >> "%TEMP_DIR%\remote-deploy.sh"

echo 5. 请手动执行以下步骤：
echo.
echo 1. 使用WinSCP或其他工具连接到服务器 %SERVER_IP%
echo    用户名: %SERVER_USER%
echo    密码: %SERVER_PASSWORD%
echo.
echo 2. 将临时目录中的所有文件上传到服务器的 /opt/arch-ai-review/ 目录
echo    临时目录: %TEMP_DIR%
echo.
echo 3. 在服务器上执行以下命令：
echo    cd /opt/arch-ai-review
echo    chmod +x remote-deploy.sh
echo    ./remote-deploy.sh
echo.
echo 4. 部署完成后访问：
echo    前端: http://%SERVER_IP%
echo    后端: http://%SERVER_IP%:8080/api/v1
echo    默认账号: admin / admin
echo.

pause
