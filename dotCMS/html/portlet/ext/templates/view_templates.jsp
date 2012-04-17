<%@ include file="/html/portlet/ext/templates/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.dotmarketing.util.*"%>
<%@ page import="com.dotmarketing.business.APILocator"%>

<script type='text/javascript' src='/dwr/interface/TemplateAjax.js'></script>
<%
	Boolean hostChanged = (Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_HOST_CHANGED);
	int pageNumber = 1;
	if (!hostChanged && (request.getParameter("pageNumber") != null) && (request.getParameter("resetQuery") == null)) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
	}

	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/templates/view_templates"});
	params.put("pageNumber",new String[] { pageNumber + "" });

	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	referer = java.net.URLEncoder.encode(referer,"UTF-8");
	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String showDeleted = (request.getParameter("showDeleted")!=null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_SHOW_DELETED);
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_QUERY);
	String orderby = (request.getParameter("orderby")!=null) ? request.getParameter("orderby") : "";
	String depResp = (String) pageContext.getAttribute("depResp");
	boolean dependencies = UtilMethods.isSet(depResp);

	String hostId = "";
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID) != null)
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		if (request.getParameter("host_id") != null && InodeUtils.isSet(request.getParameter("host_id")))
			   hostId = request.getParameter("host_id");
	} catch (NumberFormatException e) {
	}

%>

<jsp:include page="/html/portlet/ext/folders/context_menus_js.jsp" />
<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<script language="Javascript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

function resetSearch() {
	form = document.getElementById('fm');
	form.showDeleted.value = '';
	form.resetQuery.value = "true";
	form.query.value = '';
	//form.host_id.value = "";
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/templates/view_templates" /></portlet:renderURL>';
	submitForm(form);
}

function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/templates/view_templates" /></portlet:renderURL>';
	submitForm(form);
}

function submitfmPublish() {
	form = document.getElementById('fm');
	form.cmd.value = 'prepublish';
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/publish_templates" /></portlet:actionURL>';
	submitForm(form);
}

function submitfmDelete() {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.delete.template")) %>'))
	{
		form = document.getElementById('fm');
		form.cmd.value = 'full_delete_list';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="full_delete_list" /></portlet:actionURL>';
		submitForm(form);
	}
}

function addAsset(event) {
	window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="edit" /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>';
	dojo.stopEvent(event);
}

// *********************** BEGIN GRAZIANO issue-12-dnd-template
function designAsset(event) {	
	window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="design" /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>';
	dojo.stopEvent(event);
}
//*********************** END GRAZIANO issue-12-dnd-template

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

function editTemplate(inode){
	top.location="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=" + inode + "&referer=<%=referer%>";
}

function delTemplate(inode, referer) {

	var callMetaData = {
			  callback:handleDepResponse,
			  arg: inode + '|' + referer, // specify an argument to pass to the callback and exceptionHandler
			};

	TemplateAjax.checkDependencies(inode, callMetaData);
}

function handleDepResponse(data, arg1) {
	var params = arg1.split('|');
	var inode = params[0];
	var referer = params[1];


	if(data!=null) {
		dojo.byId("depDiv").innerHTML = "<br />" + data;
		dijit.byId("dependenciesDialog").show();
	} else {
		processDelete(inode, referer);
	}
}

function processDelete(inode, referer) {
	top.location="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&inode=" + inode + "&referer=" + referer;
}


</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-templates-all") %>' />

