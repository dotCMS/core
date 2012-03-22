<%@page import="com.dotmarketing.beans.Host" %>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.files.business.FileAPI"%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.portlets.files.struts.FileForm"%>
<%@ include file="/html/portlet/ext/files/init.jsp" %>

<%@ include file="/html/portlet/ext/files/edit_text_inc.jsp" %>




<!---  When the file upload is in a popup -->
<%
	session.removeAttribute(com.dotmarketing.util.WebKeys.IMAGE_TOOL_SAVE_FILES);
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	FileAPI fileAPI = APILocator.getFileAPI();
	FolderAPI folderAPI = APILocator.getFolderAPI();
	int showDim = 300;
	if(request.getAttribute("fileInode") != null){
		String fileInode = (String) request.getAttribute("fileInode"); 
		String fileName = (String) request.getAttribute("fileName"); 
		File myFile = (File)InodeFactory.getInode(fileInode, File.class);
%>
	<script language="Javascript">
	
	
			try {
				opener.setImage('<%=myFile.getInode()%>','<%=myFile.getFileName()%>');
			} catch (e) { }
	<%if(request.getParameter("popup") != null) {%>
			try {
				opener.callback<%=request.getParameter("popup")%>('<%=myFile.getInode()%>', '<%=myFile.getIdentifier()%>', '<%=myFile.getFileName()%>', '<%=myFile.getURI()%>');
			} catch (e) { }
	<%}%>
			window.close();
		</script>
	<%
		return;
	}
	%>
<!---  end of When the file upload is in a popup -->

<%
	// variable that is set to make page a popup
boolean popup = false;
if(request.getParameter("popup")!=null){
	popup = true;
}
boolean inFrame = false;
if(request.getParameter("in_frame")!=null){
	inFrame = true;
}
//gets file object
com.dotmarketing.portlets.files.model.File file;
if (request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT)!=null) {
	file = (com.dotmarketing.portlets.files.model.File) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT);
}
else {
	file = (com.dotmarketing.portlets.files.model.File)APILocator.getFileAPI().get(request.getParameter("inode"), user, false); 
}
//gets parent identifier to get the categories selected for this file
com.dotmarketing.beans.Identifier identifier = null;
if(file!=null && UtilMethods.isSet(file.getInode())) {
    identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find(file);
}
//gets parent folder
Folder folder = null;
if(file!=null && UtilMethods.isSet(file.getParent())){
  folder = (Folder) folderAPI.find(file.getParent(),user,false);
}
//The host of the file
Host host = folder != null?APILocator.getHostAPI().findParentHost(folder, APILocator.getUserAPI().getSystemUser(), false):null;

//Permissions variables
boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
boolean ownerHasPubPermission = (hasOwnerRole && perAPI.doesRoleHavePermission(file, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));

boolean ownerHasWritePermission = (hasOwnerRole && perAPI.doesRoleHavePermission(file, PermissionAPI.PERMISSION_WRITE,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
boolean canUserWriteToFile = ownerHasWritePermission || hasAdminRole || perAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_WRITE, user, false);
boolean canUserPublishFile = ownerHasPubPermission || hasAdminRole || perAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_PUBLISH, user, false);

// if we are a new file, check folder permissions
if( !InodeUtils.isSet(file.getInode()) && folder != null && InodeUtils.isSet(folder.getInode())){
	if(perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)){
	    canUserWriteToFile=true;
	    if(!canUserPublishFile){
			canUserPublishFile = perAPI.doesUserHaveInheriablePermissions(folder, file.getPermissionType(), PermissionAPI.PERMISSION_PUBLISH, user);
		}
	}
}





// the link to the resource
StringBuffer resourceLink = new StringBuffer();
if (identifier!=null && InodeUtils.isSet(identifier.getInode())){
	if(request.isSecure()){ 
		resourceLink.append("https://");
	}else{
		resourceLink.append("http://");
	}
	resourceLink.append(host.getHostname());
	if(request.getServerPort() != 80 && request.getServerPort() != 443){
		resourceLink.append(":" + request.getServerPort());
	}
	resourceLink.append(UtilMethods.encodeURIComponent(identifier.getURI()));
}




