@echo off
REM Setup Google OAuth2 Environment Variables for Windows
REM Run this file before starting the application

$env:GOOGLE_CLIENT_ID="3354716..."
$env:GOOGLE_CLIENT_SECRET="GOCSPX-..."


echo Google OAuth2 Environment Variables đã được set!
echo.
echo Client ID: %GOOGLE_CLIENT_ID%
echo.
echo Bây giờ bạn có thể start application với:
echo ./mvnw.cmd spring-boot:run
echo.
echo Hoặc nếu đang chạy trong IDE, đảm bảo IDE được khởi động từ terminal này.
echo.
pause

