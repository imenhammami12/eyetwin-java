@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Starting EyeTwin Platform...
echo Using JAVA_HOME: %JAVA_HOME%
cd /d "%~dp0"
call mvnw.cmd clean javafx:run
pause
