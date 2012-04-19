<%@ page import="com.dotmarketing.beans.Host"%>
<%@ page import="com.dotmarketing.beans.Identifier" %>
<%@ page import="com.dotmarketing.business.IdentifierFactory" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.InodeUtils"%>
<%@ page import="com.dotmarketing.portlets.templates.struts.TemplateForm"%>
<%@ page import="java.net.URLDecoder"%>
<%@ include file="/html/portlet/ext/templates/init.jsp" %>
<%@page import="com.dotmarketing.portlets.containers.business.ContainerAPI"%>

<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>

<style type="text/css">
	@import url(/html/css/template/draw-template.css);
	@import url(/html/css/template/reset-fonts-grids.css);
</style>

<%
 
	boolean overrideBody = (Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.OVERRIDE_DRAWED_TEMPLATE_BODY);
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	ContainerAPI containerAPI = APILocator.getContainerAPI();
	HostAPI hostAPI = APILocator.getHostAPI();
	
	com.dotmarketing.portlets.templates.model.Template template;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT)!=null) {
		template = (com.dotmarketing.portlets.templates.model.Template) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT);
	}
	else {
		template = (com.dotmarketing.portlets.templates.model.Template) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"),com.dotmarketing.portlets.templates.model.Template.class);
	}
	
	StringBuffer drawedBodyTemplate = new StringBuffer();
	if(overrideBody)
		drawedBodyTemplate = new StringBuffer(template.getDrawedBody());
	
	//Permissions variables
	boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
	boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId());
	boolean canUserWriteToTemplate = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(template,PermissionAPI.PERMISSION_WRITE,user);
	boolean canUserPublishTemplate = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(template,PermissionAPI.PERMISSION_PUBLISH,user);
	//http://jira.dotmarketing.net/browse/DOTCMS-1473 basically if teh user has portlet permissions they can add new templates and containers
	Identifier id=null;
	if(!InodeUtils.isSet(template.getInode())){
		canUserWriteToTemplate = true;
		canUserPublishTemplate = true;
	}
	else {
		id = APILocator.getIdentifierAPI().find(template);
	}
	
	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = URLDecoder.decode(request.getParameter("referer"), "UTF-8");
		referer = UtilMethods.encodeURL(referer);
	} else {
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action",new String[] {"/ext/templates/view_templates"});
		if (request.getParameter("pageNumber")!=null) {
			int pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
			params.put("pageNumber",new String[] { pageNumber + "" });
		}
		referer = UtilMethods.encodeURL(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params));
	}
	
	TemplateForm form = (TemplateForm)request.getAttribute("TemplateForm");
	
	//Getting the list of containers for the container selector
	String hostId = "";
	if(form.getHostId()!=null) 
		hostId = form.getHostId();
	List<Host> listHosts= (List <Host>) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_HOSTS);
	if(!UtilMethods.isSet(hostId)) {
		if(request.getParameter("host_id") != null) {
			hostId = request.getParameter("host_id");
		} else {
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		}	
	}
	Host host = null;
	if(UtilMethods.isSet(hostId)) {
		host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
	}	

%>
<script language="JavaScript" src="/html/js/template/dwr/interface/ContainerAjaxDrawedTemplate.js"></script>
<script language="Javascript">

	dojo.require('dotcms.dijit.form.FileSelector');
	dojo.require('dotcms.dojo.data.ContainerReadStoreDrawedTemplate');
	
	var referer = '<%=referer%>';

	function submitfm(form,subcmd) {
		if (form.admin_l2) {
			for (var i = 0; i < form.admin_l2.length; i++) {
				form.admin_l2.options[i].selected = true;
			}
		}
		saveBody();
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD_DESIGN%>';
		form.<portlet:namespace />subcmd.value = subcmd;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>';
		submitForm(form);
	}
	
	var copyAsset = false;

	function cancelEdit() {
		self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
	}

	function submitfmDelete() {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.delete.template")) %>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
		}
	}

	function showAddContainerDialog(idDiv) {
		dijit.byId('containersList').attr('value', '');
		dijit.byId('containerSelector').show();
		document.getElementById("idDiv").value=idDiv;
	}
	
	function addContainer() {
		var idDiv = document.getElementById("idDiv");	
		dijit.byId('containerSelector').hide();
		var value = dijit.byId('containersList').attr('value');
		var container = dijit.byId('containersList').attr('item');
		
		addDrawedContainer(idDiv,container,value);
	}

	function addFile() {
		fileSelector.show();
	}

	function addFileCallback(file) {
		if(file.extension == 'js') {
			var html = '<script type="text/javascript" src="' + file.path + file.fileName + '" >' + '<' + '/script' + '>';
		} else if (file.extension == 'css') {
			var html = '<link href="' + file.path + file.fileName + '" rel="stylesheet" type="text/css" />';
		}
		addFileToTemplate(html,file);
	}

	function selectTemplateVersion(objId,referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.replace.template")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer;
		}
	}

    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.delete.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId  + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId  + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=edit&inode=' + objId  + '&referer=' + referer;
	}
	
	function getContainerMockContent(title){
		return "<h2>Container: "+title+"</h2><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>";
	}
	
