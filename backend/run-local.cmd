@echo off
setlocal

set "DB_USERNAME=%DB_USERNAME%"
if not defined DB_USERNAME set "DB_USERNAME=root"

set "DB_PASSWORD=%DB_PASSWORD%"
if not defined DB_PASSWORD set "DB_PASSWORD=123456"

set "DB_URL=%DB_URL%"
if not defined DB_URL set "DB_URL=jdbc:mysql://localhost:3306/techmob?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"

cd /d "%~dp0"

echo.
echo Iniciando backend TechMob...
echo Banco: %DB_URL%
echo Usuario: %DB_USERNAME%
echo.

call .\mvnw.cmd spring-boot:run

endlocal
