<%@ include file="/html/portlet/ext/containers/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.beans.Source" %>
<%@ page import="com.dotmarketing.beans.Identifier" %>


<script type='text/javascript' src='/dwr/interface/ContainerAjax.js'></script>

<%
	Boolean hostChanged = (Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_HOST_CHANGED);
	int pageNumber = 1;
	if (!hostChanged && (request.getParameter("pageNumber") != null) && (request.getParameter("resetQuery") == null)) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
	}
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/containers/view_containers"});
	params.put("pageNumber",new String[] { pageNumber + "" });

	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);

	java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	String showDeleted = (request.getParameter("showDeleted")!=null) ? request.getParameter("showDeleted") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_SHOW_DELETED);
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : (String) session.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_QUERY);
	String orderby = (request.getParameter("orderby")!=null) ? request.getParameter("orderby") : "";

	String structureId ="";
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_STRUCTURE_ID) != null)
			structureId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_STRUCTURE_ID);
		if (request.getParameter("structure_id") != null)
		    structureId = request.getParameter("structure_id");
	} catch (NumberFormatException e) {	}

	Structure st;
	List<Structure> allStructures = StructureFactory.getStructures(user, false, true);
    List<Structure> structures = new ArrayList<Structure>();
    for (Structure struct : allStructures) {
        if (!struct.isWidget()) {
            structures.add(struct);
        }
    }

	String hostId = "";
	try {
		if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID) != null)
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		if (request.getParameter("host_id") != null)
		          hostId =request.getParameter("host_id");
	} catch (NumberFormatException e) {
	}

%>

<script language="Javascript">
var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";
var inFrame=<%=(UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME)) && (Boolean)request.getSession().getAttribute(WebKeys.IN_FRAME))?true:false%>;

function resetSearch() {
	form = document.getElementById('fm');
	form.showDeleted.value = '';
	form.resetQuery.value = 'true';
    dojo.query("input[type='hidden']",'fm').forEach(function(node, index, arr){
		 	if(node.id === '' || node.id === 'structure_id'){
	   	   		node.value="";
		 	}
	 });
	form.query.value = '';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/containers/view_containers" /></portlet:renderURL>';
	submitForm(form);
}
function submitfm() {
	form = document.getElementById('fm');
	form.pageNumber.value = 1;
	if (document.getElementById('showDeletedCB').checked)
		document.getElementById('showDeleted').value='true';
	else
		document.getElementById('showDeleted').value='false';
	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/containers/view_containers" /></portlet:renderURL>';
	submitForm(form);
}
function submitfmPublish() {
	form = document.getElementById('fm_publish');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/publish_containers" /></portlet:actionURL>';
	submitForm(form);
}
function submitfmDelete() {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "confirm.containers.delete.containers")) %>'))
	{
		var containerInodesToDelete = dojo.query("input[name='publishInode']")
        									.filter(function(x){return x.checked;})
        									.map(function(x){return x.value;}).toString();		
		
		delContainer(containerInodesToDelete, "<%=referer%>", false);
	}
}
function addAsset(event) {
	 var href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /><portlet:param name="referer" value="<%=referer%>" /></portlet:actionURL>';
     window.location= href;
}

function checkAll() {
	var check = dijit.byId("checkAll").checked;

	dojo.query('input[type=checkbox]', document).forEach(function(tag){
		var id = tag.id;
		if(id != undefined && id.indexOf("publishInode") >-1){
			dijit.byId(id).setChecked(check);
		}
	});
	togglePublish();
}


/* =========== CHANGE TO DOJO CODE ============================ */
function togglePublish(){
	var cbArray = document.getElementsByName("publishInode");
	var cbCount = cbArray.length;
	for(i = 0;i<cbCount;i++){
		if (cbArray[i].checked) {
		    dijit.byId("publishButton").setAttribute("disabled", false);
            dijit.byId("deleteButton").setAttribute("disabled", false);
			break;
		}
			dijit.byId("publishButton").setAttribute("disabled", true);
            dijit.byId("deleteButton").setAttribute("disabled", true);
		}
}

