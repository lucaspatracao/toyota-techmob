@echo off
REM Script para iniciar Node-RED localizado no projeto
cd /d "%~dp0"
"%APPDATA%\npm\node-red.cmd" --userDir "%CD%\node-red" --settings "%CD%\node-red\settings.js"