//gets referer
String referer = null;
if(!popup){
	if (request.getParameter("referer") != null) {
		referer = java.net.URLDecoder.decode(request.getParameter("referer"), "UTF-8");
		referer = UtilMethods.encodeURL(referer);
	} else {
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action",new String[] {"/ext/files/view_files"});
		referer = UtilMethods.encodeURL(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params));
	}
	String cmd = request.getParameter(Constants.CMD);
	if( cmd == null && referer != null ) {
		referer = UtilMethods.encodeURL(referer);
	}
}

java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/files/edit_file"});
String inode = ((request.getParameter("inode")!=null) ? request.getParameter("inode") : "");
params.put("inode",new String[] { inode });

//url to use as a referer to get back to this page
String this_page = java.net.URLEncoder.encode(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params),"UTF-8");


String parent = ((request.getParameter("parent") != null )? request.getParameter("parent") : "" );

if(!InodeUtils.isSet(parent)){ // DOTCMS - 3861 
	parent = file.getParent();
}

//gets user roles
Role[] roles = (Role[])com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);

//for the calendars
int[] monthIds = CalendarUtil.getMonthIds();
String[] months = CalendarUtil.getMonths(locale);
String[] days = CalendarUtil.getDays(locale);
//gets publish date from the file
java.util.Date publishDate = (file.getPublishDate()!=null) ? file.getPublishDate() : new java.util.Date();

//Page width used in popups
String pageWidth = request.getParameter("page_width");

//This variable controls the name of the struts action used when the form is submitted
//the normal action is /ext/contentlet/edit_file but that can be changed 
String formAction = request.getParameter("struts_action") == null?"/ext/files/edit_file":request.getParameter("struts_action");

FileForm fileForm = (FileForm) request.getAttribute("FileForm");

String hostId = null;
if(host != null) {
	hostId = host.getIdentifier();
} else if (request.getParameter("host_id") != null) {
	hostId = request.getParameter("host_id");
} else {
	hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
}
	

