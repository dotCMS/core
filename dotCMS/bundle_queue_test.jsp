<%@page import="com.dotcms.publisher.myTest.PushPublisher"%>
<%@page import="com.dotcms.publishing.IBundler"%>
<%@page import="com.dotcms.publisher.myTest.PushPublisherBundler"%>
<%@page import="org.apache.commons.collections.ListUtils"%>
<%@page import="com.dotcms.publisher.myTest.PushPublisherConfig"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publishing.PublisherConfig"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%
	User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
ContentletAPI conAPI = APILocator.getContentletAPI();
PublisherAPI pubAPI = PublisherAPI.getInstance();  

List<Map<String,Object>> iresults =  null;

String sortBy = request.getParameter("sort");
if(!UtilMethods.isSet(sortBy)){
	sortBy="id asc";
}
String offset = request.getParameter("offset");
if(!UtilMethods.isSet(offset)){
	offset="0";
}
String limit = request.getParameter("limit");
if(!UtilMethods.isSet(limit)){
	limit="10"; //TODO Put this value in a properties file
}
String query = request.getParameter("query");
if(!UtilMethods.isSet(query)){
	query="";
}

iresults =  pubAPI.getPublishQueueQueueContentletsPaginated(query, sortBy, offset, limit);

String luceneQuery = null;
PushPublisherConfig pconf = new PushPublisherConfig();
List<Class> clazz = new ArrayList();
List<IBundler> bundler = new ArrayList();
bundler.add(new PushPublisherBundler());
clazz.add(PushPublisher.class);
int counter = 0;
for(Map<String,Object> c : iresults) {
	luceneQuery = (String)c.get("asset");
	pconf.setLuceneQuery(luceneQuery);
	pconf.setId((String) c.get("bundle_id"));
	pconf.setUser(APILocator.getUserAPI().getSystemUser());
	pconf.runNow();

	pconf.setPublishers(clazz);
	pconf.setIncremental(false);
	pconf.setLiveOnly(false);
	pconf.setBundlers(bundler);
	APILocator.getPublisherAPI().publish(pconf);
	out.print(pconf.getId() +" Done! <br />"); 
}

/* Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
SiteSearchConfig pconf = new SiteSearchConfig();

pconf.setIndexName("sitesearch_willtest");
pconf.setId("2012-04-11");

pconf.setUser(APILocator.getUserAPI().getSystemUser());
pconf.setHosts(Arrays.asList(host));
Date start = new Date(112, 2, 5);

Date end = new Date(112, 3, 17); */

//pconf.setStartDate(start);
//pconf.setEndDate(end);
//List<String> inc = Arrays.asList("/images/*");

//pconf.setIncludePatterns(inc);

/* List<Class> clazz = new ArrayList();
clazz.add(ESSiteSearchPublisher.class);

pconf.setPublishers(clazz);
pconf.setIncremental(false);
pconf.setLiveOnly(false);


APILocator.getPublisherAPI().publish(pconf); */
%>
