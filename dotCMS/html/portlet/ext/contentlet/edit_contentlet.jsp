<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.viewtools.CategoriesWebAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>

<script type='text/javascript' src='/dwr/interface/LanguageAjax.js'></script>

<%@ include file="/html/portlet/ext/contentlet/field/edit_file_asset_text_inc.jsp" %>

<%
	PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	ContentletAPI conAPI = APILocator.getContentletAPI();
	String inode=request.getParameter("inode");
	if(!UtilMethods.isSet(inode)){
		inode="0";
	}
	Contentlet contentlet = (Contentlet) request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT);
	contentlet = (contentlet != null ? contentlet : conAPI.find(request.getParameter("inode"),user,false));

	ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");

	String copyOptions = ((String) request.getParameter("copyOptions"))==null?"":(String) request.getParameter("copyOptions");

	//Content structure or user selected structure
	Structure structure = contentletForm.getStructure();


	// if host, set this to the current viewing host
	if(structure!= null && UtilMethods.isSet(structure.getInode()) && structure.getVelocityVarName().equals("Host")) {
		if(contentlet != null && UtilMethods.isSet(contentlet.getIdentifier()))
				session.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,contentlet.getIdentifier() );

	}

	boolean canUserPublishContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_PUBLISH,user);

	if(!InodeUtils.isSet(contentlet.getInode())) {
		canUserPublishContentlet = conPerAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_PUBLISH,user);
		//Set roles = conPerAPI.getPublishRoles();
		if(!canUserPublishContentlet){
			canUserPublishContentlet = conPerAPI.doesRoleHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole());
		}
	}
	request.setAttribute("canUserPublishContentlet", new Boolean(canUserPublishContentlet));

	if (!InodeUtils.isSet(structure.getInode())){
		structure = StructureFactory.getStructureByInode(request.getParameter("sibblingStructure"));
	}
	List<Field> fields = structure.getFields();

	//Categories
	String[] selectedCategories = contentletForm.getCategories ();

	//Contentlet relationships
	ContentletRelationships contentletRelationships = (ContentletRelationships)
		request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_RELATIONSHIPS_EDIT);
	List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = contentletRelationships.getRelationshipsRecords();

	//Contentlet references
	List<Map<String, Object>> references = null;
	try{
		references = conAPI.getContentletReferences(contentlet, user, false);
	}catch(DotContentletStateException dse){
		references = new ArrayList<Map<String, Object>>();
	}

	//This variable controls the name of the struts action used when the form is submitted
	//the normal action is /ext/contentlet/edit_contentlet but that can be changed
	String formAction = request.getParameter("struts_action") == null?"/ext/contentlet/edit_contentlet":request.getParameter("struts_action");

	//Variable used to return after the work is done with the contentlet
	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = request.getParameter("referer");
	} else {
		Map params = new HashMap();
		params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
		params.put("inode",new String[] { contentlet.getInode() + "" });
		params.put("cmd",new String[] { Constants.EDIT });
		referer = PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	}

	//Setting request attributes used by the included jsp pages
	request.setAttribute("contentlet", contentlet);
	//request.setAttribute("contentletForm", contentletForm);
	request.setAttribute("structure", structure);
	request.setAttribute("selectedCategories", selectedCategories);
	request.setAttribute("relationshipRecords", relationshipRecords);
	request.setAttribute("references", references);
	request.setAttribute("referer", referer);
	request.setAttribute("fields", fields);

	request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
	request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, structure);


	List<Structure> structures = StructureFactory.getStructuresByUser(user, "", "name", 0, 100,"asc");




	/*### DRAW THE DYNAMIC FIELDS ###*/

	int counter = 0;
	boolean tabDividerOpen  = false;
	boolean categoriesTabFieldExists = false;
	boolean permissionsTabFieldExists = false;
	boolean relationshipsTabFieldExists = false;

	/* Events code only */
	Field startDateField = null;
	Field endDateField = null;
	Field locationField = null;
	/* End of Events code only */

	Field widgetUsageField = null;

	for(int i = 0; i < fields.size(); i++){
		if(fields.get(i).getFieldName().equals("Widget Usage")){
			widgetUsageField = fields.get(i);
			fields.remove(i);
			break;
		}
	}

	boolean canEditAsset = conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);

	boolean contentEditable = (UtilMethods.isSet(contentlet.getInode())?(Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.CONTENT_EDITABLE):false);
	Integer catCounter = 0;

