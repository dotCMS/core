<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.publisher.business.DotPublisherException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<script type="text/javascript">
   dojo.require("dijit.Tooltip");
</script>  
<%
  	User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
    ContentletAPI conAPI = APILocator.getContentletAPI();
    PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();
    
    if(user == null){
    	response.setStatus(403);
    	return;
    }

    PublisherAPI pubAPI = PublisherAPI.getInstance();  
    String viewFilterStr = request.getParameter("viewFilter");
    Integer viewFilter = null;
    if(UtilMethods.isSet(viewFilterStr)){
    	viewFilter=Integer.valueOf(viewFilterStr);
    }
    

    String nastyError = null;

    boolean userIsAdmin = false;
    if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
    	userIsAdmin=true;
    }

    List<Map<String,Object>> iresults =  null;
    String counter =  "0";

    try{
   		iresults =  publishAuditAPI.getAllPublishAuditStatus();
   		counter =  String.valueOf(iresults.size());	
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }
  %>

<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}else if(iresults.size() >0){ %>				
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:250px"><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>	
			<th style="width:100px"><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>	
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Updated") %></strong></th>
		</tr>
		<% for(Map<String,Object> c : iresults) {
			String errorclass="";
			if(UtilMethods.isSet(c.get("last_results"))){
				errorclass="class=\"solr_red\"";				 
			}
		%>
			<tr <%=errorclass%>>
				<td><%=c.get("bundle_id")%></td>
			    <td><%=PublishAuditStatus.getStatusByCode((Integer)c.get("status")) %></td>
			    <td><%=UtilMethods.dateToHTMLDate((Date)c.get("create_date"),"MM/dd/yyyy hh:mma") %></td>
			    <td><%=UtilMethods.dateToHTMLDate((Date)c.get("status_updated"),"MM/dd/yyyy hh:mma") %></td>
			</tr>
		<%}%>
	</table>
<%
}else{ 
%>
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:250px"><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>	
			<th style="width:100px"><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>	
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Updated") %></strong></th>
		</tr>
		<tr>
			<td colspan="4" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%} %>
