<%@page import="java.text.SimpleDateFormat"%>
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
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<script type="text/javascript">
   dojo.require("dijit.Tooltip");
</script>  
<%

    ContentletAPI conAPI = APILocator.getContentletAPI();
    PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();
    


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



    List<Map<String,Object>> iresults =  null;
    String counter =  "0";

    boolean deleteQueueElements=false;
    boolean deleteBundleElements=false;
    String deleteQueueElementsStr = request.getParameter("delete");
    String deleteBundleElementsStr = request.getParameter("deleteBundle");
    if(UtilMethods.isSet(deleteQueueElementsStr)){
    	deleteQueueElements=true;	
    }
    else if(UtilMethods.isSet(deleteBundleElementsStr)){
    	deleteBundleElements=true;	
    }
    String elementsToDelete=null;

    try{
    	if(deleteQueueElements){
	    	for(String identifier : deleteQueueElementsStr.split(",")){
	    		pubAPI.deleteElementFromPublishQueueTable(identifier);
	    	}
    	}
    	
    	if(deleteBundleElements){
	    	for(String bundleId : deleteBundleElementsStr.split(",")){
	    		pubAPI.deleteElementsFromPublishQueueTable(bundleId);
	    	}
    	}
    	
    	
   		iresults =  pubAPI.getQueueBundleIds();
   		counter =  pubAPI.countQueueElements().get(0).get("count").toString();
    	
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }
  %>
<script type="text/javascript">

 function checkAllBundle(x){
	var chk = dijit.byId("bundle_to_delete_" + x).checked;
	 dojo.query(".b" + x  + " input").forEach(function(box){
		 dijit.byId(box.id).setValue(chk);
	})

 }
 
 
 
   function doQueuePagination(offset,limit) {		
		var url="layout=<%=layout%>";
		url+="&offset="+offset;
		url+="&limit="+limit;		
		refreshQueueList(url);
	}
   
   function deleteQueue(){
	   var url="layout=<%=layout%>&offset=0&limit=<%=limit%>";	

		var ids="";
		var nodes = dojo.query('.queue_to_delete');
		   dojo.forEach(nodes, function(node) {
			   if(dijit.getEnclosingWidget(node).checked){
				   var nodeValue = dijit.getEnclosingWidget(node).value;
				   ids+=","+nodeValue.split("$")[0];
			   }
		   });
		if(ids != ""){   
			url+="&delete="+ids.substring(1);
		}
		
		deleteBundle(url);
   }
   
   function deleteBundle(url) {	
	   
	   var ids="";
		var nodes = dojo.query('.bundle_to_delete');
		   dojo.forEach(nodes, function(node) {
			   if(dijit.getEnclosingWidget(node).checked){
				   ids+=","+dijit.getEnclosingWidget(node).value; 
			   }
		   });
		if(ids != ""){   
			url+="&deleteBundle="+ids.substring(1);
		}
		refreshQueueList(url);
   }
</script>
<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}else if(iresults.size() >0){ 
	List<Map<String,Object>> bundleAssets = null;
	for(Map<String,Object> bundle : iresults) {
		bundleAssets = pubAPI.getQueueElementsByBundleId((String)bundle.get("bundle_id"));
%>

	<table  style="border:0px;width:99%;margin:10px;">
		<tr>
			<td>
				<%= LanguageUtil.get(pageContext, "publisher_PubUnpubDate") %>: 
				<%=new SimpleDateFormat("yyyy-MM-dd H:mm").format((Date) bundle.get("publish_date")) %>
			
			</td>
			<td align="right">
				<%= LanguageUtil.get(pageContext, "publisher_Identifier") %>: <%=bundle.get("bundle_id") %>
			
			</td>
		</tr>
	</table>					
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px">
			<input dojoType="dijit.form.CheckBox" 
					type="checkbox" 
					class="bundle_to_delete" 
					name="bundle_to_delete" 
					value="<%=bundle.get("bundle_id") %>" 
					id="bundle_to_delete_<%=bundle.get("bundle_id") %>" 
					onclick="checkAllBundle('<%=bundle.get("bundle_id") %>')"/></th>		
			<th style="width:250px"><strong><%= LanguageUtil.get(pageContext, "Title") %></strong></th>	
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>
		</tr>
		<% for(Map<String,Object> c : bundleAssets) {
			String errorclass="";
			if(UtilMethods.isSet(c.get("last_results"))){
				errorclass="class=\"solr_red\"";				 
			}
		%>
			<tr <%=errorclass%>>
				<td><input 
						dojoType="dijit.form.CheckBox" 
						type="checkbox" 
						class="queue_to_delete b<%=bundle.get("bundle_id") %>" 
						name="queue_to_delete" 
						value="<%=c.get("asset") %>$<%=c.get("operation") %>" 
						id="queue_to_delete_<%=c.get("asset") %>$<%=c.get("operation") %>" /></td>
				<%try{
					Contentlet con = conAPI.findContentletByIdentifier((String)c.get("asset"),false,Long.parseLong(c.get("language_id").toString()),user, false);
				%>
				<td>
					<a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=con.getInode() %>&referer=<%=referer %>"><%=con.getTitle()%></a>
				</td>
				<%
				}catch(Exception e){
					nastyError=e.getMessage();
				%>
					<td><%= LanguageUtil.get(pageContext, "publisher_No_Title") %></td> 
				<%} %>
				<td  style="text-align: center;width:25px"><img class="center" src="/html/images/icons/<%=(c.get("operation").toString().equals("1")?"add.png":"cross.png")%>"/></td>
			    <td><%=UtilMethods.dateToHTMLDate((Date)c.get("entered_date"),"MM/dd/yyyy hh:mma") %></td>
			</tr>
		<%}%>
	</table>
<%}%>
<%}else{%>
	<table class="listingTable shadowBox">
		<tr>
			<th style="width:30px">&nbsp;</th>			
			<th style="width:40px"><strong><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></strong></th>
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></strong></th>			
			<th><strong><%= LanguageUtil.get(pageContext, "publisher_Status") %></strong></th>
		</tr>
		<tr>
			<td colspan="14" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%} %>
