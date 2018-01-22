<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.calendar.struts.EventForm"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.util.InodeUtils" %>
<%@page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Role"%>

<%
	//this file is a copy of the edit_contentlet.jsp that has
	//some modifications to support events custom fields and actions
 %>

<script type='text/javascript' src='/dwr/interface/LanguageAjax.js'></script>
<script type='text/javascript' src='/html/js/scriptaculous/prototype.js'></script>

<%
	PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	ContentletAPI conAPI = APILocator.getContentletAPI();

	Contentlet contentlet = request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT) != null ?(Contentlet) request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT) :	conAPI.find(request.getParameter("inode"),user,false);

	Date dateOfStart = contentlet.getDateProperty("startDate");

	Date dateOfEnd = contentlet.getDateProperty("endDate");

	EventForm contentletForm = (EventForm) request.getAttribute("CalendarEventForm");

	//Content structure or user selected structure
	Structure structure = contentletForm.getStructure();
	if (structure!=null && !InodeUtils.isSet(structure.getInode())){
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
	request.setAttribute("contentletForm", contentletForm);
	request.setAttribute("structure", structure);
	request.setAttribute("selectedCategories", selectedCategories);
	request.setAttribute("relationshipRecords", relationshipRecords);
	request.setAttribute("references", references);
	request.setAttribute("referer", referer);
	request.setAttribute("fields", fields);


	boolean canEditAsset = conPerAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	Integer catCounter = 0;

	boolean contentEditable = (UtilMethods.isSet(contentlet.getInode())?(Boolean)request.getAttribute(com.dotmarketing.util.WebKeys.CONTENT_EDITABLE):false);
	boolean canUserPublishContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_PUBLISH,user);

	if(!InodeUtils.isSet(contentlet.getInode())) {
		canUserPublishContentlet = conPerAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_PUBLISH,user);
		//Set roles = conPerAPI.getPublishRoles();
		if(!canUserPublishContentlet){
			canUserPublishContentlet = conPerAPI.doesRoleHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole());
		}
	}
	request.setAttribute("canUserPublishContentlet", new Boolean(canUserPublishContentlet));
	String copyOptions = ((String) request.getParameter("copyOptions"))==null?"":(String) request.getParameter("copyOptions");

%>

<!-- global included dependencies -->


<script language='javascript' type='text/javascript'>
var editButtonRow="editEventButtonRow";
</script>

<jsp:include page="/html/portlet/ext/contentlet/field/edit_field_js.jsp" />

<style media="all" type="text/css">
	/* @import url(/html/portlet/ext/contentlet/edit_contentlet.css);
	@import url(/html/css/widget.css);
	@import url(/html/portlet/ext/contentlet/field/edit_field.css);
	@import url(/html/portlet/ext/calendar/edit_event.css);*/
</style>