function delContainer(inode, referer, isFromMenu) {

	var callMetaData = {
			  callback:handleDepResponse,
			  arg: inode + '|' + referer + '|' + isFromMenu, 
			   // specify an argument to pass to the callback and exceptionHandler
			};

	ContainerAjax.checkDependencies(inode, callMetaData);
}

function handleDepResponse(data, arg1) {
	var params = arg1.split('|');
	var inode = params[0];
	var referer = params[1];
	var isFromMenu = params[2] == 'true';
	
	if(data!=null) {
		dojo.byId("depDiv").innerHTML = "<br />" + data;
		dijit.byId("dependenciesDialog").show();
	}else if(isFromMenu){
		processDelete(inode, referer);
	}else {
		form = document.getElementById('fm_publish');
		form.cmd.value = 'full_delete_list';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="full_delete_list" /></portlet:actionURL>';
		submitForm(form);		
	}
}

function processDelete(inode, referer) {
	var loc="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&inode=" + inode + '&referer=' + referer;
	if(inFrame){
		window.location = loc;
	}else{
		top.location = loc;
	}
}

</script>
<%  referer = java.net.URLEncoder.encode(referer,"UTF-8"); %>

<jsp:include page="/html/portlet/ext/folders/context_menus_js.jsp" />
<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-containers-all") %>' />

<div class="portlet-main">
	
<form id="fm" method="post">
<input type="hidden" name="resetQuery" value="">
<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
<input type="hidden" name="host_id" id="host_id" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>">
<input type="hidden" id="showDeleted" name="showDeleted" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "value=\"true\"" : "value=\"false\"" %> >

<div class="portlet-toolbar">
	<div class="portlet-toolbar__actions-primary">
		<select name="structure_id" id="structure_id" autocomplete="false"  dojoType="dijit.form.FilteringSelect" onChange="submitfm()" >
			<OPTION value=" " <%=!UtilMethods.isSet(structureId)?"selected":""%>><%= LanguageUtil.get(pageContext, "Any-Structure") %></OPTION>
			<%
				Iterator structuresIt = structures.iterator();
				while (structuresIt.hasNext()) {
					Structure next = (Structure)structuresIt.next();
					if(!next.getName().equals("Host")){
			%>
				<OPTION value="<%=next.getInode()%>" <%=structureId.equalsIgnoreCase(next.getInode())?"selected":""%>><%=next.getName()%></OPTION>
			<%		}
				} %>
		</select>
		
		<input type="text" name="query" dojoType="dijit.form.TextBox" style="width:175px;" value="<%= com.dotmarketing.util.UtilMethods.isSet(query) ? query : "" %>">
	    
	    <button dojoType="dijit.form.Button" type="submit" onClick="submitfm()" iconClass="searchIcon">
	        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
	    </button>
	    
		<button dojoType="dijit.form.Button" onClick="resetSearch()" iconClass="resetIcon">
	          <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
	    </button>
	    
	    &nbsp; &nbsp;
	    
	    <input type="checkbox" dojoType="dijit.form.CheckBox"  name="showDeletedCB" id="showDeletedCB"onClick="javascript:submitfm();" <%= (showDeleted!=null) && (showDeleted.equals("true")) ? "checked" : "" %> value="true">
		<label for="showDeletedCB" style="font-size:85%;"><%= LanguageUtil.get(pageContext, "Show-Archived") %></label>
	</div>
	
	<div class="portlet-toolbar__info"></div>
	<div class="portlet-toolbar__actions-secondary">
		<!-- START Actions -->			
			<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
	            <span></span>
	
	            <div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
					<% if((Boolean) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_CAN_ADD)) { %>
						<div data-dojo-type="dijit/MenuItem" onClick="javascript:addAsset(); return false;" iconClass="plusIcon">
						    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-container")) %>
						</div>
					<% } %>

					<div data-dojo-type="dijit/MenuItem" onClick="submitfmPublish();"  disabled="true" id="publishButton" iconClass="publishIcon">
					    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
					</div>
					
					<div data-dojo-type="dijit/MenuItem" onClick="submitfmDelete();" id="deleteButton" disabled="true" iconClass="deleteIcon">
					    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
					</div>
				</div>
			</div>
		<!-- END Actions -->
	</div>
