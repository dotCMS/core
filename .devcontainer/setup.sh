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

# Function to check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Verify Docker is available
echo -e "${BLUE}Checking Docker availability...${NC}"
if ! command_exists docker; then
  echo -e "${RED}✗ Docker not found. Please ensure Docker is properly installed.${NC}"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}✗ Cannot connect to Docker daemon. Please check Docker setup.${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Docker is available${NC}"

echo "Waiting for Docker daemon..."
for i in {1..60}; do
  if docker info >/dev/null 2>&1; then
    echo "✓ Docker is ready"
    break
  fi
  sleep 1
done

if ! docker info >/dev/null 2>&1; then
  echo "✗ Docker daemon not ready after waiting."
  exit 1
fi

# Verify docker-compose is available
echo -e "${BLUE}Checking Docker Compose availability...${NC}"
if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
  echo -e "${RED}✗ Docker Compose not found. Please ensure Docker Compose is installed.${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Docker Compose is available${NC}"

# Navigate to the docker-compose directory
# Detect workspace root (works with both local and Codespaces paths)
if [ -d "/workspaces" ]; then
  # In Codespaces
  WORKSPACE_ROOT=$(find /workspaces -maxdepth 1 -type d -name "*" ! -path /workspaces | head -n 1)
else
  # Local development - get git root
  WORKSPACE_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
fi

COMPOSE_DIR="$WORKSPACE_ROOT/docker/docker-compose-examples/single-node-demo-site"

if [ ! -f "$COMPOSE_DIR/docker-compose.yml" ]; then
  echo -e "${RED}✗ docker-compose.yml not found at $COMPOSE_DIR${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}Starting dotCMS services...${NC}"
echo -e "${YELLOW}This may take 5-10 minutes on first startup${NC}"
echo ""

cd "$COMPOSE_DIR"

# Pull images first to show progress
echo -e "${BLUE}Pulling Docker images...${NC}"
docker-compose pull

# Start services in background
echo -e "${BLUE}Starting services...${NC}"
docker-compose up -d

# Wait a moment for containers to start
sleep 5

# Show status
echo ""
echo -e "${GREEN}✓ Services started successfully!${NC}"
echo ""
echo -e "${BLUE}Container Status:${NC}"
docker-compose ps

echo ""
echo -e "${YELLOW}Note: dotCMS needs 3-5 minutes to fully initialize.${NC}"
echo -e "${YELLOW}You can monitor progress with: docker logs -f dotcms${NC}"
echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
