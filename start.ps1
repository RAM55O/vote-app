Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Compiling Java Backend with SQLite JDBC..." -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sources = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib/*" -d bin $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n===================================================" -ForegroundColor Green
Write-Host "  Starting TaskFlow Application..." -ForegroundColor Green
Write-Host "  Open http://localhost:8080 in your browser" -ForegroundColor Yellow
Write-Host "===================================================`n" -ForegroundColor Green

java -cp "bin;lib/*" com.todo.Main