</div>


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




<form id="fm_publish" method="post">
<input type="hidden" name="referer" value="<%=referer%>">
<input type="hidden" name="cmd" value="prepublish">
<table class="listingTable">
	<tr>

		<th nowrap style="width:25px;text-align:center;">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="cb2" id="checkAll" value="1" onclick="checkAll"/>
			<span dojoType="dijit.Tooltip" connectId="checkAll" id="ckeckAll_tooltip"><%= LanguageUtil.get(pageContext, "Check-all") %> / <%= LanguageUtil.get(pageContext, "Uncheck-all") %></span>
		</th>
		<th nowrap width="50%">
			<a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/containers/view_containers" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"title asc\")?\"title desc\":\"title asc\"%>" /></portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Title") %></a>
		</th>
		<th nowrap style="width:35px;"><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th nowrap width="35%"><%= LanguageUtil.get(pageContext, "Description") %></th>
		<th nowrap width="15%">
			<a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/containers/view_containers" />
			<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber) %>" />
			<portlet:param name="orderby" value="<%=orderby.equals(\"mod_date desc\")?\"mod_date asc\":\"mod_date desc\"%>" /></portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "Mod-Date") %></a>
		</th>
	</tr>

<!-- Start Listing Results -->
	<%
		java.util.List containers = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINERS_VIEW);
		int containersSize = ((Long) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINERS_VIEW_COUNT)).intValue();
		String userId = user.getUserId();

		for (int k=0;k<containers.size();k++) {
			com.dotmarketing.beans.PermissionAsset permAsset = (com.dotmarketing.beans.PermissionAsset) containers.get(k);

			//gets permissions listings
			java.util.List permissions = permAsset.getPermissions();
			//gets template
			com.dotmarketing.portlets.containers.model.Container container = (com.dotmarketing.portlets.containers.model.Container) permAsset.getAsset();
			String str_style = "";
			if ((k%2)==0) {
				str_style = "class=\"alternate_1\"";
			}
			else{
				str_style = "class=\"alternate_2\"";
            }

			//container properties and permissions
			String inode = container.getInode();
			String live = (container.isLive())?"1":"0";
			String working = (container.isWorking())?"1":"0";
			String write = (permissions.contains(String.valueOf(PermissionAPI.PERMISSION_WRITE)))?"1":"0";

			Host host = APILocator.getHostAPI().findParentHost(container, APILocator.getUserAPI().getSystemUser(), false);
			boolean isDBSource = container.getSource() == Source.DB;
			String path        = "";
			if (container.getSource() == Source.FILE) {

				Identifier identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());
				if (null != identifier) {

					path = identifier.getParentPath();
				}
			}


		%>
		<tr <%=str_style%> id="tr<%=k%>">

			<% if (isDBSource) { %>
			<td nowrap style="text-align:center;">

				<% if (permissions.contains(PermissionAPI.PERMISSION_PUBLISH)) { %>
					<input dojoType="dijit.form.CheckBox" type="checkbox" name="publishInode" id="publishInode<%= container.getInode() %>" value="<%= container.getInode() %>" onclick="togglePublish()" />
				<% } %>
			</td>
			<td nowrap <%if(!container.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>" ><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=<%=container.getInode()%>&referer=<%=referer%>'"<%} %>>
					<%=container.getTitle()%>
			</td>
			<td nowrap <%if(!container.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>" ><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=<%=container.getInode()%>&referer=<%=referer%>'"<%} %>><%= com.dotmarketing.util.UtilHTML.getStatusIcons(container) %></td>
			<td <%if(!container.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>" ><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=<%=container.getInode()%>&referer=<%=referer%>'"<%} %>><%=container.getFriendlyName()%></td>
			<td nowrap <%if(!container.isDeleted()){%>onclick="javascript:window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>" ><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=<%=container.getInode()%>&referer=<%=referer%>'"<%} %>>
				<%=modDateFormat.format(container.getModDate())%>
				<script language="JavaScript">
					//popup div for the containers
					document.write(getContainerPopUp('<%=k%>','<%= CTX_PATH %>', '<%=container.getInode()%>', '<%=container.getIdentifier()%>','',
					   '<%=referer%>','<%=(container.isLive()) ? "1" : "0"%>',
					   '<%=(container.isWorking()) ? "1" : "0"%>',
					   '<%=(container.isDeleted()) ? "1" : "0"%>',
					   '<%=(container.isLocked()) ? "1" : "0"%>'
					   ,'<%=permissions.contains(PermissionAPI.PERMISSION_READ) ? "1" : "0" %>'
					   ,'<%=permissions.contains(PermissionAPI.PERMISSION_WRITE) ? "1" : "0" %>'
					   ,'<%=permissions.contains(PermissionAPI.PERMISSION_PUBLISH) ? "1" : "0" %>'
					   ,'<%=user.getUserId()%>'
					   ,'<%=container.hasLiveVersion() ? "1" : "0"%>'));
				</script>
			</td>
			<% } else { %>
			<td nowrap style="text-align:center;">

				&nbsp;&nbsp;&nbsp;
			</td>
			<td nowrap>
				<%=container.getTitle()%> | <%=path%>
			</td>
			<td nowrap >
				<%= com.dotmarketing.util.UtilHTML.getStatusIcons(container) %>
			</td>
			<td><%=container.getFriendlyName()%></td>
			<td nowrap >
				<%=modDateFormat.format(container.getModDate())%>
			</td>
			<% } %>
		</tr>

	<%}%>
