# ----------------------------------------------
# Stage 1:  Minimal java image with sdkman + Ubuntu LTS
# ----------------------------------------------
FROM fedora:41 AS base-builder

WORKDIR /srv



# Installing basic packages 
RUN sudo dnf update -y && \
    sudo dnf install java-1.8.0-openjdk-devel zip unzip wget tomcat-native tzdata tini ca-certificates openssl apr libpq-devel mimalloc  -y --setopt=install_weak_deps=False


# Cleanup
RUN dnf clean all

# ----------------------------------------------
# Stage 2:  Flatten everything to 1 layer
# ----------------------------------------------
FROM scratch
COPY --from=base-builder / /

