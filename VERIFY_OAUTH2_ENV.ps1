# Verify OAuth2 Environment Variables
# Run this script to verify GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are set correctly

Write-Host "=== OAuth2 Environment Variables Verification ===" -ForegroundColor Cyan
Write-Host ""

$clientId = $env:GOOGLE_CLIENT_ID
$clientSecret = $env:GOOGLE_CLIENT_SECRET

Write-Host "1. Checking GOOGLE_CLIENT_ID..." -ForegroundColor Yellow
if ([string]::IsNullOrWhiteSpace($clientId)) {
    Write-Host "   ❌ GOOGLE_CLIENT_ID is NOT set!" -ForegroundColor Red
    Write-Host "   Run .\SETUP_OAUTH2.ps1 to set environment variables." -ForegroundColor Red
} else {
    Write-Host "   ✅ GOOGLE_CLIENT_ID is set" -ForegroundColor Green
    Write-Host "   Value: $clientId" -ForegroundColor White
    
$env:GOOGLE_CLIENT_ID = $Env:GOOGLE_CLIENT_ID
    if ($clientId -eq $expectedClientId) {
        Write-Host "   ✅ Client ID matches expected value" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Client ID does NOT match expected value!" -ForegroundColor Yellow
        Write-Host "   Expected: $expectedClientId" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "2. Checking GOOGLE_CLIENT_SECRET..." -ForegroundColor Yellow
if ([string]::IsNullOrWhiteSpace($clientSecret)) {
    Write-Host "   ❌ GOOGLE_CLIENT_SECRET is NOT set!" -ForegroundColor Red
    Write-Host "   Run .\SETUP_OAUTH2.ps1 to set environment variables." -ForegroundColor Red
} else {
    Write-Host "   ✅ GOOGLE_CLIENT_SECRET is set" -ForegroundColor Green
    Write-Host "   Length: $($clientSecret.Length) characters" -ForegroundColor White
    Write-Host "   First 10 chars: $($clientSecret.Substring(0, [Math]::Min(10, $clientSecret.Length)))..." -ForegroundColor White
    Write-Host "   Last 4 chars: ...$($clientSecret.Substring($clientSecret.Length - 4))" -ForegroundColor White
    
$env:GOOGLE_CLIENT_SECRET = $Env:GOOGLE_CLIENT_SECRET
    if ($clientSecret -eq $expectedSecret) {
        Write-Host "   ✅ Client Secret matches expected value" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Client Secret does NOT match expected value!" -ForegroundColor Yellow
        Write-Host "   Expected ends with: ...11T8" -ForegroundColor Yellow
        Write-Host "   Actual ends with: ...$($clientSecret.Substring($clientSecret.Length - 4))" -ForegroundColor Yellow
        Write-Host "   If secret doesn't match, it may have been regenerated in Google Console." -ForegroundColor Yellow
        Write-Host "   You may need to get the current secret from Google Console and update SETUP_OAUTH2.ps1" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
if ([string]::IsNullOrWhiteSpace($clientId) -or [string]::IsNullOrWhiteSpace($clientSecret)) {
    Write-Host "❌ OAuth2 environment variables are NOT properly set!" -ForegroundColor Red
    Write-Host "   Run: .\SETUP_OAUTH2.ps1" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "✅ OAuth2 environment variables are set" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Start application from THIS terminal: ./mvnw.cmd spring-boot:run" -ForegroundColor White
    Write-Host "2. Check application logs for OAuth2 configuration confirmation" -ForegroundColor White
    Write-Host "3. Verify redirect URI in Google Console: http://localhost:8080/login/oauth2/code/google" -ForegroundColor White
    exit 0
}

