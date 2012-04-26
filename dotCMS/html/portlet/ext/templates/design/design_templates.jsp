<%@ page import="com.dotmarketing.portlets.templates.design.bean.SplitBody"%>
<%@ page import="com.dotmarketing.portlets.templates.design.bean.DesignTemplateJSParameter"%>
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
<%@ page import="com.dotmarketing.portlets.containers.business.ContainerAPI"%>

<%@ page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>

<style type="text/css">
	@import url(/html/css/template/draw-template.css);
	@import url(/html/css/template/drawed-reset-fonts-grids.css);
</style>

<%
 
	boolean overrideBody = (Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.OVERRIDE_DRAWED_TEMPLATE_BODY);
	
	DesignTemplateJSParameter parameters = (DesignTemplateJSParameter)request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_JAVASCRIPT_PARAMETERS);

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
		var title = document.getElementById("titleField");
		if(null==title.value || ''==title.value){
			alert('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "template-title-mandatory"))%>');
			return;
		}
		
		var addContainerLinks = window.parseInt(document.getElementById("countAddContainerLinks").value);
		var containersAdded = window.parseInt(document.getElementById("countContainersAdded").value);
		if(containersAdded==0){
			if(!confirm('Your template does not contains Containers. In this case you can\'t add contents. Are you sure you want to save?'))
				return;
		}else if(addContainerLinks>containersAdded){
			if(!confirm('Not all sections have an associated Container. This could cause problems in editing of the Html Page. Are you sure you want to save?'))
				return;
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
		
		addDrawedContainer(idDiv,container,value,'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "container-already-exists"))%>');
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
		addFileToTemplate(html,file,'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "file-already-exists"))%>');
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

<script src="/html/js/cms_ui_utils.js" type="text/javascript"></script>
<script src="/html/js/template/utility-left-menu.js" type="text/javascript"></script>
<script src="/html/js/template/utility-add-metadata.js" type="text/javascript"></script>
<script src="/html/js/codemirror/js/codemirror.js" type="text/javascript"></script>

<script type="text/javascript">
	dojo.addOnLoad(function() {
		drawDefault(<%=overrideBody%>,'<%= LanguageUtil.get(pageContext, "Add-Container") %>','<%= LanguageUtil.get(pageContext, "Remove-Container") %>');
		setTimeout('codeMirrorArea()',1);
	});
	
	var editor;
	function codeMirrorArea(){
		editor = CodeMirror.fromTextArea("headerField", {
		    width: "95%",
		    height:"100%",
			parserfile: ["parsedummy.js","parsexml.js", "parsecss.js", "tokenizejavascript.js", "parsejavascript.js", "parsehtmlmixed.js"],
			stylesheet: ["/html/js/codemirror/css/xmlcolors.css", "/html/js/codemirror/css/jscolors.css", "/html/js/codemirror/css/csscolors.css"],
			path: "/html/js/codemirror/js/"
		});
	} 
	
	function codeMirrorColoration(){
		dijit.byId("toggleEditor").disabled=true;
		if (dijit.byId("toggleEditor").checked) {
			codeMirrorArea();
		} else {
			var editorText = editor.getCode();
			if (dojo.isIE) {
    			var node = dojo.query('.CodeMirror-wrapping')[0];
    			node.parentNode.removeChild(node);
			} else {
				dojo.query('.CodeMirror-wrapping')[0].remove();
			}
			dojo.query('#headerField').style({display:''})
			dojo.query('#headerField')[0].value = editorText;
		}
		dijit.byId("toggleEditor").disabled=false;
	}	
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
	<input name="countAddContainer" type="hidden" id="countAddContainerLinks" value="<%=template.getCountAddContainer()!=null?template.getCountAddContainer():"0"%>"/>
	<input name="countContainers" type="hidden" id="countContainersAdded" value="<%=template.getCountContainers()!=null?template.getCountContainers():"0"%>"/>
		
<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" style="height: 100%; min-height: 881px;" >
	<div id="properties" dojoType="dijit.layout.ContentPane" style="padding:0;height: 100%; min-height:851px;" title="<%= LanguageUtil.get(pageContext, "Properties") %>">
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
	<div id="template" dojoType="dijit.layout.ContentPane" style="padding:0; height: 100%; min-height: 851px;" title="<%= LanguageUtil.get(pageContext, "draw-template") %>" >	
		<div class="wrapperRight" style="position:relative;" id="containerBodyTemplate">
			<div id="addFileToTemplate">
				<button dojoType="dijit.form.Button" onClick="addFile()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-js-css")) %>
				</button>
				<!--
					NEXT FEATURE: add meta tag to head HTML and add Head code.
					
					TO DO..
					 
				<button id="buttonOne" data-dojo-type="dijit.form.Button" type="button" onClick="showAddMetatagDialog()">
					<%=LanguageUtil.get(pageContext, "add-meta-tag")%>
				</button>
				 -->							
			</div>
			<div class="clear"></div>
			<div id="bodyTemplate"></div>
		</div>
	</div>