%>


<!-- global included dependencies -->


<script language='javascript' type='text/javascript'>
var editButtonRow="editContentletButtonRow";
</script>

<%@ include file="/html/portlet/ext/contentlet/field/edit_field_js.jsp" %>


<html:form action="<%= formAction %>" styleId="fm" onsubmit="return false;">
	<input name="wfActionAssign" id="wfActionAssign" type="hidden" value="">
	<input name="wfActionComments" id="wfActionComments" type="hidden" value="">
	<input name="wfActionId" id="wfActionId" type="hidden" value="">

	<!-- PUSH PUBLISHING ACTIONLET -->
	<input name="wfPublishDate" id="wfPublishDate" type="hidden" value="">
	<input name="wfPublishTime" id="wfPublishTime" type="hidden" value="">
	<input name="wfExpireDate" id="wfExpireDate" type="hidden" value="">
	<input name="wfExpireTime" id="wfExpireTime" type="hidden" value="">
	<input name="wfNeverExpire" id="wfNeverExpire" type="hidden" value="">
	<input name="whereToSend" id="whereToSend" type="hidden" value="">

	<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<div dojoAttachPoint="cmsFileBrowserImage" currentView="thumbnails" jsId="cmsFileBrowserImage" onFileSelected="addFileImageCallback" mimeTypes="image"  dojoType="dotcms.dijit.FileBrowserDialog"></div>
	<div dojoAttachPoint="cmsFileBrowserFile" currentView="list" jsId="cmsFileBrowserFile" onFileSelected="addFileCallback" dojoType="dotcms.dijit.FileBrowserDialog"></div>

	<% if(structure.getStructureType() == Structure.STRUCTURE_TYPE_CONTENT){ %>
		<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-contentlet\") %>" />
	<%} else { %>
		<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-widget\") %>" />
	<%} %>

	<%if(!UtilMethods.isSet(structure.getInode())){ %>

		<div dojoType="dijit.Dialog" id="selectStructureDiv" title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %>'>
			<table style="margin:20px">
				<tr>
				  <td align="center"><%=LanguageUtil.get(pageContext, "Select-type") %>:</td>
					<td align="center">
					<select name="selectedStructAux" id="selectedStructAux" onChange="updateSelectedStructAux" dojoType="dijit.form.FilteringSelect">
						<option = ""></option>
						<%for(Structure struc : structures) {%>
							<option value="<%=struc.getInode() %>"><%=struc.getName() %></option>
						<%} %>
				    	</select>
				    </td>
				</tr>
			</table>
		</div>
		<script>
			dojo.ready(function(){
				dijit.byId("selectStructureDiv").show();
			});

			function updateSelectedStructAux(){
				var newloc = window.location.href;
				console.log(newloc);
				var struc = dijit.byId("selectedStructAux").getValue();
				if(newloc.indexOf("selectedStructure=") > -1){
					var s = newloc.indexOf("selectedStructure=");
					var e = newloc.indexOf("&", s);
					if(e>-1){
						var val = newloc.substring(s + "selectedStructure=".length, e);
						newloc = newloc.replace("selectedStructure=" + val, "selectedStructure=" + struc);
					}
					else{
						newloc = newloc.replace("selectedStructure=", "selectedStructure=" + struc);

					}
				}
				else{
					newloc = newloc+ "&selectedStructure=" + struc;
				}
				console.log(newloc);
				window.location=newloc;
			}

		</script>
	<%} %>
	<!--  FIRST TAB -->
	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" >







		<!--  IF THE FIRST FIELD IS A TAB-->
		<% if(fields != null &&
			fields.size()>0 &&
			fields.get(0) != null &&
			fields.get(0).getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
				Field f0 = fields.get(0);
				fields.remove(0);%>
			<div id="<%=f0.getVelocityVarName()%>" style="padding:0;" dojoType="dijit.layout.ContentPane" title="<%=f0.getFieldName()%>" onShow="showEditButtonsRow()">
		<%} else {	%>
			<div id="properties" dojoType="dijit.layout.ContentPane" style="padding:0;" title="<%= LanguageUtil.get(pageContext, "Content") %>" onShow="showEditButtonsRow()">
		<%}%>

		<!-- START Right Column -->
			<div class="wrapperRight" style="position:relative;">
					<div class="fieldWrapper">&nbsp;</div>
					<% if(widgetUsageField != null && UtilMethods.isSet(widgetUsageField.getValues())){ %>
						<div class="fieldWrapper">
							<div class="fieldName">
								<%=widgetUsageField.getFieldName()%>:
							</div>
							<div class="fieldValue">
								<%
								String textValue = widgetUsageField.getValues();
								textValue = textValue.replaceAll("&", "&amp;");
								textValue =  UtilMethods.htmlLineBreak(textValue);
								%>
								<%=textValue %>
							</div>
                            <div class="clear"></div>
						</div>
					<% } %>

					<%-- Begin Looping over fields --%>

					<% boolean fieldSetOpen = false;
						int fieldCounter =0;
						for (Field f : fields) {
							if (fieldSetOpen &&
								(f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
								 f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()) )) {
								fieldSetOpen = false;%>
							<%}%>
				    		<%if(f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())) {%>
				    			<div class="lineDividerTitle"><%=f.getFieldName() %></div>
							<%}else if(f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
			    				tabDividerOpen = true;%>
									</div>
								</div>
								<div id="<%=f.getVelocityVarName()%>" style="padding:0;" dojoType="dijit.layout.ContentPane" title="<%=f.getFieldName()%>" onShow="showEditButtonsRow()">
									<div class="wrapperRight" style="position:relative;">
										<div class="fieldWrapper">&nbsp;</div>
							<%}else if(f.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()) && !categoriesTabFieldExists) {
			   	    			categoriesTabFieldExists = true;%>
								<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_categories.jsp" />
			    			<%}else if(f.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString()) && !permissionsTabFieldExists){
			    	  			permissionsTabFieldExists = true;%>
								<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
							<%}else if(f.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())){%>
					   			<% if(fieldCounter==0){
									relationshipsTabFieldExists =  true;
									request.setAttribute("isRelationsihpAField",true); //DOTCMS-6893%>
							  		<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp" />
				    			<% }
				    	   		counter++;
					   			%>
					 		<%}else if(f.getFieldType().equals(Field.FieldType.HIDDEN.toString())){%>

							<%}else{
								request.setAttribute("field", f);
					    	  	Object formValue = null;
					    	  	if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
					    			CategoryAPI catAPI = APILocator.getCategoryAPI();
					    			List<Category> formCategoryList = new ArrayList<Category>();
					    			String[] formCategories = contentletForm.getCategories();
					    			if(UtilMethods.isSet(formCategories)){
					    				for(String catId : formCategories){
					    					formCategoryList.add(catAPI.find(catId,user,false));
					    				}
					    			}
									String fInode = f.getInode();
					    			try {
					    				Category category = catAPI.find(f.getValues(), user, false);
						    			if(category != null && catAPI.canUseCategory(category, user, false)) {
						    				catCounter++;
						    			}
					    			} catch(Exception e) {
					    				Logger.debug(this, "Error in CategoryAPI", e);
					    			}
					    			formValue = (List<Category>) formCategoryList;
					    	  	} else {
				    				formValue = (Object) contentletForm.getFieldValueByVar(f.getVelocityVarName());

                                    //We need to verify for the metadata field cached data
                                    if ( Contentlet.isMetadataFieldCached( contentletForm.getStructureInode(), f.getVelocityVarName(), formValue ) ) {

                                        /*
                                         If we populated the information using another language we should use the contentlet used
                                         for that language in order to get the cached metadata.
                                        */
                                        if ( !InodeUtils.isSet( request.getParameter( "inode" ) )
                                                && UtilMethods.isSet( request.getSession().getAttribute( "ContentletForm_lastLanguage" ) ) ) {

                                            if ( !InodeUtils.isSet( request.getParameter( "inode" ) )
                                                    && (UtilMethods.isSet( request.getParameter( "reuseLastLang" ) )
                                                    && Boolean.parseBoolean( request.getParameter( "reuseLastLang" ) )) ) {

                                                ContentletForm reusedForm = (ContentletForm) request.getSession().getAttribute( "ContentletForm_lastLanguage" );
                                                //Load its content from cache
                                                formValue = reusedForm.getFieldValueByVar( f.getVelocityVarName() );
                                            }
                                        }

                                    }
				    	  		}
				    	  		request.setAttribute("value", formValue);

					    	  	if (f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
					    			List<String> disabled = contentlet.getDisabledWysiwyg();
					    			if(InodeUtils.isSet(contentlet.getInode()) && disabled.contains(f.getFieldContentlet())) {
					    				request.setAttribute("wysiwygDisabled", true);
					    			} else {
					    				request.setAttribute("wysiwygDisabled", false);
					    			}
					    	  	}
					    	  	//http://jira.dotmarketing.net/browse/DOTCMS-3232
					    	  	if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
					    	  		if(InodeUtils.isSet(contentlet.getHost())) {
										request.setAttribute("host",contentlet.getHost());
										Identifier ident = APILocator.getIdentifierAPI().find(contentlet);
										Folder identFolder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), contentlet.getHost(), user, false);
										request.setAttribute("folder", identFolder.getInode());
					    	  		} else if(f.isRequired()) {
					    	  			String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
					    	  			String folderId = (String) request.getParameter("folder");
										request.setAttribute("host", hostId);
										request.setAttribute("folder", folderId);
					    	  		} else if(!f.isRequired()) {
					    	  			String hostId = (String) APILocator.getHostAPI().findSystemHost().getIdentifier();
										request.setAttribute("host", hostId);
										request.setAttribute("folder", null);
					    	  		}
						  	    }
					    	  	if (f.getFieldType().equals(Field.FieldType.BINARY.toString())) {
					    	  		if(InodeUtils.isSet(contentlet.getHost())) {
										request.setAttribute("host",contentlet.getHost());
					    	  		} else if(f.isRequired()) {
					    	  			String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
										request.setAttribute("host", hostId);
					    	  		} else if(!f.isRequired()) {
					    	  			String hostId = (String) APILocator.getHostAPI().findSystemHost().getIdentifier();
										request.setAttribute("host", hostId);
					    	  		}
					    	  	}
					    	  	request.setAttribute("inode",contentlet.getInode());
							request.setAttribute("counter", catCounter.toString());
					  	    %>

						<jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
					<%}%>
			   	<%}%>
			   	<div class="clear"></div>
			</div>
			<!-- END Right Column -->

	</div>
	<!-- END Contentlet Properties -->

	<!-- Relationships -->
	<% if(relationshipRecords != null && relationshipRecords.size() > 0 && !relationshipsTabFieldExists){
		   relationshipsTabFieldExists = true;
		   request.setAttribute("isRelationsihpAField",false); //DOTCMS-6893
	%>
		<div id="relationships" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Relationships") %>" onShow="hideEditButtonsRow()">
			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp" />
		</div>
	<%}%>

	<!-- Permissions -->
	<%if(!permissionsTabFieldExists && canEditAsset){%>
		<div id="permissions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
			<%
				HTMLPage permParent = (HTMLPage) InodeFactory.getInode(request.getParameter("htmlpage_inode"), HTMLPage.class);
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
				if(permParent != null)
					request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, permParent);
				else
					request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, structure);
			%>
			<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
		</div>
    <%}%>

    <%if(InodeUtils.isSet(contentlet.getInode())){ %>
		<!-- Versions Tab -->
		<%if(contentlet != null && InodeUtils.isSet(contentlet.getInode())){
				com.dotmarketing.portlets.contentlet.business.Contentlet fatty = new com.dotmarketing.portlets.contentlet.business.Contentlet();
				APILocator.getContentletAPI().convertContentletToFatContentlet(contentlet, fatty);
		 		request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, fatty);
		}%>

		<div id="versions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "History") %>" onShow="refreshVersionCp();hideEditButtonsRow();">
			<div id="contentletVersionsDiv" style="height:100%;">
			</div>

			<div>
			<%@ include file="/html/portlet/ext/common/edit_publishing_status_inc.jsp"%>
			</div>
		</div>



		<!-- References Tab -->
		<%if(references != null && references.size() > 0){ %>
			<div id="references" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "References") %>" onShow="hideEditButtonsRow()">
				<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_references.jsp" />
			</div>
		<%}%>
	<%}%>
	</div>

	<%@ include file="/html/portlet/ext/contentlet/edit_contentlet_js_inc.jsp" %>

	<!-- START Left Column -->
	 <div class="buttonRow-left lineRight" id="editContentletButtonRow">

		<%if (InodeUtils.isSet(structure.getInode())) {%>
			<%--If the user has permissions to publish--%>
			<%--A special case happens when the contentlet is new and CMS owner has permissions to publish --%>
			<%--Then the save and publish button should appear--%>

			<div class="gradient2">
				<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_basic_properties.jsp" />
			</div>

			<div class="gradient title"><%=LanguageUtil.get(pageContext, "Actions") %></div>
			<div id="contentletActionsHanger">
				<%@ include file="/html/portlet/ext/contentlet/contentlet_actions_inc.jsp" %>
			</div>
		<% } %>
		</div>