if(com.dotmarketing.util.UtilMethods.isImage(file.getFileName())){
	/*
	if(file.getWidth() > file.getHeight()){
		showDim = (file.getWidth() > showDim) ? showDim : file.getWidth() ;
	}else{
		showDim = (file.getHeight() > showDim) ? showDim : file.getHeight() ;
	}

	showDim = (32 > showDim) ? 32 : showDim ;
	*/
}
%>
<script type="text/javascript">
	dojo.require("dotcms.dijit.image.ImageEditor");
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");


	var copyAsset = false;
	var currReferer = '<%=referer%>';

	var subcmd;
	var filename;
	
    function doUpload(subcmd){

    	
        var form = document.getElementById("fm");

		if (form.categorySelect) {
	        for (i=0;i<form.categorySelect.options.length;i++) {
	        	if (form.categorySelect.options[i].selected) {
	        		form.<portlet:namespace />categories.value += form.categorySelect.options[i].value + ",";
	        	}
	        }
	    }
		if (!isInodeSet(form.parent.value)) {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm")) %>');
			return false;
		}

        x = document.getElementById("<portlet:namespace />uploadedFile").value.split("\\");
        var myFileName = x[x.length -1];

        while(myFileName.indexOf(" ") > -1){
        	myFileName = myFileName.replace(" ", "");
        }

		<% if (!InodeUtils.isSet(file.getInode())) { %>
        if (myFileName.length ==0) {
    		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.alert.please.upload")) %>');
    		return false;
        }
		<% } %>
        if (myFileName.length !=0) {
        	if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Upload-file")) %> '  + myFileName + "?")){
        		return false;
        	}
        }

    	this.filename = myFileName;
        
        var fileName = this.filename;
        var form = document.getElementById("fm");
        
		var sdMonth = parseFloat(document.getElementById('calendar_0_month').value) + 1;
		var sdDay = document.getElementById('calendar_0_day').value;
		var sdYear = document.getElementById('calendar_0_year').value;

		form.webPublishDate.value = sdMonth + "/" + sdDay + "/" + sdYear;

        //document.getElementById("tableDiv").style.display = "none";
        //document.getElementById("messageDiv").style.display = "";

		var referer = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:actionURL>';
		if (form.<portlet:namespace />referer.value == '')
			form.<portlet:namespace />referer.value=referer;
        form.<portlet:namespace />fileName.value=fileName;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>';
        form.<portlet:namespace />subcmd.value = subcmd;
        form.<portlet:namespace />cmd.value="<%= Constants.ADD %>";

        submitForm(form);

    	<% if(inFrame) { %>
        if(parent.fileSubmitted) {
            parent.fileSubmitted();
        }
        <% } %>
    }


	function submitParent(element){
	<% if(!popup && !inFrame){ %>
		if (copyAsset) {
			var form = document.getElementById("fm");
			disableButtons(form);
			var parent = document.getElementById("parent").value;
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="copy" /><portlet:param name="inode" value="<%=String.valueOf(file.getInode())%>" /></portlet:actionURL>&parent=' + parent + '&referer=' + currReferer;
		}
	<% } %>
	}
	function cancelEdit()
	{
		<% if(!popup){ %>
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=String.valueOf(file.getInode())%>" /></portlet:actionURL>&referer=' + currReferer;
		<% }else{ %>
			self.close();
		<% } %>
	}

	function submitfmDelete()
	{	
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.delete")) %>'))
		{		
		<%
			if(!popup){
		%>
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=String.valueOf(file.getInode())%>" /></portlet:actionURL>&referer=' + currReferer;			
		<%
			}
		%>
		
		}
	}

	<% if(!popup){ %>
	function editText(inode, identifier) {
		editTextManager.editText(inode, identifier);
	}
	<% } %>

	function beLazier(){

			var ele = document.getElementById("<portlet:namespace />uploadedFile").value;

			var arr = ele.split("\\");
			if(arr.length ==1){
				var arr = ele.split("/");
			}
			val = arr[(arr.length -1)];
			if(val.lastIndexOf(".") > 0){
				val = val.substring(0, val.lastIndexOf("."));
			}
			var arg=/[\+\%\&\!\"\'\#\<%= "$" %>\/\\\=\?\¡\¿}\:\;\*\<\>\`\´\||_]/g ;
			val = val.replace(arg," ");
			while (val.indexOf("  ") > -1){
				val = val.replace("  ", " ");
			}

					
		
		var ele = document.getElementById("titleField");
		if(ele.value.length ==0 ){
			ele.value = val;
		}
	}

    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.delete.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + currReferer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId + '&referer=' + currReferer;
	    }
	}
	function selectFileVersion(objId,referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.replace.version.file")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + currReferer;
		}
	}


	function setDate(id, month, day, year) {
			if (id == "calendar_0") {
				myForm.calendar_0_month.selectedIndex = getIndex(myForm.calendar_0_month, month);
				myForm.calendar_0_day.selectedIndex = getIndex(myForm.calendar_0_day, day);
				myForm.calendar_0_year.selectedIndex = getIndex(myForm.calendar_0_year, year);
			}
	}
	function hideEditButtonsRow() {	
		dojo.style('editFileButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editFileButtonRow', { display: '' });
		changesMadeToPermissions=false;
	}
			
	function showOnMenuChanged() {
		var checkBox = document.getElementById('showOnMenu');
		var sortOrder = document.getElementById('sortOrder');

		if (checkBox.checked) {
			sortOrder.disabled = false;
		} else {
			sortOrder.disabled = true;
			sortOrder.value = 0;
		}
	}

	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=edit&inode=' + objId + '&parent=<%=parent%>' + '&referer=' + currReferer;
	}

   
	<liferay:include page="/html/js/calendar/calendar_js.jsp" flush="true">
		<liferay:param name="calendar_num" value="1" />
	</liferay:include>



function closeImageWindow(){
	if(dojo.byId("imgDialog")){
		dijit.byId("imgDialog").destroyRecursive();
	}
}



function editImage(inode, callingImg){
	closeImageWindow();
	var url = "/html/portlet/ext/contentlet/image_tools/?inode=" + inode + "&callingImg="+ callingImg ;

	var x = new dijit.Dialog({
	      title: "<%= LanguageUtil.get(pageContext, "image-editor") %>",
	      content: "<iframe scrollbars='no' src='"+url +"' height='1000' width='100%' frameborder='0' scrolling='no'></iframe>",
	      style:"width:1000px;height:750px;",
	      id:"imgDialog",
	      widgetId:"imgDialog"
	  });


	x.show();


}







