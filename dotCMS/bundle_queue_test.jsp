<%@page import="com.dotcms.publisher.business.PublishAuditHistory"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus.Status"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
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
PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance(); 

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

iresults =  pubAPI.getQueueElements();

PushPublisherConfig pconf = new PushPublisherConfig();
List<Class> clazz = new ArrayList();
List<IBundler> bundler = new ArrayList();
bundler.add(new PushPublisherBundler());
clazz.add(PushPublisher.class);
int counter = 0;

List<Map<String,Object>> bundles = pubAPI.getQueueBundleIds(100,0);
List<Map<String,Object>> tempBundleContents = null;
PublishAuditStatus status = null;
PublishAuditHistory historyPojo = null;
String tempBundleId = null;

for(Map<String,Object> bundle: bundles) {
	tempBundleId = (String)bundle.get("bundle_id");
	tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);
	
	//Setting Audit objects
	//History
	historyPojo = new PublishAuditHistory();
	//Retriving assets
	List<String> assets = new ArrayList<String>();
	
	
	for(Map<String,Object> c : tempBundleContents) {
		assets.add((String) c.get("asset"));	
	}
	
	historyPojo.setAssets(assets);
	
	pconf.setLuceneQueries(prepareQueries(tempBundleContents));
	
	//Status
	status =  new PublishAuditStatus(tempBundleId);
	status.setStatusPojo(historyPojo);
	
	//Insert in Audit table
	pubAuditAPI.insertPublishAuditStatus(status);
	
	pconf.setId(tempBundleId);
	pconf.setUser(APILocator.getUserAPI().getSystemUser());
	pconf.runNow();

	pconf.setPublishers(clazz);
	pconf.setIncremental(false);
	pconf.setLiveOnly(false);
	pconf.setBundlers(bundler);
	
	if(((Long) bundle.get("operation")).intValue() == PublisherAPI.ADD_OR_UPDATE_ELEMENT)
		pconf.setOperation(PushPublisherConfig.Operation.PUBLISH);
	else
		pconf.setOperation(PushPublisherConfig.Operation.UNPUBLISH);
	
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

<%!
List<String> prepareQueries(List<Map<String,Object>> bundle) {
	StringBuilder assetBuffer = new StringBuilder();
	List<String> assets;
	assets = new ArrayList<String>();
	
	if(bundle.size() == 1) {
		assetBuffer.append("+identifier:"+(String) bundle.get(0).get("asset"));
		
		assets.add(assetBuffer.toString() +" +live:true");
		assets.add(assetBuffer.toString() +" +working:true");
		
	} else {
		int counter = 1;
		Map<String,Object> c = null;
		for(int ii = 0; ii < bundle.size(); ii++) {
			c = bundle.get(ii);
			
			assetBuffer.append("identifier:"+c.get("asset"));
			assetBuffer.append(" ");
			
			if(counter == 20 || (ii+1 == bundle.size())) {
				assets.add("+("+assetBuffer.toString()+") +live:true");
				assets.add("+("+assetBuffer.toString()+") +working:true");
				
				assetBuffer = new StringBuilder();
				counter = 0;
			} else
				counter++;
		}
	}
	return assets;
}
%>
