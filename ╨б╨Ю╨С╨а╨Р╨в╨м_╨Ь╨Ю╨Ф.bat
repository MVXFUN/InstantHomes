@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0СОБРАТЬ_МОД.ps1"
pause
