<%@page import="com.dotmarketing.cache.FieldsCache"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ include file="/html/portlet/ext/files/init.jsp" %>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@ page import="com.dotmarketing.util.*" %>

<%
//gets referer
String referer = (request.getParameter("referer") != null ) ? request.getParameter("referer") : "" ;
String selectedStructure = (request.getParameter("selectedStructure") != null &&  request.getParameter("selectedStructure").trim().length() > 0)
		? request.getParameter("selectedStructure") : CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode();

Structure s = StructureFactory.getStructureByInode(selectedStructure);
List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().findSchemesForStruct(s);

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
FolderAPI folderAPI = APILocator.getFolderAPI();

// Retrieves the parent folder where the new files will be uploaded
com.dotmarketing.portlets.folders.model.Folder parentFolder = (com.dotmarketing.portlets.folders.model.Folder) request.getAttribute("PARENT_FOLDER");
com.dotmarketing.portlets.folders.model.Folder folder = folderAPI.find(parentFolder.getInode(),user,false);

// Retrieves the site of the parent folder
Host host = folder != null?APILocator.getHostAPI().findParentHost(folder, APILocator.getUserAPI().getSystemUser(), false):null;

boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
boolean canUserWriteToFile = hasOwnerRole || hasAdminRole;
boolean canUserPublishFile = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user);

boolean inFrame = false;
if(request.getParameter(WebKeys.IN_FRAME)!=null){
	inFrame = true;
}
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"file-upload\") %>" />
    <body>
    
    
	<% if(hasExplicitRequiredFields){ %>
		
		<div class="callOutBox">
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
					<input type="hidden" name="cmd" value="<%=Constants.ADD%>">
					<input type="hidden" name="subcmd" value="">
					<input type="hidden" name="redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>">
			 		<input type="hidden" name="categories" value="">
					<html:hidden property="maxSize" />
					<html:hidden property="maxWidth" />
					<html:hidden property="maxHeight" />
					<html:hidden property="minHeight" />
					<html:hidden property="parent"  />
					<html:hidden property="selectedparent"  />

                    <% if ( request.getHeader( "User-Agent" ).contains( "MSIE" ) ) { %>
                        <input type="hidden" name="p_p_action" value="1">
                        <input type="hidden" name="p_p_id" value="<%=PortletID.SITE_BROWSER%>">
                        <input type="hidden" name="p_p_state" value="maximized">
                        <input type="hidden" name="p_p_mode" value="view">
                        <input type="hidden" name="struts_action" value="/ext/files/upload_multiple">
                    <% } %>

                    <input type="hidden" name="titles"  id="titles" value=""/>
					<input type="hidden" name="friendlyNames" id="friendlyNames" value=""/>
					<input type="hidden" name="fileNames" id="fileNames" value=""/>
					<input type="hidden" name="userId" value="<%= user.getUserId() %>">
					<input name="referer" type="hidden" value="<%= referer %>">

                    <div class="inline-form">
				 		<label><%= LanguageUtil.get(pageContext, "Folder") %>:</label>
				 		<html:textarea readonly="true" rows="2" style="resize:none;border:0;width:100%;height:14px"
							property="selectedparentPath" styleId="selectedparentPath" />
                    </div>
                     <div class="callOutBox">
                    	 <b><%= LanguageUtil.get(pageContext, "Note") %></b>: <%= LanguageUtil.get(pageContext, "Hold-down-ctrl-to-select-multiple-files") %>
                     </div>

					    <div class="buttonRow">
							<input name="uploadedFile" multiple="true" type="file" id="uploader"
   									dojoType="dojox.form.Uploader" label="<%= LanguageUtil.get(pageContext, "Select-file(s)-to-upload") %>" >
   						</div>
				        <div id="files" dojoType="dojox.form.uploader.FileList" uploaderId="uploader"
				        style="height:170px; overflow-y: auto;"
				        	<% if(request.getHeader("User-Agent").contains("MSIE")){ %>
				        		headerFilesize="" 
				        	<% } %>				        
				        ></div>
	                   

	                 <%
						canUserWriteToFile = perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user);
                     %>

					<div class="buttonRow-right">
						<select dojoType="dijit.form.FilteringSelect" name="wfActionId" id="wfActionId" store="actionStore" value="" >
				        </select>
						<button dojoType="dijit.form.Button" class="dijitButtonFlat" type="button">
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
                        <button dojoType="dijit.form.Button" onClick="doUpload('')" id="saveButton">
					        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Execute")) %>
					        <script type="dojo/method" event="onClick" args="evt">

                            //Submit the form
                            uploadFiles(dijit.byId("uploader"), "<%=referer%>");

                            <% if(inFrame) { %>
                                if(parent.fileSubmitted) {
                                    parent.fileSubmitted(uploadFiles.length,'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlets.batch.reindexing.background")) %>');
                                }
                            <% } %>
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

<% // Include javascript method to upload multiple files %>
<%@ include file="/html/portlet/ext/files/upload_multiple_js_inc.jsp" %>
</body>
</liferay:box>