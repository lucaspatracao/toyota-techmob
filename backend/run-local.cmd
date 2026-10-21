@echo off
setlocal

REM Variáveis lidas pelo application.properties:
REM   DB_HOST (localhost), DB_PORT (3306), DB_NAME (techmob), DB_USER (root), DB_PASSWORD
if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_NAME set "DB_NAME=techmob"
if not defined DB_USER set "DB_USER=root"

REM A senha não fica gravada no repositório: use a variável ou digite agora.
if not defined DB_PASSWORD set /p "DB_PASSWORD=Senha do MySQL (%DB_USER%): "

cd /d "%~dp0"

echo.
echo Iniciando backend TechMob...
echo Banco: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo Usuario: %DB_USER%
echo.

call .\mvnw.cmd spring-boot:run

endlocal