<form id="fm" method="post" >
<div class="yui-gc portlet-toolbar">
	<div class="yui-u first">
			<input type="hidden" name="resetQuery" value="">
			<input type="hidden" name="host_id" id="host_id" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>">
			<input type="text" dojoType="dijit.form.TextBox" style="width:175px;" name="query" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
		    <button dojoType="dijit.form.Button"  onClick="submitfm()" iconClass="searchIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search" )) %>
		    </button>

			<button dojoType="dijit.form.Button" onClick="resetSearch()" iconClass="resetIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
		    </button>

			<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
	</div>
	<div class="yui-u" style="text-align:right;">
		<input  dojoType="dijit.form.CheckBox" type="checkbox" name="showDeleted" id="showDeleted" onClick="javascript:submitfm();" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : "" %> value="true" />
		<label for="showDeleted" style="font-size:85%;"><%= LanguageUtil.get(pageContext, "Show-Archived") %></label>

		<% if((Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_CAN_ADD)) { %>
		<button dojoType="dijit.form.Button" onClick="addAsset" iconClass="plusIcon">
	        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-template")) %>
	    </button>
		<% } %>
		<!-- *********************** BEGIN GRAZIANO issue-12-dnd-template -->
		<% if((Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_CAN_DESIGN)) { %>
		<button dojoType="dijit.form.Button" onClick="designAsset" iconClass="designTemplateIcon">
	        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "design-template")) %>
	    </button>
		<% } %>		
		<!-- *********************** END GRAZIANO issue-12-dnd-template -->
	</div>
</div>

<input type="hidden" name="referer" value="<%=referer%>">
<input type="hidden" name="cmd" value="">

<!-- START Listing Results -->
<table class="listingTable">
	<tr>

		<th nowrap style="width:25px;text-align:center;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="cb2" id="checkAll" value="1" onclick="checkAll"/>
			<span dojoType="dijit.Tooltip" connectId="checkAll" id="ckeckAll_tooltip"><%= LanguageUtil.get(pageContext, "Check-all") %> / <%= LanguageUtil.get(pageContext, "Uncheck-all") %></span>
		</th>
		<th nowrap width="30%">
			<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/templates/view_templates" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"title asc\")?\"title desc\":\"title asc\"%>" />
			</portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Title") %></a>
		</th>
		<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th nowrap width="45%"><%= LanguageUtil.get(pageContext, "Description") %></th>
		<th nowrap width="10%">
			<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/templates/view_templates" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"modDate desc\")?\"modDate asc\":\"modDate desc\"%>" />
			</portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Mod-Date") %>
		</th>
	</tr>

	<%
		java.util.List templates = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATES_VIEW);
		int templatesSize = ((Long) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATES_VIEW_COUNT)).intValue();
		String userId = user.getUserId();

		for (int k=0;k<templates.size();k++) {
			com.dotmarketing.beans.PermissionAsset permAsset = (com.dotmarketing.beans.PermissionAsset) templates.get(k);
			java.util.List permissions = permAsset.getPermissions();
			com.dotmarketing.portlets.templates.model.Template template = (com.dotmarketing.portlets.templates.model.Template) permAsset.getAsset();

			String str_style = "";
			if ((k%2)==0) {
				str_style = "class=\"alternate_1\"";
			}
			else{
				str_style = "class=\"alternate_2\"";
            }

			//container properties and permissions
			String inode = template.getInode();
			boolean live = (template.isLive());
			boolean working = (template.isWorking());
			boolean deleted = (template.isDeleted());
			boolean locked = (template.isLocked());
			boolean write = APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_EDIT, user);
			boolean publish = APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_PUBLISH, user);
			Host host = APILocator.getHostAPI().findParentHost(template, APILocator.getUserAPI().getSystemUser(), false);
		%>
			<tr id="tr<%=k%>" <%=str_style%>>
				<td nowrap style="text-align:center;">
					<% if (publish) { %>
								<input dojoType="dijit.form.CheckBox" type="checkbox" name="publishInode" id="publishInode<%= template.getInode() %>" value="<%= template.getInode() %>" onclick="togglePublish()" />
					<% } %>
				</td>
				<td nowrap>
					<span class="templateIcon"></span>
					<a href="javascript:editTemplate('<%=inode%>');" ><%=template.getTitle()%></a>
				</td>
				<td nowrap><%= com.dotmarketing.util.UtilHTML.getStatusIcons(template) %></td>
				<td><%=template.getFriendlyName()%></td>
				<td nowrap>
					<%=modDateFormat.format(template.getModDate())%>
					<script language="JavaScript">
						//popup div for the template
						document.write(getTemplatePopUp('<%=k%>','<%= CTX_PATH %>', '<%=template.getInode()%>','',
							'<%=referer%>','<%=(template.isLive()) ? "1" : "0"%>',
							'<%=(template.isWorking()) ? "1" : "0"%>',
							'<%=(template.isDeleted()) ? "1" : "0"%>',
							'<%=(template.isLocked()) ? "1" : "0"%>',
							'<%=permissions.contains(PermissionAPI.PERMISSION_READ) ? "1" : "0" %>',
							'<%=permissions.contains(PermissionAPI.PERMISSION_WRITE) ? "1" : "0" %>',
							'<%=permissions.contains(PermissionAPI.PERMISSION_PUBLISH) ? "1" : "0" %>',
							'<%=user.getUserId()%>'));
					</script>
				</td>
			</tr>
		<%}%>