<html:form action="<%= formAction %>" styleId="fm">
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
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-event\") %>" />
	<div dojoAttachPoint="cmsFileBrowserImage" currentView="thumbnails" jsId="cmsFileBrowserImage" onFileSelected="addFileImageCallback" mimeTypes="image" dojoType="dotcms.dijit.FileBrowserDialog"></div>
	<div dojoAttachPoint="cmsFileBrowserFile" currentView="list" jsId="cmsFileBrowserFile" onFileSelected="addFileCallback" dojoType="dotcms.dijit.FileBrowserDialog"></div>

	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" class="content-edit__main">

		<!--  Contentlet structure fields -->
		<% if(fields != null && fields.size()>0 &&  fields.get(0) != null && fields.get(0).getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
				Field f0 = fields.get(0);
				fields.remove(0);
		%>
			<div id="<%=f0.getFieldContentlet()%>" dojoType="dijit.layout.ContentPane" title="<%=f0.getFieldName()%>">
		<% } else { %>
			<div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Content") %>">
		<% } %>

		<!-- START Right Column -->
		<div class="content-edit__form">
		<%
		/*### DRAW THE DYNAMIC FIELDS ###*/

		int counter = 0;
		boolean tabDividerOpen  = false;
		boolean categoriesTabFieldExists = false;
		boolean permissionsTabFieldExists = false;
		boolean relationshipsTabFieldExists = false;

		/* Events code only */
			Field startDateField = null;
			Field endDateField = null;
			Field locationField = new Field();
		/* End of Events code only */

   		for (Field f : fields) {
			if("hidden".equals(f.getFieldType())){
				continue;
			}
    		if(f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())) { %>
				<div class="lineDividerTitle"><%=f.getFieldName() %></div>
   			<% } else if(f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
    			tabDividerOpen = true;%>
					</div>
				</div>
				<div id="<%=f.getVelocityVarName()%>" dojoType="dijit.layout.ContentPane" title="<%=f.getFieldName()%>">
					<div class="wrapperRight" style="height:100%;">
						<div style="height:20px;"></div>
			<% } else if(f.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
   	    		categoriesTabFieldExists = true;%>
 				<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_categories.jsp" />
			<% } else if(f.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString())){
    	  		permissionsTabFieldExists = true;
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, structure);%>
				<%@include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
			<% } else if(f.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())){%>
    	   		<%if(counter==0){%>
					<% relationshipsTabFieldExists =  true; %>
                    <jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp" />
				<%}%>
    	    	<%counter++;%>
			<% } else  {
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
    				formValue =  (List<Category>) formCategoryList;

    				try {
	    				Category category = catAPI.find(f.getValues(), user, false);
		    			if(category != null && catAPI.canUseCategory(category, user, false)) {
		    				catCounter++;
		    			}
	    			} catch(Exception e) {
	    				Logger.debug(this, "Error in CategoryAPI", e);
	    			}
    	  		} else {
    				formValue = (Object) contentletForm.getFieldValueByVar(f.getVelocityVarName());
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

    	  		/* Calendar special fields */
				if(f.getVelocityVarName().equals("startDate")) {
					startDateField = f;%>
					<jsp:include page="/html/portlet/ext/calendar/edit_event_start_date_field.jsp" />
				<%} else if (f.getVelocityVarName().equals("location")) {
					locationField = f;%>
					<jsp:include page="/html/portlet/ext/calendar/edit_event_location_field.jsp" />
				<%	} else { /* END Calendar special fields */
	    	  		if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
	    	  			if(InodeUtils.isSet(contentlet.getHost())) {
							request.setAttribute("host",contentlet.getHost());
							request.setAttribute("folder",contentlet.getFolder());
	    	  			} else {
	    	  				String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
							request.setAttribute("host", hostId);
							request.setAttribute("folder", null);
	    	  			}
		  	   		 }
		  	   		request.setAttribute("inode",contentlet.getInode());
		  	   		request.setAttribute("counter", catCounter.toString());
		  	   		%>
						<jsp:include page="/html/portlet/ext/contentlet/field/edit_field.jsp" />
						<%-- END DATE is followed by the recurrance jsp --%>
						<%if(f.getVelocityVarName().equals("endDate")) {
							endDateField = f;%>
							<%@ include file="/html/portlet/ext/calendar/edit_event_recurrence_inc.jsp" %>
						<%}%>
					<%}%>
				<%}%>
   			<%}%>
			<%@include file="/html/portlet/ext/calendar/edit_event_js_inc.jsp" %>
		</div>
		<!-- END Right Column -->
	</div>
	<!-- END Contentlet Tab -->

	<!-- Contentlet categories Tab -->
	<% if(categoriesTabFieldExists){ %>
		<div id="categories" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Categories") %>">
			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_categories.jsp" />
		</div>
	<% } %>
	<!-- END Contentlet categories Tab -->

	<!-- Relationships Tab -->
    <% if(relationshipRecords != null && relationshipRecords.size() > 0 && !relationshipsTabFieldExists){
        relationshipsTabFieldExists = true;%>
		<div id="relationships" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Relationships") %>">
			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_relationships.jsp" />
		</div>
	<% } %>
	<!-- Relationships Tab -->

	<!-- Permissions Tab -->
	<% if(!permissionsTabFieldExists && canEditAsset){ %>
		<div id="permissions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
			<%
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, structure);
			%>
			<%@include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
		</div>
    <% } %>
	<!-- END Permissions Tab -->

	<!-- Versions Tab -->
	<div id="versions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>">
		<%@include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>
	</div>
	<!-- END Versions Tab -->

	<!-- References Tab -->
	<%if(references != null && references.size() > 0){ %>
		<div id="references" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "References") %>">
			<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_references.jsp" />
		</div>
	<%}%>
	<!-- END References Tab -->
</div>

<!--  action buttons -->
<% if (InodeUtils.isSet(structure.getInode())) { %>
	<!-- START Left Column -->
	<div class="content-edit__sidebar" id="editEventButtonRow">
		<jsp:include page="/html/portlet/ext/contentlet/edit_contentlet_basic_properties.jsp" />

		<div class="content-edit-actions">
			<%--<h3><%=LanguageUtil.get(pageContext, "Actions") %></h3>--%>
			<div id="contentletActionsHanger">
				<%@ include file="/html/portlet/ext/contentlet/contentlet_actions_inc.jsp" %>
			</div>
		</div>
	</div>
<% } %>

	<%@include file="/html/portlet/ext/contentlet/edit_contentlet_js_inc.jsp" %>

</liferay:box>

</html:form>

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
	    		contentletForm = (EventForm) request.getSession().getAttribute("ContentletForm_lastLanguage");
	    		contentletForm.setLanguageId(newlanguage);
	    		contentletForm.setInode("");
	    		request.setAttribute("ContentletForm", contentletForm);
	    	}
	    } else {
	    	LanguageAPI langAPI = APILocator.getLanguageAPI();
	    	Language prepopulateLanguage = langAPI.getLanguage( ((ContentletForm) request.getSession().getAttribute("ContentletForm_lastLanguage")).getLanguageId());
	    	String previousLanguage = prepopulateLanguage.getLanguage() + " - " + prepopulateLanguage.getCountry().trim();
	    	String reUseInode = ((Contentlet) request.getSession().getAttribute("ContentletForm_lastLanguage_permissions")).getInode();

	    	Map<String, String[]> params = new HashMap<String, String[]>();
	    	params.put("struts_action", new String[] { "/ext/calendar/edit_event" });
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
	    	.toString(), params)+"&inode=&lang="+ contentletForm.getLanguageId()+ "&reuseLastLang=true&populateaccept=true&reUseInode="+reUseInode;

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
    }
	/*########################## END  DOTCMS-2692 ###############################*/


%>
