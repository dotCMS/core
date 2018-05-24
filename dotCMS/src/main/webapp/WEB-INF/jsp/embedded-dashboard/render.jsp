<%@page import="org.apache.velocity.context.Context"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/uservalidation.jsp"%>
<%

	String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	Host host = (null==hostId) ?  WebAPILocator.getHostWebAPI().getHost(request) :  APILocator.getHostAPI().find(hostId, user, false);
	String embeddedDashboard = host.getStringProperty(Host.EMBEDDED_DASHBOARD);
	if(null!=embeddedDashboard){
		Context ctx = VelocityUtil.getWebContext(request, response);
		ctx.put("host", host);
		embeddedDashboard=VelocityUtil.eval(embeddedDashboard, ctx);
	}
	
	

%>

<div style="width: 100%; background: #f1f1f1;">
  <div style="max-width: 1200px; margin: auto;">
    <%if(!UtilMethods.isSet(embeddedDashboard)){ %>
        <div style="padding:30px;max-width:800px;margin: auto;"">
	       Please create/set the field host.embeddedDashboard to the embed url for your Dashboard visualization. It will be parsed as velocity for variable substitution, e.g. $host.identifer.
	   </div>
	<%}else{ %>
        <iframe width="100%" height="2585" src="<%=embeddedDashboard %>" frameborder="0" style="border:0" allowfullscreen></iframe>
     <%} %>
  </div>
</div>