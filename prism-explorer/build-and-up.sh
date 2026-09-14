#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXPLORER="$(cd "$(dirname "$0")" && pwd)"

echo "=== 0. Clean up old environment ==="
cd "$EXPLORER"
docker compose down || echo "compose down failed" >&2

echo "--- Clean up dangling images ---"
docker image prune -f || echo "Image cleanup failed" >&2
echo ""

echo "=== 1. Build prism-explorer ==="
cd "$EXPLORER"
mvn clean package -DskipTests -B

echo "=== 2. Build prism-apm-common ==="
cd "$ROOT"
mvn clean install -pl prism-apm-common -DskipTests -B

echo "=== 3. Build prism-apm-ui-server ==="
cd "$ROOT"
mvn clean package -pl prism-apm-ui-server -DskipTests -B

echo "=== 4. Build prism-apm-ui (frontend) ==="
cd "$ROOT/prism-apm-ui"
npm install
npm run build

echo "=== 5. Start Docker Compose ==="
cd "$EXPLORER"
docker compose up -d --build

echo "=== All done ==="
echo ""
echo -e "\U0001F680 Prism Explorer is running!"
echo -e "\U00002728 Ready to explore? Open http://localhost in your browser to experience the dashboard with mock telemetry data."
