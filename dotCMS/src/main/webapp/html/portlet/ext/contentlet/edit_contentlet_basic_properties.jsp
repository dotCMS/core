<%@page import="com.dotmarketing.portlets.calendar.business.EventAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowTask"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.*"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>

<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotDataException"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.util.XMLUtils"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>

<%@ page import="com.dotmarketing.business.PermissionLevel" %>


<%
	session.removeAttribute(com.dotmarketing.util.WebKeys.IMAGE_TOOL_SAVE_FILES);
	LanguageAPI langAPI = APILocator.getLanguageAPI();

	ContentletForm contentletForm = (ContentletForm) request.getAttribute("contentletForm");
	if(contentletForm ==null){
		contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
	}
	Contentlet contentlet = (Contentlet) request.getAttribute("contentlet");
	Structure structure = (Structure) request.getAttribute("structure");
	String referer = (String) request.getAttribute("referer");
	
	Identifier id = null;
	if(UtilMethods.isSet(contentlet.getIdentifier())){
		id = APILocator.getIdentifierAPI().find(contentlet);
	}else{
	   final String identifier = (String)request.getAttribute("identifier");
       if(UtilMethods.isSet(identifier)){
		   id = APILocator.getIdentifierAPI().find(identifier);
       }
	}
	
	List<Language> languages = langAPI.getLanguages();
	Language defaultLang = langAPI.getDefaultLanguage();
	List<Field> fields = structure.getFields();
	if(contentletForm.getLanguageId() == 0) {
		contentletForm.setLanguageId(defaultLang.getId());
	}

	Map<String, String[]> params = new HashMap<String, String[]>();
	params.put("struts_action", new String[] { "/ext/contentlet/edit_contentlet" });
	params.put("cmd", new String[] { contentlet != null && InodeUtils.isSet(contentlet.getInode()) ? "edit" : "new" });

	if (InodeUtils.isSet(request.getParameter("selectedStructure"))) {
		params.put("selectedStructure", new String[] {request.getParameter("selectedStructure")});
	}
	
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
	if(structure.getVelocityVarName().equals(EventAPI.EVENT_STRUCTURE_VAR)){
		params.put("struts_action",new String[] {"/ext/calendar/edit_event"});
	}
	String editURL = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
	WorkflowScheme scheme = null;
	WorkflowTask wfTask = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet);

	List<WorkflowStep> wfSteps = null;
	WorkflowStep wfStep = null;
	List<WorkflowAction> wfActions = null;
	try{
		wfSteps = APILocator.getWorkflowAPI().findStepsByContentlet(contentlet);
		if(null != wfSteps && !wfSteps.isEmpty() && wfSteps.size() == 1) {
			wfStep = wfSteps.get(0);
			scheme = APILocator.getWorkflowAPI().findScheme(wfStep.getSchemeId());
		}
		wfActions = APILocator.getWorkflowAPI().findActions(wfSteps, user, contentlet);
	}
	catch(Exception e){
		wfActions = new ArrayList();
	}

%>

<%
	List<Language> allowContentLang = new ArrayList(languages);
	boolean haveEditPermission = true;

	if (UtilMethods.isSet(contentlet.getInode())) {
		haveEditPermission = APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionLevel.EDIT.getType(), user, false);

		if (!haveEditPermission) {
			allowContentLang.clear();
			List<Contentlet> contentlets = APILocator.getContentletAPIImpl().getAllLanguages(contentlet, false, user, false);

			Map<Long, Language> languagesMap = new HashMap();

			for (Language lang: languages) {
				languagesMap.put(lang.getId(), lang);
			}

			for (Contentlet content : contentlets){
				if (languagesMap.containsKey(content.getLanguageId())) {
					allowContentLang.add(languagesMap.get(content.getLanguageId()));
				}
			}
		}
	}
