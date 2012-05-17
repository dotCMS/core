<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="javax.portlet.WindowState"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.util.Constants"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@ page import="com.dotmarketing.portlets.structure.struts.StructureForm"%>
<%@ page import="com.dotmarketing.util.Config"%>
<%@ page import="com.dotmarketing.util.InodeUtils"%>
<%@ include file="/html/portlet/ext/structure/init.jsp"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.portlets.widget.business.WidgetAPI"%>
<%@page import="com.dotmarketing.portlets.structure.business.FieldAPI"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.util.HostUtils" %>


<%@page import="com.dotcms.enterprise.LicenseUtil"%><script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/FieldVariableAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>
<%
	FieldAPI fAPI = APILocator.getFieldAPI();
    com.dotmarketing.portlets.contentlet.business.HostAPI hostAPI = APILocator.getHostAPI();
	Structure structure = (Structure) request.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE);
	java.util.Map params = new java.util.HashMap();
    params.put("struts_action",
		new String[] { "/ext/formhandler/view_form" });
    String formReferer = com.dotmarketing.util.PortletURLUtil.getActionURL(
			request, WindowState.MAXIMIZED.toString(), params);
    params = new java.util.HashMap();
	params.put("struts_action",
			new String[] { "/ext/structure/edit_structure" });
	params.put("inode", new String[] {structure
			.getInode() });
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(
			request, WindowState.MAXIMIZED.toString(), params);

	params = new java.util.HashMap();
	params.put("struts_action",
			new String[] { "" });
	String viewStructures = com.dotmarketing.util.PortletURLUtil.getRenderURL(
			request, WindowState.MAXIMIZED.toString(), params);

	boolean hasWritePermissions = false;
	PermissionAPI strPerAPI = APILocator.getPermissionAPI();
	if (strPerAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_PUBLISH, user)) {
		hasWritePermissions = true;
    }

	List<WorkflowScheme> wfSchemes=new ArrayList<WorkflowScheme>();

	if(LicenseUtil.getLevel() > 100){

		wfSchemes = APILocator.getWorkflowAPI().findSchemes(false);
	}
	else{
		wfSchemes.add(APILocator.getWorkflowAPI().findDefaultScheme());
	}


	WorkflowScheme wfScheme = APILocator.getWorkflowAPI().findSchemeForStruct(structure);


	List<Role> roles = APILocator.getRoleAPI().findAllAssignableRoles(false);
	request.setAttribute ("roles", roles);

    StructureForm form = (StructureForm)request.getAttribute("StructureForm");
    int structureType = 1;
    try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE) != null)
			structureType = (Integer)session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE);
		if (request.getParameter("structureType") != null)
			structureType = Integer.parseInt(request.getParameter("structureType"));
	} catch (NumberFormatException e) {
	}
	if(!UtilMethods.isSet(form.getInode())){
		form.setStructureType(structureType);
	}

%>


