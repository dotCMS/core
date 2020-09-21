<%@ include file="/html/portlet/ext/scheduler/init.jsp" %>

<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>

<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, com.dotcms.repackage.javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>


<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

	<div id="system_jobs" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "scheduler.system.jobs") %>" <%= ((request.getParameter("group") != null) && request.getParameter("group").equals("user_jobs")) ? "selected=\"true\"" : "" %> >		
		
	
		<%@ include file="/html/portlet/ext/scheduler/system_jobs.jsp" %>
	
	
	
	</div>
	
</div>