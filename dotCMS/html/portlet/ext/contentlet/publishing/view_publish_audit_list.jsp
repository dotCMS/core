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
    
    String offset = request.getParameter("offset");
    if(!UtilMethods.isSet(offset)){
    	offset="0";
    }
    String limit = request.getParameter("limit");
    if(!UtilMethods.isSet(limit)){
    	limit="50"; //TODO Load from properties
    }
    
    String layout = request.getParameter("layout");
    if(!UtilMethods.isSet(layout)) {
    	layout = "";
    }

    String nastyError = null;

    boolean userIsAdmin = false;
    if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
    	userIsAdmin=true;
    }

    List<Map<String,Object>> iresults =  null;
    String counter =  "0";

    try{
   		iresults =  publishAuditAPI.getAllPublishAuditStatus(new Integer(limit),new Integer(offset));
   		counter =   String.valueOf(publishAuditAPI.countAllPublishAuditStatus().get(0).get("count"));	
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }
  %>
  
  <script type="text/javascript">
   function doAuditPagination(offset,limit) {		
		var url="layout=<%=layout%>";
		url+="&offset="+offset;
		url+="&limit="+limit;		
		refreshAuditList(url);
	}
</script> 

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
	<table class="solr_listingTableNoBorder">
		<tr>
			<%
			long begin=Long.parseLong(offset);
			long end = Long.parseLong(offset)+Long.parseLong(limit);
			long total = Long.parseLong(counter);
			if(begin > 0){ 
				long previous=(begin-Long.parseLong(limit));
				if(previous < 0){
					previous=0;					
				}
			%>
			<td style="width:130px"><button dojoType="dijit.form.Button" onClick="doAuditPagination(<%=previous%>,<%=limit%>);return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
			<td class="solr_tcenter" colspan="2"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
			<%if(end < total){ 
				long next=(end < total?end:total);
			%>
			<td style="width:130px"><button class="solr_right" dojoType="dijit.form.Button" onClick="doAuditPagination(<%=next%>,<%=limit%>);return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
		</tr>
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
