<%!



	public boolean isFullScreenField(com.dotcms.contenttype.model.field.Field field) {
		return APILocator.getContentTypeFieldAPI().isFullScreenField(field);
	}


	public boolean isFullScreenField(Field oldField) {

		try {

			return isFullScreenField(LegacyFieldTransformer.from(oldField));
		} catch (Exception e) {
			return false;
		}


	}
	public boolean isNextFieldFullScreen(Structure structure, Field oldField) {

		try{
			ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(structure.getInode());
			com.dotcms.contenttype.model.field.Field fieldIn = LegacyFieldTransformer.from(oldField);
			com.dotcms.contenttype.model.field.Field field = type.fields().subList(type.fields().indexOf(fieldIn), type.fields().size()).stream().filter(f->!(f instanceof RowField || f instanceof ColumnField || f instanceof TabDividerField)).findFirst().get();
			return isFullScreenField(field);
		}
		catch(Exception e){
			return false;
		}


	}

%>

<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotcms.contenttype.transform.field.LegacyFieldTransformer"%>
<%@page import="com.dotmarketing.business.LayoutAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.rendering.velocity.viewtools.CategoriesWebAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@ page import="com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage"%>
<%@ page import="com.dotmarketing.db.DbConnectionFactory" %>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="com.dotcms.contenttype.model.field.*" %>
<%@ page import="io.vavr.control.Try" %>
<%@ page import="com.dotcms.contenttype.transform.contenttype.StructureTransformer" %>
<%@ page import="org.apache.poi.ss.usermodel.Row" %>
<%@ page import="com.dotcms.contenttype.transform.field.FieldTransformer" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="com.dotmarketing.util.ConfigUtils" %>
<!DOCTYPE html>
<script type='text/javascript' src='/dwr/interface/LanguageAjax.js'></script>

<!--  dotCMS Block Editor Builder -->
	<script src="/dotcms-block-editor/polyfills.js" type="module"></script>
	<script src="/dotcms-block-editor/generator-runtime.js" defer></script>
	<script src="/dotcms-block-editor/main.js" type="module"></script>
<!--   End dotCMS Block Editor -->

<style>
.dijitTree {
    width: 100% !important;
    height: 100%;
}
.classAce{
  display: none;
}

</style>

<%
	if (ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_NEW_BINARY_FIELD")) {
%>

<!--  dotCMS Binary Field Builder -->
	<script src="/dotcms-binary-field-builder/polyfills.js" type="module"></script>
	<script src="/dotcms-binary-field-builder/generator-runtime.js" defer></script>
	<script src="/dotcms-binary-field-builder/main.js" type="module"></script>
<!--  dotCMS End Binary Field Builder -->

<% } %>

<script type="text/javascript">
	const relationsLoadedMap = {};

	function waitForRelation() {
		return new Promise((resolve) => {
			const observer = new MutationObserver((mutations) => {
				if (allRelationsHaveLoad()) {
					resolve(true);
					observer.disconnect();
				}
			});

			observer.observe(document.body, {
				childList: true,
				attributes: true,
				characterData: true,
				subtree: true
			});
		});
	}

	function allRelationsHaveLoad() {
		// Check all the Relation fields exist.
		return !(Object.values(relationsLoadedMap).filter((loaded) => !loaded).length);
	}
</script>

<script language="javascript">
	require(["vs/editor/editor.main"], function() {
		// Hack to avoid MonacoEditorLoaderService to load the editor again
		// That service not works in `dojo` environment Dojo amdLoader. See docs: [https://dojotoolkit.org/reference-guide/1.7/loader/amd.html?highlight=packages%20location%20name%20main]
		window.monacoEditorAlreadyInitialized = !!window.monaco;
	});
</script>

