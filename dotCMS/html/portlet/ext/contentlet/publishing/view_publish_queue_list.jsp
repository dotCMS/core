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
    
    
    String sortBy="entered_date desc";
    
    String offset = request.getParameter("offset");
    if(!UtilMethods.isSet(offset)){
    	offset="0";
    }
    String limit = request.getParameter("limit");
    if(!UtilMethods.isSet(limit)){
    	limit="10"; //TODO Put this value in a properties file
    }

    String nastyError = null;

    boolean userIsAdmin = false;
    if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
    	userIsAdmin=true;
    }

    String layout = request.getParameter("layout");
    if(!UtilMethods.isSet(layout)) {
    	layout = "";
    }

    String referer = new URLEncoder().encode("/c/portal/layout?p_l_id=" + layout + "&p_p_id=EXT_CONTENT_PUBLISHING_TOOL&");
    List<Map<String,Object>> iresults =  null;
    String counter =  "0";

    boolean deleteQueueElements=false;
    String deleteQueueElementsStr = request.getParameter("delete");
    if(UtilMethods.isSet(deleteQueueElementsStr)){
    	deleteQueueElements=true;	
    }
    String elementsToDelete=null;

    try{
    	if(deleteQueueElements){
    		if(deleteQueueElementsStr.equals("all")){
    			pubAPI.deleteAllElementsFromPublishQueueTable();
    		}else{
		    	for(String bundleId : deleteQueueElementsStr.split(",")){
		    		pubAPI.deleteElementFromPublishQueueTable(bundleId);
		    		publishAuditAPI.deletePublishAuditStatus(bundleId);
		    	}
    		}
    	}
    	
    	
   		iresults =  pubAPI.getQueueElementsGroupByBundleId(offset, limit);
   		counter =  pubAPI.countQueueElementsGroupByBundleId().get(0).get("count").toString();
    	
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }
  %>
<script type="text/javascript">
 function solrQueueCheckUncheckAll(){
	   var check=false;
/* 	   if(dijit.byId("queue_all").checked){
		   check=true;
	   } */
	   var nodes = dojo.query('.queue_to_delete');
	   dojo.forEach(nodes, function(node) {
		    dijit.getEnclosingWidget(node).set("checked",check);
	   }); 
   }
   function doQueuePagination(offset,limit) {		
		var url="layout=<%=layout%>";
		url+="&offset="+offset;
		url+="&limit="+limit;		
		refreshQueueList(url);
	}
   
   function deleteQueue(){
	   var url="layout=<%=layout%>&offset=0&limit=<%=limit%>";	
/* 		if(dijit.byId("queue_all").checked){
			url+="&delete=all";
		}else{ */
			var ids="";
			var nodes = dojo.query('.queue_to_delete');
			   dojo.forEach(nodes, function(node) {
				   if(dijit.getEnclosingWidget(node).checked){
					   ids+=","+dijit.getEnclosingWidget(node).value; 
				   }
			   });
			if(ids != ""){   
				url+="&delete="+ids.substring(1);
			}
		//}
		refreshQueueList(url);	   
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
			<th><!-- <input dojoType="dijit.form.CheckBox" type="checkbox" name="queue_all" value="all" id="queue_all" onclick="solrQueueCheckUncheckAll()" /> --></th>		
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>	
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
		</tr>
		<% for(Map<String,Object> c : iresults) {
			String errorclass="";
			if(UtilMethods.isSet(c.get("last_results"))){
				errorclass="class=\"solr_red\"";				 
			}
		%>
			<tr <%=errorclass%>>
				<td><input dojoType="dijit.form.CheckBox" type="checkbox" class="queue_to_delete" name="queue_to_delete" value="<%=c.get("bundle_id") %>" id="queue_to_delete_<%=c.get("bundle_id") %>" /></td>
				<%try{
					Contentlet con = conAPI.findContentletByIdentifier((String)c.get("asset"),true,Long.parseLong(c.get("language_id").toString()),user, false);
				%>
				<td><a href="/c/portal/layout?p_l_id=<%=layout%>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=con.getInode() %>&referer=<%=referer %>"><%=con.getTitle()%></a></td>
				<%
				}catch(Exception e){
					nastyError=e.getMessage();
				%>
				<td><%=c.get("bundle_id") %></td>
				<%} %>
				<td style="width:40px"><img class="center" src="/html/images/icons/<%=(c.get("operation").toString().equals("1")?"plus.png":"cross.png")%>"/></td>
			    <td><%=UtilMethods.dateToHTMLDate((Date)c.get("entered_date"),"MM/dd/yyyy hh:mma") %></td>
			    <td><%=PublishAuditStatus.getStatusByCode(Integer.valueOf((Integer) c.get("status")))%></td>
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
			<td style="width:130px"><button dojoType="dijit.form.Button" onClick="doQueuePagination(<%=previous%>,<%=limit%>);return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
			<td class="solr_tcenter"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
			<%if(end < total){ 
				long next=(end < total?end:total);
			%>
			<td style="width:130px"><button class="solr_right" dojoType="dijit.form.Button" onClick="doQueuePagination(<%=next%>,<%=limit%>);return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
			<%}else{ %>
			<td style="width:130px">&nbsp;</td>
			<%} %>
		</tr>
	</table>
<% }else{ %>
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px">&nbsp;</th>		
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></strong></th>	
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>			
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
		</tr>
		<tr>
			<td colspan="5" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%} %>
