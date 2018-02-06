FROM openjdk:8-jdk-alpine3.7

LABEL maintainer="support@dotcms.com"

ENV DOTCMS_HOME=/opt/dotcms

WORKDIR ${DOTCMS_HOME}

EXPOSE 8080

ADD dotcms.tar.gz ${DOTCMS_HOME}

RUN rm -rf /opt/dotcms/plugins/com.dotcms.config/

ENTRYPOINT bin/deploy-plugins.sh && bin/startup.sh && sleep 60 && tail -f ${DOTCMS_HOME}/dotserver/tomcat-8.5.24/webapps/ROOT/dotsecure/logs/dotcms.log
