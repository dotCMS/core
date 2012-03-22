<%@ include file="/html/portlet/ext/links/init.jsp" %>

<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.portlets.links.model.Link" %>
<%@ page import="com.dotmarketing.portlets.links.struts.LinkForm"%>
<%@ page import="com.dotmarketing.portlets.links.model.Link.LinkType"%>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.portlets.folders.business.FolderAPI" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@ page import="com.dotmarketing.business.Role"%>
<%@ page import="com.dotmarketing.portlets.links.factories.LinkFactory"%><script>
<%
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	LinkForm linkForm = (LinkForm)request.getAttribute("LinkForm");
	Link link = new Link();

	if(request.getAttribute("inode") != null){
		link = (Link)LinkFactory.getLinkFromInode(request.getAttribute("inode").toString(),user.getUserId());
		Identifier identifier = APILocator.getIdentifierAPI().find(link);
%>
		
			selectLink('<%=link.getInode()%>','<%=link.getWorkingURL()%>','<%=link.getTitle()%>','<%=link.getTarget()%>','<%=identifier.getInode()%>');
		</script>
<%
	return;
	}

	// variable that is set to make page a popup
	boolean popup = false;
	if(UtilMethods.isSet(request.getParameter("popup"))){
		popup = true;
	}

	com.dotmarketing.portlets.links.model.Link contentLink;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.LINK_EDIT)!=null) {
		contentLink = (com.dotmarketing.portlets.links.model.Link) request.getAttribute(com.dotmarketing.util.WebKeys.LINK_EDIT);
	}
	else {
		contentLink = (Link) LinkFactory.getLinkFromInode(request.getParameter("inode"),user.getUserId());
	}
	//Permissions variables
	boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
	boolean ownerHasPubPermission = (hasOwnerRole && perAPI.doesRoleHavePermission(contentLink, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
	boolean ownerHasWritePermission = (hasOwnerRole && perAPI.doesRoleHavePermission(contentLink, PermissionAPI.PERMISSION_WRITE,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
	boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
	boolean canUserWriteToLink = ownerHasWritePermission || hasAdminRole || perAPI.doesUserHavePermission(contentLink,PermissionAPI.PERMISSION_WRITE,user);
	boolean canUserPublishLink = ownerHasPubPermission || hasAdminRole || perAPI.doesUserHavePermission(contentLink,PermissionAPI.PERMISSION_PUBLISH,user);

	com.dotmarketing.beans.Identifier identifier = null;
	if(contentLink!=null && UtilMethods.isSet(contentLink.getInode()))
		identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find(contentLink);

	String referer = "";
	if(!popup){
		if (request.getParameter("referer") != null) {
	referer = UtilMethods.encodeURL(request.getParameter("referer"));
		} else {
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/links/view_links"});
	referer = UtilMethods.encodeURL(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params));
		}
		String cmd = request.getParameter(Constants.CMD);
		if( cmd == null && (referer != null && referer.length() > 0) ) {
	referer = UtilMethods.encodeURL(referer);
		}
	}

	
	Role[] roles = (Role[])com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
	Folder folder = null;
	if(UtilMethods.isSet(contentLink.getParent()))
		folder = APILocator.getFolderAPI().find(contentLink.getParent(),user,false);


    String pageWidth = request.getParameter("page_width");
    
	//This variable controls the name of the struts action used when the form is submitted
	//the normal action is /ext/contentlet/edit_link but that can be changed 
	String formAction = request.getParameter("struts_action") == null?"/ext/links/edit_link":request.getParameter("struts_action"); 
	
	//The host of the file
	Host host = link != null?APILocator.getHostAPI().findParentHost(link, APILocator.getUserAPI().getSystemUser(), false):null;
	String hostId = null;
	if(host != null) {
		hostId = host.getIdentifier();
	} else if (request.getParameter("host_id") != null) {
		hostId = request.getParameter("host_id");
	} else {
		hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
	}
%>

<script type="text/javascript" src="/html/js/htmlarea/popups/popup.js"></script>


<script language="Javascript">

	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require('dotcms.dijit.form.FileSelector');
	
	function selectLink(inode,url,title,target,identifier) {
	
		if (isInodeSet(inode)) {
		<% if (UtilMethods.isSet(request.getParameter("wysiwyg"))) { %>
			var param = new Object();
		    param["f_href"] = url;
		    param["f_title"] = title;
		    param["f_target"] = target;
			__dlg_close(param);
			return false;
		<% } else { %>
			opener.setLink(inode,url,title,identifier);
			window.close();
		<% } %>
		}
	}


	var referer = '<%=referer%>';

	function submitfm(form,subcmd) {
			if (document.getElementById("externalLinkType").checked) {
				if ((form.url.value.indexOf("http://") > -1) ||
					(form.url.value.indexOf("https://") > -1) ||
					(form.url.value.indexOf("mailto:") > -1) ||
					(form.url.value.indexOf("ftp://") > -1) ||
					(form.url.value.indexOf("javascript:") > -1)) {

					if (form.url.value.indexOf(form.protocal.value) == -1) {
						alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.alert.url.protocol")) %>');
						form.url.focus();
						return false;
					}
					else {
						form.url.value = form.url.value.replace(form.protocal.value,"");
					}
				}
				form.internalLinkIdentifier.value = '';
			}
			if(document.getElementById("codeLinkType").checked)
				form.protocal.value = "";
				
			if (!isInodeSet(form.parent.value)) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.alert.select.parent.folder")) %>');
				return false;
			}

			form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
			form.<portlet:namespace />subcmd.value = subcmd;
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>';

			submitForm(form);

	}


	function cancelEdit() {
		<% if(popup){ %>
			self.close();
		<% }else{ %>
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=String.valueOf(contentLink.getInode())%>" /></portlet:actionURL>&referer=' + referer;
		<% } %>
	}

	function submitfmDelete() {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.confirm.delete.link")) %>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=String.valueOf(contentLink.getInode())%>" /></portlet:actionURL>&referer=' + referer;
		}
	}

	function submitParent(element){
		if(document.getElementById('existinglink').value.length > 0 && document.getElementById('selectedexistinglink').value.length>0){
			<% if (pageWidth != null) { %>
				page_width = '&page_width=<%= pageWidth %>';
			<% } %>
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" 
value="edit" /><portlet:param name="popup" value="1" /><portlet:param name="browse" value="1" /><portlet:param name="wysiwyg" 
value='<%=(request.getParameter("wysiwyg")!=null)? request.getParameter("wysiwyg") : "" %>' /></portlet:actionURL>&inode=' + + document.getElementById('existinglink').value + '&child=true' + page_width;
		}
	}

	function hideShowOptions(){

		if(document.getElementById("internalLinkType").checked == true){
			// it is internal
			document.getElementById("internalURL").style.display='';
			document.getElementById("externalURL").style.display='none';
			document.getElementById("codeLink").style.display='none';
			document.getElementById("target").style.display='';
		} else if (document.getElementById("externalLinkType").checked == true) {
			// it is external
			document.getElementById("internalURL").style.display='none';
			document.getElementById("externalURL").style.display='';
			document.getElementById("codeLink").style.display='none';
			document.getElementById("target").style.display='';
		} else {
			// it is code type of link
			document.getElementById("internalURL").style.display='none';
			document.getElementById("externalURL").style.display='none';
			document.getElementById("target").style.display='none';
			document.getElementById("codeLink").style.display='';
		}

	}
	
    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.confirm.delete.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.links.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=edit&inode=' + objId + '&referer=' + referer;
	}

	function hideEditButtonsRow() {
		dojo.style('editLinkButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editLinkButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}

</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-link\") %>" />
<% if (pageWidth != null) { %>
	<liferay:param name="box_width" value="<%=pageWidth%>" />
<% } %>

<html:form action='/ext/links/edit_link' styleId="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">

<% if(!popup){ %>
  	<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/links/view_links" /></portlet:renderURL>">
<% } %>

<input name="<portlet:namespace />subcmd" type="hidden" value="">
<input name="<portlet:namespace />child" type="hidden" value="<%=request.getParameter("child")%>">
<input name="<portlet:namespace />inode" type="hidden" value="<%=contentLink.getInode()%>">
<input type="hidden" name="existinglink" id="existinglink" value="">
<input type="hidden" name="selectedexistinglink" id="selectedexistinglink" value="">

<!-- START TABS -->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
	
<!-- START Link Properties -->
	<div id="fileBasicTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>" onShow="showEditButtonsRow()">
				
		<dl>
		
		<input name="referer" type="hidden" value="<%=referer%>">
		
		<input name="<%= Constants.CMD %>" type="hidden" value="add">
		<input type="hidden" name="userId" value="<%= user.getUserId() %>">
		
		<%if(identifier!=null){%>
			<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
			<dd><%= identifier.getId() %></dd>
		<%}%>
		
			<dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
			<dd>
				<input type="text" dojoType="dijit.form.TextBox" style="width:250px;" name="title" id="titleField" value="<%= UtilMethods.isSet(linkForm.getTitle()) ? linkForm.getTitle() : "" %>" />
				<html:hidden  property="friendlyName" styleId="friendlyNameField"/>
			</dd>
		
			<dt><%= LanguageUtil.get(pageContext, "Folder") %>:</dt>
			<dd>
				<% if(!InodeUtils.isSet(contentLink.getParent())) { %>
					<div id="folder" name="parent" onlySelectFolders="true" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" <%= UtilMethods.isSet(hostId)?"hostId=\"" + hostId + "\"":"" %>></div>
				<% } else { %>
					<input type="text" readonly="readonly" styleClass="form-text" value="<%= APILocator.getIdentifierAPI().find(folder).getPath() %>" />
					<html:hidden styleClass="form-text" property="parent" styleId="parent" />
				<% } %>
			</dd>
			
			<dt><%= LanguageUtil.get(pageContext, "Type") %>:</dt>
			<dd>
				<input dojoType="dijit.form.RadioButton" type="radio" <%= linkForm.getLinkType().equals(LinkType.INTERNAL.toString())?"checked":"" %> id="internalLinkType" name="linkType" value="<%= LinkType.INTERNAL.toString() %>" onclick="hideShowOptions()">
				<label for="internalLinkType"><%= LanguageUtil.get(pageContext, "Internal-Link") %></label>
				
				<input dojoType="dijit.form.RadioButton" type="radio" <%= linkForm.getLinkType().equals(LinkType.EXTERNAL.toString())?"checked":"" %> id="externalLinkType" name="linkType" value="<%= LinkType.EXTERNAL.toString() %>" onclick="hideShowOptions()">
				<label for="externalLinkType"><%= LanguageUtil.get(pageContext, "External-Link") %></label>
				
				<input dojoType="dijit.form.RadioButton" type="radio" <%= linkForm.getLinkType().equals(LinkType.CODE.toString())?"checked":"" %> id="codeLinkType" name="linkType" value="<%= LinkType.CODE.toString() %>" onclick="hideShowOptions()">
				<label for="codeLinkType"><%= LanguageUtil.get(pageContext, "Code-Link") %></label>
			</dd>
		</dl>

		<!-- If External Link -->	
		<dl id="externalURL" style="display:<% if(contentLink.getLinkType() != Link.LinkType.EXTERNAL.toString()) { %>none;<% } %>">
			<dt>&nbsp;</dt>
			<dd>
				<select dojoType="dijit.form.ComboBox" autocomplete="false" name="protocal" id="protocal" style="width:94px;"value="<%= UtilMethods.isSet(linkForm.getProtocal()) ? linkForm.getProtocal() : "" %>" >
					<option>http://</option>
					<option>https://</option>
					<option>mailto:</option>
					<option>ftp://</option>
					<option>javascript:</option>
				</select>
				<input type="text" dojoType="dijit.form.TextBox" style="width:200px;" name="url" id="url" value="<%= UtilMethods.isSet(linkForm.getUrl()) ? linkForm.getUrl() : "" %>" />
			</dd>
		</dl>
		<!-- /If External Link -->

		<!-- If Internal Link -->						
		<dl id="internalURL" style="display:<% if(contentLink.getLinkType() != Link.LinkType.INTERNAL.toString()) { %>none;<% } %>">
			<dt>&nbsp;</dt>	
			<dd>
				<input type="text" name="internalLinkIdentifier" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="list"  
					value="<%= linkForm.getInternalLinkIdentifier() %>" showThumbnail="false" />			
			</dd>
		</dl>
		<!-- /If Internal Link -->

		<!-- If Code Link -->
		<dl id="codeLink" style="display:<% if(contentLink.getLinkType() != Link.LinkType.CODE.toString()) { %>none;<% } %>">
			<SCRIPT language="JavaScript" src="/html/js/cms_ui_utils.js"></SCRIPT>
			<dt>
				<!-- Resize TextArea -->
					<table align="right">
						<tr>
							<td><a href="javascript:makeNarrower('linkCode');"><IMG border="0" src="/html/images/icons/arrow-180-medium.png" width="16" height="16" alt="make narrower"></a></td>
							<td>
								<a href="javascript:makeShorter('linkCode');"><IMG border="0" src="/html/images/icons/arrow-090-medium.png" width="16" height="16" alt="make shorter"></a><br />
								<a href="javascript:makeTaller('linkCode');"><IMG border="0" src="/html/images/icons/arrow-270-medium.png" width="16" height="16" alt="make taller"></a>
							</td>
							<td><a href="javascript:makeWider('linkCode');"><IMG border="0" src="/html/images/icons/arrow-000-medium.png" width="16" height="16" alt="make wider"></a></td>
						</tr>
					</table>
				<!-- /Resize TextArea -->
			</dt>	
			<dd>
				<%--html:textarea onkeydown="return catchTab(this,event)" style="width:450px; height:150px; font-size: 12px" property="linkCode" styleId="linkCode"></html:textarea--%>
				<textarea dojoType="dijit.form.Textarea" style="width:250px; min-height:150px; font-size:12px" name="linkCode" id="linkCode"><%= UtilMethods.isSet(linkForm.getLinkCode()) ? linkForm.getLinkCode() : "" %></textarea>
				<script>
					dojo.connect(dijit.byId('linkCode'), 'onkeydown', function(e) { return catchTab(document.getElementById('linkCode'), e) });
				</script>
			</dd>
		</dl>
		<!-- /If Code Link -->

		<!-- Link Target -->
		<dl id="target" style="display:<% if(contentLink.getLinkType() != Link.LinkType.CODE.toString()) { %>none;<% } %>">
			<dt><%= LanguageUtil.get(pageContext, "Target") %>:</dt>
			<dd>
				<select dojoType="dijit.form.FilteringSelect" autocomplete="false" name="target" id="target" value="<%= UtilMethods.isSet(linkForm.getTarget()) ? linkForm.getTarget() : "" %>">
					<option value="_self"><%= LanguageUtil.get(pageContext, "Same-Window") %></option>
					<option value="_blank"><%= LanguageUtil.get(pageContext, "New-Window") %></option>
					<option value="_top"><%= LanguageUtil.get(pageContext, "Parent-Window") %></option>
				</select>
			</dd>
		</dl>
		<!-- /Link Target -->

		<dl>
			<dt><%= LanguageUtil.get(pageContext, "sort-order") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" name="sortOrder" style="width:50px;" id="sortOrder" size="3" value="<%= linkForm.getSortOrder() %>" /></dd>
			
			<dt><%= LanguageUtil.get(pageContext, "Show-on-Menu") %>:</dt>
			<dd>
				<!--<html:checkbox styleClass="form-text" property="showOnMenu" />-->
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="showOnMenu" id="showOnMenu" <%= linkForm.isShowOnMenu() ? "checked" : "" %> />
			</dd>
		</dl>

	</div>
<!-- END Link Properties -->

<!-- Permissions Tab -->
<%
	boolean canEditAsset = perAPI.doesUserHavePermission(contentLink, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="filePermissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentLink);
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, folder);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>	
	</div>
<%
	}
%>
<!-- /Permissions Tab  -->
		
<!-- START Versions Tab -->
	<%if(contentLink != null && InodeUtils.isSet(contentLink.getInode())){ %>
		<div id="fileVersionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>" onShow="showEditButtonsRow()">
			<%@ include	file="/html/portlet/ext/common/edit_versions_inc.jsp"%>
		</div>
	<% } %>
<!-- END Versions Tab -->

</div>
<!-- END TABS -->

<div class="clear"></div>							

<!-- Button Row --->
<div class="buttonRow" id="editLinkButtonRow">
	<% 
		if(!InodeUtils.isSet(link.getInode())) { 
         	canUserWriteToLink = perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user);
       	}
	%>
	<% if (!InodeUtils.isSet(contentLink.getInode()) || contentLink.isLive() || contentLink.isWorking()) { %>
	<% if (!UtilMethods.isSet(request.getParameter("browse"))) { %>

	<% if( canUserWriteToLink ) { %>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'')" iconClass="saveIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
		</button>
	<% } %>

	<% if( canUserPublishLink ) { %>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'publish')" iconClass="publishIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
		</button>
	<% } %>
	<% } else { %>
	<%
		String title = contentLink.getTitle();
		if (title!=null) {
		title = title.replaceAll("\'","\\\\\'");
		}
	%>
		<button dojoType="dijit.form.Button" onClick="selectLink('<%=contentLink.getInode()%>','<%=contentLink.getWorkingURL()%>', '<%= title %>', '<%= contentLink.getTarget() %>')" iconClass="linkIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "select-link")) %>
		</button>
	<% } %>
	<% } else { %>
		<button dojoType="dijit.form.Button" onClick="selectVersion(<%=contentLink.getInode()%>, '<%=referer%>')" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
		</button>
	<% } %>
	
	<% if (InodeUtils.isSet(contentLink.getInode()) && contentLink.isDeleted())  { %>
		<button dojoType="dijit.form.Button" onClick="submitfmDelete()" iconClass="deleteIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Link")) %>
		</button>
	<% } %>
	
	<button dojoType="dijit.form.Button" onClick="cancelEdit()"  iconClass="cancelIcon" type="button">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	</button>

</div>
<!-- /Button Row -->

</html:form>
</liferay:box>
<script language=javascript>
	dojo.addOnLoad(//DOTCMS-5038
		 function(){			
			self.focus();
			if(dijit.byId("titleField").isFocusable()){
				dijit.byId("titleField").focus();
			}else{
				setTimeout("if(dijit.byId('titleField').isFocusable()){dijit.byId('titleField').focus();}",500);
			}
			hideShowOptions();
		}
	);
</script>

