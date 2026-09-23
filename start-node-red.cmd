@echo off
REM Instale as dependencias com: npm install --prefix node-red
cd /d "%~dp0"
if not exist "%CD%\node-red\node_modules\.bin\node-red.cmd" (
	echo Node-RED nao esta instalado. Execute: npm install --prefix node-red
	exit /b 1
)
"%CD%\node-red\node_modules\.bin\node-red.cmd" --userDir "%CD%\node-red" --settings "%CD%\node-red\settings.js"