</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"file-upload\") %>" />


	<html:form action="/ext/files/edit_file" method="POST"  styleId="fm" enctype="multipart/form-data">
		<input type="hidden" name="inode" value="<%=file.getInode()%>">
		<input type="hidden" name="inode_version" value="<%=request.getParameter("inode_version")%>">
		<input type="hidden" name="<portlet:namespace />cmd" value="<%=Constants.ADD%>">
		<input type="hidden" name="<portlet:namespace />subcmd" value="">
		<% if (request.getParameter("child")!=null && request.getParameter("child").equals("true")) { %>
			<input type="hidden" name="child" value="true">
		<% } %>
		<% if(!popup && !inFrame){ %>
			<input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>">
		<% } %>
		<input type="hidden" name="<portlet:namespace />fileName" value="">
	 	<input type="hidden" name="<portlet:namespace />categories" value="">
		<html:hidden property="maxSize" />
		<html:hidden property="maxWidth" />
		<html:hidden property="maxHeight" />
		<html:hidden property="minHeight" />
	 	<input type="hidden" name="webPublishDate" value="">
	 	<input type="hidden" name="_imageToolSaveFile" id="_imageToolSaveFile" value="">
		<input type="hidden" name="userId" value="<%= user.getUserId() %>">
		<% if(popup){ %>
			<input name="popup" type="hidden" value="<%= request.getParameter("popup") %>">
		<% } %>
		<% if(inFrame){ %>
			<input name="in_frame" type="hidden" value="<%= request.getParameter("in_frame") %>">
		<% } %>
		<input name="<portlet:namespace />referer" type="hidden" value="<%= referer %>">




<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
     
<!-- Basic Properties -->    
      <div id="fileBasicTab" dojoType="dijit.layout.ContentPane" onShow="showEditButtonsRow()" title="<%= LanguageUtil.get(pageContext, "Basic-Properties") %>">
			<dl>
				<%if(identifier!=null && InodeUtils.isSet(identifier.getInode())){%>
					<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
					<dd><%= identifier.getInode() %></dd>
				<%}%>

				<dt><%= LanguageUtil.get(pageContext, "Upload-New-File") %>:</dt>
				<dd><input type="file" class="form-text" style="width:350" name="<portlet:namespace />uploadedFile" id="<portlet:namespace />uploadedFile" onChange="beLazier();"></dd>
				

				 <%if(InodeUtils.isSet(file.getInode())){%>
					 <dt>&nbsp;</dt>
					 <dd><%= LanguageUtil.get(pageContext, "must-be-type") %>: <%=file.getMimeType()%></dd>
				 <% } %>
				 
				<dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:250px" name="title"  id="titleField" value="<%= UtilMethods.isSet(file.getTitle()) ? file.getTitle() : "" %>" /></dd>
				 
				<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:250px" name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(file.getFriendlyName()) ? file.getFriendlyName() : "" %>" /></dd>
				
				<dt><%= LanguageUtil.get(pageContext, "Folder") %>:</dt>
				<dd>
					<% if(!InodeUtils.isSet(parent)) { %>
						<div id="folder" name="parent" onlySelectFolders="true" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" <%= UtilMethods.isSet(hostId)?"hostId=\"" + hostId + "\"":"" %>></div>
					<% } else { %>
						<html:hidden property="selectedparent" styleId="selectedparent" />
						<input type="text" dojoType="dijit.form.TextBox" readonly="true" style="250px" name="selectedparentPath" id="selectedparentPath" value="<%= UtilMethods.isSet(fileForm.getSelectedparentPath()) ? fileForm.getSelectedparentPath() : "" %>" />
						<html:hidden styleClass="form-text" property="parent" styleId="parent" />
					<% } %>
					
				</dd>
				
				<%if(InodeUtils.isSet(file.getInode())){%>
				
					<%if(canUserWriteToFile){%>
						<dt><%= LanguageUtil.get(pageContext, "Resource-Link") %>:</dt>
						<dd>

								<a href="<%=resourceLink %>" target="_new">

						
							<%=identifier.getURI()%></a>
								<% if (file.getMimeType()!=null  && file.getMimeType().indexOf("text")!=-1 || file.getMimeType().indexOf("xml")!=-1) { %>
									<% if (InodeUtils.isSet(file.getInode()) && canUserWriteToFile && !popup) { %>
										<% if (file.getMimeType()!=null && file.getMimeType().indexOf("text")!=-1 || file.getMimeType().indexOf("xml")!=-1) { %>
											<button iconClass="editIcon" dojoType="dijit.form.Button" onClick="editText('<%= file.getInode()%>','<%= file.getIdentifier()%>')" type="button">
												<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit-text")) %>
											</button>
										<% } %>
									<% } %>
							<% } %>
						<dd>
					<% } %>
                                   
					<%if(com.dotmarketing.util.UtilMethods.isImage(identifier.getURI())){%>
						<dt><%= LanguageUtil.get(pageContext, "Image") %>:</dt>
						<dd>
							
							<%if(("100".equals(System.getProperty("dotcms_level")))){ %>
								<div style="position:relative;width:<%=showDim+40 %>px;">
									<img src="/contentAsset/image/<%=file.getInode() %>/?byInode=1&filter=Thumbnail&thumbnail_w=<%=showDim %>&thumbnail_h=<%=showDim %>" 
											class="thumbnailDiv" 
											onmouseover="dojo.attr(this, 'className', 'thumbnailDivHover');"
											onmouseout="dojo.attr(this, 'className', 'thumbnailDiv');"
											onclick="dijit.byId('fileDia').show()"> 
								</div>
								
	
								<div dojoType="dijit.Dialog" id="fileDia" title="<%=LanguageUtil.get(pageContext,"Image") %>"  style="width:760px;height:500px;display:none;"">
									<div style="text-align:center;margin:auto;overflow:auto;width:700px;height:400px;">
										<img src="/contentAsset/image/<%=file.getInode() %>/?byInode=1" />
									</div>
									<div class="callOutBox">
									<%=LanguageUtil.get(pageContext,"dotCMS-Enterprise-comes-with-an-advanced-Image-Editor-tool") %>
									</div>
								</div>
	
							<%}else{ %>
								<div  dojoType="dotcms.dijit.image.ImageEditor" editImageText="<%= LanguageUtil.get(pageContext, "Edit-Image") %>" inode="<%= file.getInode()%>" saveAsFileName="<%=file.getFileName() %>">
								</div>
							<%} %>
							<div style="width:<%=showDim %>px;text-align:right">
								<%if(file.getWidth() > 0){%>
									<%=file.getWidth() %>x<%=file.getHeight() %> | 
								<%} %>
								<%
									int showSize  = file.getSize() /1024;
									if(showSize < 1) showSize = 1;
								%>
								<%=NumberFormat.getInstance().format(showSize) %>k
							</div>
						</dd>
                    <%}%>   
					             
				<%}%>
			</dl>
      </div>
