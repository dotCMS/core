docker-compose -f open-distro-docker-compose.yml \
  up \
  --abort-on-container-exit

# Cleaning up
docker-compose -f open-distro-docker-compose.yml down