<!-- END Left Column -->

</liferay:box>




</html:form>

<%-- http://jira.dotmarketing.net/browse/DOTCMS-2273 --%>
<!-- To show lightbox effect "Saving Content.."  -->
<div id="savingContentDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "saving-content") %>" style="display: none;">
	<div id="maxSizeFileAlert" style="color:red; font-weight:bold; width: 200px; margin-bottom: 8px"></div>
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>
<script type="text/javascript">
	dojo.addOnLoad(function () {
		dojo.style(dijit.byId('savingContentDialog').closeButtonNode, 'visibility', 'hidden');
	});
</script>

<div id="saveContentErrors" style="display: none;" dojoType="dijit.Dialog">
	<div dojoType="dijit.layout.ContentPane" id="exceptionData" hasShadow="true"></div>
	<div class="formRow" style="text-align:center">
		<button dojoType="dijit.form.Button"  onClick="dijit.byId('saveContentErrors').hide()" type="button"><%= LanguageUtil.get(pageContext, "close") %></button>
	</div>
</div>





<%
	/*########################## BEGINNING  DOTCMS-2692 ###############################*/
    if(InodeUtils.isSet(request.getParameter("inode"))){
    	request.getSession().setAttribute("ContentletForm_lastLanguage",contentletForm);
    	request.getSession().setAttribute("ContentletForm_lastLanguage_permissions", contentlet);
    }
    if(!InodeUtils.isSet(request.getParameter("inode")) && UtilMethods.isSet(request.getSession().getAttribute("ContentletForm_lastLanguage"))){
	    if(!InodeUtils.isSet(request.getParameter("inode")) && (UtilMethods.isSet(request.getParameter("reuseLastLang"))
	    		&& Boolean.parseBoolean(request.getParameter("reuseLastLang")))){
	    	long newlanguage = contentletForm.getLanguageId();
	    	if(UtilMethods.isSet(request.getSession().getAttribute("ContentletForm_lastLanguage"))){
	    		contentletForm = (ContentletForm) request.getSession().getAttribute("ContentletForm_lastLanguage");
	    		contentletForm.setLanguageId(newlanguage);
	    		contentletForm.setInode("");
	    		request.setAttribute("ContentletForm", contentletForm);
	    	}
	    } else {
	    	LanguageAPI langAPI = APILocator.getLanguageAPI();
	    	Language prepopulateLanguage = langAPI.getLanguage( ((ContentletForm) request.getSession().getAttribute("ContentletForm_lastLanguage")).getLanguageId());
	    	String previousLanguage = prepopulateLanguage.getLanguage() + " - " + prepopulateLanguage.getCountry().trim();

	    	Map<String, String[]> params = new HashMap<String, String[]>();
	    	params.put("struts_action", new String[] { "/ext/contentlet/edit_contentlet" });
	    	params.put("cmd", new String[] { "edit" });

	    	if (request.getParameter("referer") != null) {
	    		params.put("referer", new String[] { request.getParameter("referer") });
	    	}

	    	// container inode
	    	if (request.getParameter("contentcontainer_inode") != null) {
	    		params.put("contentcontainer_inode", new String[] { request.getParameter("contentcontainer_inode") });
	    	}

	    	// html page inode
	    	if (request.getParameter("htmlpage_inode") != null) {
	    		params.put("htmlpage_inode", new String[] { request.getParameter("htmlpage_inode") });
	    	}

	    	if (InodeUtils.isSet(contentlet.getInode())) {
	    		params.put("sibbling", new String[] { contentlet.getInode() + "" });
	    	} else {
	    		params.put("sibbling", new String[] { (request.getParameter("sibbling") != null) ? request
	    		.getParameter("sibbling") : "" });
	    	}

	    	if (InodeUtils.isSet(contentlet.getInode())) {


	    		params.put("sibblingStructure", new String[] { ""+structure.getInode() });
	    	}else if(InodeUtils.isSet(request.getParameter("selectedStructure"))){
	    		params.put("sibblingStructure", new String[] { request.getParameter("selectedStructure")});

	    	}else if(InodeUtils.isSet(request.getParameter("sibblingStructure"))){
	    		params.put("sibblingStructure", new String[] { request.getParameter("sibblingStructure")});
	    	} else {
	    		params.put("sibblingStructure", new String[] { (request.getParameter("selectedStructureFake") != null) ? request
	    		.getParameter("selectedStructureFake") : "" });
	    	}

	    	String editURL = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED
	    	.toString(), params)+"&inode=&lang="+ contentletForm.getLanguageId()+ "&reuseLastLang=true&populateaccept=true";

	    	%>





		<script type="text/javascript">
			 function runpopulate(){
		      	window.location="<%=editURL%>";
		      	dijit.byId('populateDialog').hide();
		     }
			 dojo.addOnLoad(function () { dijit.byId('populateDialog').show(); });
		</script>

        <div dojoType="dijit.Dialog" id="populateDialog" title='<%=LanguageUtil.get(pageContext, "Populate-Confirmation") %>' style="display: none">
        <table>
	        <tr>
	        	<%
	        		long newlanguage = contentletForm.getLanguageId();
		    		Language newLang=langAPI.getLanguage(contentletForm.getLanguageId());
	    			String newLanguageName=newLang.getLanguage() + " - " + newLang.getCountry().trim();
	        		String message = LanguageUtil.get(pageContext, "Populate-the-new-language-content-with-previous-language-content");
	        		message = LanguageUtil.format(pageContext, "Populate-the-new-language-content-with-previous-language-content",new String[]{newLanguageName,previousLanguage},false);
	        	%>
		        <td colspan="2" align="center"><%= message %></td>
		    </tr>
		    <tr>
		        <td colspan="2" align="center">
		        <button dojoType="dijit.form.Button" onClick="runpopulate();" type="button"><%= LanguageUtil.get(pageContext, "Yes") %></button>
		        &nbsp; &nbsp;
		        <button dojoType="dijit.form.Button" onClick="dijit.byId('populateDialog').hide();" type="button"><%= LanguageUtil.get(pageContext, "No") %></button>
		        </td>
	        </tr>
        </table>
        </div>

	<%}
    }
	/*########################## END  DOTCMS-2692 ###############################*/


%>

