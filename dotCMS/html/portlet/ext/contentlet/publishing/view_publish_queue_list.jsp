<%@page import="com.dotmarketing.util.DateUtil"%>
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
    
    int offset = 0;
    try{offset = Integer.parseInt(request.getParameter("offset"));}catch(Exception e){}
    if(offset <0) offset=0;
    int limit = 10;
    try{limit = Integer.parseInt(request.getParameter("limit"));}catch(Exception e){}
    if(limit <0 || limit > 1000) limit=10;

	
	
    String nastyError = null;



    List<Map<String,Object>> iresults =  null;
    int counter =  0;

    boolean deleteQueueElements=false;
    boolean deleteBundleElements=false;
    String deleteQueueElementsStr = request.getParameter("delete");
    String deleteBundleElementsStr = request.getParameter("deleteBundle");
    if(UtilMethods.isSet(deleteQueueElementsStr)){
    	deleteQueueElements=true;	
    }
    if(UtilMethods.isSet(deleteBundleElementsStr)){
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
    	
    	
   		iresults =  pubAPI.getQueueBundleIds(limit, offset);
   		counter =  pubAPI.countQueueBundleIds();
    	
    }catch(DotPublisherException e){
    	iresults = new ArrayList();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList();
    	nastyError = pe.toString();
    }
 
	long begin=offset;
	long end = offset+limit;
	long total = counter;
	long previous=(begin-limit);
	if(previous < 0){previous=0;}
 
 %>
  
  
  
<script type="text/javascript">


	dojo.require("dijit.Tooltip");



	function checkAllBundle(x){
		var chk = dijit.byId("bundle_to_delete_" + x).checked;
		 dojo.query(".b" + x  + " input").forEach(function(box){
			 dijit.byId(box.id).disabled = chk;
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
	   var url="layout=<%=layout%>&offset=<%=offset%>&limit=<%=limit%>";	

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
	
		
<%}else if(iresults.size() ==0){ %>
	<table class="listingTable">
		<tr>
			<th style="width:30px">&nbsp;</th>			
			<th style="width:40px"><%= LanguageUtil.get(pageContext, "publisher_Operation_Type") %></th>
			<th><%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %></th>			
			<th><%= LanguageUtil.get(pageContext, "publisher_Status") %></th>
		</tr>
		<tr>
			<td colspan="14" align="center"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
		
<%} else {
	List<Map<String,Object>> bundleAssets = null;
	for(Map<String,Object> bundle : iresults) {
		bundleAssets = pubAPI.getQueueElementsByBundleId((String)bundle.get("bundle_id"));%>
				
	<table class="listingTable" style="margin:10px;margin-bottom:20px;">
		<tr>
			
			<th style="width:30px;text-align:center;">
				<input dojoType="dijit.form.CheckBox" 
						type="checkbox" 
						class="bundle_to_delete" 
						name="bundle_to_delete" 
						value="<%=bundle.get("bundle_id") %>" 
						id="bundle_to_delete_<%=bundle.get("bundle_id") %>" 
						onclick="checkAllBundle('<%=bundle.get("bundle_id") %>')"/>
			</th>		

			<th style="width:100%">
			
				<%= LanguageUtil.get(pageContext, "publisher_PubUnpubDate") %>: 
				<%	
					Date publishDate = null;
					if (bundle.get("publish_date") instanceof java.util.Date) {
						publishDate = (Date) bundle.get("publish_date");
					} else if (bundle.get("publish_date") instanceof oracle.sql.TIMESTAMP){
						publishDate = new Date(((oracle.sql.TIMESTAMP) bundle.get("publish_date")).timeValue().getTime());
					}
				%>
				<span style="color:<%=new Date().before(publishDate) ?"gray" : "red"%>;font-weight: normal;">
					<%=new SimpleDateFormat("MM/dd/yyyy hh:mma").format(publishDate) %>
				</span>
			
			
			<%-- 
				<%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %>: 
				<span style="color:gray;font-weight: normal;">
					<%=UtilMethods.dateToHTMLDate((Date)((Map<String,Object>)bundleAssets.get(0)).get("entered_date"),"MM/dd/yyyy hh:mma") %>
				</span>
			--%>
			
			
				<div style="float:right;">
					<%= LanguageUtil.get(pageContext, "publisher_Identifier") %>: <span style="color:gray;font-weight: normal;"><%=bundle.get("bundle_id") %></span>
				</div>
			
			
			
			
			
			</th>	
			

		</tr>
		<% for(Map<String,Object> c : bundleAssets) {
			String errorclass="";
			if(UtilMethods.isSet(c.get("last_results"))){
				errorclass="class=\"solr_red\"";				 
			}
		%>
			<tr <%=errorclass%>>
				<td style="width:30px;text-align:center;">
					<input 
							dojoType="dijit.form.CheckBox" 
							type="checkbox" 
							class="queue_to_delete b<%=bundle.get("bundle_id") %>" 
							name="queue_to_delete" 
							value="<%=c.get("asset") %>$<%=c.get("operation") %>" 
							id="queue_to_delete_<%=c.get("asset") %>$<%=c.get("operation") %>" />
				</td>
				

				<td valign="top">
					<%=(c.get("operation").toString().equals("1")?"<span class='addIcon' style='opacity:.6'></span>":"<span class='closeIcon' style='opacity:.6'></span>")%>&nbsp;
					<%try{
						Contentlet con = conAPI.findContentletByIdentifier((String)c.get("asset"),false,Long.parseLong(c.get("language_id").toString()),user, false);%>
						<a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=con.getInode() %>&referer=<%=referer %>"><%=con.getTitle()%></a>
						<div style="float:right;color:silver">
							<%=con.getStructure().getName() %>
						
					    </div>
						
					
					
					
					<%}catch(Exception e){nastyError=e.getMessage();%>
						<%= LanguageUtil.get(pageContext, "publisher_No_Title") %>
					<%} %>

    			</td>
			</tr>
		<%}%>
	</table>
<%}%>
	
<table width="97%" style="margin:10px;" >
	<tr>
		<%
		if(begin > 0){ %>
			<td width="33%" ><button dojoType="dijit.form.Button" onClick="refreshQueueList('offset=<%=previous%>&limit=<%=limit%>');return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "publisher_Previous") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
			<td  width="34%"  colspan="2" align="center"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "publisher_Of") %> <%=total%> </strong></td>
		<%if(end < total){ 
			long next=(end < total?end:total);
		%>
			<td align="right" width="33%" ><button class="solr_right" dojoType="dijit.form.Button" onClick="refreshQueueList('offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
	</tr>
</table>

	
	
	

<%} %>
