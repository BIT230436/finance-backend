# Setup Google OAuth2 Environment Variables for PowerShell
# Run this file before starting the application: .\SETUP_OAUTH2.ps1

$env:GOOGLE_CLIENT_ID="3354716..."
$env:GOOGLE_CLIENT_SECRET="GOCSPX-..."

Write-Host "Google OAuth2 Environment Variables đã được set!" -ForegroundColor Green
Write-Host ""
Write-Host "Client ID: $env:GOOGLE_CLIENT_ID" -ForegroundColor Cyan
Write-Host ""
Write-Host "Bây giờ bạn có thể start application với:" -ForegroundColor Yellow
Write-Host "./mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "Hoặc nếu đang chạy trong IDE, đảm bảo IDE được khởi động từ terminal này." -ForegroundColor Yellow

