FROM openjdk:8

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Base CMS"

ENV DOTCMS_HOME /opt/dotcms
ENV DOTCMS_VERSION 5.0.0

RUN mkdir -p ${DOTCMS_HOME}
WORKDIR ${DOTCMS_HOME}

COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

COPY dotcms_${DOTCMS_VERSION}.tar.gz ${DOTCMS_HOME} 
RUN	gzip -d dotcms_${DOTCMS_VERSION}.tar.gz && \
	tar xf dotcms_${DOTCMS_VERSION}.tar && \
	rm dotcms_${DOTCMS_VERSION}.tar
COPY log4j2.xml dotserver/tomcat-8.0.18/webapps/ROOT/WEB-INF/log4j/log4j2.xml
RUN mv plugins plugins-dist && mkdir plugins

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]
