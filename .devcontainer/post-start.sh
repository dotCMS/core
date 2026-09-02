#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  dotCMS Development Environment Status ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

command_exists() { command -v "$1" >/dev/null 2>&1; }

# 1) Repo root (same strategy as setup.sh)
if git rev-parse --show-toplevel >/dev/null 2>&1; then
  WORKSPACE_ROOT="$(git rev-parse --show-toplevel)"
else
  echo -e "${RED}✗ Not inside a git repository. Cannot locate workspace root.${NC}"
  exit 1
fi

# 2) Compose location (same as setup.sh)
COMPOSE_DIR="${WORKSPACE_ROOT}/docker/docker-compose-examples/single-node-demo-site"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"

if [ ! -f "${COMPOSE_FILE}" ]; then
  echo -e "${RED}✗ Compose file not found:${NC} ${COMPOSE_FILE}"
  exit 1
fi

# 3) Compose command (prefer docker compose v2)
if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command_exists docker-compose; then
  COMPOSE="docker-compose"
else
  echo -e "${RED}✗ Docker Compose not found.${NC}"
  exit 1
fi

# 4) Docker ready check (quick)
if ! docker info >/dev/null 2>&1; then
  echo -e "${YELLOW}⚠ Docker daemon not reachable yet.${NC}"
  echo -e "${YELLOW}  Try:${NC} docker info"
  exit 0
fi

cd "${COMPOSE_DIR}"

echo -e "${BLUE}Compose directory:${NC} ${COMPOSE_DIR}"
echo -e "${BLUE}Service Status:${NC}"
${COMPOSE} -f "${COMPOSE_FILE}" ps
echo ""

# 5) dotCMS readiness check
echo -e "${BLUE}Checking dotCMS availability on http://localhost:8082 ...${NC}"
max_attempts=60
attempt=0
dotcms_ready=false
last_code=""

while [ $attempt -lt $max_attempts ]; do
  last_code="$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082 || true)"
  if echo "$last_code" | grep -Eq "200|302|301"; then
    echo -e "${GREEN}✓ dotCMS is responding (HTTP ${last_code})${NC}"
    dotcms_ready=true
    break
  fi
  sleep 3
  attempt=$((attempt + 1))
done

if [ "$dotcms_ready" = false ]; then
  echo -e "${YELLOW}⚠ dotCMS is still initializing (last HTTP code: ${last_code})${NC}"
  echo -e "${YELLOW}  Monitor progress with:${NC} ${COMPOSE} -f ${COMPOSE_FILE} logs -f --tail=200 dotcms"
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Available Services:${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  • dotCMS HTTP:${NC}       http://localhost:8082"
echo -e "${GREEN}  • dotCMS HTTPS:${NC}      https://localhost:8443"
echo -e "${GREEN}  • OpenSearch:${NC}        http://localhost:9200"
echo -e "${GREEN}  • PostgreSQL:${NC}        localhost:5432"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Default Credentials:${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  dotCMS Admin:${NC}"
echo -e "    Username: ${YELLOW}admin@dotcms.com${NC}"
echo -e "    Password: ${YELLOW}admin${NC}"
echo ""
echo -e "${GREEN}  PostgreSQL:${NC}"
echo -e "    Database: ${YELLOW}dotcms${NC}"
echo -e "    Username: ${YELLOW}dotcmsdbuser${NC}"
echo -e "    Password: ${YELLOW}password${NC}"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Quick Commands:${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Docker Management:${NC}"
echo -e "    ${COMPOSE} -f ${COMPOSE_FILE} logs -f --tail=200 dotcms"
echo -e "    ${COMPOSE} -f ${COMPOSE_FILE} logs -f --tail=200 opensearch"
echo -e "    ${COMPOSE} -f ${COMPOSE_FILE} logs -f --tail=200 db"
echo -e "    ${COMPOSE} -f ${COMPOSE_FILE} restart"
echo -e "    ${COMPOSE} -f ${COMPOSE_FILE} down"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
echo ""