</div>		
<div class="buttonRow-left lineRight" id="editContentletButtonRow" style="height: 100%; min-height: 617px;">
<%
	if(null!=parameters) { // retrieve the parameters for auto-populate the fields
%>
	<div class="gradient2">
		<div class="fieldWrapperSide">
			<div class="leftProperties">		
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "page-width") %>:</dt>						
					<dd><select id="pageWidth" dojoType="dijit.form.FilteringSelect" name="pageWidth" onchange="javascript: addPageWidth(this.value)">
							<option value="doc-template"  <%if(parameters.getPageWidth().equals("doc-template")) { %>selected="selected"<%}%>>750px</option>
							<option value="doc2-template" <%if(parameters.getPageWidth().equals("doc2-template")) { %>selected="selected"<%}%>>950px</option>
							<option value="doc3-template" <%if(parameters.getPageWidth().equals("doc3-template")) { %>selected="selected"<%}%>>100%</option>
							<option value="doc4-template" <%if(parameters.getPageWidth().equals("doc4-template")) { %>selected="selected"<%}%>>974px</option>
						</select>
					</dd>
				</dl>
			</div>	
		</div>
		<div class="clear"></div>	
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "Layout") %>:</dt>						
					<dd><select id="layout" dojoType="dijit.form.FilteringSelect" name="layout" onchange="javascript: addLayout(this.value)">
							<option value="none" <%if(parameters.getLayout().equals("none")) { %>selected="selected"<%}%>></option>
							<option value="yui-t1-template" <%if(parameters.getLayout().equals("yui-t1-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-160-left") %></option>
							<option value="yui-t2-template" <%if(parameters.getLayout().equals("yui-t2-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-180-left") %></option>
							<option value="yui-t3-template" <%if(parameters.getLayout().equals("yui-t3-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-300-left") %></option>
							<option value="yui-t4-template" <%if(parameters.getLayout().equals("yui-t4-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-180-right") %></option>
							<option value="yui-t5-template" <%if(parameters.getLayout().equals("yui-t5-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-240-right") %></option>
							<option value="yui-t6-template" <%if(parameters.getLayout().equals("yui-t6-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "layout-300-right") %></option>
						</select>
					</dd>
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "Header") %>:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_header" onclick="javascript: addHeader(this.checked)" <%if(parameters.isHeader()) { %>checked="checked"<%}%>/>
					</dd>
				</dl>	
				<div class="clear"></div>
			    <dl>
					<dt><%=LanguageUtil.get(pageContext, "Footer") %>:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_footer" onclick="javascript: addFooter(this.checked)" <%if(parameters.isFooter()) { %>checked="checked"<%}%>/>
					</dd>	
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="gradient title"><%=LanguageUtil.get(pageContext, "body-rows") %></div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt id="tableContainer">
						<table id="splitBodyTable" cellspacing="4" cellpadding="2">
							<%
								for(SplitBody sb : parameters.getBodyRows()){
							%>	
						        <tr id="_selectRow<%=sb.getIdentifier()%>" class="spaceUnder">
							        <td style="width: 16px;">
							        	<% if(sb.getIdentifier()>0) { %>
							        		<img src="/html/images/icons/cross.png" alt="delete" title="delete row" style="cursor: pointer;" onclick="javascript: deleteRow('splitBodyTable','<%=sb.getId()+sb.getIdentifier()%>','splitBody<%=sb.getIdentifier()%>',<%=sb.getIdentifier()%>)"/>
							        	<% }else{ %>
							        		&nbsp;&nbsp;
							        	<% } %></td>
							        <td>
										<select name="<%=sb.getId()+sb.getIdentifier()%>" dojoType="dijit.form.FilteringSelect" onchange="javascript: addGrid(this.value, 'splitBody<%=sb.getIdentifier()%>',<%=sb.getIdentifier()%>)">
											<option value="1" selected="selected"><%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %></option>
											<option value="yui-gc-template" <%if(sb.getValue().equals("yui-gc-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %></option>
											<option value="yui-gd-template" <%if(sb.getValue().equals("yui-gd-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %></option>
											<option value="yui-ge-template" <%if(sb.getValue().equals("yui-ge-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %></option>
											<option value="yui-gf-template" <%if(sb.getValue().equals("yui-gf-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %></option>
											<option value="yui-gb-template" <%if(sb.getValue().equals("yui-gb-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %></option>
										</select>				        			
							        </td>
								</tr>							
							<%
								}
							%>
						</table>					
					</dt>
				</dl>
				<div class="buttonRow" style="text-align: center">
					<button id="addRow" dojoType="dijit.form.Button" onClick="javascript: addRow('splitBodyTable','select_splitBody','splitBody')" iconClass="plusIcon" type="button">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-row")) %>
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
<%
	} else {
%>	
	<div class="gradient2">
		<div class="fieldWrapperSide">
			<div class="leftProperties">		
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "page-width") %>:</dt>						
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
					<dt><%=LanguageUtil.get(pageContext, "Layout") %>:</dt>						
					<dd><select id="layout" dojoType="dijit.form.FilteringSelect" name="layout" onchange="javascript: addLayout(this.value)">
							<option value="none" selected="selected"></option>
							<option value="yui-t1-template"><%= LanguageUtil.get(pageContext, "layout-160-left") %></option>
							<option value="yui-t2-template"><%= LanguageUtil.get(pageContext, "layout-180-left") %></option>
							<option value="yui-t3-template"><%= LanguageUtil.get(pageContext, "layout-300-left") %></option>
							<option value="yui-t4-template"><%= LanguageUtil.get(pageContext, "layout-180-right") %></option>
							<option value="yui-t5-template"><%= LanguageUtil.get(pageContext, "layout-240-right") %></option>
							<option value="yui-t6-template"><%= LanguageUtil.get(pageContext, "layout-300-right") %></option>
						</select>
					</dd>
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "Header") %>:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_header" onclick="javascript: addHeader(this.checked)" checked="checked"/>
					</dd>
				</dl>	
				<div class="clear"></div>
			    <dl>
					<dt><%=LanguageUtil.get(pageContext, "Footer") %>:</dt>						
					<dd>
						<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="_footer" onclick="javascript: addFooter(this.checked)" checked="checked"/>
					</dd>	
				</dl>
			</div>
		</div>	
		<div class="clear"></div>
		<div class="gradient title"><%=LanguageUtil.get(pageContext, "body-rows") %></div>
		<div class="fieldWrapperSide">	
			<div class="leftProperties">
				<dl>
					<dt id="tableContainer">
						<table id="splitBodyTable" cellspacing="4" cellpadding="2">
					        <tr id="_selectRow0" class="spaceUnder">
						        <td style="width: 16px;">&nbsp;&nbsp;</td>
						        <td>
									<select name="select_splitBody0" dojoType="dijit.form.FilteringSelect" onchange="javascript: addGrid(this.value, 'splitBody0',0)">
										<option value="1" selected="selected"><%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %></option>
										<option value="yui-gc-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %></option>
										<option value="yui-gd-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %></option>
										<option value="yui-ge-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %></option>
										<option value="yui-gf-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %></option>
										<option value="yui-gb-template"><%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %></option>
									</select>				        			
						        </td>
							</tr>	
						</table>					
					</dt>
				</dl>
				<div class="buttonRow" style="text-align: center">
					<button id="addRow" dojoType="dijit.form.Button" onClick="javascript: addRow('splitBodyTable','select_splitBody','splitBody')" iconClass="plusIcon" type="button">
						<%=LanguageUtil.get(pageContext, "add-row") %>
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
<%
	}
%>
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

<!-- ADD CONTAINER DIALOG BOX -->
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
<!-- /ADD CONTAINER DIALOG BOX -->

<!-- ADD METADATA DIALOG BOX -->
<div id="dialogOne" dojoType="dijit.Dialog" title="<%=LanguageUtil.get(pageContext, "add-meta-tag")%>" style="width: 800px; height: 600px; padding: 0pt;">
    <div dojoType="dijit.layout.TabContainer" style="min-height: 500px; padding: 0pt;">
        <div dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "metadata-tab")%>" style="width: auto; padding: 0pt;">
   			<p><%=LanguageUtil.get(pageContext, "metadata-tab-description")%></p>
   			<dl>
   				<dt><%=LanguageUtil.get(pageContext, "Choose-Attribute")%></dt>
   				<dd>
   					<select id="choose-attribute" dojoType="dijit.form.FilteringSelect" name="choose-attribute" onchange="javascript: showSelectedAttribute(this.value)">
						<option value="-1" selected="selected"></option>
						<option value="0"><%= LanguageUtil.get(pageContext, "Name-Attribute") %></option>
						<option value="1"><%= LanguageUtil.get(pageContext, "Http-Equiv-Attribute") %></option>
					</select>
   				</dd>
   				<div id="name-div" style="display: none;">
	   				<dt><%=LanguageUtil.get(pageContext, "Name-Attribute")%></dt>
	   				<dd>
	   					<select id="name-attribute" dojoType="dijit.form.FilteringSelect" name="name-attribute">
							<option value="none" selected="selected"></option>
							<option value="<%= LanguageUtil.get(pageContext, "name-author") %>"><%= LanguageUtil.get(pageContext, "name-author") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "name-description") %>"><%= LanguageUtil.get(pageContext, "name-description") %></option>						
							<option value="<%= LanguageUtil.get(pageContext, "name-keywords") %>"><%= LanguageUtil.get(pageContext, "name-keywords") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "name-generator") %>"><%= LanguageUtil.get(pageContext, "name-generator") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "name-revised") %>"><%= LanguageUtil.get(pageContext, "name-revised") %></option>
						</select>
					</dd>
				</div>
				<div id="http-equiv-div" style="display: none;">
	   				<dt><%=LanguageUtil.get(pageContext, "Http-Equiv-Attribute")%></dt>
	   				<dd>
	   					<select id="http-equiv-attribute" dojoType="dijit.form.FilteringSelect" name="http-equiv-attribute">
							<option value="none" selected="selected"></option>
							<option value="<%= LanguageUtil.get(pageContext, "http-equiv-content-type") %>"><%= LanguageUtil.get(pageContext, "http-equiv-content-type") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "http-equiv-content-style-type") %>"><%= LanguageUtil.get(pageContext, "http-equiv-content-style-type") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "http-equiv-expires") %>"><%= LanguageUtil.get(pageContext, "http-equiv-expires") %></option>
							<option value="<%= LanguageUtil.get(pageContext, "http-equiv-set-cookie") %>"><%= LanguageUtil.get(pageContext, "http-equiv-set-cookie") %></option>
						</select>
					</dd>
				</div>				
   				<dt><%=LanguageUtil.get(pageContext, "Content-Attribute")%></dt>
   				<dd>
   					<input type="text" dojoType="dijit.form.TextBox" style="width:300px" name="content-attribute" id="content-attribute" value="" />
				</dd>   				   			
   			</dl>
   			<hr />
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" onclick="addMetatag()" type="button" iconClass="plusIcon"><%=LanguageUtil.get(pageContext, "add-meta")%></button>
			</div>
			<hr />
			<table class="listingTable" id="metadataTable">
				<tr>
					<th nowrap style="width:5%; text-align:center;">
						<%= LanguageUtil.get(pageContext, "Action") %>
					</th>				
					<th nowrap style="width:10%;text-align:center;">
						<%= LanguageUtil.get(pageContext, "Attribute") %>
					</th>
					<th nowrap style="width:15%;text-align:center;">
						<%= LanguageUtil.get(pageContext, "Attribute-Value") %>
					</th>					
					<th nowrap style="width:20%;text-align:center;">
						<%= LanguageUtil.get(pageContext, "Content") %>
					</th>
					<th nowrap style="width:50%;text-align:center;">
						<%= LanguageUtil.get(pageContext, "Generated-HTML") %>
					</th>					
				</th>	
			</table>
			<div class="clear"></div>
			<br />
        </div>
        <div dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "header-code-tab")%>" style="width: auto;">
			<div id="textEditorArea" style="border: 0px;  width: auto; height: 80%;">
				<textarea onkeydown="return catchTab(this,event)" style="width: 95%; height: 100%; font-size: 12px" id="headerField"></textarea>
			</div>
			<br />
		    <input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor" id="toggleEditor"  onClick="codeMirrorColoration();"  checked="checked"  />
		    <label for="toggleEditor"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label> 
        </div>
    </div>
    <div class="buttonRow">
		<button dojoType="dijit.form.Button" onclick="saveMetaAndHeaderCode()" type="button"><%=LanguageUtil.get(pageContext, "Add")%></button> 
		<button dojoType="dijit.form.Button" onclick="dijit.byId('dialogOne').hide()" type="button"><%=LanguageUtil.get(pageContext, "Cancel")%></button>
	</div>    
</div>
<!-- /ADD METADATA DIALOG BOX -->


