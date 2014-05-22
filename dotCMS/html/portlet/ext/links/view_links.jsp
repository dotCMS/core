<%@ include file="/html/portlet/ext/links/init.jsp" %>

<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.portlets.links.model.Link" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@ page import="com.dotmarketing.portlets.links.model.Link.LinkType"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%
	Boolean hostChanged = (Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.LINK_HOST_CHANGED);
	int pageNumber = 1;
	if (!hostChanged && (request.getParameter("pageNumber") != null) && (request.getParameter("resetQuery") == null)) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");

	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/links/view_links"});
	params.put("pageNumber",new String[] { pageNumber + "" });	
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);

	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String showDeleted = (request.getParameter("showDeleted")!=null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.LINK_SHOW_DELETED);
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.LINK_QUERY);
	String orderby = (request.getParameter("orderby")!=null) ? request.getParameter("orderby") : "";

	//long hostId = 0;
	String hostId ="";
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID) != null)
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		if (request.getParameter("host_id") != null)
		    hostId = request.getParameter("host_id");
	} catch (NumberFormatException e) {
	}

%>

<script language="JavaScript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

function resetSearch() {
	form = document.getElementById('fm');
	form.resetQuery.value = "true";
	form.showDeleted.value = '';
	//form.host_id.value = '';
	form.query.value = '';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/links/view_links" /></portlet:renderURL>';
	submitForm(form);
}

function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	if (document.getElementById('showDeletedCB').checked)
		document.getElementById('showDeleted').value='true';
	else
		document.getElementById('showDeleted').value='false';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/links/view_links" /></portlet:renderURL>';
	submitForm(form);
}

function submitfmPublish() {
	form = document.getElementById('fm_publish');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/publish_links" /></portlet:actionURL>';
	submitForm(form);
}

function submitfmDelete() {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.confirm.delete.link")) %>'))
	{
		form = document.getElementById('fm_publish');
		form.cmd.value = 'full_delete_list';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="full_delete_list" /></portlet:actionURL>';
		submitForm(form);
	}
}
function addAsset() {
	window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>';
}

function checkAll() {
	var check = dijit.byId("checkAll").checked;
	dojo.query('input[type=checkbox]', document).forEach(function(tag){
		var id = tag.id;
		if(id != undefined && id.indexOf("publishInode") >-1){
			dijit.byId(id).setChecked(check);
		}
	});
	togglePublish();
}

function submitParent() {
	var parent = document.getElementById("parent").value;
	var actionURL = document.getElementById("actionURL").value;
	self.location = actionURL + '&parent=' + parent + document.getElementById("submitLocation").value;
}

function togglePublish(){
	var cbArray = document.getElementsByName("publishInode");
	var cbCount = cbArray.length;
	for(i = 0;i<cbCount;i++){
		if (cbArray[i].checked) {
            dijit.byId("publishButton").setAttribute("disabled", false);
            dijit.byId("deleteButton").setAttribute("disabled", false);
		    break;
		}
            dijit.byId("publishButton").setAttribute("disabled", true);
            dijit.byId("deleteButton").setAttribute("disabled", true);
	}
}
</script>

<jsp:include page="/html/portlet/ext/folders/context_menus_js.jsp" />
<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<%  referer = java.net.URLEncoder.encode(referer,"UTF-8"); %>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "view-links-all")) %>' />

<!-- START Toolbar -->
<form id="fm" method="post">

	<!--- Hidden fields to copy the asset to a new folder -->
		<input type="hidden" name="parent" id="parent" value="">
		<input type="hidden" name="selectedparent" id="selectedparent" value="">
		<input type="hidden" name="submitParent" id="submitParent" value="">
		<input type="hidden" name="submitLocation" id="submitLocation" value="">
		<input type="hidden" name="actionURL" id="actionURL" value="">
		<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
		<input type="hidden" name="showDeleted" id="showDeleted" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "value=\"true\"" : "value=\"false\"" %> >
	<!--- Hidden fields to copy the asset to a new folder -->

	<div class="yui-gc portlet-toolbar">
		<div class="yui-u first">
			<input type="hidden" name="resetQuery" value="">
            <input type="hidden" name="host_id" id="host_id" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>"> 
			<input type="text"  dojoType="dijit.form.TextBox" style="width:175px;" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
            <button dojoType="dijit.form.Button" onClick="submitfm()" iconClass="searchIcon">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
            </button>
            <button dojoType="dijit.form.Button" onClick="resetSearch()" iconClass="resetIcon">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
            </button>
		</div>
		<div class="yui-u" style="text-align:right;">
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="showDeletedCB" id="showDeletedCB"  onClick="submitfm();" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : "" %> value="true">
			<label for="showDeletedCB" style="font-size:85%;"><%= LanguageUtil.get(pageContext, "Show-Archived") %></label>
		
			<button dojoType="dijit.form.Button"  type="button" onClick="javascript:addAsset()" iconClass="plusIcon">
	  		 <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-link")) %>
	</button>
		</div>
	</div>
