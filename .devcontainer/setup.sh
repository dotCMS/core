#!/bin/bash
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Setting up dotCMS Development Environment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

command_exists() { command -v "$1" >/dev/null 2>&1; }

# ✅ Always use git to find repo root (works in Codespaces)
if git rev-parse --show-toplevel >/dev/null 2>&1; then
  WORKSPACE_ROOT="$(git rev-parse --show-toplevel)"
else
  echo -e "${RED}✗ Not inside a git repository. Cannot locate workspace root.${NC}"
  exit 1
fi

echo -e "${BLUE}Workspace root:${NC} ${WORKSPACE_ROOT}"

# Verify Docker is installed
echo -e "${BLUE}Checking Docker availability...${NC}"
if ! command_exists docker; then
  echo -e "${RED}✗ Docker not found in PATH.${NC}"
  exit 1
fi

# Wait for Docker daemon
echo -e "${BLUE}Waiting for Docker daemon...${NC}"
for i in {1..90}; do
  if docker info >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker daemon is ready${NC}"
    break
  fi
  sleep 1
done

if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}✗ Cannot connect to Docker daemon after waiting.${NC}"
  echo -e "${YELLOW}Tip:${NC} If you're using docker-in-docker, ensure devcontainer.json has \"privileged\": true and you are NOT mounting /var/run/docker.sock."
  exit 1
fi

# Pick compose command
if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command_exists docker-compose; then
  COMPOSE="docker-compose"
else
  echo -e "${RED}✗ Docker Compose not found.${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Using compose command:${NC} ${COMPOSE}"

# ✅ Compose is in subfolder
COMPOSE_DIR="${WORKSPACE_ROOT}/docker/docker-compose-examples/single-node-demo-site"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"

if [ ! -f "${COMPOSE_FILE}" ]; then
  echo -e "${RED}✗ Compose file not found:${NC} ${COMPOSE_FILE}"
  exit 1
fi

echo -e "${BLUE}Compose file:${NC} ${COMPOSE_FILE}"
cd "${COMPOSE_DIR}"

echo ""
echo -e "${BLUE}Starting dotCMS services...${NC}"
echo -e "${YELLOW}This may take several minutes on first startup${NC}"
echo ""

${COMPOSE} -f "${COMPOSE_FILE}" pull
${COMPOSE} -f "${COMPOSE_FILE}" up -d

echo ""
echo -e "${GREEN}✓ Services started (containers launching)${NC}"
echo ""
echo -e "${BLUE}Container Status:${NC}"
${COMPOSE} -f "${COMPOSE_FILE}" ps

echo ""
echo -e "${YELLOW}Note:${NC} dotCMS can take a few minutes to fully initialize."
echo -e "${YELLOW}Monitor with:${NC} ${COMPOSE} -f ${COMPOSE_FILE} logs -f --tail=200 dotcms"
echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
