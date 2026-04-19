@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0build-release.ps1" %*
exit /b %errorlevel%