</script>

<script language="JavaScript" src="/html/js/cms_ui_utils.js"></script>
<script language="JavaScript" src="/html/js/template/utility-left-menu.js"></script>

<script type="text/javascript">
	dojo.addOnLoad(function() {
		drawDefault(<%=overrideBody%>);
	});
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"draw-template\") %>" /> 

	<html:form action='/ext/templates/edit_template' styleId="fm">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
	<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
	<input name="<portlet:namespace />inode" type="hidden" value="<%=template.getInode()%>">
	<input name="<portlet:namespace />subcmd" type="hidden" value="">
	<input name="userId" type="hidden" value="<%= user.getUserId() %>">
	<input name="admin_l_list" type="hidden" value="">
		
<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" style="height: 100%; min-height: 881px;" >
	<div id="properties" dojoType="dijit.layout.ContentPane" style="padding:0;height: 100%; min-height:851px;" title="Properties">
		<div class="wrapperRight" style="position:relative; height: 500px;">
			<dl>
				<%if(id!=null) {%>
					<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
					<dd><%= id.getId() %></dd>
				<%}%>
				<% if(host != null) { %>
					<html:hidden property="hostId" value="<%=hostId%>"/>
					<dt><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</dt>
					<dd><%= host.getHostname() %></dd>					
				<%	} else { %>
					<dt><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</dt>
					<dd>
					<select id="hostId" name="hostId" dojoType="dijit.form.FilteringSelect" value="<%=hostId%>">
					<% for(Host h: listHosts) { %>		
						<option value="<%=h.getIdentifier()%>"><%=host.getHostname()%></option>	
					<% } %>1
					</select>
					</dd>
				<% } %>
				<dt>
					<span class="required"></span>
					<%= LanguageUtil.get(pageContext, "Title") %>:
				</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px" name="title" id="titleField" value="<%= UtilMethods.isSet(template.getTitle()) ? template.getTitle() : "" %>" /></dd>
					
				<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px" name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(template.getFriendlyName()) ? template.getFriendlyName() : "" %>" /></dd>
				
				<dt><%= LanguageUtil.get(pageContext, "Screen-Capture-Image") %>:</dt>
				<dd>
					<input type="text" name="image" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" mimeTypes="image" 
						value="<%= UtilMethods.isSet(form.getImage())?form.getImage():"" %>" showThumbnail="true" />			
				</dd>
				<dd>
	    			<html:textarea style="display: none" property="body" styleId="bodyField"></html:textarea>
				</dd>
				<dd>
	    			<html:textarea style="display: none" property="drawedBody" styleId="drawedBodyField"></html:textarea>
				</dd>									
			</dl>	
		</div>	
	</div>
	<div id="template" dojoType="dijit.layout.ContentPane" style="padding:0; height: 100%; min-height: 851px;" title="Design Template" >	
		<div class="wrapperRight" style="position:relative;" id="containerBodyTemplate">
			<div id="addFileToTemplate">
				<button dojoType="dijit.form.Button" onClick="addFile()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-js-css")) %>
				</button>			
			</div>
			<div class="clear"></div>
			<div id="bodyTemplate"></div>
		</div>
	</div>