<script language="javascript">

	dojo.require('dotcms.dijit.form.FileSelector');
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require("dojo.dnd.Container");
	dojo.require("dojo.dnd.Manager");
	dojo.require("dojo.dnd.Source");

	var structureInode = '<%=structure.getInode()%>';
	<%-- This is the javascript Array that controls what is shown or hidden --%>
	<%@ include file="/html/portlet/ext/structure/field_type_js_array.jsp" %>


	function writeLabel(fieldType){
		fieldType = fieldType.toLowerCase();
		for(i=0;i<myData.items.length;i++){
			if(fieldType == myData.items[i].id){
				return myData.items[i].label;
			}
		}
		return fieldType;
	}



	function isInteger(campo,A){
		if(validateDots(campo))
		{
			campo = removeDots(campo);
			var regular = eval ("/^-?\\d{0," + A + "}$/");
			//alert (regular)
			var resultado = regular.exec(campo);
			//alert(resultado+(resultado==null));
			if (resultado == null)
				return false;
			else
				return true;
		}
		else return false;
	}

	function addNewStructure()
	{

		var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
		href = href + "<portlet:param name='cmd' value='add' />";
		<%if(structure.getStructureType() == 3){%>
		   href = href + "<portlet:param name='referer' value='<%=formReferer%>' />";
		<%}else{%>
		   href = href + "<portlet:param name='referer' value='<%=referer%>' />";
		<%}%>
		href = href + "</portlet:actionURL>";

		document.forms[0].action = href;
		document.forms[0].submit();
	}

	function addNewField(event)
	{

		var href= "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_field' />";
		href = href + "<portlet:param name='referer' value='<%=referer%>' />";
		href = href + "<portlet:param name='structureInode' value='<%=structure.getInode()%>' />";
		href = href + "</portlet:actionURL>";
		document.location.href = href;
		dojo.stopEvent(event);

	}


	function reorderFields(){
		var inodes = dojo.query(".hiddenInodeField");
		var orders = dojo.query(".orderBox");
		var list = "";
		for(i = 0; i<inodes.length; i++){
			var mynode = dojo.trim(inodes[i].innerHTML );
			if(list.indexOf("," + mynode + " @ ") <0){ //DOTCMS-3797
				list = list + "," + mynode + " @ " + orders[i].value;
			}
		}
		StructureAjax.reorderfields(structureInode, list, showReOrderAlert);
	}
	
	function showReOrderAlert(data){
		showDotCMSSystemMessage(data);
	}

	function cancel(event)
	{
		var structureType = dijit.byId("structureType");
		if(structureType == null || structureType == 'undefined'){
			structureType = document.getElementById("structureType").value;
		}
		if(structureType == <%= Structure.STRUCTURE_TYPE_FORM %>){
			var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
			href = href + "<portlet:param name='struts_action' value='/ext/formhandler/view_form' />";
			href = href + "</portlet:actionURL>";
			document.location = href;
			dojo.stopEvent(event);
		}else{
			var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
			href = href + "<portlet:param name='struts_action' value='/ext/structure/view_structure' />";
			href = href + "</portlet:actionURL>";
			document.location = href;
			dojo.stopEvent(event);
		}

	}

	function deleteStructure() {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.structure.delete.structure.and.content")) %>')) {
	  	  StructureAjax.checkDependencies(structureInode, handleDepResponse);
	  	}
	}

	function handleDepResponse(data) {
		if(data!=null) {
			dojo.byId("depDiv").innerHTML = "<br />" + data;
			dijit.byId("dependenciesDialog").show();
		} else {
			processDelete();
		}
	}

	function processDelete() {
		var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
		href = href + "<portlet:param name='referer' value='<%=viewStructures%>' />";
		href = href + "<portlet:param name='cmd' value='<%=Constants.DELETE%>' />";
		href = href + "</portlet:actionURL>";
		href = href + "&inode=" + structureInode;
		document.location.href = href;
	}


	function reviewChange(disregard) {
		var enable = dojo.byId("reviewContent").checked;
		var resetReviewBtn = dijit.byId("resetReviewsButtonId");

		dijit.byId("reviewIntervalNumId").attr('disabled', !enable);
		dijit.byId("reviewIntervalSelectId").attr('disabled', !enable);
		dijit.byId("reviewerRoleId").attr('disabled', !enable);
		if(resetReviewBtn != undefined){
			resetReviewBtn.attr('disabled',!enable);
		}
	}

	function resetReviews() {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.structure.reset.intervals")) %>')) {
	      var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	  		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
	  	 	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	  	  	href = href + "<portlet:param name='cmd' value='<%=com.dotmarketing.util.Constants.RESET%>' />";
	  	  	href = href + "<portlet:param name='inode' value='<%=String.valueOf(structure.getInode())%>' />";
	  		href = href + "</portlet:actionURL>";
	  		document.location.href = href;
		}
	}

	function deleteField(fieldInode) {

	  if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.structure.delete.fields")) %>')){
	      var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	      href = href + "<portlet:param name='struts_action' value='/ext/structure/deleteField' />";
	      href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	      href = href + "<portlet:param name='cmd' value='<%=Constants.DELETE%>' />";
	      href = href + "</portlet:actionURL>";
	      href = href + "&inode=" + fieldInode;
	      document.location.href = href;
	   }

	}

	function editField(fieldInode) {

	     var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	     href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_field' />";
	     href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	     href = href + "</portlet:actionURL>";
	     href = href + "&inode=" + fieldInode;
	     href = href + "&structureInode=<%=structure.getInode()%>";
	     document.location.href = href;

	}

	//called on load to resize the divs to ~100% width
	function resizeTableAndAddIcons(){
		var viewport = dijit.getViewport();
		var myWidth = (viewport.w - 915) +"px";
		var labels = dojo.query(".structureFieldLabelClass");
		for(i = 0; i<labels.length; i++){
			dojo.style(labels[i], "width", myWidth);
		}

		var icons = dojo.query(".fieldTypeCell");
		for(i = 0; i<icons.length; i++){
			var fieldType = dojo.trim(icons[i].innerHTML.toLowerCase() );
			for(j=0;j<myData.items.length;j++){
				if(fieldType == myData.items[j].id){
					icons[i].innerHTML = myData.items[j].label;
					break;
				}
			}
		}


	}

	function renumberRecolorAndReorder(){
		recolorRows();
		eles = dojo.query(".orderBox");
		for(i = 0;i<eles.length;i++){
			eles[i].value=i+1;
		}
		reorderFields();
	}

	function recolorRows() {
		var eles = dojo.query(".dojoDndItem");
		for(i = 0;i<eles.length;i++){
			if(i % 2 ==0){
				dojo.style(eles[i], "background", "#fff");
			}
			else{
				dojo.style(eles[i], "background", "#eee");
			}
		}
		var eles = dojo.query(".dojoDndItem .item_cell");
		var bg = "#eee";
		for(i = 0;i<eles.length;i++){
			if(i % 8 == 0)
				bg = bg == "#fff"?"#eee":"#fff";
			dojo.style(eles[i], "background", bg);
		}
	}

	function initDND(){

		var ele = dojo.byId("dragNDropBox");
	    // example subscribe to events
	    dojo.subscribe("/dnd/drop", function(source){
	      renumberRecolorAndReorder();
	    });

	    recolorRows();

	 };
	function hideEditButtonsRow() {
		dojo.style('editStructureButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editStructureButtonRow', { display: '' });
		changesMadeToPermissions = false
	}