</form>
<!-- END Toolbar -->

<!-- START Listing Results -->
<form id="fm_publish" method="post">
	<input type="hidden" name="referer" value="<%=referer%>">
	<input type="hidden" name="cmd" value="prepublish">

	<table class="listingTable">
		<tr>
			
			<th nowrap style="width:25px;text-align:center;">
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="cb2" id="checkAll" value="1" onclick="checkAll"/> 
				<span dojoType="dijit.Tooltip" connectId="checkAll" id="ckeckAll_tooltip"><%= LanguageUtil.get(pageContext, "Check-all") %> / <%= LanguageUtil.get(pageContext, "Uncheck-all") %></span>
			</th>			
			<th nowrap width="50%">
				
					<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/links/view_links" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
				</portlet:renderURL>">
				<%= LanguageUtil.get(pageContext, "Title") %></a>
			</th>
			<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "Status") %></th>
			<th nowrap width="30%"><%= LanguageUtil.get(pageContext, "URL") %></th>
			<th nowrap width="10%"><%= LanguageUtil.get(pageContext, "Folder") %></th>
			<th nowrap width="10%">
				<a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/links/view_links" />
				<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
				<portlet:param name="orderby" value="modDate desc" /></portlet:renderURL>">
				<%= LanguageUtil.get(pageContext, "Mod-Date") %>
				</a>
			</th>
		</tr>
	
		<% 
			java.util.List links = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.LINKS_VIEW);
			int linksSize = ((Long) request.getAttribute(com.dotmarketing.util.WebKeys.LINKS_VIEW_COUNT)).intValue();
			String userId = user.getUserId();
	
			for (int k=0;k<links.size();k++) { 
	
				com.dotmarketing.beans.PermissionAsset permAsset = (com.dotmarketing.beans.PermissionAsset) links.get(k);							
				
				//gets permissions listings
				java.util.List permissions = permAsset.getPermissions();
				String pathToMe = permAsset.getPathToMe();
	
				if (pathToMe.length()>30) {
					pathToMe = pathToMe.substring(0,30) + "...";
				}
	
				//gets link
				Link link = (Link) permAsset.getAsset();
				String url = link.getUrl();
				if ((url!=null) && (url.length()>30)) {
					url = url.substring(0,30);
				}
	
				String str_style = "";
				if ((k%2)==0) {
					str_style = "class=\"alternate_1\"";
				}
				else{
					str_style = "class=\"alternate_2\"";
	            }
	
				//link properties and permissions
				String parent = link.getParent();
				String inode = link.getInode();
				String live = (link.isLive())?"1":"0";
				String working = (link.isWorking())?"1":"0";
				String write = (permissions.contains(String.valueOf(PermissionAPI.PERMISSION_WRITE)))?"1":"0";
				
				Host host = APILocator.getHostAPI().findParentHost(link, APILocator.getUserAPI().getSystemUser(), false);
				
			%>
			<tr <%=str_style%> id="tr<%=k%>">
				<td nowrap style="text-align:center;">
					<% if (permissions.contains(PermissionAPI.PERMISSION_PUBLISH)) { %>						
						<input dojoType="dijit.form.CheckBox" type="checkbox" name="publishInode" id="publishInode<%= link.getInode() %>" value="<%= link.getInode() %>" onclick="togglePublish()" />						
					<% } %>
				</td>
				<td nowrap <%if(!link.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=link.getInode()%>&referer=<%=referer%>'"<%} %>>
						<span class="linkIcon"></span>&nbsp;<%= link.getTitle() %>
				</td>
				<td nowrap="true" <%if(!link.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=link.getInode()%>&referer=<%=referer%>'"<%} %>><%= com.dotmarketing.util.UtilHTML.getStatusIcons(link) %></td>
				<td nowrap>
					<% if ((link.getLinkType() == null) || (!link.getLinkType().equals(Link.LinkType.CODE.toString()))) { %>
						<a  href="<%=link.getWorkingURL()%>" target="_blank" ><% if(link.getWorkingURL()!=null){ %><% if(link.getWorkingURL().length() > 45){ %><%= link.getWorkingURL().substring(0,45) + "..." %><% }else{ %><%= link.getWorkingURL() %><% } %><% } %></a>
					<% } else { %>
						<%= LanguageUtil.get(pageContext, "Code-Link") %>
					<% } %>
				</td>
				<td nowrap <%if(!link.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=link.getInode()%>&referer=<%=referer%>'"<%} %>><%=pathToMe%></td>
				<td nowrap <%if(!link.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=link.getInode()%>&referer=<%=referer%>'"<%} %>>
					<%=modDateFormat.format(link.getModDate())%>
					<script language="JavaScript">
						//popup div for the links
						document.write(getLinkPopUp('<%=k%>','<%= CTX_PATH %>', '<%=link.getInode()%>', '<%=link.getIdentifier()%>','0','',
							'<%=referer%>','<%=(link.isLive()) ? "1" : "0"%>',
							'<%=(link.isWorking()) ? "1" : "0"%>',
							'<%=(link.isDeleted()) ? "1" : "0"%>',
							'<%=(link.isLocked()) ? "1" : "0"%>'
							,'<%=permissions.contains(PermissionAPI.PERMISSION_READ) ? "1" : "0" %>'
							,'<%=permissions.contains(PermissionAPI.PERMISSION_WRITE) ? "1" : "0" %>'
							,'<%=permissions.contains(PermissionAPI.PERMISSION_PUBLISH) ? "1" : "0" %>'
							,'<%=user.getUserId()%>'));
						
					</script>
				</td>
			</tr>
	
		<%}%>
	<!-- END Listing Results -->
	
	<!-- Start No Results -->	
		<% if (linksSize ==0) { %>
			<tr>
				<td colspan="7">
					<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-links-to-show") %></div>
				</td>
			</tr>
		<% } %>
	<!-- End No Results -->
	
	</table>

<!-- Start Pagination -->
<div class="yui-gb buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/view_links" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) links.get(0)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="previous" /></portlet:renderURL>';" iconClass="previousIcon">
				<%= LanguageUtil.get(pageContext, "Previous") %> 
			</button>
		<% } %> &nbsp;
	</div>
	<div class="yui-u">
		<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> -
<%
	if (maxIndex > (minIndex + linksSize)) {
%>
	<%= minIndex + linksSize %>
<%
	} else {
%>
	<%= maxIndex %>
<%
	}
%>
	<%= LanguageUtil.get(pageContext, "of1") %>
<%
	if (100 <= linksSize) {
%>
	<%= LanguageUtil.get(pageContext, "hundreds") %>
<%
	} else {
%>
	<%= minIndex + linksSize %>
<%
	}
%>
	</div>
	<div class="yui-u" style="text-align:right;">
		<% if (maxIndex < (minIndex + linksSize)) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/view_links" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) links.get(links.size() - 1)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="next" /></portlet:renderURL>';" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %>
			</button>
		<% } %>
	</div>
</div>
<!-- END Pagination -->


<!-- START Buton Row -->	
<div class="buttonRow">
	<button dojoType="dijit.form.Button" id="publishButton" onClick="submitfmPublish()" disabled="true" iconClass="publishIcon">
	    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
	</button>
	
	<button dojoType="dijit.form.Button" id="deleteButton" onClick="submitfmDelete()" disabled="true" iconClass="deleteIcon">
	    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
	</button>
</div>
<!-- END Buton Row -->

</form>
</liferay:box>