<!-- /Basic Properties -->
	
	
<!-- Advanced Properties -->    
	<div id="fileAdvancedTab" refreshOnShow="true" onShow="showEditButtonsRow()" preload="true"  dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Advanced-Properties") %>">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Publish-Date") %></dt>
			<dd>
				<script language="JavaScript">
					var myForm = document.getElementById("fm");

					function dateSelected(id) {
						var date = dijit.byId(id).attr('value');
						document.getElementById(id + '_month').value = date.getMonth();
						document.getElementById(id + '_day').value = date.getDate();
						document.getElementById(id + '_year').value = date.getFullYear();
					}
				</script>
				<!--select name="calendar_0_month">
					<%
						Calendar publishDateCal = new GregorianCalendar();
						publishDateCal.setTime(publishDate);
						String sdMonth = Integer.toString(publishDateCal.get(Calendar.MONTH));
						for (int i = 0; i < months.length; i++) {
					%>
						<option <%= (sdMonth.equals(Integer.toString(monthIds[i]))) ? LanguageUtil.get(pageContext, "selected")  : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>
					<% } %>
				</select>
				<select name="calendar_0_day">
					<%
						String sdDay = Integer.toString(publishDateCal.get(Calendar.DATE));
						for (int i = 1; i <= 31; i++) {
					%>
						<option <%= (sdDay.equals(Integer.toString(i))) ? LanguageUtil.get(pageContext, "selected") : "" %> value="<%= i %>"><%= i %></option>
					<% } %>
				</select>
				<select name="calendar_0_year">
					<%
						int currentYear = publishDateCal.get(Calendar.YEAR);
						String sdYear = Integer.toString(publishDateCal.get(Calendar.YEAR));
						for (int i = currentYear; i <= currentYear + 10; i++) {
					%>
						<option <%= (sdYear.equals(Integer.toString(i))) ? LanguageUtil.get(pageContext, "selected") : "" %> value="<%= i %>"><%= i %></option>
					<% } %>
				</select>
				<span class="calMonthIcon" id="<portlet:namespace />calendar_input_0_button" onClick="<portlet:namespace />calendarOnClick_0('<portlet:namespace />calObj_0');"></span>-->
				<input type="text" dojoType="dijit.form.DateTextBox" validate='return false;' invalidMessage="" id="calendar_0" name="calendar_0" value="<%= publishDateCal.get(Calendar.YEAR) + "-" + (publishDateCal.get(Calendar.MONTH) < 9 ? "0" : "") + (publishDateCal.get(Calendar.MONTH) + 1) + "-" + (publishDateCal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + publishDateCal.get(Calendar.DAY_OF_MONTH) %>" onchange="dateSelected('calendar_0');" />
				<input type="hidden" name="calendar_0_month" id="calendar_0_month" value="<%= publishDateCal.get(Calendar.MONTH) %>" />
				<input type="hidden" name="calendar_0_day" id="calendar_0_day" value="<%= publishDateCal.get(Calendar.DATE) %>" />
				<input type="hidden" name="calendar_0_year" id="calendar_0_year" value="<%= publishDateCal.get(Calendar.YEAR) %>" />

			</dd>

			<dt><%= LanguageUtil.get(pageContext, "Author") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" style="250px" name="author" id="author" value="<%= UtilMethods.isSet(file.getAuthor()) ? file.getAuthor() : "" %>" /></dd>
			
			<dt><%= LanguageUtil.get(pageContext, "Show-on-Menu") %>:</dt>
			<dd><input type="checkbox" dojoType="dijit.form.CheckBox" name="showOnMenu" id="showOnMenu" onclick="showOnMenuChanged();" <%= file.isShowOnMenu() ? "checked" : "" %> /></dd>

			<dt><%= LanguageUtil.get(pageContext, "Sort-Order") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" name="sortOrder" id="sortOrder" style="width: 50px;" value="<%= file.getSortOrder() %>" /></dd>
	</dl>
 </div>