<%
	PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	ContentletAPI conAPI = APILocator.getContentletAPI();
	Contentlet contentlet = (Contentlet) request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT);
	String inode=request.getParameter("inode") != null ? request.getParameter("inode") :  (String) request.getAttribute("inode");

	contentlet = (contentlet != null) ? contentlet : (inode!=null) ? conAPI.find(inode,user,false) : new Contentlet();

	ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
	if(contentletForm==null)contentletForm=new ContentletForm();
	String copyOptions = ((String) request.getParameter("copyOptions"))==null?"":(String) request.getParameter("copyOptions");
	if(copyOptions == ""){
		copyOptions = ((String) request.getParameter("_copyOptions"))==null?"":(String) request.getParameter("_copyOptions");
	}
	//Content structure or user selected structure

	Structure structure = contentletForm.getStructure();
	if(structure==null){
	    structure=new StructureTransformer( APILocator.getContentTypeAPI(user).findDefault()).asStructure();
	}
	// if host, set this to the current viewing host
	if(structure!= null && UtilMethods.isSet(structure.getInode()) && structure.getVelocityVarName().equals("Host")) {
		if(contentlet != null && UtilMethods.isSet(contentlet.getIdentifier()))
				session.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,contentlet.getIdentifier() );

	}

	boolean canUserPublishContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_PUBLISH,user, PageMode.get(request).respectAnonPerms);

	if(!InodeUtils.isSet(contentlet.getInode())) {
		canUserPublishContentlet = conPerAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_PUBLISH,user, PageMode.get(request).respectAnonPerms);
		//Set roles = conPerAPI.getPublishRoles();
		if(!canUserPublishContentlet){
			canUserPublishContentlet = conPerAPI.doesRoleHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole());
		}
	}
	request.setAttribute("canUserPublishContentlet", new Boolean(canUserPublishContentlet));

	if (!InodeUtils.isSet(structure.getInode())){
		structure = StructureFactory.getStructureByInode(request.getParameter("sibblingStructure"));
	}


	List<Field> fields = new ArrayList<>(structure.getFields());

	//Categories
	String[] selectedCategories = contentletForm.getCategories ();

	//Contentlet relationships
	ContentletRelationships contentletRelationships = (ContentletRelationships)
		request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_RELATIONSHIPS_EDIT);

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
	if (request.getHeader("referer") != null &&
	                (request.getHeader("referer").contains("baseType") ||
	                                request.getHeader("referer").contains("structure_id"))){
	    referer = request.getHeader("referer");
	}
	else if (request.getParameter("referer") != null) {
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

	List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = (contentletRelationships==null) ? new ArrayList<ContentletRelationships.ContentletRelationshipRecords>() : contentletRelationships.getRelationshipsRecords();
	List<ContentletRelationships.ContentletRelationshipRecords> legacyRelationshipRecords = contentletRelationships.getLegacyRelationshipsRecords();

	request.setAttribute("references", references);
	request.setAttribute("referer", referer);
	request.setAttribute("fields", fields);

	request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
	request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, structure);

	boolean fullScreenField  = isNextFieldFullScreen(structure, fields.get(0));
	String fullScreenClass= fullScreenField ? "edit-content-full-screen": "";
	boolean fullScreenNextField = isNextFieldFullScreen(structure, fields.get(0));
	String fullScreenNextClass= fullScreenNextField ? "edit-content-full-screen": "";


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

	boolean canEditAsset = conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, PageMode.get(request).respectAnonPerms);
	final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
    boolean canSeeRules = layoutAPI.doesUserHaveAccessToPortlet("rules", user)
            && conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_USE, user, PageMode.get(request).respectAnonPerms)
            && conPerAPI.doesUserHavePermissions(contentlet.getParentPermissionable(), "RULES: " + PermissionAPI.PERMISSION_USE, user, PageMode.get(request).respectAnonPerms);

    boolean hasViewPermision = layoutAPI.doesUserHaveAccessToPortlet("permissions", user)
            && conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_USE, user, PageMode.get(request).respectAnonPerms)
            && conPerAPI.doesUserHavePermissions(contentlet.getParentPermissionable(), "PERMISSIONS: " + PermissionAPI.PERMISSION_USE, user, PageMode.get(request).respectAnonPerms);

    Boolean isContentEditable = (Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.CONTENT_EDITABLE);
	isContentEditable = isContentEditable != null ? isContentEditable : false;
	boolean contentEditable = (UtilMethods.isSet(contentlet.getInode()) ? isContentEditable : false);

    Integer catCounter = 0;

	String targetFrame="_top";
	boolean isAngularFrame = UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME));
	if(isAngularFrame){
	   targetFrame = (String)request.getSession().getAttribute(WebKeys.FRAME);
	}

    boolean isLocked=(request.getParameter("sibbling") != null) ? false : contentlet.isLocked();

