@echo off
chcp 65001 >nul
echo ========================================
echo   在线考试系统 - 客户端启动
echo ========================================
echo.

cd /d "%~dp0"

:: 收集所有源文件
dir /s /b src\*.java > sources.tmp 2>nul

echo Compiling...
javac -encoding UTF-8 -d bin @sources.tmp 2>&1
del sources.tmp 2>nul

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ======== COMPILATION FAILED ========
    echo Please check error messages above.
    echo Make sure JDK is installed and javac is in PATH.
    echo.
    pause
    exit /b 1
)

echo.
echo Compilation successful. Starting client...
echo.

java -Dfile.encoding=UTF-8 -cp bin com.exam.client.LoginGUI
pause