<!-- END Listing Results -->

<!-- Start No Results -->
<% if (templatesSize ==0) { %>
	<tr>
		<td colspan="5">
			<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Templates-to-show") %></div>
		</td>
	</tr>
<% } %>
<!-- End No Results -->
</table>

<!-- Start Pagination -->
	<div class="yui-gb buttonRow">
		<div class="yui-u first">
			<% if (minIndex != 0) { %>
				<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/view_templates" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) templates.get(0)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="previous" /></portlet:renderURL>';" iconClass="previousIcon">
					<%= LanguageUtil.get(pageContext, "Previous")%>
				</button>
			<% } %>&nbsp;
		</div>
		<div class="yui-u" style="text-align:center;">
			<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> -
<%
	if (maxIndex > (minIndex + templatesSize)) {
%>
	<%= minIndex + templatesSize %>
<%
	} else {
%>
	<%= maxIndex %>
<%
	}
%>
	<%= LanguageUtil.get(pageContext, "of1") %>
<%
	if (100 < templatesSize) {
%>
	<%= LanguageUtil.get(pageContext, "hundreds") %>
<%
	} else {
%>
	<%= minIndex + templatesSize %>
<%
	}
%>
		</div>
		<div class="yui-u" style="text-align:right;">
			<% if (maxIndex < (minIndex + templatesSize)) { %>
				<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/view_templates" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) templates.get(templates.size() - 1)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="next" /></portlet:renderURL>';" iconClass="nextIcon">
					<%= LanguageUtil.get(pageContext, "Next") %>
				</button>
			<% } %>&nbsp;
		</div>
	</div>
<!-- END Pagination -->



<!-- Start Buttons -->
	<div class="buttonRow">
	    <button dojoType="dijit.form.Button" id="publishButton" onClick="submitfmPublish()" disabled="true" iconClass="publishIcon">
	       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
	    </button>

	    <button dojoType="dijit.form.Button" id="deleteButton" onClick="submitfmDelete()" disabled="true" iconClass="deleteIcon">
	       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete")) %>
	    </button>
	</div>
<!-- END Buttons -->

</form>

</liferay:box>

<script>

dojo.addOnLoad(function() {
if(<%=dependencies%>)
{
	dojo.byId("depDiv").innerHTML = "<br />" + "<%=depResp%>";
	dijit.byId("dependenciesDialog").show();
}

}) ;
</script>

<div id="dependenciesDialog" dojoType="dijit.Dialog" style="display:none;width:630px;height:300px;vertical-align: middle; " draggable="true"
	title="<%= LanguageUtil.get(pageContext, "Delete-Template") %>" >

	<span style="color: red; font-weight: bold"><%= LanguageUtil.get(pageContext, "message.template.full_delete.error") %></span>

	<div id="depDiv" style="overflow: auto; height: 220px"></div>
</div>

