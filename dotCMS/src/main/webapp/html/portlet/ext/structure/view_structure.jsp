<%@ page import="java.util.ArrayList" %>
<%@ page import="com.dotcms.repackage.javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ include file="/html/portlet/ext/structure/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>

<%
	java.util.Map params = new java.util.HashMap();
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL( request, WindowState.MAXIMIZED.toString(), params );
	List structures = (List) request.getAttribute( com.dotmarketing.util.WebKeys.Structure.STRUCTURES );
	Map<String, Long> entriesNumber = (Map<String, Long>) request.getAttribute( com.dotmarketing.util.WebKeys.Structure.ENTRIES_NUMBER );

	int pageNumber = 1;
	if (request.getParameter("pageNumber") != null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
	}
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	//java.util.Map params = new java.util.HashMap();
	params.put("struts_action", new String[] { "/ext/structure/view_structure" });
	params.put("pageNumber", new String[] { pageNumber + "" });


	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
			java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String query = (request.getParameter("query") != null) ? request
			.getParameter("query") : (String) session
			.getAttribute(com.dotmarketing.util.WebKeys.STRUCTURE_QUERY);
	String orderby = (request.getParameter("orderBy") != null) ? request
			.getParameter("orderBy")
			: "";

	int STRUCTURE_TYPE_CONTENT = 1;
	int STRUCTURE_TYPE_WIDGET = 2;
	int STRUCTURE_TYPE_FORM = 3;
	int STRUCTURE_TYPE_FILEASSET= 4;
	int STRUCTURE_TYPE_HTMLPAGE= 5;
	int STRUCTURE_TYPE_PERSONA= 6;
	int CONTENT_TYPE_VANITY_URL = 7;
	int CONTENT_TYPE_KEY_VALUE = 8;
	List<Integer> structureTypes = new ArrayList<Integer>();
	structureTypes.add(STRUCTURE_TYPE_CONTENT);
	structureTypes.add(STRUCTURE_TYPE_WIDGET);
	if ( LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level ) {
		structureTypes.add( STRUCTURE_TYPE_FORM );
	}
	structureTypes.add(STRUCTURE_TYPE_FILEASSET);
	structureTypes.add(STRUCTURE_TYPE_HTMLPAGE);
	if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level){
		structureTypes.add(STRUCTURE_TYPE_PERSONA);
	}
	structureTypes.add(CONTENT_TYPE_VANITY_URL);
	structureTypes.add(CONTENT_TYPE_KEY_VALUE);
	int structureType = 0;
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE) != null)
			structureType = (Integer)session.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURE_EDIT_TYPE);
		if (request.getParameter("structureType") != null)
			structureType = Integer.parseInt(request.getParameter("structureType"));
	} catch (NumberFormatException e) {
	}

	params = new java.util.HashMap();
	params.put("struts_action",
			new String[] { "" });
	String viewStructures = com.dotmarketing.util.PortletURLUtil.getRenderURL(
			request, WindowState.MAXIMIZED.toString(), params);


	boolean enterprise = LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;

	PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
	List<PublishingEndPoint> sendingEndpointsList = pepAPI.getReceivingEndPoints();
	boolean sendingEndpoints = UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty();
