@echo off
title RMI Server - Mini Library
cd /d "%~dp0"
echo ==================================================
echo   DANG KHOI DONG MINI LIBRARY RMI SERVER...
echo ==================================================
call mvn compile
java -cp target/classes server.RMIServer
pause

