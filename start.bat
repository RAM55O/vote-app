@echo off
echo ===================================================
echo   Compiling Java Backend with SQLite JDBC...
echo ===================================================

if not exist "bin" mkdir bin

javac -encoding UTF-8 -cp "lib/*" -d bin src/com/todo/model/*.java src/com/todo/util/*.java src/com/todo/dao/*.java src/com/todo/handler/*.java src/com/todo/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   Starting TaskFlow Application...
echo   Open http://localhost:8080 in your browser
echo ===================================================
echo.

java -cp "bin;lib/*" com.todo.Main
