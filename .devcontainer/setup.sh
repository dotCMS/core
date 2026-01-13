#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Setting up dotCMS Development Environment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Detect workspace root
if [ -d "/workspaces" ]; then
  WORKSPACE_ROOT=$(find /workspaces -maxdepth 1 -type d -name "*" ! -path /workspaces | head -n 1)
else
  WORKSPACE_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
fi

if [ -z "$WORKSPACE_ROOT" ]; then
  WORKSPACE_ROOT="$(pwd)"
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
for i in {1..60}; do
  if docker info >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker daemon is ready${NC}"
    break
  fi
  sleep 1
done

if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}✗ Cannot connect to Docker daemon after waiting.${NC}"
  echo -e "${YELLOW}Tip:${NC} If you're using docker-in-docker, ensure the feature is enabled in devcontainer.json."
  exit 1
fi

# Pick compose command
COMPOSE=""
if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command_exists docker-compose; then
  COMPOSE="docker-compose"
else
  echo -e "${RED}✗ Docker Compose not found.${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Using compose command:${NC} ${COMPOSE}"

# Find compose file (root preferred)
COMPOSE_FILE=""
if [ -f "${WORKSPACE_ROOT}/docker-compose.yml" ]; then
  COMPOSE_FILE="${WORKSPACE_ROOT}/docker-compose.yml"
elif [ -f "${WORKSPACE_ROOT}/compose.yml" ]; then
  COMPOSE_FILE="${WORKSPACE_ROOT}/compose.yml"
else
  echo -e "${RED}✗ No docker-compose.yml or compose.yml found in repo root:${NC} ${WORKSPACE_ROOT}"
  echo -e "${YELLOW}If your compose is in a subfolder, update this script to point there.${NC}"
  exit 1
fi

echo -e "${BLUE}Compose file:${NC} ${COMPOSE_FILE}"
cd "${WORKSPACE_ROOT}"

echo ""
echo -e "${BLUE}Starting dotCMS services...${NC}"
echo -e "${YELLOW}This may take several minutes on first startup${NC}"
echo ""

# Pull + up
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
