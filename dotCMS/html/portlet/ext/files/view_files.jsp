<%@page import="com.dotmarketing.util.UUIDGenerator"%>
<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@ include file="/html/portlet/ext/files/init.jsp" %>

<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.business.APILocator"%>

<%
	Boolean hostChanged = (Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_HOST_CHANGED);
	int pageNumber = 1;
	if (!hostChanged && (request.getParameter("pageNumber") != null) && (request.getParameter("resetQuery") == null)) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/files/view_files"});
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	
	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String showDeleted = (request.getParameter("showDeleted")!=null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.FILE_SHOW_DELETED);
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.FILE_QUERY);
	String orderby = (request.getParameter("orderby")!=null) ? request.getParameter("orderby") : "";

	//long hostId = 0;
	String hostId ="";
	
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID) != null)
			//hostId = Long.parseLong((String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID));
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		if (request.getParameter("host_id") != null)
		    //hostId = Long.parseLong(request.getParameter("host_id"));
		hostId = request.getParameter("host_id");
	} catch (NumberFormatException e) {
	}
	
%>

<script language="Javascript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

var newwindow;

function popitup(url , title) {
	if (newwindow && !newwindow.closed)
	{ newwindow.focus(); newwindow.document.clear() }
	else
	{ newwindow=window.open('','','width=100,height=100,resizable=0') }
	newwindow.document.writeln('<> <head> <title>' + title + '<\/title> <\/head> <body bgcolor=\"black\"> <center>');
	newwindow.document.writeln('<img src=' + url + ' title=\"' + title + '\" alt=\"' + title + '\" >');
	newwindow.document.writeln('<\/center> <\/body> <\/html>');
	newwindow.document.close();
}

function tidyPopUp()
	{
		if (newwindow && !newwindow.closed) { newwindow.close(); }
	}
	
function resetSearch() {
	form = document.getElementById('fm');
	form.resetQuery.value = "true";
	form.showDeleted.value = '';
	form.query.value = '';
	//form.host_id.value = '';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>';
	submitForm(form);
}

function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	if (document.getElementById('showDeletedCB').checked)
		document.getElementById('showDeleted').value = 'true';
	else
		document.getElementById('showDeleted').value = 'false';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>';
	submitForm(form);
}

function submitfmPublish() {
	form = document.getElementById('fm_publish');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/publish_files" /></portlet:actionURL>';
	submitForm(form);
}

function submitfmDelete() {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.delete")) %>'))
	{
		form = document.getElementById('fm_publish');
		form.cmd.value = 'full_delete_list';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="full_delete_list" /></portlet:actionURL>';
		submitForm(form);
	}
}

function addAsset() {
	window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>';
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
<%  referer = java.net.URLEncoder.encode(referer,"UTF-8"); %>


<jsp:include page="/html/portlet/ext/folders/context_menus_js.jsp" />
<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<!-- START Toolbar -->
<form id="fm" method="post">
	
	<!--- Hidden fields to copy the asset to a new folder
	
	 -->
		<input type="hidden" name="parent" id="parent" value="">
		<input type="hidden" name="selectedparent" id="selectedparent" value="">
		<input type="hidden" name="submitParent" id="submitParent" value="">
		<input type="hidden" name="submitLocation" id="submitLocation" value="">
		<input type="hidden" name="actionURL" id="actionURL" value="">
		<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
		<input type="hidden" id="showDeleted" name="showDeleted" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "value=\"true\"" : "value=\"false\"" %> >
		
	<!--- Hidden fields to copy the asset to a new folder -->

	<div class="yui-gc portlet-toolbar">
		<div class="yui-u first">
			<input type="hidden" name="resetQuery" value="">
	        <input type="hidden" name="host_id" id="host_id" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>"> 
			<input type="text" dojoType="dijit.form.TextBox" style="width:175px;" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
		    <button dojoType="dijit.form.Button" onClick="submitfm()" iconclass="searchIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
		    </button>
			<button dojoType="dijit.form.Button" onClick="resetSearch()" iconclass="resetIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
		    </button>
		    <input type="hidden" name="pageNumber" value="<%=pageNumber%>">
		</div>
		<div class="yui-u" style="text-align:right;">
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="showDeletedCB" id="showDeletedCB" onClick="javascript:submitfm();" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : "" %> value="true">
			<label for="showDeletedCB" style="font-size:85%;"><%= LanguageUtil.get(pageContext, "Show-Archived") %></label>
	
			<button dojoType="dijit.form.Button" onClick="javascript:addAsset()" iconClass="plusIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-file")) %>
			</button>
		</div>
	</div>
	
