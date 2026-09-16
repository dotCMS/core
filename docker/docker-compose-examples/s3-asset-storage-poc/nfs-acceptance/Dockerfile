FROM dotcms/s3-assets-poc:acceptance-20260915
USER root
RUN rm -f /etc/apt/sources.list.d/pgdg.sources && apt-get update && apt-get install -y --no-install-recommends nfs-kernel-server nfs-common && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /export /mnt/nfs /run/rpcbind
COPY exports /etc/exports
COPY start-nfs.sh /usr/local/bin/start-nfs
RUN chmod +x /usr/local/bin/start-nfs
HEALTHCHECK NONE
ENTRYPOINT ["/usr/local/bin/start-nfs"]
