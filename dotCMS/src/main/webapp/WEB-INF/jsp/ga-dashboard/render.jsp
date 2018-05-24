<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/uservalidation.jsp"%>
<%

  String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
  Host host = (null==hostId) ?  WebAPILocator.getHostWebAPI().getHost(request) :  APILocator.getHostAPI().find(hostId, user, false);
  final String gaDashboard = host.getStringProperty(Host.GA_DASHBOARD);
%>

<div style="width: 100%; background: #f1f1f1;">
  <div style="max-width: 1200px; margin: auto;">
    <%if(!UtilMethods.isSet(gaDashboard)){ %>
    <div style="padding:30px;max-width:800px;margin: auto;"">
	   Please create/set the field host.<%=Host.GA_DASHBOARD %> to the embed url for your Google Data Studio visualization.
	   </div>
	<%}else{ %>
  
        <iframe width="100%" height="2585" src="<%=gaDashboard %>" frameborder="0" style="border:0" allowfullscreen></iframe>
     
     <%} %>
  </div>
</div>