%>

<!-- global included dependencies -->


<%@ include file="/html/portlet/ext/contentlet/field/edit_field_js.jsp" %>


<html:form action="<%= formAction %>" styleId="fm" target="<%= targetFrame %>" onsubmit="return false;">
	<input name="wfActionAssign" id="wfActionAssign" type="hidden" value="">
	<input name="wfActionComments" id="wfActionComments" type="hidden" value="">
	<input name="wfActionId" id="wfActionId" type="hidden" value="">
	<input name="wfPathToMove" id="wfPathToMove" type="hidden" value="">

	<!-- PUSH PUBLISHING ACTIONLET -->
	<input name="wfPublishDate" id="wfPublishDate" type="hidden" value="">
	<input name="wfPublishTime" id="wfPublishTime" type="hidden" value="">
	<input name="wfExpireDate" id="wfExpireDate" type="hidden" value="">
	<input name="wfExpireTime" id="wfExpireTime" type="hidden" value="">
	<input name="wfNeverExpire" id="wfNeverExpire" type="hidden" value="">
	<input name="whereToSend" id="wfWhereToSend" type="hidden" value="">
	<input name="wfiWantTo" id="wfiWantTo" type="hidden" value="">
	<input name="wfFilterKey" id="wfFilterKey" type="hidden" value="">
	<input name="wfTimezoneId" id="wfTimezoneId" type="hidden" value="">

	<div dojoAttachPoint="cmsFileBrowserImage" currentView="thumbnails" jsId="cmsFileBrowserImage" onFileSelected="addFileImageCallback" mimeTypes="image" sortBy="modDate" sortByDesc="true" dojoType="dotcms.dijit.FileBrowserDialog"></div>
	<div dojoAttachPoint="cmsFileBrowserFile" currentView="list" jsId="cmsFileBrowserFile" onFileSelected="addFileCallback" dojoType="dotcms.dijit.FileBrowserDialog"></div>



	<!--  START TABS -->
	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" class="content-edit__main">
		<!--  IF THE FIRST FIELD IS A TAB-->
        <% if(fields != null &&
			fields.size()>0 &&
			fields.get(0) != null &&
			fields.get(0).getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
				Field f0 = fields.get(0);
				fields.remove(0);%>
			<div id="tab_<%=f0.getVelocityVarName()%>" dojoType="dijit.layout.ContentPane" title="<%=f0.getFieldName()%>">
		<%} else {	%>
			<div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Content") %>">
		<%}%>

        <!-- START EDIT CONTENT FORM -->
        <div  class="content-edit__form <%=fullScreenNextClass%>" >
            <% if(widgetUsageField != null && UtilMethods.isSet(widgetUsageField.getValues())){ %>
                <div class="fieldWrapper">
                    <div class="fieldName">
                        <%=widgetUsageField.getFieldName()%>
                    </div>
                    <div class="fieldValue <%=fullScreenClass%>">
                        <%
                        String textValue = widgetUsageField.getValues();
                        textValue = textValue.replaceAll("&", "&amp;");
                        textValue =  UtilMethods.htmlLineBreak(textValue);
                        %>
                        <%=textValue %>
                    </div>
                </div>
            <% } %>

			<div>
				<span class="editcontentlet__col">

            <%-- Begin Looping over fields --%>
            <%
            	boolean legacyContenTType = fields.size() == 0 ||   !fields.get(0).getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString());
            	boolean fieldSetOpen = false;
                int fieldCounter =0;
                int i = legacyContenTType ? 0 : 2;
                boolean rowOpen = true;
                boolean columnOpen = true;


                for (; i < fields.size(); i++) {

                    Field f = fields.get(i);
                    com.dotcms.contenttype.model.field.Field newField = new LegacyFieldTransformer(f).from();
					fullScreenField = isFullScreenField(f);
					fullScreenNextField = isNextFieldFullScreen(structure, f);

					fullScreenClass=fullScreenField ? "edit-content-full-screen": "";
					fullScreenNextClass=fullScreenNextField ? "edit-content-full-screen": "";
					request.setAttribute("DOT_FULL_SCREEN_FIELD",fullScreenField );
					request.setAttribute("DOT_FULL_SCREEN_NEXT_FIELD",fullScreenNextField );







                    if (fieldSetOpen &&
                        (f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
                         f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()) )) {
                        fieldSetOpen = false;%>
                    <%}%>

					<%if(newField instanceof RowField){%>
						<%if(rowOpen){%>
							</div>
						<%}
							rowOpen = true;
						%>

						<div class="editcontentlet__row">
                    <%} else if(newField instanceof ColumnField){%>
						<%if(columnOpen){%>
							</span>
						<%}
							columnOpen = true;
						%>

						<span class="editcontentlet__col">
                    <%} else if(f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())) {%>
                        <div class="lineDividerTitle"><%=f.getFieldName() %></div>
                    <%}else if(f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
						tabDividerOpen = true;
					%>
							<%if (rowOpen) {%>
                        			</span> <!--Closing column-->
								</div> <!--Closing row-->
							<%
								rowOpen= false;
								columnOpen= false;
							}%>
							</div>
						</div>

                        <div id="tab_<%=f.getVelocityVarName()%>" class="custom-tab" dojoType="dijit.layout.ContentPane" title="<%=f.getFieldName()%>">
                            <div class="content-edit__advaced-form <%=fullScreenNextClass%> <%=fullScreenClass%>">

                    <%}else if(f.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()) && !categoriesTabFieldExists) {
                        categoriesTabFieldExists = true;%>
                        <jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_categories.jsp" />
                    <%}else if(f.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString()) && !permissionsTabFieldExists){
                        permissionsTabFieldExists = true;%>
                        <%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
                    <%}else if(f.getFieldType().equals(Field.FieldType.RELATIONSHIP.toString()) || f.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())){%>
                        <% if(fieldCounter==0){

                            if (f.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())){
                                relationshipsTabFieldExists =  true;
                                request.setAttribute("isRelationsihpAField",true); //DOTCMS-6893
                            }%>

                            <div class="fieldName">
                                <% if(f.isRequired()) {%>
                                    <label class="required">
                            		<%} else {%>
                            			<label>
                            		<% } %>
                                <%=f.getFieldName()%></label>
                            </div>
								<div class="fieldValue" style="overflow-x: scroll">
                                <%
                                    if(f.getFieldType().equals(Field.FieldType.RELATIONSHIP.toString())){
                                        //field on the other side of the relationship
                                        request.setAttribute("relationshipRecords", contentletRelationships.getRelationshipsRecordsByField(f));
										request.setAttribute("relatedField", newField);
								%>
                                        <jsp:include page="/html/portlet/ext/contentlet/field/relationship_field.jsp"/>
                                <%  } else {

                                        request.setAttribute("relationshipRecords", legacyRelationshipRecords); %>
                                        <jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp"/>
                                <%  }
                                %>

                            </div>
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

                        }

						if (newField instanceof StoryBlockField) {
							if (UtilMethods.isSet(formValue)) {
								formValue = APILocator.getStoryBlockAPI().refreshStoryBlockValueReferences(formValue, contentlet.getIdentifier()).getValue();
							}
						}

                        request.setAttribute("value", formValue);

                        if (f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
                            List<String> disabled = contentlet.getDisabledWysiwyg();
                            if(InodeUtils.isSet(contentlet.getInode()) && disabled!=null && disabled.contains(f.getFieldContentlet())) {
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
                                Host host = APILocator.getHostAPI().findSystemHost();

                                String hostId = host.getIdentifier();
                                if (!conPerAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, PageMode.get(request).respectAnonPerms)) {
                                    hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
                                }

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

                    <%if(contentlet.isCalendarEvent()){
						final String velocityVarName = f.getVelocityVarName();

						if ("startDate".equals(velocityVarName) || "endDate".equals(velocityVarName)) {

								if ("startDate".equals(velocityVarName)) {
									startDateField = f;
									%>

									<%if(contentlet.isNew()){%>
									   <jsp:include page="/html/portlet/ext/calendar/edit_event_start_date_field.jsp" />
									<%}else{%>
									   <jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
									<%}%>
									<%
								}

								if ("endDate".equals(velocityVarName)) {
									endDateField = f;
									%>
								    <jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
									<%
								}
                                if (startDateField != null && endDateField != null){%>
                                    <%@ include file="/html/portlet/ext/calendar/edit_event_recurrence_inc.jsp" %>
                                <%}
						} else {
								%>
								<jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
								<%
						}
                    } else if (!f.getFieldType().equals(Field.FieldType.CONSTANT.toString())) { %>
						<jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
					<% } %>

                <%}%>
            <%}
			if (rowOpen) {
			%>
				</span>
			</div>
			<%}%>
        </div>
        <!-- END START EDIT CONTENT FORM -->
	</div>
	<!-- END TABS -->

    <%if(contentlet.isCalendarEvent()){%>
	  <%@include file="/html/portlet/ext/calendar/edit_event_js_inc.jsp" %>
	<%}%>

	<!-- Relationships -->
	<% if(legacyRelationshipRecords != null && legacyRelationshipRecords.size() > 0 && !relationshipsTabFieldExists){
		   relationshipsTabFieldExists = true;
		   request.setAttribute("isRelationsihpAField",false); //DOTCMS-6893
		   request.setAttribute("relationshipRecords", legacyRelationshipRecords);
		   request.removeAttribute("fieldRelationType");
	%>
		<div id="relationships" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Relationships") %>">
			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp" />
		</div>
	<%}%>


	<!-- -Rules -->
	<% if (canSeeRules) { %>
	   	<%if(InodeUtils.isSet(contentlet.getInode()) && contentlet.getStructure()!=null ){ %>
	   		<%if(contentlet.isHost() || contentlet.getStructure().isHTMLPageAsset()){ %>
				<div id="rulez" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Rules") %>" onShow="refreshRulesCp()">
					<div id="contentletRulezDiv" class="rules__tab-container" style="height:100%;">
					</div>
				</div>
			<%} %>
		<%} %>
	<% } %>


    <!-- Permissions -->
    <% if (hasViewPermision) { %>
		<div id="permissionsTab" disabled="<%=!UtilMethods.isSet(contentlet.getInode()) %>" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="refreshPermissionsTab()">
			<div id="permissionsTabDiv">
                <%-- This loads the edit_permission_tab_inc_wrapper.jsp passing in the contentletId as a request parameter --%>
			</div>
        </div>
    <% } %>



		<!-- Versions Tab -->
		<%---if(contentlet != null && InodeUtils.isSet(contentlet.getInode())){
				com.dotmarketing.portlets.contentlet.business.Contentlet fatty = new com.dotmarketing.portlets.contentlet.business.Contentlet();
				APILocator.getContentletAPI().convertContentletToFatContentlet(contentlet, fatty);
		 		request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, fatty);
		}--%>

		<div id="versionsTab" disabled="<%=!UtilMethods.isSet(contentlet.getInode()) %>" class="history" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "History") %>" onShow="refreshVersionCp();">
			<div id="contentletVersionsDiv" style="height:100%;" class="content-edit__history-version">
			</div>
			<hr class="history__divider">
			<div class="history__status">
			<%@ include file="/html/portlet/ext/common/edit_publishing_status_inc.jsp"%>
			</div>
		</div>

		<!-- References Tab -->
		<%if(references != null && references.size() > 0){ %>
			<div id="references" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "References") %>">
				<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_references.jsp" />
			</div>
		<%}%>

	</div>

		<!-- START CONTENT ACTIONS -->
		<div class="content-edit__sidebar" id="editContentletButtonRow">
			<%if (InodeUtils.isSet(structure.getInode())) {%>
			<%--If the user has permissions to publish--%>
			<%--A special case happens when the contentlet is new and CMS owner has permissions to publish --%>
			<%--Then the save and publish button should appear--%>

			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_basic_properties.jsp" />



				<div id="contentletActionsHanger">
					<%@ include file="/html/portlet/ext/contentlet/contentlet_actions_inc.jsp" %>
				</div>




			<% } %>
		</div>
		<!-- END CONTENT ACTIONS -->

	<%@ include file="/html/portlet/ext/contentlet/edit_contentlet_js_inc.jsp" %>



