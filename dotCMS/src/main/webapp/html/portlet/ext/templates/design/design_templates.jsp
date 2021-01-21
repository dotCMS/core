<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@ page import="com.dotmarketing.util.Config"%>
<%@ page import="com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow"%>
<%@ page import="com.dotmarketing.portlets.templates.design.bean.TemplateLayout"%>
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
<%@ page import="com.dotmarketing.portlets.templates.business.TemplateAPI" %>
<%@ page import="com.dotmarketing.portlets.containers.model.Container"%>

<%@ page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@ page import="com.dotcms.rendering.velocity.viewtools.DotTemplateTool" %>
<%@ include file="/html/js/template/utility-left-menu.jsp" %>
<style type="text/css">
	@import url(/html/css/template/draw-template.css);
	@import url(/html/css/template/drawed-reset-fonts-grids.css);
	.gradient2{padding-top:30px;padding-bottom:20px;}
	.hidden { visibility: hidden; display: none;}
</style>

<%
	boolean enablePreview = null!=Config.getStringProperty(com.dotmarketing.util.WebKeys.PREVIEW_TEMPLATE_DESIGN_ENABLE)?Boolean.parseBoolean(Config.getStringProperty(com.dotmarketing.util.WebKeys.PREVIEW_TEMPLATE_DESIGN_ENABLE)):false;

	boolean overrideBody = (Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.OVERRIDE_DRAWED_TEMPLATE_BODY);

	TemplateLayout parameters = (TemplateLayout)request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_JAVASCRIPT_PARAMETERS);

	PermissionAPI perAPI = APILocator.getPermissionAPI();
	ContainerAPI containerAPI = APILocator.getContainerAPI();
	TemplateAPI templateAPI = APILocator.getTemplateAPI();
	HostAPI hostAPI = APILocator.getHostAPI();

	com.dotmarketing.portlets.templates.model.Template template;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT)!=null) {
		template = (com.dotmarketing.portlets.templates.model.Template) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT);
	}
	else {
		template = APILocator.getTemplateAPI().find(request.getParameter("inode"),user,false);
	}
	List<Container> containersList = null;
	StringBuilder containersStr = new StringBuilder();
	int containerSize = 0 ;
	
	if(template != null){
		 containersList = templateAPI.getContainersInTemplate(template, user, true);
		 if(containersList != null && containersList.size() > 0){
			 containerSize = containersList.size();
			for (int i = 0; i < containersList.size(); i++) {
				    Container container = (Container)containersList.get(i);
				    containersStr.append(container.getTitle()+",");
				}
		 }
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
<script type='text/javascript' src='/dwr/interface/TemplateAjax.js'></script>
<script language="JavaScript" src="/html/js/template/dwr/interface/ContainerAjaxDrawedTemplate.js"></script>
<script language="Javascript">

	dojo.require('dotcms.dijit.form.FileSelector');
	dojo.require('dotcms.dojo.data.ContainerReadStoreDrawedTemplate');
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");

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

		var theme = dijit.byId("themeDiv");

		if(null==theme.value || ''==theme.value){
			alert('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "template-theme-mandatory"))%>');
			return;
		}

        //Update the counter of the "Add Container" links
        updateAddContainerLinksCount();
        //Update the counter of added containers
        parseCurrentContainers();

		var addContainerLinks = window.parseInt(document.getElementById("countAddContainerLinks").value);
		var containersAdded = window.parseInt(document.getElementById("countContainersAdded").value);

		if(containersAdded==0){
			if(!confirm('Your template does not contains Containers. In this case you can\'t add contents. Are you sure you want to save?'))
				return;
		}else if(addContainerLinks>containersAdded){
			if(!confirm('Not all sections have an associated Container. This could cause problems in editing of the Html Page. Are you sure you want to save?'))
				return;
		}

		if(null!=theme.value && ''!=theme.value){
			dojo.byId("theme").value = dijit.byId("themeDiv").value;
			dojo.byId("themeName").value = dijit.byId("themeDiv").displayedValue;
		}

		saveBody();
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD_DESIGN%>';
		form.<portlet:namespace />subcmd.value = subcmd;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>';
		form.removeAttribute('onsubmit');

        //Before to submit lets verify the title
        verifyTitle(form);
	}

    function verifyTitle(form) {

        function response(data) {
            if (data) {
                alert("The template title must be unique and this template title is already taken.");
            } else {
                submitForm(form);
            }
        }

        TemplateAjax.duplicatedTitle(document.getElementById("titleField").value, "<%=template.getInode()%>", "<%=host.getIdentifier()%>", {callback:response});
    }

	var copyAsset = false;

	function cancelEdit() {
		self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
	}

	function submitfmDelete() {
		if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.delete.template"))%>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
		}
	}

	function addHeadCodeDialog(){
		headerCode.show();
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

		addDrawedContainer(idDiv,container,value,'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "container-already-exists"))%>','<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "container-with-contents-already-exists"))%>');
	}

	function addFile() {
		fileSelector.show();
	}

	function previewTemplate(name, params) {

		var theme = dijit.byId("themeDiv");

		if(null==theme.value || ''==theme.value){
			alert('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "template-theme-mandatory"))%>');
			return;
		}

		var url = '/servlets/template/design/preview';
		openWindowWithPost(url, name, params);
	}

	var previewWindow;
	function openWindowWithPost(url, name, params) {
		if (previewWindow) {
			previewWindow.close();

		}

		previewWindow = window.open(url, "previewWindow", params);
		if (!previewWindow) return false;
		var bodyTemplateHTML = document.getElementById('bodyTemplate').innerHTML;
		var theme = dijit.byId("themeDiv").value;
		var themeName = dijit.byId("themeDiv").displayedValue;
		var headerCheck = dijit.byId("headerCheck").checked;
		var footerCheck = dijit.byId("footerCheck").checked;
		var hostId = dojo.byId("hostId").value;

		var html = "";
		html += '<html><head></head><body><form id="previewForm" name="previewForm" method="post" action="' + url + '">';
		html += '<input type="hidden" name="theme" value="'+theme+'">';
		html += '<input type="hidden" name="themeName" value="'+themeName+'">';
		html += '<input type="hidden" name="headerCheck" value="'+headerCheck+'">';
		html += '<input type="hidden" name="footerCheck" value="'+footerCheck+'">';
		html += '<input type="hidden" name="hostId" value="'+hostId+'">';
		html += '<textarea style="display: none;" name="bodyTemplateHTML">'+bodyTemplateHTML+'</textarea></form><script>document.previewForm.submit();<' + '/script' + '>' + '</body></html>';
		previewWindow.document.write(html);
		return previewWindow;
	}


    function refreshPreviewTab() {

        var bodyTemplateHTML = document.getElementById('bodyTemplate').innerHTML;
        var theme = dijit.byId("themeDiv").value;
        var themeName = dijit.byId("themeDiv").displayedValue;

        var headerCheck = dijit.byId("headerCheck").checked;
        var footerCheck = dijit.byId("footerCheck").checked;
        var hostId = dojo.byId("hostId").value;

        if (null == theme || '' == theme) {
            alert('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "template-theme-mandatory"))%>');
            return;
        }

        dojo.byId("themePreviewRandom").value = Math.floor(Math.random() * 1123213213);
        dojo.byId("themePreviewTheme").value = theme;
        dojo.byId("themePreviewName").value = themeName;
        dojo.byId("templateTitle").value = document.getElementById("titleField").value;
        dojo.byId("themePreviewHeaderCheck").value = headerCheck;
        dojo.byId("themePreviewFooterCheck").value = footerCheck;
        dojo.byId("themePreviewBodyTemplateHTML").value = bodyTemplateHTML;
        dojo.byId("themePreviewHostId").value = hostId;

        dojo.byId("themePreviewForm").submit();
    }

	dojo.addOnLoad (function(){
		var tab =dijit.byId("mainTabContainer");
	   	dojo.connect(tab, 'selectChild',
			function (evt) {
			 	selectedTab = tab.selectedChildWidget;
				  	if(selectedTab.id =="previewThemeTab"){
				  		refreshPreviewTab();
				  	}
			});

	});



	function addFileCallback(file) {
		if(file.extension == 'js') {
			var html = '<script type="text/javascript" src="' + file.path + file.fileName + '" >' + '<' + '/script' + '>';
		} else if (file.extension == 'css') {
			var html = '<link href="' + file.path + file.fileName + '" rel="stylesheet" type="text/css" />';
		}
		addFileToTemplate(html,file,'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "file-already-exists"))%>');
	}

	function selectTemplateVersion(objId,referer) {
		if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.replace.template"))%>')){
			window.location = '<portlet:actionURL windowState="<%=WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer;
		}
	}

    function deleteVersion(objId){
        if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.delete.version"))%>')){
			window.location = '<portlet:actionURL windowState="<%=WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId  + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.replace.version"))%>')){
			window.location = '<portlet:actionURL windowState="<%=WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId  + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%=WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=edit&inode=' + objId  + '&referer=' + referer;
	}

	function getContainerMockContent(title){
		return "<h2>Container: "+title+"</h2><p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>";
	}