</form>
<!-- END Toolbar -->

<form id="fm_publish" method="post">
<input type="hidden" name="referer" value="<%=referer%>">
<input type="hidden" name="cmd" value="prepublish">

<!-- START Listing Results -->
<table class="listingTable">
	<tr>
		
		
		<th nowrap style="width:25px;text-align:center;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="cb2" id="checkAll" value="1" onclick="checkAll"/> 
			<span dojoType="dijit.Tooltip" connectId="checkAll" id="ckeckAll_tooltip"><%= LanguageUtil.get(pageContext, "Check-all") %> / <%= LanguageUtil.get(pageContext, "Uncheck-all") %></span>
		</th>
		
		<th nowrap width="50%">
			<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/files/view_files" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"title asc\")?\"title desc\":\"title asc\"%>"/></portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Title") %></a>
		</th>
				
		<th nowrap style="width:35px;" align=""><%= LanguageUtil.get(pageContext, "Status") %></th>
		
		<th nowrap width="30%">
			<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/files/view_files" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"fileName asc\")?\"fileName desc\":\"fileName asc\"%>"/></portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Filename") %></a>
		</th>
		
		<th nowrap width="10%"><%= LanguageUtil.get(pageContext, "Folder") %></th>
		
		<th nowrap width="10%">
			<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/files/view_files" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"modDate asc\")?\"modDate desc\":\"modDate asc\"%>" /></portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Mod-Date") %>
			</a>
		</th>
	</tr>