</html:form>

<%-- http://jira.dotmarketing.net/browse/DOTCMS-2273 --%>

<div id="savingContentDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "saving-content") %>" style="display: none;" onclick="dijit.byId('savingContentDialog').hide()">
	<div id="maxSizeFileAlert" style="color:red; font-weight:bold; width: 200px; margin-bottom: 8px"></div>
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>
<script type="text/javascript">
	dojo.addOnLoad(function () {
		dojo.style(dijit.byId('savingContentDialog').closeButtonNode, 'visibility', 'hidden');

        var tab = dijit.byId("mainTabContainer");
 		dojo.connect(tab, 'selectChild', function (evt) {
            selectedTab = tab.selectedChildWidget;

            var isCustomTab = false;
            if (selectedTab.class != undefined && selectedTab.class == "custom-tab") {
                isCustomTab = true;
            }
        });

<%
    final String titleFieldValue = (contentlet != null ? contentlet.getTitle() : "").replace("'", "\'");
%>
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-contentlet-loaded",
            data: {
                contentType: '<%=CacheLocator.getContentTypeCache().getStructureByInode(structure.getInode() ).getName()%>',
                pageTitle: "<%=titleFieldValue%>"
            }
        });
        setTimeout(function() {
            document.dispatchEvent(customEvent);
        }, 2000);

        var resourceLink = dojo.byId('resourceLink');
        if(resourceLink){
            var name = resourceLink.text;
            name = shortenString(name, 100);
            resourceLink.text = name;
        }

	});

	var onBeforeUnloadHandle = dojo.connect(dijit.byId('mainTabContainer'), "onkeypress", activateOnBeforeUnload);
	function activateOnBeforeUnload(){
		window.onbeforeunload=function(){return "";};
		dojo.disconnect(onBeforeUnloadHandle);
	}