</script>

<script src="/html/js/cms_ui_utils.js" type="text/javascript"></script>
<script src="/html/js/template/utility-add-head-code.js" type="text/javascript"></script>

<%
	final StringBuffer maxUUIDStr = new StringBuffer("{");
	final Map<String, Long> maxUUID = DotTemplateTool.getMaxUUID(template);

	for (Map.Entry<String, Long> maxUUIDEntry : maxUUID.entrySet()) {
		maxUUIDStr.append("'" + maxUUIDEntry.getKey() + "'");
		maxUUIDStr.append(":");
		maxUUIDStr.append(maxUUIDEntry.getValue());
		maxUUIDStr.append(",");
	}

	maxUUIDStr.append("}");
%>
<script type="text/javascript">
	dojo.addOnLoad(function() {
		drawDefault(
				<%=overrideBody%>,
				'<%=LanguageUtil.get(pageContext, "Add-Container")%>',
				'<%=LanguageUtil.get(pageContext, "Remove-Container")%>',
				'<%=containersStr.toString()%>',
				<%=containerSize%>,
				<%=maxUUIDStr.toString()%>);
		//setTimeout('codeMirrorArea()',1);
		//dojo.byId("titleField").focus(true);
	});

/*	var editor;
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
	}*/
</script>


<html:form action='/ext/templates/edit_template' styleId="fm">
<input name="<portlet:namespace /><%=Constants.CMD%>" type="hidden" value="add">
<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
<input name="<portlet:namespace />inode" type="hidden" value="<%=template.getInode()%>">
<input name="<portlet:namespace />subcmd" type="hidden" value="">
<input name="userId" type="hidden" value="<%=user.getUserId()%>">
<input name="admin_l_list" type="hidden" value="">
<input name="countAddContainer" type="hidden" id="countAddContainerLinks" value="<%=template.getCountAddContainer()!=null?template.getCountAddContainer():"0"%>"/>
<input name="countContainers" type="hidden" id="countContainersAdded" value="<%=template.getCountContainers()!=null?template.getCountContainers():"0"%>"/>


