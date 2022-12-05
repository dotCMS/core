FROM postgres:9.6-alpine AS predeploy

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Postgresql-9.6"

WORKDIR /

COPY db_initbuild.sh /tmp
RUN chmod 775 /tmp/db_initbuild.sh && /tmp/db_initbuild.sh

FROM postgres:9.6-alpine

ENV PGDATA /data/pg

COPY --from=predeploy /tmp/db.tgz /tmp/db.tgz
RUN mkdir -p /data/pg
WORKDIR /data/pg

COPY entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod 775 /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]