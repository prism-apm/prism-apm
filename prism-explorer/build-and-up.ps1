$root = Resolve-Path "$PSScriptRoot\.."
$explorer = "$PSScriptRoot"

Write-Host "=== 0. Clean up old environment ===" -ForegroundColor Cyan
Push-Location $explorer
docker compose down
if ($LASTEXITCODE -ne 0) {
    Write-Host "compose down failed" -ForegroundColor Yellow
}
Pop-Location

Write-Host "--- Clean up dangling images ---" -ForegroundColor Cyan
docker image prune -f
if ($LASTEXITCODE -ne 0) {
    Write-Host "Image cleanup failed" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "=== 1. Build prism-explorer ===" -ForegroundColor Cyan
Push-Location $explorer
mvn clean package -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "prism-explorer build failed" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

Write-Host "=== 2. Build prism-apm-common ===" -ForegroundColor Cyan
Push-Location $root
mvn clean install -pl prism-apm-common -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "prism-apm-common build failed" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

Write-Host "=== 3. Build prism-apm-ui-server ===" -ForegroundColor Cyan
Push-Location $root
mvn clean package -pl prism-apm-ui-server -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "prism-apm-ui-server build failed" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

Write-Host "=== 4. Build prism-apm-ui (frontend) ===" -ForegroundColor Cyan
Push-Location "$root\prism-apm-ui"
npm install
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend build failed" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

Write-Host "=== 5. Start Docker Compose ===" -ForegroundColor Cyan
Push-Location $explorer
docker compose up -d --build
Pop-Location

Write-Host "=== All done ===" -ForegroundColor Green
Write-Host ""
Write-Host "🚀 Prism Explorer is running!" -ForegroundColor Cyan
Write-Host "✨ Ready to explore? Open http://localhost in your browser to experience the dashboard with mock telemetry data." -ForegroundColor Cyan
