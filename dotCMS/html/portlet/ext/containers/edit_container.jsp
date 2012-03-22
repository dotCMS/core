<%@ page import="com.dotmarketing.portlets.user.factories.*" %>
<%@ page import="com.dotmarketing.portlets.user.model.*" %>
<%@ page import="com.dotmarketing.beans.Identifier" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.containers.struts.ContainerForm" %>
<%@ page import="com.dotmarketing.business.IdentifierFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.factories.InodeFactory" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.business.Role"%>

<%@ include file="/html/portlet/ext/containers/init.jsp" %>

<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<script src="/html/js/codemirror/js/codemirror.js" type="text/javascript"></script>
<%

	PermissionAPI perAPI = APILocator.getPermissionAPI();

	com.dotmarketing.portlets.containers.model.Container contentContainer;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_EDIT)!=null) {
		contentContainer = (com.dotmarketing.portlets.containers.model.Container) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_EDIT);
	}
	else {
		contentContainer = (com.dotmarketing.portlets.containers.model.Container) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"),com.dotmarketing.portlets.containers.model.Container.class);
	}
	//Permissions variables
	boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
	boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
	boolean canUserWriteToContainer = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(contentContainer,PermissionAPI.PERMISSION_WRITE,user);
	boolean canUserPublishContainer = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(contentContainer,PermissionAPI.PERMISSION_PUBLISH,user);
//http://jira.dotmarketing.net/browse/DOTCMS-1473 basically if teh user has portlet permissions they can add new templates and containers
    Identifier id=null;
	if(!InodeUtils.isSet(contentContainer.getInode())){
		canUserWriteToContainer = true;
		canUserPublishContainer = true;		
	}
	else {
		id = APILocator.getIdentifierAPI().find(contentContainer);
	}

	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = request.getParameter("referer");
	} else {
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action",new String[] {"/ext/containers/view_containers"});
		referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	}
	
	String cmd = request.getParameter(Constants.CMD);
		if( (cmd == null || !cmd.equals(Constants.UPDATE)) && referer != null ) { // Avoiding URL-encoding if reloading (updating) the view itself
			referer = UtilMethods.encodeURL(referer);
	}

	
	if (contentContainer.getSortContentletsBy()==null) {
		contentContainer.setSortContentletsBy("tree_order");
	}

	//gets permissions to check if the save and publish button should be displayed
	Role[] roles = (Role[])com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);

	//Getting user preferences
	String codeWidth = "650px";
	String codeHeight = "350px";
	String preLoopWidth = "650px";
	String preLoopHeight = "150px";
	String postLoopWidth = "650px";
	String postLoopHeight = "150px";

	//Getting structures info
	ContainerForm form = (ContainerForm)request.getAttribute("ContainerForm");
	Structure currentStructure = (Structure)InodeFactory.getInode(form.getStructureInode(), Structure.class);

	//Setting structures in the request to list them
	//Only contents are shown in the drop down menu
	//http://jira.dotmarketing.net/browse/DOTCMS-2065
	List<Structure> allStructures = StructureFactory.getStructures();
	List<Structure> structures = new ArrayList<Structure>();
	
	for(Structure st : allStructures){
		if(!st.isWidget()){
			structures.add(st);
		}
	}
	request.setAttribute("structures", structures);
	
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


<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script type="text/javascript" src="/html/js/edit_area/edit_area_full.js"></script>
<script language="JavaScript" src="/html/js/cms_ui_utils.js"></script>
<script type="text/javascript">
	<%@ include file="/html/portlet/ext/containers/edit_container_js_inc.jsp" %>
	dojo.addOnLoad(function () {
		setHeights();setWidths();
	});
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-container\") %>" />

<html:form action='/ext/containers/edit_container' styleId="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/containers/view_containers" /></portlet:renderURL>">
<input name="<portlet:namespace />subcmd" type="hidden" value="">
<input name="<portlet:namespace />inode" type="hidden" value="<%=contentContainer.getInode()%>">
<input name="userId" type="hidden" value="<%= user.getUserId() %>">

<!-- START TABS -->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
		
	<!-- START PROPERTIES TAB -->
		<div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "properties") %>" onShow="showEditButtonsRow()">
			<dl>
				<%if(id!=null){%>
					<dt><%= LanguageUtil.get(pageContext, "Identity") %>:&nbsp;</dt>
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
					<% } %>
					</select>
					</dd>
				<% } %>
				<dt>
					<span class="required"></span>
					<%= LanguageUtil.get(pageContext, "Title") %>:&nbsp;
				</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:300" name="title" id="titleField" value="<%= form.getTitle() %>" /></dd>
				
				<dt><%= LanguageUtil.get(pageContext, "Description") %>:&nbsp;</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:300" name="friendlyName" id="friendlyNameField" value="<%= form.getFriendlyName() %>" /></dd>
				
				<dt><%= LanguageUtil.get(pageContext, "Max-Contents") %>:&nbsp;</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:30" maxlength="2" name="maxContentlets" id="maxContentlets" onchange="showHideCode()" value="<%= form.getMaxContentlets() %>" /></dd>
			</dl>
	
			<dl id="structureControls">
				<dt><%= LanguageUtil.get(pageContext, "Content-Type") %>:&nbsp;</dt>
				<dd>
					<select dojoType="dijit.form.FilteringSelect" name="structureInode" id="structureSelect" onchange="structureChanged()" value="<%= form.getStructureInode() %>">
<%
					for (Structure structure: structures) {
%>
						<option value="<%= structure.getInode() %>"><%= structure.getName() %></option>
<%
					}