%>

	<input type="hidden" name="submitParent" id="submitParent" value="">
	<input type="hidden" name="wysiwyg_image" id="wysiwyg_image" value="">
	<input type="hidden" name="selectedwysiwyg_image" id="selectedwysiwyg_image" value="">
	<input type="hidden" name="selectedIdentwysiwyg_image" id="selectedIdentwysiwyg_image" value="">
	<input type="hidden" name="folderwysiwyg_image" id="folderwysiwyg_image" value="">
	<input type="hidden" name="wysiwyg_file" id="wysiwyg_file" value="">
	<input type="hidden" name="selectedwysiwyg_file" id="selectedwysiwyg_file" value="">
	<input type="hidden" name="selectedIdentwysiwyg_file" id="selectedIdentwysiwyg_file" value="">
	<input type="hidden" name="folderwysiwyg_file" id="folderwysiwyg_file" value="">

	<!--  action command and sub command to be executed, command can be save and sub command publish, meaning save and publish -->
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
	<input name="<portlet:namespace />subcmd" type="hidden" value="">
	<!--  the url used to redirect when exiting from this page -->
	<input name="<portlet:namespace />referer" id="<portlet:namespace />referer" type="hidden" value="<%= referer %>">
	<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL>
		<portlet:param name="struts_action" value="/ext/contentlet/view_contentlets" /></portlet:renderURL>">
	<!--  this propperty holds the sibbling (contentlet in another language) -->
	<input name="<portlet:namespace />sibbling" type="hidden"
		value="<%= (request.getParameter("sibbling")!=null) ? request.getParameter("sibbling") : "" %>">
	<!--  Inode of the currently edited contentlet, "" if it's a new contentlet -->	
	<input name="<portlet:namespace />inode" type="hidden" value="<%= contentlet.getInode() %>">
	<!-- //jira.dotmarketing.net/browse/DOTCMS-2273 -->
	<input name="contentletInode" id="contentletInode" type="hidden" value="<%= contentlet.getInode() %>"/>
	<input name="subcmd" id="subcmd" type="hidden" value=""/>
	
	<!--  user how is editing the contentlet -->
	<input type="hidden" name="userId" value="<%= user.getUserId() %>">
	<!--  used to associate the contentlet to the given html page and container after saves it -->
	<% if(request.getParameter("htmlpage_inode")!=null){ %>
		<input type="hidden" name="htmlpage_inode" value="<%= request.getParameter("htmlpage_inode") %>">
	<% } %>
	<% if(request.getParameter("contentcontainer_inode")!=null){ %>
		<input type="hidden" name="contentcontainer_inode" value="<%= request.getParameter("contentcontainer_inode") %>">
	<% } %>
	<% if(request.getParameter("relwith")!=null){ %>
		<input type="hidden" name="relwith" value="<%= request.getParameter("relwith") %>">
	<% } %>
	<% if(request.getParameter("relisparent")!=null){ %>
		<input type="hidden" name="relisparent" value="<%= request.getParameter("relisparent") %>">
	<% } %>
	<% if(request.getParameter("reltype")!=null){ %>
		<input type="hidden" name="reltype" value="<%= request.getParameter("reltype") %>">
	<% } %>
	<% if(request.getParameter("relname")!=null && request.getParameter("relname_inodes")!=null){ %>
		<input type="hidden" name="<%=request.getParameter("relname")%>_inodes" value="<%= request.getParameter("relname_inodes") %>">
	<% } %>
	
	
	<!-- holds which wysywyg are disabled -->
	<html:hidden property="disabledWysiwyg" styleId="disabledWysiwyg" />
		<!--
			<%if(id!=null && InodeUtils.isSet(id.getInode()) && InodeUtils.isSet(contentlet.getInode())){%>
				<div>	
	                <div class="fieldName"><%= LanguageUtil.get(pageContext, "Identity") %>:</div>
	                <div class="fieldValue"><%= id.getInode() %></div>
				</div>
			<%}%>
		-->
		
		
		<div class="content-edit-language">
			<!-- SELECT STRUCTURE -->
			<%if(contentletForm.isAllowChange()){%>
				<%List structures = contentletForm.getAllStructures();%>
				<select name="selectedStructure" onChange="structureSelected()">
					<option value="none"><%= LanguageUtil.get(pageContext, "Select-type") %></option>
					<%
					for(int i = 0;i < structures.size();i++)
					{
						Structure actualStructure = (Structure) structures.get(i);
						if (InodeUtils.isSet(actualStructure.getInode())) {
							String selected = (actualStructure.getInode().equalsIgnoreCase(structure.getInode()) ? "SELECTED" : "");%>
							<option value="<%=actualStructure.getInode()%>" <%=selected%>><%=actualStructure.getName()%></option>
						<%}%>
					<%}%>
				</select>
			<%} else {%>
				<input type="hidden" name="selectedStructure" id="selectedStructure" value="<%= structure.getInode() %>">
			<%} %>
			<!-- END SELECT STRUCTURE -->

			<!--  Start Language -->
			<%if (languages.size() > 1
				&& !structure.getVelocityVarName().equalsIgnoreCase("Host")
				&& structure.getStructureType() != Structure.STRUCTURE_TYPE_FORM
				&& structure.getStructureType() != Structure.STRUCTURE_TYPE_PERSONA
				|| !haveEditPermission) {
			%>

				<h3>
					<%= LanguageUtil.get(pageContext, "language") %>
				</h3>
			
				<script>
					function changeLanguage(url){
						url=url+"";
						if(url.indexOf("sibbling=")>-1 && currentContentletInode){
                            var startSibblingParam = url.indexOf("sibbling=");
                            var paramEnd = url.indexOf("&", startSibblingParam);
                            var param = url.substring(startSibblingParam, paramEnd);
                             url=url.replace(param,"sibbling=" + currentContentletInode);
                        } else if(url.indexOf("sibbling=")<0) {
							url+="&sibbling=" + currentContentletInode ;
						}

						if(url.indexOf("lang=<%=contentletForm.getLanguageId()%>&") <0){
							if(url.indexOf("host=") <0){
								if(dojo.byId('hostId')){
									url = url + "&host=" + dojo.byId('hostId').value;
								}
								if(dojo.byId('folderInode')){
									url = url + "&folder=" + dojo.byId('folderInode').value;
								}
							}
							window.location=url;
						}
					}
				</script>

				<div class="fieldWrapperSide">
					<div id="combo_zone2">
						<input id="langcombo" />
					</div>

					<script type="text/javascript">
						<% StringBuffer buff = new StringBuffer();

						   buff.append("{identifier:'id', label:'label',imageurl:'imageurl',items:[");

						   boolean first=true;
						   for (Language lang : allowContentLang) {
							   Contentlet langContentlet= new Contentlet();
							   ContentletAPI conAPI = APILocator.getContentletAPI();
							   try {
								   if(id != null){
									 langContentlet = conAPI.findContentletForLanguage(lang.getId(), id);
								   }
							   }
							   catch(DotDataException e){
								   Logger.warn(this,"Unable to find the contentlet with identifier " + contentlet.getIdentifier() + " and languageId " + lang.getId() );
							   }

							   String value;
							   if(langContentlet != null && InodeUtils.isSet(langContentlet.getInode())) {
								   value=editURL + "&lang="+ lang.getId() + "&inode=" + langContentlet.getInode();
							   }
							   else {
								   value=editURL + "&lang="+ lang.getId() + "&inode=";
							   }

							   if(null != id){
							      value += "&identifier=" + id.getId();
							   }

							   final String display = lang.getLanguage() +  (UtilMethods.isSet(lang.getCountryCode()) ?  " (" + lang.getCountryCode().trim() + ")"  : "") ;

							   if(!first) buff.append(","); else first=false;
							   final String ccode = lang.getLanguageCode()  + "_" + lang.getCountryCode();
							   buff.append("{");
							   buff.append("id:'" + lang.getId() + "',");
							   buff.append("label:'"+display+"',");
							   buff.append("lang:'"+display+"',");
							   buff.append("value:'"+value+"'");
							   buff.append("}");
						   }
						   buff.append("]}");
						%>

						var storeData=<%=buff.toString()%>;
						var langStore = new dojo.data.ItemFileReadStore({data: storeData});
						var myselect = new dijit.form.FilteringSelect({
								 id: "langcombo",
								 name: "lang",
								 value: '<%=contentletForm.getLanguageId()%>',
								 required: true,
								 store: langStore,
								 searchAttr: "lang",
								 labelAttr: "label",
								 labelType: "html",
								 style: "width: 100%",
								 onChange: function() {
									 var obj=dijit.byId("langcombo");
									 changeLanguage(obj.item.value);
								 },
								 labelFunc: function(item, store) { return store.getValue(item, "label"); }
							},
							dojo.byId("langcombo"));

                        	dijit.byId('langcombo').set('disabled', <%=!haveEditPermission && allowContentLang.size() == 1%>);
					</script>
				</div>
			<%} %>
		 	<input type="hidden" name="languageId" id="languageId" 
				value="<%= (contentlet.getLanguageId() != 0) ? contentlet.getLanguageId() + "" : ((UtilMethods.isSet(request.getParameter("lang"))) ? request.getParameter("lang") : defaultLang.getId() + "") %>">
			<!-- END LANGUAGE -->



	</div>