</div>		
<div class="buttonRow-left lineRight" id="editContentletButtonRow" style="height: 100%; min-height: 617px;">
	<div class="gradient2">
		<div class="fieldWrapperSide">
			<div class="leftProperties">		
				<dl>
					<dt>Page Width:</dt>						
					<dd><select id="pageWidth" dojoType="dijit.form.FilteringSelect" name="pageWidth" onchange="javascript: addPageWidth(this.value)">
							<option value="doc-template">750px</option>
							<option value="doc2-template">950px</option>
							<option value="doc3-template" selected="selected">100%</option>
							<option value="doc4-template">974px</option>
						</select>
					</dd>
				</dl>
			</div>	
		</div>
		<div class="clear"></div>	
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt>Layout:</dt>						
					<dd><select id="layout" dojoType="dijit.form.FilteringSelect" name="layout" onchange="javascript: addLayout(this.value)">
							<option value="none" selected="selected"></option>
							<option value="yui-t1-template">160px on left</option>
							<option value="yui-t2-template">180px on left</option>
							<option value="yui-t3-template">300px on left</option>
							<option value="yui-t4-template">180px on right</option>
							<option value="yui-t5-template">240px on right</option>
							<option value="yui-t6-template">300px on right</option>
						</select>
					</dd>
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt>Header:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_header" onclick="javascript: addHeader(this.checked)" checked="checked"/>
					</dd>
				</dl>	
				<div class="clear"></div>
			    <dl>
					<dt>Footer:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_footer" onclick="javascript: addFooter(this.checked)" checked="checked"/>
					</dd>	
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="gradient title">Body Rows</div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt id="tableContainer">
						<table id="splitBodyTable" cellspacing="4" cellpadding="2">
					        <tr class="spaceUnder">
						        <td style="width: 16px;">&nbsp;&nbsp;</td>
						        <td>
									<select name="select_splitBody0" dojoType="dijit.form.FilteringSelect" onchange="javascript: addGrid(this.value, 'splitBody0',0)">
										<option value="1" selected="selected">1 Column (100)</option>
										<option value="yui-gc-template">2 Column (66/33)</option>
										<option value="yui-gd-template">2 Column (33/66)</option>
										<option value="yui-ge-template">2 Column (75/25)</option>
										<option value="yui-gf-template">2 Column (25/75)</option>
										<option value="yui-gb-template">3 Column (33/33/33)</option>
									</select>				        			
						        </td>
							</tr>	
						</table>					
					</dt>
				</dl>
				<div class="buttonRow" style="text-align: center">
					<button id="addRow" dojoType="dijit.form.Button" onClick="javascript: addRow('splitBodyTable','select_splitBody','splitBody')" iconClass="plusIcon" type="button">
						Add Row
					</button>					
				</div>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="gradient title"><%=LanguageUtil.get(pageContext, "Actions") %></div>
		<div id="contentletActionsHanger">		
			<% if (!InodeUtils.isSet(template.getInode()) || template.isLive() || template.isWorking()) { %>
				<% if ( canUserWriteToTemplate ) { %>
				<a onClick="submitfm(document.getElementById('fm'),'')">
					<span class="saveIcon"></span>
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
				</a>
				<% } %>
			<% 
			if ( canUserPublishTemplate ) { %>			
				<a onClick="submitfm(document.getElementById('fm'),'publish')">
					<span class="publishIcon"></span>
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
				</a>
			<% } %>
		
			<% } else if (InodeUtils.isSet(template.getInode())) { %>	
				<a onClick="selectTemplateVersion(<%=template.getInode()%>, '<%=referer%>')">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
				</a>
			<% } %>
		
			<% if (InodeUtils.isSet(template.getInode()) && template.isDeleted()) {%>	
				<a onClick="submitfmDelete()">
					<span class="deleteIcon"></span>
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-template")) %>
				</a>
			<% } %>
				<a onClick="cancelEdit()">
					<span class="cancelIcon"></span>
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
				</a>			
		</div>										
	</div>	
</div>
<div class="clear"></div>
</html:form>
</liferay:box>
<div id="editTempateBox" dojoType="dijit.Dialog" style="display:none" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-container")) %>">
	<div dojoType="dijit.layout.ContentPane" style="width:400px;height:150px;" class="box" hasShadow="true" id="editTemplateBoxCp">
	</div>
</div>

<div dojoAttachPoint="fileBrowser" jsId="fileSelector" onFileSelected="addFileCallback" fileExtensions="js,css" dojoType="dotcms.dijit.FileBrowserDialog">
</div>

<span dojoType="dotcms.dojo.data.ContainerReadStoreDrawedTemplate" jsId="containerStore"></span>

<div dojoType="dijit.Dialog" id="containerSelector" title="<%=LanguageUtil.get(pageContext, "select-a-container")%>">
	<p class="alertContainerSelector"><%=LanguageUtil.get(pageContext, "only-containers-without-html-tag")%></p>
	<p style="text-align: center">
		<%=LanguageUtil.get(pageContext, "Container")%> 
  		<select id="containersList" name="containersList" dojoType="dijit.form.FilteringSelect" 
        	store="containerStore" searchDelay="300" pageSize="10" labelAttr="fullTitle" searchAttr="title" 
            invalidMessage="<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
        </select>
        <input id="idDiv" name="idDiv" type="hidden" value="">
    </p>
    <div class="buttonRow">
		<button dojoType="dijit.form.Button" onclick="addContainer()" type="button"><%=LanguageUtil.get(pageContext, "Add")%></button> 
		<button dojoType="dijit.form.Button" onclick="dijit.byId('containerSelector').hide()" type="button"><%=LanguageUtil.get(pageContext, "Cancel")%></button>
	</div>
</div>


