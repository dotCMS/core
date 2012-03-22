<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetConverter" %>
<%@page import="com.dotmarketing.business.web.WebAPILocator" %>
<%@page import="com.dotmarketing.business.web.UserWebAPI" %>
<%@page import="com.liferay.portal.model.User" %>
<%@page import="com.dotmarketing.business.APILocator" %>
<%@page import="com.dotmarketing.business.PermissionAPI" %>
<%@page import="com.dotmarketing.business.RoleAPI" %>

<html>
<body>

<form>
<div>
<%

User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
FileAssetConverter fac  = null;
if(user!=null && APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
	if(session.getAttribute("converter")==null){
		try{
			fac = new FileAssetConverter();
			fac.start();
			session.setAttribute("converter", fac);
		}catch(Exception e){
			session.removeAttribute("converter");
		}
	}else{
		 fac =  (FileAssetConverter)session.getAttribute("converter");
	}
	
	%>
<%if(!fac.isFinished()) {%>
    Converting file assets to contentlets
<%}else{ %>
    Process Finished!
    <br />
    <b><%=fac.getSucceeded().size() %> File Assets successfully converted</b>
    <br />
    <b><%=fac.getFailed().size() %> File Assets failed</b>
    <%session.removeAttribute("converter"); %>
<%} %>	

<% }else{ %>
	<b><%= "You need to be a CMS Administrator in order to execute this action" %></b>
<%}%>
</div>

</form>

</body>

</html>