%>
<script language="Javascript">
    var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

    var currentStructureInode = "";

    function addNewStructure(){

        var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
        href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
        href = href + "</portlet:actionURL>";
        document.location.href = href;
    }


    function resetSearch() {
        form = document.getElementById('fm');
        form.query.value = '';
        var ele=dijit.byId("selectStructure");
        ele.setValue(0);
    }

    function submitfm() {
        form = document.getElementById('fm');
        form.pageNumber.value = 1;
        var system = '0';
        if (form.system.checked)
            system = '1';
        form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/structure/view_structure" /></portlet:renderURL>&system=' + system;
        submitForm(form);
    }

    function viewAllRelationship()
    {
        var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
        href = href + "<portlet:param name='struts_action' value='/ext/structure/view_relationships' />  <portlet:param name='structure_id' value='all' />";
        href = href + "</portlet:actionURL>";
        document.location = href;
    }


    function editStructure(inode){
        var x ='<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
        <portlet:param name='struts_action' value='/ext/structure/edit_structure' />
        </portlet:actionURL>' + "&inode=" + inode;
        window.location=x;
    }

    function changeBgOver(inode){
        dojo.style("tr" + inode, "background", "#CDEAFD");
    }
    function changeBgOut(inode){
        dojo.style("tr" + inode, "background", "#ffffff");
    }

	/*
	 dojo.addOnLoad(function() {
	 var menu = new dijit.Menu({
	 style: "display: none;"
	 });
	 var menuItem1 = new dijit.MenuItem({
	 label: "<%= LanguageUtil.get(pageContext, "Add-New-Structure") %>",
	 iconClass: "plusIcon",
	 onClick: function() {
	 addNewStructure();
	 }
	 });
	 menu.addChild(menuItem1);

	 var menuItem3 = new dijit.MenuItem({
	 label: "<%= LanguageUtil.get(pageContext, "View-all-Relationships") %>",
	 iconClass: "previewIcon",
	 onClick: function() {
	 viewAllRelationship();
	 }
	 });
	 menu.addChild(menuItem3);

	 var button = new dijit.form.ComboButton({
	 label: "<%= LanguageUtil.get(pageContext, "Add-New-Structure") %>",
	 iconClass: "plusIcon",
	 dropDown: menu,
	 onClick: function() {
	 addNewStructure();
	 }
	 });
	 dojo.byId("addNewStructure").appendChild(button.domNode);

	 });
	 */

    dojo.require("dotcms.dojo.push.PushHandler");
    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

    function remotePublishStructure (inode,isFixed) {
        if(isFixed){
            pushHandler.showRestrictedDialog(inode);
        }else{
            pushHandler.showDialog(inode);
        }
    }

    function addToBundle (objId) {
        pushHandler.showAddToBundleDialog(objId, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
    }

    function deleteStructure(structureInode) {
        currentStructureInode = structureInode;

        if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.structure.delete.structure.and.content")) %>')) {
            StructureAjax.checkDependencies(structureInode, handleDepResponse);
        }
    }

    function handleDepResponse(data) {

        if(data['size'] != 0) {

            var resultTableStr = '<table class="listingTable"><thead><tr><th><%=LanguageUtil.get(pageContext, "TITLE")%></th><th><%=LanguageUtil.get(pageContext, "IDENTIFIER")%></th><th><%=LanguageUtil.get(pageContext, "INODE")%></th></tr></thead><tbody>';
            var containers = data['containers'];

            for(var i = 0; i < data['size'] ; i++){
                resultTableStr = resultTableStr + "<tr><td>" + containers[i]['title'] + "</td><td>" + containers[i]['identifier'] + "</td><td>" + containers[i]['inode'] + "</td></tr>";
            }

            resultTableStr = resultTableStr + '</tbody></table>';
            dojo.byId("depDiv").innerHTML = "<br />" + resultTableStr;

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
        href = href + "&inode=" + currentStructureInode;
        document.location.href = href;
    }

    var popupMenusDiv;
    var popupMenus = "";
    var deleteLabel = "";

</script>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Structures")) %>' />

	<style>
		.dijitSelect .dijitButtonText{width:150px;text-align:left;}
	</style>

	<div class="portlet-main">
		<!-- START Toolbar -->
		<div class="portlet-toolbar">
			<div class="portlet-toolbar__actions-primary">
				<form id="fm" method="post">
					<div class="inline-form">
						<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
						<select name="structureType" autocomplete="false" dojoType="dijit.form.FilteringSelect" id="selectStructure" onChange="submitfm()">
							<%if(structureTypes.size() > 1){ %>
							<option value="0"><%= LanguageUtil.get(pageContext, "Any-Structure-Type") %></option>
							<%}
								String strTypeName="";
								for(Integer next: structureTypes){
									if(next == STRUCTURE_TYPE_CONTENT){
										strTypeName =  LanguageUtil.get(pageContext, "Content");
									}else if(next == STRUCTURE_TYPE_WIDGET){
										strTypeName = LanguageUtil.get(pageContext, "Widget");
									}else if(next == STRUCTURE_TYPE_FORM){
										strTypeName = LanguageUtil.get(pageContext, "Form");
									}else if(next == STRUCTURE_TYPE_FILEASSET){
										strTypeName = LanguageUtil.get(pageContext, "File");
									}else if(next == STRUCTURE_TYPE_HTMLPAGE){
										strTypeName = LanguageUtil.get(pageContext, "HTMLPage");
									}else if(next == STRUCTURE_TYPE_PERSONA){
										strTypeName = LanguageUtil.get(pageContext, "Persona");
									} else if (next == CONTENT_TYPE_VANITY_URL){
										strTypeName = LanguageUtil.get(pageContext, "VanityURL");
									} else if (next == CONTENT_TYPE_KEY_VALUE){
										strTypeName = LanguageUtil.get(pageContext, "KeyValue");
									}

							%>
							<option value="<%=next%>" <%=structureType == next?"selected='true'":""%>><%=strTypeName%></option>
							<%} %>
						</select>

						<input type="text" name="query" dojoType="dijit.form.TextBox" style="width:175px;" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">

						<button dojoType="dijit.form.Button" type="submit" onClick="submitfm()" iconClass="searchIcon">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
						</button>

						<button dojoType="dijit.form.Button" onClick="resetSearch()" iconClass="resetIcon">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "reset")) %>
						</button>

						<input type="checkbox" name="system" id="system" dojoType="dijit.form.CheckBox" <%if (UtilMethods.isSet(request.getParameter("system")) && request.getParameter("system").equals("1")) {%> checked="checked"<%}%> value="1" onClick="submitfm()"/>
						&nbsp; <%= LanguageUtil.get(pageContext, "Structure-show-System") %>
					</div>
				</form>
			</div>

			<div class="portlet-toolbar__actions-secondary">
				<!-- START Actions -->
				<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
					<span></span>
					<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
						<div data-dojo-type="dijit/MenuItem" onClick="addNewStructure();">
							<%= LanguageUtil.get(pageContext, "Add-New-Structure") %>
						</div>

						<div data-dojo-type="dijit/MenuItem" onClick="viewAllRelationship();">
							<%= LanguageUtil.get(pageContext, "View-all-Relationships") %>
						</div>
					</div>
				</div>
				<!-- END Actions -->

			</div>
		</div>
		<!-- END Toolbar -->


		<!-- START Listing Results -->
		<form action="" method="post" name="order">
			<div id="results_table_popup_menus"></div>
			<table class="listingTable" >
				<tr>

					<th width="40%">
						<a href="<portlet:actionURL>
					<portlet:param name='struts_action' value='/ext/structure/view_structure' />
					<portlet:param name='orderBy' value='name' /><portlet:param name='direction' value='<%=request.getAttribute("direction").toString() %>'/>
					</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Structure-Name") %></a>
					</th>
					<th width="10%">
						<a href="<portlet:actionURL><portlet:param name='struts_action' value='/ext/structure/view_structure' />
					<portlet:param name='orderBy' value='velocity_var_name' /><portlet:param name='direction' value='<%=request.getAttribute("direction").toString() %>'/>
					</portlet:actionURL>" >
							<%= LanguageUtil.get(pageContext, "Variable") %></a>

					</th>
					<th width="30%" >
						<a href="<portlet:actionURL><portlet:param name='struts_action' value='/ext/structure/view_structure' />
					<portlet:param name='orderBy' value='description' /><portlet:param name='direction' value='<%=request.getAttribute("direction").toString() %>'/>
					</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Description") %></a>
					</th>
					<th width="10%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Entries") %></th>
					<th width="10%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Relationships") %></th>
				</tr>

				<%
					int structuresSize = ((Integer) request.getAttribute(com.dotmarketing.util.WebKeys.STRUCTURES_VIEW_COUNT)).intValue();
					int k = 0;

					if (structures.size() > 0) {
						for (int i = 0; i < structures.size(); i++) {
							Structure structure = (Structure) structures.get(i);
							String key = structure.getVelocityVarName().toLowerCase();
							Long contentTypeEntriesNumber = entriesNumber.get(key) == null ? 0l :
									entriesNumber.get(key);


				%>

				<tr id="tr<%=structure.getInode()%>" class="alternate_1" onclick="editStructure('<%=structure.getInode()%>');">
					<td>
						<% if(structure.isWidget()){ %>
						<span class="gearIcon"></span>
						<% }else if(structure.isForm()){ %>
						<span class="formIcon"></span>
						<% }else if(structure.isFileAsset()){ %>
						<span class="fileIcon"></span>
						<% }else if(structure.isHTMLPageAsset()){ %>
						<span class="pageIcon"></span>
						<% }else if(structure.isPersona()){ %>
						<span class="personaIcon"></span>
						<% }else{ %>
						<span class="contentIcon"></span>
						<% } %>

						<%=structure.getName()%>
					</td>
					<td><%=structure.getVelocityVarName() %></td>
					<td><%=structure.getDescription()==null?"":structure.getDescription()%></td>
					<td align="center">
						<a href="<portlet:renderURL>
						<portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' />
						<portlet:param name='structure_id' value='<%=structure.getInode()%>' /></portlet:renderURL>">
							<%= contentTypeEntriesNumber %></a>
					</td>
					<td align="center">
						<a href="<portlet:renderURL>
						<portlet:param name='struts_action' value='/ext/structure/view_relationships' />
						<portlet:param name='structure_id' value='<%=structure.getInode()%>' /></portlet:renderURL>">
							<%= LanguageUtil.get(pageContext, "view") %></a>
					</td>
				</tr>

				<script>

                    <%if(structure.getStructureType() == 3 ){%>
                    deleteLabel = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Form-and-Entries")) %>';
                    <%}else{ %>
                    deleteLabel = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Structure-and-Content")) %>';
                    <%} %>

                    popupMenus += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popupTr<%=i%>\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"tr<%=structure.getInode()%>\">";

                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editStructure('<%=structure.getInode()%>');\"><%=LanguageUtil.get(pageContext, "Edit") %></div>";

                    <% if ( enterprise ) { %>
                    <% if ( sendingEndpoints ) { %>
                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"sServerIcon\" onClick=\"remotePublishStructure('<%=structure.getInode()%>',<%=structure.isFixed()%>);\"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>";
                    <%}%>
                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"addToBundle('<%=structure.getInode()%>');\"><%=LanguageUtil.get(pageContext, "Add-To-Bundle") %></div>";
                    <%}%>
                    <%if(!structure.isFixed()){%>
                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"stopIcon\" onClick=\"deleteStructure('<%=structure.getInode()%>');\">"+deleteLabel+"</div>";
                    <%}%>
                    popupMenus += "</div>";

                    popupMenusDiv = document.getElementById("results_table_popup_menus");
                    popupMenusDiv.innerHTML = popupMenus;
				</script>

				<% } %>
				<!-- END Listing Results -->

				<!-- Start No Results -->
				<% }else { %>
				<tr>
					<td colspan="6">
						<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Structures-to-display") %></div>
					</td>
				</tr>
				<%}%>
				<!-- End No Results -->

			</table>
	</div>

	<!-- Start Pagination -->
	<div class="yui-gb buttonRow">
		<div class="yui-u first" style="text-align:left;">
			<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/structure/view_structure" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><%if(UtilMethods.isSet(request.getParameter("system")) && request.getParameter("system").equals("1")){ %><portlet:param name="system" value="1" /><%} else {%><portlet:param name="system" value="0" /><%}%><portlet:param name="orderBy" value="<%= orderby %>" /></portlet:renderURL>';" iconClass="previousIcon" type="button">
				<%= LanguageUtil.get(pageContext, "Previous") %>
			</button>
			<% } %>&nbsp;
		</div>
		<div class="yui-u">
			<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> - <% if (maxIndex > structuresSize) { %> <%= structuresSize %> <%}else{%>  <%= maxIndex %> <% } %> <%= LanguageUtil.get(pageContext, "of1") %> <%= structuresSize %>
		</div>
		<div class="yui-u" style="text-align:right;">
			<% if (maxIndex < structuresSize) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/structure/view_structure" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><%if(UtilMethods.isSet(request.getParameter("system")) && request.getParameter("system").equals("1")){ %><portlet:param name="system" value="1" /><%} else {%><portlet:param name="system" value="0" /><%}%>%><portlet:param name="orderBy" value="<%= orderby %>" /></portlet:renderURL>';" iconClass="nextIcon" type="button">
				<%= LanguageUtil.get(pageContext, "Next") %>
			</button>
			<% } %>&nbsp;
		</div>
	</div>
	<!-- END Pagination -->

	<script language="Javascript">
        /**
         focus on search box
         **/
        require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
            dojo.require('dojox.timing');
            t = new dojox.timing.Timer(500);
            t.onTick = function(){
                focusUtil.focus(dom.byId("dijit_form_TextBox_0"));
                t.stop();
            }
            t.start();
        });
	</script>

	</form>
	<div id="dependenciesDialog" dojoType="dijit.Dialog" style="display:none;width:1000px;vertical-align: middle; " draggable="true"
		 title="<%= LanguageUtil.get(pageContext, "message.structure.cantdelete") %>" >

		<span style="color: red; font-weight: bold"><%= LanguageUtil.get(pageContext, "message.structure.notdeletestructure.container") %></span>

		<div id="depDiv" style="overflow: auto; height: 220px"></div>
	</div>
</liferay:box>