<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.dotcms.publisher.business.PublishAuditUtil"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.PermissionableProxy"%>
<%@page import="com.dotcms.publisher.business.PublishQueueElement"%>
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
<%@page import="com.dotcms.publisher.bundle.business.BundleAPI"%>
<%@page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.beans.Identifier"%>
<%@ page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@ page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@ page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure"%>
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
			   if(dijit.getEnclosingWidget(node).checked && !dijit.getEnclosingWidget(node).disabled){
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
	//Check bundle permissions
	Map<String, Boolean> permissionMap = new HashMap<String, Boolean>();
	PermissionAPI permAPI = APILocator.getPermissionAPI();
	List<PublishQueueElement> bundleAssets = null;
	for(Map<String,Object> bundle : iresults) {
		bundleAssets = pubAPI.getQueueElementsByBundleId((String)bundle.get("bundle_id"));

		for(PublishQueueElement c : bundleAssets) {

			String identifier = c.getAsset();
			String assetType = c.getType();

			PermissionableProxy pp = new PermissionableProxy();
			pp.setIdentifier(identifier);
			pp.setType(assetType);
			pp.setInode(identifier);

			permissionMap.put(
					(String) bundle.get("bundle_id"),
					new Boolean(permAPI.doesUserHavePermission(pp, PermissionAPI.PERMISSION_PUBLISH, user)));
			break;
		}
	}


	bundleAssets = null;
	for(Map<String,Object> bundle : iresults) {

		if(permissionMap.get(bundle.get("bundle_id")).equals(Boolean.TRUE)) {
		bundleAssets = pubAPI.getQueueElementsByBundleId((String)bundle.get("bundle_id"));
		Bundle bundleObj = APILocator.getBundleAPI().getBundleById((String)bundle.get("bundle_id"));
		%>

	<table class="listingTable" style="margin-bottom:20px;">
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
				<span style="color:<%=new Date().before((Date) bundle.get("publish_date")) ?"gray" : "red"%>;font-weight: normal;">
					<%=new SimpleDateFormat("MM/dd/yyyy hh:mma").format((Date) bundle.get("publish_date")) %>
				</span>


			<%--
				<%= LanguageUtil.get(pageContext, "publisher_Date_Entered") %>:
				<span style="color:gray;font-weight: normal;">
					<%=UtilMethods.dateToHTMLDate((Date)((Map<String,Object>)bundleAssets.get(0)).get("entered_date"),"MM/dd/yyyy hh:mma") %>
				</span>
			--%>


				<div style="float:right;">
					<%= LanguageUtil.get(pageContext, "publisher_Identifier") %>: <span style="color:gray;font-weight: normal;"><%=bundle.get("bundle_id")%></span>
					<% if(bundleObj!=null) { %>
					&nbsp; <%= LanguageUtil.get(pageContext, "publisher_dialog_force-push") %>: <span style="color:gray;font-weight: normal;"><%=bundleObj.isForcePush()%></span>
					<% } %>
				</div>





			</th>


		</tr>
		<% for(PublishQueueElement c : bundleAssets) {
			String errorclass="";
		%>
			<tr <%=errorclass%>>
				<td style="width:30px;text-align:center;">
					<input
							dojoType="dijit.form.CheckBox"
							type="checkbox"
							class="queue_to_delete b<%=bundle.get("bundle_id") %>"
							name="queue_to_delete"
							value="<%=c.getAsset() %>$<%=c.getOperation() %>"
							id="queue_to_delete_<%=c.getAsset() %>$<%=c.getOperation() %>" />
				</td>


				<td valign="top">
					<%=(c.getOperation().toString().equals("1")?"<span class='addIcon' style='opacity:.6'></span>":"<span class='closeIcon' style='opacity:.6'></span>")%>&nbsp;
					<%try{
						String identifier = c.getAsset();
						String assetType = c.getType();
						String structureName = "";
						String title = "";
						String inode = "";
						if ( assetType.equals( "contentlet" ) ) {
							Contentlet contentlet=null;
							Language currentLanguage = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(), locale.getCountry());
	                        
                        	if(currentLanguage.getId() != APILocator.getLanguageAPI().getDefaultLanguage().getId()){
                        		try{
                        			contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier, false, currentLanguage.getId(), user, false);
                        		}catch(Exception e){
                        			//Searches and returns for a this Identifier a Contentlet using the default language
                                	contentlet = PublishAuditUtil.getInstance().findContentletByIdentifier( identifier );
                        		}
                        	}else{
                            	//Searches and returns for a this Identifier a Contentlet using the default language
                            	contentlet = PublishAuditUtil.getInstance().findContentletByIdentifier( identifier );
                        	}
                        	title = contentlet.getTitle();
                            inode = contentlet.getInode();
                            structureName = contentlet.getStructure().getName();
                        %>
						    <a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id="+PortletID.CONTENT+"&p_p_action=1&p_p_state=maximized&p_p_mode=view&_"+PortletID.CONTENT+"_struts_action=/ext/contentlet/edit_contentlet&_"+PortletID.CONTENT+"_cmd=edit&inode=<%=inode %>&referer=<%=referer %>">
						        <%=StringEscapeUtils.escapeHtml(title)%>
                            </a>
						<% } else {
                            title = PublishAuditUtil.getInstance().getTitle(assetType, identifier);
                        %>
							<%=StringEscapeUtils.escapeHtml(title)%>
						<% } %>

						<div style="float:right;color:silver">
							<%=structureName %>
					    </div>

					<%}catch(Exception e){nastyError=e.getMessage();%>
						<span style="color:red"><%= LanguageUtil.get(pageContext, "publisher_No_Title") %></span>
					<%} %>

    			</td>
			</tr>
		<%}%>
	</table>
<%
	}
}%>

<table width="100%">
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
			<td align="right" width="33%" ><button style="float: right;" dojoType="dijit.form.Button" onClick="refreshQueueList('offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "publisher_Next") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
	</tr>
</table>





<%} %>
