@echo off
setlocal
:: Check for admin privileges
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting Administrator permissions...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

:MENU
cls
echo ========================================================
echo           SYSTEM-WIDE AD BLOCKER SETUP
echo ========================================================
echo  1. Enable Ad Blocker (Block Ads in Apps and Browsers)
echo  2. Disable Ad Blocker (Restore Default / Automatic DNS)
echo  3. Exit
echo ========================================================
set /p choice="Select an option [1-3]: "

if "%choice%"=="1" goto ENABLE
if "%choice%"=="2" goto DISABLE
if "%choice%"=="3" goto EXIT
goto MENU

:ENABLE
echo.
echo Enabling AdGuard DNS on all active network adapters...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-NetAdapter | Where-Object { $_.Status -eq 'Up' } | ForEach-Object { Set-DnsClientServerAddress -InterfaceIndex $_.InterfaceIndex -ServerAddresses ('94.140.14.14','94.140.15.15') }; Clear-DnsClientCache"
echo.
echo Done! Ads are now blocked across all apps and websites.
pause
goto MENU

:DISABLE
echo.
echo Restoring automatic DNS (DHCP)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-NetAdapter | Where-Object { $_.Status -eq 'Up' } | ForEach-Object { Set-DnsClientServerAddress -InterfaceIndex $_.InterfaceIndex -ResetServerAddresses }; Clear-DnsClientCache"
echo.
echo Default DNS settings restored.
pause
goto MENU

:EXIT
exit /b