<div class="portlet-main">
	
	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" style="height: 100%; min-height: 881px;" >
		<div id="template" dojoType="dijit.layout.ContentPane" style="padding:0; height: 100%; min-height: 851px;" title="<%=LanguageUtil.get(pageContext, "draw-template")%>" >
	
			<%-- Start Template Controls --%>
	
			<div class="portlet-sidebar-wrapper design-template__sidebar-wrapper" id="editContentletButtonRow">
				<%
					if(null!=parameters) { // retrieve the parameters for auto-populate the fields
				%>
					<div class="portlet-sidebar">

						<div class="fieldWrapperSide">
							<dl class="vertical">
								<dt><%=LanguageUtil.get(pageContext, "Theme")%>:</dt>
								<dd><div id="themeDiv" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" style="vertical-align:middle;" themesOnly=true value="<%=UtilMethods.webifyString(template.getTheme())%>" displayedValue="<%=UtilMethods.webifyString(template.getThemeName())%>"></div>
								</dd>
							</dl>
						</div>
		
						<input type="hidden" name="theme" id="theme">
						<input type="hidden" name="themeName" id="themeName">
		
						<div class="clear"></div>
		
						<div class="hidden">
								<dl class="vertical">
									<dt><%=LanguageUtil.get(pageContext, "width") %>:</dt>
									<dd>
										<select id="pageWidth" dojoType="dijit.form.FilteringSelect" name="pageWidth" onchange="javascript: addPageWidth(this.value)">
											<option value="resp-template" <%if(parameters.getPageWidth().equals("resp-template")) {%>selected="selected"<%}%>>Responsive</option>
											<option value="doc4-template" <%if(parameters.getPageWidth().equals("doc4-template")) {%>selected="selected"<%}%>>975px</option>
											<option value="doc3-template" <%if(parameters.getPageWidth().equals("doc3-template")) {%>selected="selected"<%}%>>100%</option>
										</select>
									</dd>
								</dl>
						</div>
		
						<div class="fieldWrapperSide">
								<dl class="vertical">
									<dt><%=LanguageUtil.get(pageContext, "sidebar")%>:</dt>
									<dd>
										<select id="layout" dojoType="dijit.form.FilteringSelect" name="layout" onchange="javascript: if(this.value!=''){addLayout(this.value)}">
											<option value="none" <%if(parameters.getLayout().equals("none")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "none")%></option>
											<option value="yui-t1-template" <%if(parameters.getLayout().equals("yui-t1-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-20-left")%></option>
											<option value="yui-t2-template" <%if(parameters.getLayout().equals("yui-t2-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-30-left")%></option>
											<option value="yui-t3-template" <%if(parameters.getLayout().equals("yui-t3-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-40-left")%></option>
											<option value="yui-t4-template" <%if(parameters.getLayout().equals("yui-t4-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-20-right")%></option>
											<option value="yui-t5-template" <%if(parameters.getLayout().equals("yui-t5-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-30-right")%></option>
											<option value="yui-t6-template" <%if(parameters.getLayout().equals("yui-t6-template")) {%>selected="selected"<%}%>><%=LanguageUtil.get(pageContext, "layout-40-right")%></option>
										</select>
									</dd>
								</dl>
						</div>
						<div class="clear"></div>
						<div class="checkbox">
							<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="headerCheck" id="headerCheck" onclick="javascript: addHeader(this.checked)" <%if(parameters.isHeader()) {%>checked="checked"<%}%>/>
							<label for="">
								<%=LanguageUtil.get(pageContext, "Header")%>
							</label>
						</div>
						<div class="clear"></div>
						<div class="checkbox">
							<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="footerCheck" id="footerCheck" onclick="javascript: addFooter(this.checked)" <%if(parameters.isFooter()) {%>checked="checked"<%}%>/>
							<label for="">
								<%=LanguageUtil.get(pageContext, "Footer")%>
							</label>
						</div>
						<div class="clear"></div>
		
						<div class="gradient title"><%=LanguageUtil.get(pageContext, "body-rows")%></div>
						<div class="fieldWrapperSide">
								<dl class="vertical">
									<dt id="tableContainer">
										<table id="splitBodyTable" cellspacing="4" cellpadding="2">
											<%
												for(TemplateLayoutRow sb : parameters.getBody().getRows()){
											%>
												<tr id="_selectRow<%=sb.getIdentifier()%>" class="spaceUnder">
													<td style="width: 16px;">
														<% if(sb.getIdentifier()>0) { %>
															<img src="/html/images/icons/cross.png" alt="delete" title="delete row" style="cursor: pointer;" onclick="javascript: deleteRow('splitBodyTable','<%=sb.getId()+sb.getIdentifier()%>','splitBody<%=sb.getIdentifier()%>',<%=sb.getIdentifier()%>)"/>
														<% }else{ %>
															&nbsp;&nbsp;
														<% } %></td>
													<td>
														<select name="<%=sb.getId()+sb.getIdentifier()%>" dojoType="dijit.form.FilteringSelect" onchange="javascript: if(this.value!=''){addGrid(this.value, 'splitBody<%=sb.getIdentifier()%>',<%=sb.getIdentifier()%>)}">
															<option value="1" <%if(sb.getValue().equals("")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %></option>
															<option value="yui-g-template" <%if(sb.getValue().equals("yui-g-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-5050") %></option>
															<option value="yui-gc-template" <%if(sb.getValue().equals("yui-gc-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %></option>
															<option value="yui-gd-template" <%if(sb.getValue().equals("yui-gd-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %></option>
															<option value="yui-ge-template" <%if(sb.getValue().equals("yui-ge-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %></option>
															<option value="yui-gf-template" <%if(sb.getValue().equals("yui-gf-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %></option>
															<option value="yui-gb-template" <%if(sb.getValue().equals("yui-gb-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %></option>
															<option value="yui-js-template" <%if(sb.getValue().equals("yui-js-template")) { %>selected="selected"<%}%>><%= LanguageUtil.get(pageContext, "body-rows-4-column-25252525") %></option>
														</select>
													</td>
												</tr>
											<%
												}
											%>
										</table>
									</dt>
								</dl>
								<dl class="vertical">
									<dd>
										<div class="buttonRow" style="text-align: center">
											<button id="addRow" dojoType="dijit.form.Button" onClick="javascript: addRow('splitBodyTable','select_splitBody','splitBody')" iconClass="plusIcon" type="button">
												<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-row")) %>
											</button>
										</div>
									</dd>
								</dl>
							<div class="clear"></div>
						</div>
		
		
						<!--<div class="gradient title"><%=LanguageUtil.get(pageContext, "Actions") %></div>-->
						<div class="content-edit-actions">
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
				<%
					} else {
				%>
					<div class="portlet-sidebar">
						<div class="fieldWrapperSide">
							<dl class="vertical">
								<dt><%= LanguageUtil.get(pageContext, "Theme") %>:</dt>
								<dd><div id="themeDiv" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" style="vertical-align:middle;" themesOnly=true></div>
								</dd>
							</dl>
						</div>
						<input type="hidden" name="theme" id="theme">
						<input type="hidden" name="themeName" id="themeName">
		
						<div class="clear"></div>
		
						<div class="hidden">
							<div class="fieldWrapperSide">
								<dl class="vertical">
									<dt><%=LanguageUtil.get(pageContext, "width") %>:</dt>
									<dd>
										<select id="pageWidth" dojoType="dijit.form.FilteringSelect" name="pageWidth" onchange="javascript: addPageWidth(this.value)">
											<%--<option value="doc-template">750px</option>--%>
											<%--<option value="doc2-template">950px</option>--%>
											<option value="resp-template" selected="selected">Responsive</option>
											<option value="doc4-template">975px</option>
											<option value="doc3-template">100%</option>
										</select>
									</dd>
								</dl>
							</div>
						</div>
		
						<div class="clear"></div>
		
						<div class="fieldWrapperSide">
							<dl class="vertical">
								<dt><%=LanguageUtil.get(pageContext, "sidebar") %>:</dt>
								<dd>
									<select id="layout" dojoType="dijit.form.FilteringSelect" name="layout" onchange="javascript: if(this.value!=''){addLayout(this.value)}">
										<option value="none" selected="selected"><%=LanguageUtil.get(pageContext, "none")%></option>
										<option value="yui-t1-template"><%= LanguageUtil.get(pageContext, "layout-20-left") %></option>
										<option value="yui-t2-template"><%= LanguageUtil.get(pageContext, "layout-30-left") %></option>
										<option value="yui-t3-template"><%= LanguageUtil.get(pageContext, "layout-40-left") %></option>
										<option value="yui-t4-template"><%= LanguageUtil.get(pageContext, "layout-20-right") %></option>
										<option value="yui-t5-template"><%= LanguageUtil.get(pageContext, "layout-30-right") %></option>
										<option value="yui-t6-template"><%= LanguageUtil.get(pageContext, "layout-40-right") %></option>
									</select>
								</dd>
							</dl>
						</div>
		
						<div class="clear"></div>

						<div class="checkbox">
							<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="headerCheck" id="headerCheck" onclick="javascript: addHeader(this.checked)" checked="checked"/>
							<label for=""><%=LanguageUtil.get(pageContext, "Header") %></label>
						</div>
						<div class="clear"></div>
						<div class="checkbox">
							<input style="float: left; margin-right: 10px" type="checkbox" dojoType="dijit.form.CheckBox" name="footerCheck" id="footerCheck" onclick="javascript: addFooter(this.checked)" checked="checked"/>
							<label for=""><%=LanguageUtil.get(pageContext, "Footer") %></label>
						</div>
		
		
						<div class="gradient title"><%=LanguageUtil.get(pageContext, "body-rows") %></div>
						<div class="fieldWrapperSide">
								<dl class="vertical">
									<dt id="tableContainer">
										<table id="splitBodyTable" cellspacing="4" cellpadding="2">
											<tr id="_selectRow0" class="spaceUnder">
												<td style="width: 16px;">&nbsp;&nbsp;</td>
												<td>
													<select name="select_splitBody0" dojoType="dijit.form.FilteringSelect" onchange="javascript: if(this.value!=''){addGrid(this.value, 'splitBody0',0)}">
														<option value="1" selected="selected"><%= LanguageUtil.get(pageContext, "body-rows-1-column-100") %></option>
														<option value="yui-g-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-5050") %></option>
														<option value="yui-gc-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-6633") %></option>
														<option value="yui-gd-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-3366") %></option>
														<option value="yui-ge-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-7525") %></option>
														<option value="yui-gf-template"><%= LanguageUtil.get(pageContext, "body-rows-2-column-2575") %></option>
														<option value="yui-gb-template"><%= LanguageUtil.get(pageContext, "body-rows-3-column-333333") %></option>
														<option value="yui-js-template"><%= LanguageUtil.get(pageContext, "body-rows-4-column-25252525") %></option>
		
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
							<div class="clear"></div>
						</div>
		
						<div class="content-edit-actions">
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
				<%
					}
				%>
				<div class="clear"></div>
			</div>
			<div class="clear"></div>
	
			<%-- End Template Controls --%>
	
	
	
			<div class="wrapperRight" style="position:relative;border:0px solid red" id="containerBodyTemplate">
			<div style="float:left;margin:10px;">
	
				<input tabindex="1" data-dojo-type="dijit/form/TextBox" name="title" id="titleField"
	                   placeHolder="Template Title"
	                   required="true"
	                   maxlength="255" style="color:black;font-size:120%;border:1px solid #eee;min-width:450px;"
	                   value="<%= UtilMethods.webifyString(template.getTitle())%>"><br>
				<%--<span class="caption" style="font-style: italic;font-size:87%;padding-left:10px;"><%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Title"))%></span>--%>
	
			</div>
	
				<div class="clear"></div>
				<div id="bodyTemplate"></div>
			</div>
		</div>
	
	
		<div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>">
			<div class="form-horizontal">
				<dl>
					<%if(id!=null) {%>
						<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
						<dd><%= id.getId() %></dd>
					<%}%>
				</dl>
				<% if(host != null) { %>
				<dl>
					<html:hidden property="hostId" value="<%=hostId%>" styleId="hostId"/>
					<dt><%= LanguageUtil.get(pageContext, "Host") %>:</dt>
					<dd><%= host.getHostname() %></dd>
				</dl>
				<%	} else { %>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Host") %>:</dt>
					<dd>
						<select id="hostId" name="hostId" dojoType="dijit.form.FilteringSelect" value="<%=hostId%>">
						<% for(Host h: listHosts) { %>
							<option value="<%=h.getIdentifier()%>"><%=host.getHostname()%></option>
						<% } %>1
						</select>
					</dd>
				</dl>
				<% } %>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
					<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px" name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(template.getFriendlyName()) ? template.getFriendlyName() : "" %>" /></dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Screen-Capture-Image") %>:</dt>
					<dd>
						<input type="text" name="image" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" mimeTypes="image"
							value="<%= UtilMethods.isSet(form.getImage())?form.getImage():"" %>" showThumbnail="true" />
					</dd>
				</dl>
				<dl>
					<dd>
		    			<html:textarea style="display: none" property="body" styleId="bodyField"></html:textarea>
					</dd>
					<dd>
		    			<html:textarea style="display: none" property="drawedBody" styleId="drawedBodyField"></html:textarea>
					</dd>
				</dl>
			</div>
		</div>
	
	    <div id="previewThemeTab"
	         style="top:40px;left:10px;right:10px;bottom:0;"
	         dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Preview") %>">
	
	        <div class="yui-g portlet-toolbar">
	            <div class="yui-u" style="text-align:right;">
	
	                <div dojoType="dijit.form.DropDownButton" data-dojo-props="iconClass:'mobileIcon', showLabel:true">
	                    <span><%=UtilMethods.escapeSingleQuotes( LanguageUtil.get( pageContext, "Mobile Preview" ) )%></span>
	
	                    <div dojoType="dijit.Menu">
	
	                        <div dojoType="dijit.MenuItem" onClick="previewTemplate('Preview','width=340,height=480')">
	                            <span class="appleIcon"></span> <%= LanguageUtil.get( pageContext, "iPhone" ) %>
	                        </div>
	
	                        <div dojoType="dijit.MenuItem" onClick="previewTemplate('Preview','width=1024,height=768')">
	                            <span class="appleIcon"></span> <%= LanguageUtil.get( pageContext, "iPad" ) %>
	                        </div>
	
	                        <div dojoType="dijit.MenuItem" onClick="previewTemplate('Preview','width=640,height=920')">
	                            <span class="appleIcon"></span> <%= LanguageUtil.get( pageContext, "iPhone (Retina)" ) %>
	                        </div>
	
	                        <div dojoType="dijit.MenuItem" onClick="previewTemplate('Preview','width=2048,height=1536')">
	                            <span class="appleIcon"></span> <%= LanguageUtil.get( pageContext, "iPad (Retina)" ) %>
	                        </div>
	
	                        <div dojoType="dijit.MenuItem" onClick="previewTemplate('Preview','width=460,height=640')">
	                            <span class="androidIcon"></span> <%= LanguageUtil.get( pageContext, "Android (Moto-Droid)" ) %>
	                        </div>
	
	                    </div>
	                </div>
	
	            </div>
	        </div>
	
	        <iframe id="previewThemeIFrame"
	                name="previewThemeIFrame"
	                style="width:99%; min-height:766px; height:90%; border:1px solid black;"
	                scrolling='auto'
	                frameborder='1'>
	        </iframe>
	
	    </div>
	
	
		<!-- Permissions Tab -->
		<%
			boolean canEditAsset = perAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
			if (canEditAsset) {
		%>
			<div id="filePermissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" >
				<%
					request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, template);
				%>
				<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
			</div>
		<%
			}
		%>
		<!-- /Permissions Tab  -->
	
		<!-- Versions Tab -->
			<%if(template != null && InodeUtils.isSet(template.getInode())){ %>
				<% request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, template); %>
				<div id="fileVersionTab" class="history" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "History") %>" >
					<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>
				</div>
			<%} %>
		<!-- /Versions Tab -->
	</div>

</div>

</html:form>
<form id="themePreviewForm" action="/servlets/template/design/preview" method="POST" target="previewThemeIFrame" name="themePreviewForm">
	<input type="hidden" name="theme" id="themePreviewTheme">
	<input type="hidden" name="themeName" id="themePreviewName">
	<input type="hidden" name="title" id="templateTitle">
	<input type="hidden" name="headerCheck" id="themePreviewHeaderCheck">
	<input type="hidden" name="footerCheck" id="themePreviewFooterCheck">
	<input type="hidden" name="hostId" id="themePreviewHostId">
	<input type="hidden" name="bodyTemplateHTML" id="themePreviewBodyTemplateHTML">
	<input type="hidden" name="themePreviewRandom" id="themePreviewRandom">

</form>




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