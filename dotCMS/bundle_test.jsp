<%@page import="com.dotcms.publishing.sitesearch.SiteSearchConfig"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.publishing.sitesearch.ESSiteSearchPublisher"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publishing.PublisherConfig"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%

Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
SiteSearchConfig pconf = new SiteSearchConfig();

//pconf.setIndexName("sitesearch_20120407125752");
pconf.setId("this-is-my-bundle");

pconf.setUser(APILocator.getUserAPI().getSystemUser());
pconf.setHosts(Arrays.asList(host));
Date start = new Date(112, 2, 5);

Date end = new Date(112, 3, 17);

//pconf.setStartDate(start);
//pconf.setEndDate(end);
//List<String> inc = Arrays.asList("/images/*");

//pconf.setIncludePatterns(inc);

List<Class> clazz = new ArrayList();
clazz.add(ESSiteSearchPublisher.class);

pconf.setPublishers(clazz);

pconf.setLiveOnly(false);


APILocator.getPublisherAPI().publish(pconf);


%>