<!-- END Listing Results -->

<!-- Start No Results -->
	<% if (containersSize ==0) { %>
		<tr>
			<td colspan="5">
				<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Containers-to-show") %></div>
			</td>
		</tr>
	<% } %>
<!-- End No Results -->
</table>

</div><!-- Mian -->


<!-- Start Pagination -->
<div class="yui-gb buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
			<button dojoType="dijit.form.Button"  iconClass="previousIcon" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/view_containers" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) containers.get(0)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="previous" /></portlet:renderURL>';">
			    <%= LanguageUtil.get(pageContext, "Previous") %>
			</button>
		<% } %>&nbsp;
	</div>

	<div class="yui-u" style="text-align:center;">
		<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> -
		<%
			if (maxIndex > (minIndex + containersSize)) {
		%>
			<%= minIndex + containersSize %>
		<%
			} else {
		%>
			<%= maxIndex %>
		<%
			}
		%>
			<%= LanguageUtil.get(pageContext, "of1") %>
		<%
			if (100 <= containersSize) {
		%>
			<%= LanguageUtil.get(pageContext, "hundreds") %>
		<%
			} else {
		%>
			<%= minIndex + containersSize %>
		<%
			}
		%>
	</div>

	<div class="yui-u" style="text-align:right;">
		<% if (maxIndex < (minIndex + containersSize)) { %>
			<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/view_containers" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /><portlet:param name="orderby" value="<%= orderby %>" /><portlet:param name="fromAssetId" value="<%= ((com.dotmarketing.beans.PermissionAsset) containers.get(containers.size() - 1)).getAsset().getIdentifier() %>" /><portlet:param name="show" value="next" /></portlet:renderURL>';" iconClass="nextIcon">
				<%= LanguageUtil.get(pageContext, "Next") %>
			</button>
		<% } %>&nbsp;
	</div>
</div>
<!-- END Pagination -->



</form>

</liferay:box>

<div id="dependenciesDialog" dojoType="dijit.Dialog" style="display:none;width:630px;height:300px;vertical-align: middle; " draggable="true"
	title="<%= LanguageUtil.get(pageContext, "Delete-Container") %>" >

	<span style="color: red; font-weight: bold"><%= LanguageUtil.get(pageContext, "message.containers.full_delete.error") %></span>

	<div id="depDiv" style="overflow: auto; height: 220px"></div>
</div>