</script>

<div id="saveContentErrors" style="display: none;" dojoType="dijit.Dialog" class="content-edit__dialog-error" title="<%= LanguageUtil.get(pageContext, "error") %>">
	<div dojoType="dijit.layout.ContentPane" id="exceptionData" hasShadow="true"></div>
	<div class="content-edit__dialog-error-actions">
		<button dojoType="dijit.form.Button" class="dijitButtonFlat" onClick="dijit.byId('saveContentErrors').hide()" type="button"><%= LanguageUtil.get(pageContext, "close") %></button>
	</div>
</div>


<%
String sib = request.getParameter("sibbling");
String populateaccept = request.getParameter("populateaccept");

if(!InodeUtils.isSet(inode) && UtilMethods.isSet(sib) && !UtilMethods.isSet(populateaccept)){
	// Sibbling content
	Contentlet sibbling=conAPI.find(sib, user,false);
	Language previousLanguage = APILocator.getLanguageAPI().getLanguage(sibbling.getLanguageId());
	Language newLanguage=APILocator.getLanguageAPI().getLanguage(contentletForm.getLanguageId());

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
			 dojo.addOnLoad(function () {
				dijit.byId('populateDialog').show();
             });
		</script>

        <div dojoType="dijit.Dialog" id="populateDialog" title='<%=LanguageUtil.get(pageContext, "Populate-Confirmation") %>' style="display: none">
        <table>
	        <tr>
	        	<%

	        	    String previousLanguageName     =previousLanguage.getLanguage() + " - " + previousLanguage.getCountry();
	    			String newLanguageName          =newLanguage.getLanguage() + " - " + newLanguage.getCountry();
	        		String message = LanguageUtil.get(pageContext, "Populate-the-new-language-content-with-previous-language-content");
	        		message = LanguageUtil.format(pageContext, "Populate-the-new-language-content-with-previous-language-content",new String[]{newLanguageName,previousLanguageName},false);
	        	%>
		        <td colspan="2" align="center"><%= message %></td>
		    </tr>
			<tr>
				<td>&nbsp;</td>
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

	/*########################## END  DOTCMS-2692 ###############################*/


%>
