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
	boolean yesDashBoard=false;
	if(null!=embeddedDashboard){
		Context ctx = VelocityUtil.getWebContext(request, response);
		ctx.put("host", host);
		embeddedDashboard=VelocityUtil.eval(embeddedDashboard, ctx);
		yesDashBoard=true;
	}else{
	    embeddedDashboard="https://datastudio.google.com/embed/reporting/1_wMHSB93Lb4jhKwLItn7Ctnv4F9cXfw3/page/sBV";
	    
	    
	}
	
	

%>

<%if(!yesDashBoard){ %>
   <div style="padding:20px; max-width:800px; border:1px solid gray; background: rgba(225,225,225); position: absolute;cursor:pointer;top: 250px;left:50%; margin-left: -400px;" onclick="dojo.destroy(this)">
      <%=LanguageUtil.get(user, "embedded.dashboard.instructions") %>
  </div>
<%}%>



<div style="width: 100%; background: #f1f1f1;">
   <div style="max-width: 1200px; margin: auto;">
       <iframe width="100%" height="2585" src="<%=embeddedDashboard %>" frameborder="0" style="border:0" allowfullscreen></iframe>
   </div>
</div>