%>
					</select>
				</dd>
			</dl>
			
		

			<div id="preLoopDiv">
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Pre-Loop") %>:</dt>
					<dd>
						
						<div id="preLoopEditorArea" style="border: 0px;">
							<textarea onkeydown="return catchTab(this,event)" name="preLoopMask" id="preLoopMask"><%=UtilMethods.isSet(form.getPreLoop())?UtilMethods.escapeHTMLSpecialChars(form.getPreLoop()):"" %></textarea>
							<input type="hidden" id="preLoop" name="preLoop" value=""/>
						</div>
						
						<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditorPreLoop" id="toggleEditorPreLoop"  onClick="preLoopEditor=codeMirrorToggler(preLoopEditor, 'preLoopMask','<%=preLoopWidth%>', '<%=preLoopHeight%>' );" );"  checked="checked"  />
	        	        <label for="toggleEditorPreLoop"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label> 
					</dd>
				</dl>
			</div>
			<div id="codeDiv">
				<dl>
				</dl>
			</div>
			
			<div id="codeButtonDiv">
				<dl>
					<dt>
						<span class="required"></span>
						<%= LanguageUtil.get(pageContext, "Code") %>:
					</dt>
					<dd>
						<br/>
						<div id="codeEditorArea">							
							<textarea onkeydown="return catchTab(this,event)" name="codeMask" id="codeMask"><%=UtilMethods.isSet(form.getCode())?UtilMethods.escapeHTMLSpecialChars(form.getCode()):"" %></textarea>							
							<input type="hidden" name="code" id="code" value=""/>
						</div>
						<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditorCode" id="toggleEditorCode"  onClick="codeEditor=codeMirrorToggler(codeEditor, 'codeMask','<%=codeWidth%>', '<%=codeHeight%>' );"  checked="checked"  />
	        	        <label for="toggleEditorCode"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
					</dd>
					<dd class="buttonCaption">												
						<button dojoType="dijit.form.Button"  onClick="addVariable()" iconClass="plusIcon" type="button">
				        	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-variable")) %>
				    	</button>
					</dd>
				</dl>
			</div>
			
			<div id="postLoopDiv">
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Post-Loop") %>:</dt>
					<dd>
						<br/>
						<div id="postLoopEditorArea" style="border: 0px;">
							<textarea onkeydown="return catchTab(this,event)" name="postLoopMask" id="postLoopMask"><%=UtilMethods.isSet(form.getPostLoop())?UtilMethods.escapeHTMLSpecialChars(form.getPostLoop()):"" %></textarea>
							<input type="hidden" name="postLoop" id="postLoop" value="" />
						</div>
						<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditorPostLoop" id="toggleEditorPostLoop"  onClick="postLoopEditor=codeMirrorToggler(postLoopEditor, 'postLoopMask','<%=postLoopWidth%>', '<%=postLoopHeight%>' );" );"  checked="checked"  />
	        	    	<label for="toggleEditorPostLoop"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label> 
					</dd>
				</dl>
			</div>
			
			<div id="notesDiv">
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Notes") %>:</dt>
					<dd><textarea dojoType="dijit.form.Textarea" style="width:450px; min-height:150px" name="notes" id="notes"><%= UtilMethods.isSet(form.getNotes()) ? form.getNotes() : "" %></textarea></dd>
				</dl>
				<script type="text/javascript">
					dojo.connect(dijit.byId('notes'), 'onkeydown', function(e) { return catchTab(document.getElementById('notes'), e) });
				</script>
			</div>
			
		</div>
	<!-- END PROPERTIES TAB -->
	
	<!-- START PERMISSIONS TAB -->
<%
	boolean canEditAsset = perAPI.doesUserHavePermission(contentContainer, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
		<div id="permissions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "permissions") %>" onShow="hideEditButtonsRow()">
			<%
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentContainer);
			%>
			<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>	
		</div>
<%
	}
%>
	<!-- END PERMISSIONS TAB -->
				
	<!-- START Versions TAB -->
		<%if(contentContainer != null && InodeUtils.isSet(contentContainer.getInode())){ %>
			<div id="versions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>" onShow="showEditButtonsRow()">
				<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>
			</div>
		<% } %>
	<!-- END Versions TAB -->		
		
</div>
<!-- END TABS -->

<div class="clear"></div>

<!-- START buttons -->
<div class="buttonRow" id="editContainerButtonRow">

	<% if(!InodeUtils.isSet(contentContainer.getInode())|| contentContainer.isLive() || contentContainer.isWorking() ) { 
		if( canUserWriteToContainer ) {
		%>
			<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'')" iconClass="saveIcon" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
			</button>
		<% } %>
		<% if( canUserPublishContainer ) { %>
			<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'publish')" iconClass="publishIcon" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
			</button>
		<%  } %>
		<% } else { %>
			<button dojoType="dijit.form.Button" onClick="selectVersion('<%=contentContainer.getInode()%>')" iconClass="resetIcon" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
			</button>
		<% } %>
		<% if (InodeUtils.isSet(contentContainer.getInode()) && contentContainer.isDeleted()) {%>
			<button dojoType="dijit.form.Button" onClick="submitfmDelete()" iconClass="deleteIcon" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-container")) %>
			</button>
		<% } %>
		<button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>

</div>
<!-- END buttons -->
</html:form>

</liferay:box>

<div id="addVariableDialog" dojoType="dijit.Dialog">

</div>

<iframe id="userpreferences_iframe" style="display:none;" name="userpreferences_iframe" src=""></iframe>

<script>
	dojo.addOnLoad(function () {
		initContainerPage();
	});
</script>