function changeStructureType(){
		var val = <%=form.getStructureType()%>;
		var ele;
		try{
		  ele = dijit.byId("structureType");
		}catch(ex){
			ele =  document.getElementById("structureType");
		}
		if(ele){
			val = ele.value;
		}
		dojo.style("formBuilderHelpDiv", { display: 'none' });
		dojo.style("widgetHelpDiv", { display: 'none' });
		dojo.style("reviewDiv", { display: 'none' });
		dojo.style("detailPageDiv", { display: 'none' });

		if(val==1){
			dojo.style("reviewDiv", { display: '' });
			dojo.style("detailPageDiv", { display: '' });
			<%if (!InodeUtils.isSet(structure.getInode()) || hasWritePermissions) {%>
			 toggleSaveButton(false);
			<%}%>
		}
		else if(val==2){
			dojo.style("widgetHelpDiv", { display: '' });
			<%if (!InodeUtils.isSet(structure.getInode()) || hasWritePermissions) {%>
			  toggleSaveButton(false);
			<%}%>
		}
		else if(val==3){
			dojo.style("formBuilderHelpDiv", { display: '' });
			<%if (InodeUtils.isSet(structure.getInode()) && !hasWritePermissions) {%>
			   disableFormFields();
			<%}%>
		}
		else if(val==4) {
			dojo.style("detailPageDiv", { display: '' });
		}

}

function updateHostFolderValues(){
	  if(!isInodeSet(dijit.byId('HostSelector').attr('value'))
			  && dijit.byId('HostSelector').attr('value')!='SYSTEM_HOST'
			  && dijit.byId('HostSelector').attr('value')!='SYSTEM_FOLDER'){
		 dojo.byId('hostFolder').value = "";
		 dojo.byId('host').value = "";
		 dojo.byId('folder').value = "";
		 toggleSaveButton(true);
	  }else{
		 var data = dijit.byId('HostSelector').attr('selectedItem');
		 if(data["type"]== "host"){
			dojo.byId('hostFolder').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('host').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folder').value = "";
		 }else if(data["type"]== "folder"){
			dojo.byId('hostFolder').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folder').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('host').value = "";
		}
		 <%if(!InodeUtils.isSet(structure.getInode()) || hasWritePermissions) {%>
	         toggleSaveButton(false);
	     <%}%>
	  }
	}