<script>
function showRelationshipReturn(){

    var backInode = localStorage.getItem("dotcms.relationships.relationshipReturnValue");
    
    var myCon = "<%=contentlet.getInode()%>";
    if(myCon === ""){
    	try{
    	    myCon = workingContentletInode;
    	}catch(err) {
    		   // no my con
    	}
    }

    
    
    var referer="<%=UtilMethods.webifyString(request.getParameter("referer"))%>";
    if(backInode ==null || backInode == undefined || backInode=='' || backInode =="null" || referer =="") {
    	localStorage.removeItem("dotcms.relationships.relationshipReturnValue")
        return;
    }


    backInode = JSON.parse(backInode);

    if(backInode.inode ==  myCon){
        localStorage.removeItem("dotcms.relationships.relationshipReturnValue")
        return;
    }
    

    var backButtonTmpl = `
		<div class="content-edit-actions" style="cursor:pointer;padding:5px;overflow:hidden" id="relationshipReturnValueButton">
			<table>
				<tr>
			        
			        <td><a href="#"><%= LanguageUtil.get(pageContext, "message.relationship.returnTo") %>${backInode.title}</a></td>
			        <td style="width:30px;text-align:center">&larr;</td>
				</tr>
			</table>
		</div>
    `

    document.write(backButtonTmpl)
    var button = document.getElementById("relationshipReturnValueButton");

    button.addEventListener("click", function(e) {
    	window.location.href="<%=request.getParameter("referer")%>";
       localStorage.removeItem("dotcms.relationships.relationshipReturnValue");
    });
}

showRelationshipReturn();
</script>




