<%@page import="com.dotmarketing.cache.FieldsCache"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.cache.StructureCache"%>
<%@ include file="/html/portlet/ext/files/init.jsp" %>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.portlets.files.model.File"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.files.business.FileAPI"%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@ page import="com.dotmarketing.util.*" %>
<%@ include file="/html/portlet/ext/browser/view_browser_js_inc.jsp" %>
<%
//gets referer
String referer = (request.getParameter("referer") != null ) ? request.getParameter("referer") : "" ;
String selectedStructure = (request.getParameter("selectedStructure") != null ) ? request.getParameter("selectedStructure") : StructureCache.getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode();

Structure s = StructureFactory.getStructureByInode(selectedStructure);
WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(s);

boolean hasExplicitRequiredFields = false;//GIT-191
List<Field> fields = FieldsCache.getFieldsByStructureInode(selectedStructure);
for (Field field : fields) {
	if(field.isRequired()
			// the below required fields values are implicitly set to content in UploadMultipleFilesAction._saveFileAsset()
			&& (!field.getVelocityVarName().equalsIgnoreCase("title")
					&& !field.getVelocityVarName().equalsIgnoreCase("fileName")
					&& !field.getVelocityVarName().equalsIgnoreCase("fileAsset")
					&& !field.getVelocityVarName().equalsIgnoreCase("hostFolder"))){
		hasExplicitRequiredFields = true;
	}
}


PermissionAPI perAPI = APILocator.getPermissionAPI();
FileAPI fileAPI = APILocator.getFileAPI();
FolderAPI folderAPI = APILocator.getFolderAPI();
com.dotmarketing.portlets.files.model.File file;
if (request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT)!=null) {
	file = (com.dotmarketing.portlets.files.model.File) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT);
}
else {
	file = (com.dotmarketing.portlets.files.model.File) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"), com.dotmarketing.portlets.files.model.File.class);
}
//gets parent identifier to get the categories selected for this file
if(InodeUtils.isSet(file.getInode())){
 com.dotmarketing.beans.Identifier identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find(file);	
}

//gets parent folder
com.dotmarketing.portlets.folders.model.Folder folder = (com.dotmarketing.portlets.folders.model.Folder) folderAPI.find(file.getParent(),user,false);

//The host of the file
Host host = folder != null?APILocator.getHostAPI().findParentHost(folder, APILocator.getUserAPI().getSystemUser(), false):null;

com.dotmarketing.portlets.folders.model.Folder parentFolder = (com.dotmarketing.portlets.folders.model.Folder) request.getAttribute("PARENT_FOLDER");

boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
boolean ownerHasPubPermission = (hasOwnerRole && perAPI.doesRoleHavePermission(file, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
boolean ownerHasWritePermission = (hasOwnerRole && perAPI.doesRoleHavePermission(file, PermissionAPI.PERMISSION_WRITE,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
boolean canUserWriteToFile = ownerHasWritePermission || hasAdminRole || perAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_WRITE, user, false);
boolean canUserPublishFile = ownerHasPubPermission || hasAdminRole || perAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_PUBLISH, user, false) || perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT, user, false);

boolean inFrame = false;
if(request.getParameter("in_frame")!=null){
	inFrame = true;
}
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"file-upload\") %>" />
 	<style type="text/css">
        	.dojoxUploaderFileListHeader{
				width:100%;
				text-align: left;
          	}
		    .dojoxUploaderIndex{
				width:50px;
				text-align: left;
			}
			.dojoxUploaderIcon{
				width:100px;
				text-align: left;
			}
			.dojoxUploaderFileName{
				width:400px;
				text-align: left;
			}
			.dojoxUploaderSize{
				width:50px;
				text-align: left;
				<% if(request.getHeader("User-Agent").contains("MSIE")){ %>
					display:none; 
				<% } %>				
			}
    </style>
    <body>
    
    
	<% if(hasExplicitRequiredFields){ %>
		
		<div class="callOutBox" style="margin-left:40px;margin-right:40px;margin-bottom:10px" >
	    	<b><%= LanguageUtil.get(pageContext, "Note") %></b>: <%= LanguageUtil.get(pageContext, "Multiple-File-Upload-does-not-upload-custom-files-with-explicit-required-fields") %>
	    </div>
	                 
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" iconClass="cancelIcon">
	            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	            <script type="dojo/method" event="onClick" args="evt">						
                	if(dijit.byId('addFileDialog')){
                		dijit.byId('addFileDialog').hide();
                	}else {
        				if(parent.closeAddFileDialog) {
        	    				parent.closeAddFileDialog();
    	    			}
	                }
            	</script>
	     	</button>
	    </div>
	    
	<% }else{ %>
	    
		 <div id="tableDiv" style="position:relative; z-index: 100">
			 <html:form action="/ext/files/upload_multiple" method="POST"  styleId="fm" enctype="multipart/form-data" onsubmit="return false;">
	             
					<input type="hidden" name="selectedStructure" value="<%=selectedStructure%>">
					<input type="hidden" name="<portlet:namespace />cmd" value="<%=Constants.ADD%>">
					<input type="hidden" name="<portlet:namespace />subcmd" value="">
					<input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>">
			 		<input type="hidden" name="<portlet:namespace />categories" value="">
					<html:hidden property="maxSize" />
					<html:hidden property="maxWidth" />
					<html:hidden property="maxHeight" />
					<html:hidden property="minHeight" />
					<html:hidden property="parent"  />
					<html:hidden property="selectedparent"  />

                    <% if ( request.getHeader( "User-Agent" ).contains( "MSIE" ) ) { %>
                        <input type="hidden" name="p_p_action" value="1">
                        <input type="hidden" name="p_p_id" value="EXT_BROWSER">
                        <input type="hidden" name="p_p_state" value="maximized">
                        <input type="hidden" name="p_p_mode" value="view">
                        <input type="hidden" name="struts_action" value="/ext/files/upload_multiple">
                    <% } %>

                    <input type="hidden" name="titles"  id="titles" value=""/>
					<input type="hidden" name="friendlyNames" id="friendlyNames" value=""/>
					<input type="hidden" name="fileNames" id="fileNames" value=""/>
					<input type="hidden" name="userId" value="<%= user.getUserId() %>">
					<input name="<portlet:namespace />referer" type="hidden" value="<%= referer %>">
					
					<table border="0">
						<tr>
							<td valign="bottom">
								<b><%= LanguageUtil.get(pageContext, "Folder") %>:</b>
							</td>
							<td>
								<html:text readonly="true" style="border:0px;margin:auto;padding-bottom:1px" styleClass="form-text" property="selectedparentPath" styleId="selectedparentPath" />
							</td>
						</tr>
					</table>
                     <div class="callOutBox" style="margin-left:40px;margin-right:40px;margin-bottom:10px" >
                    	 <b><%= LanguageUtil.get(pageContext, "Note") %></b>: <%= LanguageUtil.get(pageContext, "Hold-down-ctrl-to-select-multiple-files") %>
                     </div>

					    <div style="margin-left:200px;">
							<input name="<portlet:namespace />uploadedFile" multiple="true" type="file" id="uploader"
   									dojoType="dojox.form.Uploader" label="<%= LanguageUtil.get(pageContext, "Select-file(s)-to-upload") %>" >
   						</div>
				        <div id="files" dojoType="dojox.form.uploader.FileList" uploaderId="uploader" 
				        style="height:200px; border:1px solid silver; overflow-y: auto;"
				        	<% if(request.getHeader("User-Agent").contains("MSIE")){ %>
				        		headerFilesize="" 
				        	<% } %>				        
				        ></div>
	                   

	                 <%if (!InodeUtils.isSet(file.getInode()) && UtilMethods.isSet(folder)) {
		                   	if(!InodeUtils.isSet(file.getInode())) {
		                   	    boolean isRootHost=folderAPI.findSystemFolder().equals(folder);
		                   	    if(isRootHost) {
		                   	        String hostId=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		                   	        host=APILocator.getHostAPI().find(hostId, user, false);
		                   	        canUserWriteToFile = hasAdminRole || perAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user);
		                   	        canUserPublishFile = hasAdminRole || perAPI.doesUserHaveInheriablePermissions(host, file.getPermissionType(), PermissionAPI.PERMISSION_PUBLISH, user);
		                   	    }
		                   	    else {
		                   	        canUserWriteToFile = perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user);
		                   	    }
			                }
	                    }
                     %>

					<div class="buttonRow">
                     <%if (canUserPublishFile) {%>
				    <button dojoType="dijit.form.Button" onclick="doUpload('')" iconClass="saveIcon" id="saveButton">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
					<script type="dojo/method" event="onClick" args="evt">
                        //Submit the form
                        uploadFiles(dijit.byId("uploader"), "<%=referer%>", "");

                        <% if(inFrame) { %>
                            if(parent.fileSubmitted) {
                                parent.fileSubmitted(uploadFiles.length,'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlets.batch.reindexing.background")) %>');
                            }
                        <% } %>
					</script>
                </button>
                <%if(!scheme.isMandatory()) {%>
           		<button dojoType="dijit.form.Button" onClick="doUpload('publish')" iconClass="publishIcon" id="savePublishButton" type="button">
                	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
					<script type="dojo/method" event="onClick" args="evt">

                        //Submit the form
                        uploadFiles(dijit.byId("uploader"), "<%=referer%>", "publish");

                        <% if(inFrame) { %>
                            if(parent.fileSubmitted) {
                                parent.fileSubmitted(uploadFiles.length,'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlets.batch.reindexing.background")) %>');
                            }
                        <% } %>
					</script>
                </button>
                <%} %>
             <%} else if (canUserWriteToFile) { %>
                <button dojoType="dijit.form.Button" onClick="doUpload('')" iconClass="saveIcon" id="saveButton">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
					<script type="dojo/method" event="onClick" args="evt">

                        //Submit the form
                        uploadFiles(dijit.byId("uploader"), "<%=referer%>", "");

                        <% if(inFrame) { %>
                            if(parent.fileSubmitted) {
                                parent.fileSubmitted(uploadFiles.length,'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlets.batch.reindexing.background")) %>');
                            }
                        <% } %>
					</script>
             	</button>
			  <% } %>
                <button dojoType="dijit.form.Button" iconClass="cancelIcon" type="button">
                	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                	<script type="dojo/method" event="onClick" args="evt">						
                        try{
                            if(dijit.byId('addFileDialog')){
                                dijit.byId('addFileDialog').hide();
                            } else {
                                if(parent.closeAddFileDialog) {
                                    parent.closeAddFileDialog();
                                }
                            }
                        }catch(e){
                            console.error(e);
                        }
                	</script>
                </button>
			</div>
	</html:form>
</div>

<% } %>

<div id="messageDiv" class="messageBox shadowBox" style="display: none;">
	<b><%= LanguageUtil.get(pageContext, "File-Uploading") %>  . . .</b><BR>
	<%= LanguageUtil.get(pageContext, "Note") %>: <%= LanguageUtil.get(pageContext, "This-window-will-redirect-you-back-when-the-file-has-been-uploaded") %>
</div>
</body>
</liferay:box>