function toggleSaveButton(disabled){

	 var saveButton;
	    try{
		    saveButton = document.getElementById('saveButton');
	    	dijit.byId('saveButton').attr('disabled', disabled);
	    }catch(e){
		   if(saveButton){
	          saveButton.disabled = disabled;
		   }
	    }

}

function disableFormFields(){

	var structureType = dijit.byId("structureType");
	if(structureType == null || structureType == 'undefined'){
		structureType = document.getElementById("structureType").value;
	}
		var button;
		   try{
			   button = document.getElementById('field');
		       dijit.byId('field').attr('disabled', true);
		    }catch(e){
			   if(button){
				   button.disabled = true;
			   }
		    }
		toggleSaveButton(true);
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Structures")) %>' />


<html:form action="/ext/structure/edit_structure" method="post" styleId="structure">
<input type="hidden"  name="inode" value="<%=structure.getInode()%>"  />
<html:hidden property="fixed" />
<html:hidden property="content" />
<% if(InodeUtils.isSet(structure.getInode())){  %>
	<html:hidden property="structureType" />
	<%session.setAttribute("selectedStructure",structure.getInode() ); %>
<%}%>


<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- START Properties Tab -->
<div id="TabOne" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>" style="overflow:hidden;" onShow="showEditButtonsRow()">


	<div class="yui-g">

	<!-- START First Colum -->
		<div class="yui-u first">
			<dl>
				<dt><%= LanguageUtil.get(pageContext, "Type") %>:</dt>
				<dd>
					<% boolean typeDisabled = (UtilMethods.isSet(structure.getInode()) && InodeUtils.isSet(structure.getInode())) || (structureType== Structure.STRUCTURE_TYPE_FORM)? true : false;%>

						<%if(typeDisabled){%>
							<input type="hidden"  name="structureType" id="structureType" value="<%= form.getStructureType()  %>">
							<%if(form.getStructureType() ==1){%>
								<%= LanguageUtil.get(pageContext, "Content") %>
							<%}else if( form.getStructureType() ==2){%>
								<%= LanguageUtil.get(pageContext, "Widget") %>
							<%}else if(form.getStructureType() ==3){%>
								<%= LanguageUtil.get(pageContext, "Form") %>
							<%}else if(form.getStructureType() ==4){%>
								<%= LanguageUtil.get(pageContext, "File") %>
							<%} %>
						<%}else{ %>
							<select onchange="changeStructureType()" dojoType="dijit.form.FilteringSelect" name="structureType" id="structureType" style="width:150px" value="<%= form.getStructureType()  %>" >
								<option value="<%= String.valueOf(Structure.STRUCTURE_TYPE_CONTENT) %>"><%= LanguageUtil.get(pageContext, "Content") %></option>
								<option value="<%= String.valueOf(Structure.STRUCTURE_TYPE_WIDGET) %>"><%= LanguageUtil.get(pageContext, "Widget") %></option>
								<option value="<%= String.valueOf(Structure.STRUCTURE_TYPE_FILEASSET) %>"><%= LanguageUtil.get(pageContext, "File") %></option>
							</select>
						<%} %>
					<html:hidden property="system" styleId="system" />
				</dd>

				<dt><%= LanguageUtil.get(pageContext, "Name") %>:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" name="name" style="width:250px" <%if(structure.isFixed()){%> readonly="readonly"  <%} %>value="<%= UtilMethods.isSet(form.getName()) ? form.getName() : "" %>" /></dd>
				<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" name="description" style="width:250px" value="<%= UtilMethods.isSet(form.getDescription()) ? form.getDescription() : "" %>" /></dd>

				<% if(UtilMethods.isSet(structure.getInode())) { %>
					<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
					<dd><%= structure.getInode() %></dd>
					<dt>
						<span id="VariableIdTitle">
						<%= LanguageUtil.get(pageContext, "Variable-ID") %>:
						</span>
					</dt>
					<dd style="clear: none;">
				    <input type="text" value="<%= structure.getVelocityVarName() %>" readonly="readonly" style="width:250px;border:1px;" />
				    </dd>

				<% } %>
				<%
				String host = structure.getHost() != null?structure.getHost():"";
		    	String folder = structure.getFolder()!= null?structure.getFolder():"";

		    	if(!UtilMethods.isSet(structure.getInode())){
		    		String defaultHostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		    		if(structureType == Structure.STRUCTURE_TYPE_FORM){
		    			host = HostUtils.filterDefaultHostForSelect(defaultHostId, "PARENT:"+PermissionAPI.PERMISSION_CAN_ADD_CHILDREN+", STRUCTURES:"+ PermissionAPI.PERMISSION_PUBLISH, user);
		    		}else{
		    			host = HostUtils.filterDefaultHostForSelect(defaultHostId, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user);

		    		}
		    	}

		    	String selectorValue = UtilMethods.isSet(folder) && !folder.equals(com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER)?folder:host;
				%>
				<dt><%= LanguageUtil.get(pageContext, "Host-Folder") %>:</dt>
				<dd>
				<% if(structureType== Structure.STRUCTURE_TYPE_FORM){%>
				 <div id="HostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" requiredPermissions="PARENT:<%=PermissionAPI.PERMISSION_CAN_ADD_CHILDREN%>, STRUCTURES:<%= PermissionAPI.PERMISSION_PUBLISH %>" onChange="updateHostFolderValues();" value="<%= selectorValue %>"></div>
                    <input type="hidden" name="hostFolder" id="hostFolder" value="<%= selectorValue %>"/>
                    <input type="hidden" name="host" id="host" value="<%=host%>"/>
                    <input type="hidden" name="folder" id="folder" value="<%=folder%>"/>
                 <%}else{ %>
                 <div id="HostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" requiredPermissions="PARENT:<%=PermissionAPI.PERMISSION_CAN_ADD_CHILDREN%>" onChange="updateHostFolderValues();" value="<%= selectorValue %>"></div>
                    <input type="hidden" name="hostFolder" id="hostFolder" value="<%= selectorValue %>"/>
                    <input type="hidden" name="host" id="host" value="<%=host%>"/>
                    <input type="hidden" name="folder" id="folder" value="<%=folder%>"/>
                 <%} %>
				</dd>
				<dt><%= LanguageUtil.get(pageContext, "Workflow-Scheme") %>:</dt>
				<dd>
				<%	if(LicenseUtil.getLevel() > 100){ %>
					<select name="workflowScheme" id="workflowScheme" dojoType="dijit.form.FilteringSelect" value="<%=wfScheme.getId()%>">
						<%for(WorkflowScheme scheme : wfSchemes){ %>
							<option value="<%=scheme.getId()%>"><%=scheme.getName() %></option>
						<%} %>
					</select>
				<%}else{ %>
					<input type="hidden" name="workflowScheme" value="<%=APILocator.getWorkflowAPI().findDefaultScheme().getId()%>"><%=LanguageUtil.get(pageContext, "Only-Default-Scheme-is-available-in-Community") %>
				<%} %>
				</dd>


			</dl>
		</div>

	<!-- End First Column -->

	<!-- Start Second Column -->
		<div class="yui-u" id="secondColumnDiv">

		<div class="callOutBox2" id="formBuilderHelpDiv" style="width:400px;display:none;text-align:left;">
			<h3><%= LanguageUtil.get(pageContext, "getting-started-forms") %></h3>
			<span style="font-size:88%;">
				<%= LanguageUtil.get(pageContext, "getting-started-forms-help") %>
			</span>
		</div>
		<div class="callOutBox2" id="widgetHelpDiv" style="width:400px;display:none;text-align:left;">
			<h3><%= LanguageUtil.get(pageContext, "getting-started-widgets") %></h3>
			<span style="font-size:88%;">
				<%= LanguageUtil.get(pageContext, "getting-started-widgets-help") %>
			</span>


		</div>
			<dl class="secondColumn">
				<div id="reviewDiv" style="display:none">
					<dt><%= LanguageUtil.get(pageContext, "Enable-Review") %>:</dt>
					<dd>
						<input type="checkbox" dojoType="dijit.form.CheckBox" name="reviewContent" id="reviewContent" value="true" <%if(form.isReviewContent()){ %>checked="checked"<%}%> onclick="reviewChange(true);">
						&nbsp;&nbsp;
						<% if(InodeUtils.isSet(structure.getInode())){ %>
							<button dojoType="dijit.form.Button" <%if(!form.isReviewContent()){ %>disabled='true'<%}%> name="resetReviewsButton" id="resetReviewsButtonId" onclick="resetReviews()" iconClass="resetIcon" type="button">
								<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset-Review-in-All-Contents")) %>
							</button>
						<%} %>
					</dd>
					<dt style="font-weight:normal;"><%= LanguageUtil.get(pageContext, "Review-Every") %>:</dt>
					<dd>
			 			<select dojoType="dijit.form.FilteringSelect" name="reviewIntervalNum" id="reviewIntervalNumId" style="width: 65px" value="<%= UtilMethods.isSet(form.getReviewIntervalNum()) ? form.getReviewIntervalNum() : "" %>" >
							<%for (int i = 1; i < 32; ++i) {%>
								<option value="<%= i %>"><%= i %></option>
							<%}%>
						</select>
						<select dojoType="dijit.form.FilteringSelect" name="reviewIntervalSelect" id="reviewIntervalSelectId" style="width: 100px" value="<%= UtilMethods.isSet(form.getReviewIntervalSelect()) ? form.getReviewIntervalSelect() : "" %>" >
							<option value="d"><%= LanguageUtil.get(pageContext, "Day") %></option>
							<option value="m"><%= LanguageUtil.get(pageContext, "Month") %></option>
							<option value="y"><%= LanguageUtil.get(pageContext, "Year") %></option>
						</select>
					</dd>
					<dd>
						<select dojoType="dijit.form.FilteringSelect" name="reviewerRole" id="reviewerRoleId" value="<%= UtilMethods.isSet(form.getReviewerRole()) ? form.getReviewerRole() : "" %>" >/
							<option value="0"><%=LanguageUtil.get(pageContext, "Select-a-Role")%></option>
							<%for (Role role: roles) {%>
								<option value="<%= role.getId() %>"><%= role.getName() %></option>
							<%}%>
				        </select>
					</dd>
				</div>
				<div id="detailPageDiv" style="display:none">
					<dt><%= LanguageUtil.get(pageContext, "Detail-Page") %>:</dt>
					<dd>
						<input type="text" name="detailPage" dojoType="dotcms.dijit.form.FileSelector" allowFileUpload="false" fileBrowserView="details"
							mimeTypes="application/dotpage" value="<%= UtilMethods.isSet(form.getDetailPage())?form.getDetailPage():"" %>" showThumbnail="false" style="width: 450px;" />
					</dd>
					<dt><%= LanguageUtil.get(pageContext, "URL-Map-Pattern") %>:</dt>
					<dd>
						<input type="text" dojoType="dijit.form.TextBox" name="urlMapPattern" id="urlMapPattern" style="width:250px" value="<%= UtilMethods.isSet(form.getUrlMapPattern()) ? form.getUrlMapPattern() : "" %>" />
						<span id="multiHintHook" style="cursor:pointer;">?</span>
	                    <span dojoType="dijit.Tooltip" connectId="multiHintHook" id="multiHint" class="fieldHint">
	                   		<%= LanguageUtil.get(pageContext, "URL-Map-Pattern-hint1") %>
	                   	</span>
					</dd>
				</div>
			</dl>
		</div>
	<!-- END Second Column -->

	</div>
<div class="clear"></div>


<!-- START Button Row -->
<div class="buttonRow" id="editStructureButtonRow">
	<%if(InodeUtils.isSet(structure.getInode())){ // >0%>
		<% if (hasWritePermissions && !structure.isFixed()) { %>
			<button dojoType="dijit.form.Button" id="delete" onClick="deleteStructure('<%=structure.getInode()%>');" iconClass="deleteIcon" type="button">
				<%if(structure.getStructureType() == 3 ){%>
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Form-and-Entries")) %>
				<%}else{ %>
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Structure-and-Content")) %>
				<%} %>
			</button>
		<%}%>
	<%} %>
	<button dojoType="dijit.form.Button" id="saveButton" onClick="addNewStructure();" iconClass="saveIcon" type="button">

				<%if(structure.getStructureType() == 3 ){%>
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Form")) %>
				<%}else{ %>
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
				<%} %>
	</button>&nbsp;&nbsp;
	<button dojoType="dijit.form.Button" onClick="cancel" iconClass="cancelIcon" type="button">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
	</button>
</div>
<!-- END Button Row -->



	<% if(InodeUtils.isSet(structure.getInode())){ // >0%>

	<hr/>

		<div class="buttonRow" style="width:97%;">
			<div class="yui-g">
				<% if(structure.getFields().size() > 1){%>
				<div class="yui-u first" style="text-align:left">
					<%= LanguageUtil.get(pageContext, "Drag-and-drop-the-items-to-the-desired-position-order") %>
				</div>
				<%} %>
				<div class="yui-u" style="text-align:right">
					<%if(InodeUtils.isSet(structure.getInode())){// >0%>
						<button dojoType="dijit.form.Button" id="field" onClick="addNewField" iconClass="plusIcon" type="button">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Field")) %>
						</button>
					<%}%>
				</div>
			</div>
		</div>


<!-- START Listing Table -->

	<div id="fieldsTable" style="width:97%;">

		<!-- START Table Header -->
		<div id="fieldsTableHeader">
			<div id="fieldsTableHeaderCell00" class="item_header_cell" style="width:60px;"><% if (hasWritePermissions) { %><b><%= LanguageUtil.get(pageContext, "Action") %></b><% } %></div>
			<div id="fieldsTableHeaderCell01" class="item_header_cell structureFieldLabelClass" style="width:150px;"><b><%= LanguageUtil.get(pageContext, "Label") %></b></div>
			<div id="fieldsTableHeaderCell02" class="item_header_cell" style="width:100px;"><b><%= LanguageUtil.get(pageContext, "Display") %></b></div>
			<div id="fieldsTableHeaderCell03" class="item_header_cell" style="width:120px;"><b><%= LanguageUtil.get(pageContext, "Variable") %></b></div>
			<div id="fieldsTableHeaderCell04" class="item_header_cell" style="width:100px;"><b><%= LanguageUtil.get(pageContext, "Index-Name") %></b></div>
			<div id="fieldsTableHeaderCell05" class="item_header_cell" style="width:70px;"><b><%= LanguageUtil.get(pageContext, "Required") %></b></div>
			<div id="fieldsTableHeaderCell06" class="item_header_cell" style="width:70px;"><b><%= LanguageUtil.get(pageContext, "Indexed") %></b></div>
			<div id="fieldsTableHeaderCell07" class="item_header_cell" style="width:80px;white-space:nowrap;"><b><%= LanguageUtil.get(pageContext, "Show-in-List") %></b></div>
			<div style="clear:both;"></div>
		</div>
		<!-- END Table Header -->

		<!-- START Table Results -->
		<div dojoType="dojo.dnd.Source" jsId="dragNDropBox" class="dndContainer">

		<% List fields = structure.getFields();
			if (fields.size() > 0) {
			for (int i = 0; i < fields.size(); i++) {
				Field field = (Field) fields.get(i);
				String color = (i % 2 == 0 ? "#FFFFFF" : "#EEEEEE");
				String inode = field.getInode();  %>

				<% if(!field.getFieldType().equals(Field.FieldType.HIDDEN.toString()) || fAPI.isElementConstant(field)){ %>
					<div class="dojoDndItem" style="white-space:no-wrap;" id="<%=field.getInode() %>">
						<span class="hiddenInodeField" style="display:none; width: 0px;"><%=field.getInode()%></span>
						<input class="orderBox" type="hidden" value="<%=i+1%>">

						<div class="item_cell" style="width:60px;">
			            	<% if (hasWritePermissions) { %>
								<a href="javascript:editField('<%=inode%>');"><span class="editIcon"></span></a>
								<% if (!field.isFixed()) { %>
				            		<a href="javascript:deleteField('<%=inode%>');"><span class="deleteIcon"></span></a>
			            		<% }//end of if (!field.isFixed()) %>
			            	<% }//end of if (hasWritePermissions) %>
						</div>
						<div class="item_cell structureFieldLabelClass" style="width:150px;">
							<%=field.getFieldName()%>
						</div>
						<div class="item_cell fieldTypeCell" style="width:100px;">
							<%=field.getFieldType()%>
						</div>
						<div class="item_cell" style="width:120px;">
							<%=(field.getVelocityVarName()!=null) ? field.getVelocityVarName() : "&nbsp;"%>
						</div>
						<div class="item_cell" style="width:100px;">
							<%=(field.getFieldContentlet()!=null) ? field.getFieldContentlet() : "&nbsp;"%>
						</div>
						<div class="item_cell" style="width:70px;">
							<%=(field.isRequired()) ? LanguageUtil.get(pageContext, "Yes"): LanguageUtil.get(pageContext, "")%>
						</div>
						<div class="item_cell" style="width:70px;">
							<%=(field.isIndexed()) ? LanguageUtil.get(pageContext, "Yes"): LanguageUtil.get(pageContext, "")%>
						</div>
						<div class="item_cell" style="width:100px;">
							<%=(field.isListed()) ? LanguageUtil.get(pageContext, "Yes"):LanguageUtil.get(pageContext, "")%>
						</div>
						<div style="clear:both;"></div>
					</div>
					<script type="text/javascript">
		            dojo.addOnLoad(function() {
		                var pMenu = new dijit.Menu({
		                    targetNodeIds: ["<%=inode%>"]
		                });
		                pMenu.addChild(new dijit.MenuItem({
		                    label: "<%= LanguageUtil.get(pageContext, "Edit-Field-Variables") %>",
		                    onClick: function() {
		                    	editFieldVariables("<%=inode%>");
		                    }
		                }));
		                pMenu.startup();
		            });
					</script>
				<%} %>
			<%}//end for %>
		<% } else { //else of if (fields.size() > 0)%>
	  		<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-fields-to-display") %></div>
		<%}//end if (fields.size() > 0)%>
		</div>
		<!-- END Table Results -->

	</div>
<!-- END Listing Table -->

<%} %>


	</div>
<!-- END Property Tab -->


<!-- START Permission Tab -->
<%
	boolean canEditAsset = strPerAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="TabTwo" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, structure);
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, null);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
	</div>