<% 
	java.util.List files = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.FILES_VIEW);
	int filesSize = ((Long) request.getAttribute(com.dotmarketing.util.WebKeys.FILES_VIEW_COUNT)).intValue();
	String userId = user.getUserId();

	for (int k=0;k<files.size();k++) { 

		com.dotmarketing.beans.PermissionAsset permAsset = (com.dotmarketing.beans.PermissionAsset) files.get(k);							
		
		//gets permissions listings
		java.util.List permissions = permAsset.getPermissions();
		//gets htmlpage
		com.dotmarketing.portlets.files.model.File file = (com.dotmarketing.portlets.files.model.File) permAsset.getAsset();
		String pathToMe = permAsset.getPathToMe();

		if (pathToMe == null)
		    continue;
		if (pathToMe.length()>30) {
			pathToMe = pathToMe.substring(0,30) + "...";
		}

		String str_style = "";
		if ((k%2)==0) {
			str_style = "class=\"alternate_1\"";
		}
		else{
		    str_style="class=\"alternate_2\"";
		}

		//file properties and permissions
		//String parent = Long.toString(file.getParent());
		//String inode = Long.toString(file.getInode());
		String parent = file.getParent();
		String inode = file.getInode();
		String live = (file.isLive())?"1":"0";
		String working = (file.isWorking())?"1":"0";
		String write = (permissions.contains(PermissionAPI.PERMISSION_WRITE))?"1":"0";
		
		Host host = APILocator.getHostAPI().findParentHost(file, APILocator.getUserAPI().getSystemUser(), false);
	%>

	<tr <%=str_style %> id="tr<%=k%>">
	
		
		<td nowrap style="text-align:center;">
			<% if (permissions.contains(PermissionAPI.PERMISSION_PUBLISH)) { %>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="publishInode" id="publishInode<%= file.getInode() %>" value="<%= file.getInode() %>" onclick="togglePublish()" /> 
			<% } %>
		</td>
		
		<td id="td<%=k%>" <%if(!file.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=file.getInode()%>&userId=<%=user.getUserId()%>&referer=<%=referer%>'"<%} %>>
			<span class="uknIcon <%=UtilMethods.getFileExtension(file.getFileName())%>Icon"></span>
			<%=file.getFileName()%>
		</td>
		<td nowrap="true" <%if(!file.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=file.getInode()%>&userId=<%=user.getUserId()%>&referer=<%=referer%>'"<%} %>>
		
			<%=UtilHTML.getStatusIcons(file) %>


		</td>
		<td <%if(!file.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=file.getInode()%>&userId=<%=user.getUserId()%>&referer=<%=referer%>'"<%} %>><%=file.getTitle()==null?"":file.getTitle()%></td>
		<td nowrap <%if(!file.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=file.getInode()%>&userId=<%=user.getUserId()%>&referer=<%=referer%>'"<%} %>><%=pathToMe%></td>
		<td nowrap <%if(!file.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=UUIDGenerator.generateUuid()%>&inode=<%=file.getInode()%>&userId=<%=user.getUserId()%>&referer=<%=referer%>'"<%} %>>
			<%=modDateFormat.format(file.getModDate())%>
			<script language="JavaScript">
			//popup div for the htmlpages
				document.write(getFilePopUp('<%=k%>','<%= CTX_PATH %>', '<%=file.getInode()%>','0','',
				'<%=referer%>','<%=com.dotmarketing.util.UtilMethods.getFileExtension(file.getFileName())%>','<%=(file.isLive()) ? "1" : "0"%>',
				'<%=(file.isWorking()) ? "1" : "0"%>',
				'<%=(file.isDeleted()) ? "1" : "0"%>',
				'<%=(file.isLocked()) ? "1" : "0"%>',
				'<%=permissions.contains(PermissionAPI.PERMISSION_READ) ? "1" : "0" %>',
				'<%=permissions.contains(PermissionAPI.PERMISSION_WRITE) ? "1" : "0" %>',
				'<%=permissions.contains(PermissionAPI.PERMISSION_PUBLISH) ? "1" : "0" %>',
				'<%=user.getUserId()%>','<%= file.getIdentifier() %>'));
			</script>
		</td>
	</tr>
	
<%}%>
<!-- END Listing Results -->

<!-- Start No Results -->	
<% if (filesSize == 0) { %>
	<tr>
		<td colspan="7">
			<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Files-to-show") %></div>
		</td>
	</tr>
<% } %>
<!-- End No Results -->

</table>

<!-- Start Pagination -->
<div class="yui-gb buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/view_files" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) files.get(0)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="previous" /></portlet:renderURL>';" iconClass="previousIcon">
				<%= LanguageUtil.get(pageContext, "Previous") %>
			</button>
		<% } %> &nbsp;
	</div>
	<div class="yui-u" style="text-align:center;">
		<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> - 
<%
	if (maxIndex > (minIndex + filesSize)) {
%>
	<%= minIndex + filesSize %>
<%
	} else {
%>
	<%= maxIndex %>
<%
	}
%>
	<%= LanguageUtil.get(pageContext, "of1") %>
<%
	if (100 <= filesSize) {
%>
	<%= LanguageUtil.get(pageContext, "hundreds") %>
<%
	} else {
%>
	<%= minIndex + filesSize %>
<%
	}
%>
	</div>
	<div class="yui-u" style="text-align:right;">
		<% if (maxIndex < (minIndex + filesSize)) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/view_files" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) files.get(files.size() - 1)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="next" /></portlet:renderURL>';" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %> 
			</button>
		<% } %> &nbsp;
	</div>
</div>
<!-- END Pagination -->


<!-- START Buton Row -->
<div class="buttonRow">
	<button dojoType="dijit.form.Button"  id="publishButton" disabled="true" onClick="submitfmPublish()" iconClass="publishIcon">
	   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
	</button>
	
	<button dojoType="dijit.form.Button" id="deleteButton" onClick="submitfmDelete()" disabled="true" iconClass="deleteIcon">
	   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
	</button>
</div>
<!-- END Buton Row -->

</form>

