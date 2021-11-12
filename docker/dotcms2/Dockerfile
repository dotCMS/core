
# ----------------------------------------------
# Stage 1:  Build dotCMS from our builder image
# ----------------------------------------------
FROM tomcat:9-jdk11-openjdk

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

ENV CATALINA_OPTS="-Dfile.encoding=UTF8 \
                   --illegal-access=warn \
                   --add-opens=java.base/java.lang=ALL-UNNAMED \
                   --add-opens=java.base/java.io=ALL-UNNAMED \
                   --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED\
                   -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl \
                   -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl \
                   -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"

ENV CATALINA_HOME=/usr/local/tomcat
ENV CATALINA_BASE=/usr/local/dotcms

ADD target/war /usr/local/dotcms/webapps/ROOT
ADD target/drivers /usr/local/dotcms/lib
ADD target/starter/starter.zip /usr/local/dotcms/webapps/ROOT/starter.zip


## Ports
# Direct connect
EXPOSE 8080
# Direct connect for HTTPS, secure
EXPOSE 8443
# Connect from proxy, HTTP/80, non-secure
EXPOSE 8081
# Connect from proxy, HTTPS/443, secure
EXPOSE 8082
# Java Debugging - must be configured via CMS_JAVA_OPTS
EXPOSE 8000
# Glowroot profiler - must be configured via CMS_JAVA_OPTS
EXPOSE 4000

ENTRYPOINT ["tail", "-f", "/dev/null"]