<%
	}
%>
<!-- END Permission Tab -->

</div>


<div class="clear"></div>



</html:form>
</liferay:box>
<%@ include file="/html/portlet/ext/structure/view_field_variables_inc.jsp" %>
<script language="javascript">
<%if (!InodeUtils.isSet(structure.getInode())) {
	if(InodeUtils.isSet(structure.getInode())){%>
	var button;
	   try{
		   button = document.getElementById('field');
	       dijit.byId('field').attr('disabled', true);
	    }catch(e){
		   if(button){
			   button.disabled = true;
		   }
	    }
  <%}
}%>

    dojo.addOnLoad(	function () {reviewChange(true);});
	dojo.addOnLoad(resizeTableAndAddIcons);
	dojo.addOnLoad(initDND);
	dojo.addOnLoad(updateHostFolderValues);
	dojo.addOnLoad(changeStructureType);

	<%if(InodeUtils.isSet(structure.getInode()) && !hasWritePermissions) {%>
	    dojo.addOnLoad(disableFormFields);
	<%}%>


	<% if(InodeUtils.isSet(structure.getInode()) && InodeUtils.isSet(structure.getInode())){ %>
		var stType = document.getElementById("structureType");
		stType.disabled = true;
	<%}%>
	// DOTCMS-6298
	function editFieldVariables(fieldId){
		fieldVariablesAdmin.showFieldVariables(fieldId,true);
	}
</script>

<div id="dependenciesDialog" dojoType="dijit.Dialog" style="display:none;width:630px;height:300px;vertical-align: middle; " draggable="true"
	title="<%= LanguageUtil.get(pageContext, "message.structure.cantdelete") %>" >

	<span style="color: red; font-weight: bold"><%= LanguageUtil.get(pageContext, "message.structure.notdeletestructure.container") %></span>

	<div id="depDiv" style="overflow: auto; height: 220px"></div>
</div>