<!-- /Advanced Properties -->    
	
<!-- Permissions Tab -->
<%
	boolean canEditAsset = perAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="filePermissionTab"  dojoType="dijit.layout.ContentPane" onShow="hideEditButtonsRow()" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, file);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
	</div>
<%
	}
%>
<!-- /Permissions Tab  -->
	
<!-- Versions Tab -->    
	<%if(UtilMethods.isSet(file) && InodeUtils.isSet(file.getInode())){ %>
		<div id="fileVersionTab" dojoType="dijit.layout.ContentPane" onShow="showEditButtonsRow()" title="<%= LanguageUtil.get(pageContext, "Versions") %>">
			<%@ include file="/html/portlet/ext/common/edit_versions_file_asset_inc.jsp"%>
		</div>
	<%} %>
<!-- /Versions Tab -->    
      
</div>
<!-- /TabContainer-->  
<div class="clear"></div>
<!-- Button Row --->
<div class="buttonRow" id="editFileButtonRow" style="">
		
		<%--check permissions to display the save and publish button or not--%> 
		
		<% if (!InodeUtils.isSet(file.getInode()) || file.isLive() || file.isWorking()) { %>
		
			<%  if (canUserPublishFile) {%>
				<button dojoType="dijit.form.Button"  onClick='doUpload("")' iconClass="saveIcon" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
				</button>
				<button dojoType="dijit.form.Button" onClick="doUpload('publish')"  iconClass="publishIcon" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
				</button>
			<%} else if (canUserWriteToFile) { %>
				<button dojoType="dijit.form.Button"  onClick='doUpload("")' iconClass="saveIcon" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
				</button>
			<% } %>
		<% } else if (canUserWriteToFile) { %>
			<button dojoType="dijit.form.Button" onClick="selectFileVersion('<%=file.getInode()%>', '<%=referer%>')" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
			</button>
		<% } %>
		
		<% if (InodeUtils.isSet(file.getInode()) && file.isDeleted()) { %>
			<button dojoType="dijit.form.Button" onClick='submitfmDelete()' iconClass="saveIcon" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-file")) %>
			</button>
		<% } %>

		<button dojoType="dijit.form.Button" onClick='cancelEdit()' iconClass="cancelIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>
	</div>
<!-- /Button Row -->

<!-- Messages --> 
	<div id="messageDiv" style="display: none;show; position:relative; z-index: 100">
		<%= LanguageUtil.get(pageContext, "File-Uploading") %>  . . .<BR>  <%= LanguageUtil.get(pageContext, "Note") %>: <%=LanguageUtil.get(pageContext, "This-window-will-redirect-you-back-when-the-file-has-been-uploaded") %>
	</div>
<!-- /Messages --> 

</html:form>
</liferay:box>

<script>
	showOnMenuChanged ();
</script>
