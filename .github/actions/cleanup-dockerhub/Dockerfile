# Container image that runs your code
FROM ubuntu:22.04 

RUN apt update && \
    apt upgrade -y && \
    apt install -y  curl jq ca-certificates openssl


# Copies your code file from your action repository to the filesystem path `/` of the container
COPY delete-old-docker-images.sh /delete-old-docker-images.sh

# Code file to execute when the docker container starts up (`entrypoint.sh`)
ENTRYPOINT ["/delete-old-docker-images.sh"]