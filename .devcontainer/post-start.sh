#!/bin/bash

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

# Change to docker-compose directory
# Detect workspace root (works with both local and Codespaces paths)
if [ -d "/workspaces" ]; then
  # In Codespaces
  WORKSPACE_ROOT=$(find /workspaces -maxdepth 1 -type d -name "*" ! -path /workspaces | head -n 1)
else
  # Local development - get git root
  WORKSPACE_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
fi

COMPOSE_DIR="$WORKSPACE_ROOT/docker/docker-compose-examples/single-node-demo-site"

if [ -d "$COMPOSE_DIR" ]; then
  cd "$COMPOSE_DIR"

  # Check if services are running
  echo -e "${BLUE}Service Status:${NC}"
  docker-compose ps
  echo ""

  # Check dotCMS readiness
  echo -e "${BLUE}Checking dotCMS availability...${NC}"

  max_attempts=60
  attempt=0
  dotcms_ready=false

  while [ $attempt -lt $max_attempts ]; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8082 2>/dev/null | grep -q "200\|302"; then
      echo -e "${GREEN}✓ dotCMS is ready and responding!${NC}"
      dotcms_ready=true
      break
    else
      sleep 3
      attempt=$((attempt + 1))
    fi
  done

  if [ "$dotcms_ready" = false ]; then
    echo -e "${YELLOW}⚠ dotCMS is still initializing...${NC}"
    echo -e "${YELLOW}  This is normal on first startup (5-10 minutes total)${NC}"
    echo -e "${YELLOW}  Monitor progress with: docker logs -f dotcms${NC}"
  fi
else
  echo -e "${RED}✗ Docker compose directory not found${NC}"
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
echo -e "${GREEN}  Development:${NC}"
echo -e "    ./mvnw clean install -DskipTests    # Full build"
echo -e "    ./mvnw install -pl :dotcms-core -DskipTests  # Quick core build"
echo -e "    just build                          # Full build (if just is available)"
echo ""
echo -e "${GREEN}  Testing:${NC}"
echo -e "    ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest"
echo -e "    ./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false"
echo ""
echo -e "${GREEN}  Docker Management:${NC}"
echo -e "    docker logs -f dotcms               # View dotCMS logs"
echo -e "    docker logs -f db                   # View PostgreSQL logs"
echo -e "    docker logs -f opensearch           # View OpenSearch logs"
echo -e "    cd $COMPOSE_DIR && docker-compose restart"
echo -e "    cd $COMPOSE_DIR && docker-compose down"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
echo ""
