
<%@page import="com.dotcms.enterprise.publishing.timemachine.TimeMachineConfig"%>
<%@page import="com.dotcms.publishing.PublishStatus"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchPublisher"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.enterprise.publishing.timemachine.TimeMachineConfig" %>
<%@page import="com.dotcms.enterprise.publishing.timemachine.TimeMachinePublisher" %>
<%@page import="com.dotcms.publishing.PublisherConfig"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>


<%

Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
TimeMachineConfig tmconfig = new TimeMachineConfig();

tmconfig.setUser(APILocator.getUserAPI().getSystemUser());
tmconfig.setHosts(Arrays.asList(host));

// tmconfig.setSourceBundle()
tmconfig.setId("timeMachineBundle");
tmconfig.setDestinationBundle("tt_destination");
// tmconfig.setExcludePatterns(Arrays.asList("*.dot"));
// tmconfig.setLiveOnly(false);
//Optional: time machine publisher will make it true
tmconfig.setIncremental(true);

PublishStatus status = APILocator.getPublisherAPI().publish(tmconfig);

%>
<%=status.getStartProgress() %> : <%=status.getEndProgress() %>

You are done!