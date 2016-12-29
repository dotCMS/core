<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchPublisher"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publishing.PublisherConfig"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@ page import="com.dotcms.enterprise.publishing.staticpublishing.StaticConfig" %>
<%@ page import="com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher" %>
<%@ page import="com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration" %>
<%

Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
StaticConfig pconf = new StaticConfig();

pconf.setId("2012-04-11");

pconf.setUser(APILocator.getUserAPI().getSystemUser());
pconf.setHosts(Arrays.asList(host));

List<Class> clazz = new ArrayList();
clazz.add(StaticPublisher.class);

pconf.setPublishers(clazz);
pconf.setIncremental(false);
pconf.setLiveOnly(false);
pconf.put(StaticPublisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID, "dot-test-bucket");
pconf.setAwss3Configuration
        (new AWSS3Configuration.Builder().accessKey(request.getParameter("access").trim())
                .secretKey(request.getParameter("secret").trim()).build());

APILocator.getPublisherAPI().publish(pconf);

%>
