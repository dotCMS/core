<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.InodeUtils" %>
<%@page import="com.dotmarketing.business.Role"%>

<%
	UserManagerListSearchForm form = (UserManagerListSearchForm)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLISTFORM);
	//by setting the bean on context we can use the form tag instead of the struts form tag, and use the autocomplete=off on the form
	pageContext.setAttribute("org.apache.struts.taglib.html.BEAN",form);

	int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

	String userFilterInode = (String)request.getAttribute(com.dotmarketing.util.WebKeys.USER_FILTER_LIST_INODE);

	UserFilter uf = new UserFilter();
	Set<Role> readRoles = new HashSet<Role>();
	Set<Role> writeRoles = new HashSet<Role>();

	PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	if (InodeUtils.isSet(userFilterInode)) {
		uf = UserFilterFactory.getUserFilter(userFilterInode);
		readRoles = perAPI.getReadRoles(uf);
		writeRoles = perAPI.getWriteRoles(uf);
	}
	
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/mailinglists/view_mailinglists"});
	
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
%>

<script type="text/javascript">
<!--
	function enableDisable(mailingListType) {
		var mlTitle  = dijit.byId('usermanagerListTitle');
		var mlSelect = dijit.byId('usermanagerListInode');
		var actionList = document.getElementById('actionList');
		if (mailingListType == 'create') {
			mlTitle.attr('disabled', false);
			mlSelect.attr('disabled', true);
			actionList.value = "<%= com.liferay.portal.util.Constants.ADD %>";
		}
		else if (mailingListType == 'append') {
			mlTitle.attr('disabled', true);
			mlSelect.attr('disabled', false);
			actionList.value = "<%= com.liferay.portal.util.Constants.SAVE %>";
		}
	}
	
	function toggleBox(szDivID, labelID, height)
	{
		if(document.layers)	   //NN4+
	    {
	    	var obj = document.layers[szDivID];
	        if (obj.visibility == '' || obj.visibility == "hidden") {
	        	obj.visibility = "show";
				document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.height = height + 'px';
	        }
	        else {
	        	obj.visibility = "hide";
	        	document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>';
				obj.height = '0px';
	        }
	    }
	    else if(document.getElementById)	  //gecko(NN6) + IE 5+
	    {
	        var obj = document.getElementById(szDivID);
	        if (obj.style.display == '' || obj.style.display == "none") {
	        	obj.style.display = "inline";
				document.getElementById(labelID).innerHTML ='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.style.height = height + 'px';
	        }
	        else {
	        	obj.style.display = "none";
	        	document.getElementById(labelID).innerHTML ='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>';
				obj.style.height = '0px';
	        }
	    }
	    else if(document.all)	// IE 4
	    {
	    	var obj = document.all[szDivID]
	        if (obj.style.display == '' || obj.style.display == "none") {
	        	obj.style.display = "inline";
				document.getElementById(labelID).innerHTML ='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.style.height = height + 'px';
	        }
	        else {
	        	obj.style.display = "none";
	        	document.getElementById(labelID).innerHTML ='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>' ;
				obj.style.height = '0px';
	        }
	    }
	}
	function doLoadSubmit () {
		var form = document.getElementById('searchForm');
		var actionList = document.getElementById('actionList');

		if (actionList.value == "<%= com.liferay.portal.util.Constants.ADD %>") {
			var mlTitle = dijit.byId('usermanagerListTitle');
			if (mlTitle.attr('value') == '') {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.Mailinglist.title.mandatory")) %>');
				return false;
			}
		}
		else if (actionList.value == "<%= com.liferay.portal.util.Constants.SAVE %>") {
			var mlSelect = dijit.byId('usermanagerListInode');
			if (!isInodeSet(mlSelect.attr('value'))) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.select.Mailinglist")) %>');
				return false;
			}
		}
		
		var condition = saveSelectedGroupsRoles();
		
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.ADD%>" /></portlet:actionURL>' + condition;
	    loadButton = dijit.byId("loadButton");
		loadButton.attr('disabled', true);
		form.submit();
	}
   function downloadCSVTemplate(){
		window.location.href='<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="downloadCSVTemplate" /></portlet:actionURL>';
   }
   
	function saveSelectedGroupsRoles() {
	    var groups = listSelect(document.UserManagerListSearchForm.<portlet:namespace />users_selected_groups);
	    var roles = listSelect(document.UserManagerListSearchForm.<portlet:namespace />users_selected_roles);
	    
	    var condition = "&groups=" + groups + "&roles=" + roles;

		return condition;
   }

//-->
</script>

<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "loadusers-usermanager-box-title") %>' />
<form id="searchForm" name="UserManagerListSearchForm" method="post" action="/ext/usermanager/view_usermanagerlist" autocomplete="off" enctype="multipart/form-data">
<input type="hidden" name="actionList" id="actionList" value="<%= com.liferay.portal.util.Constants.ADD %>">
<input type="hidden" name="referer" id="referer" value="<%= referer %>">

<!-- START Tabs -->
<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

	<!-- START Main Tab -->
	<div id="TabOne" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Main") %>">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Users-CSV-File") %>:</dt>
			<dd><input type="file" name="<portlet:namespace />newUsersFile"></dd>
			
			<dt>&nbsp;</dt>
			<dd>
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="ignoreHeaders" id="ignoreHeaders" checked="checked" onClick="console.log('clicked cb1')">
				<label for="ignoreHeaders"><%= LanguageUtil.get(pageContext, "Ignore-Column-Headers") %></label>
			</dd>
			
			<dt>&nbsp;</dt>
			<dd>
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="updateDuplicatedUsers" />
				<label for="updateDuplicatedUsers"><%= LanguageUtil.get(pageContext, "Update-Duplicated-Users") %></label>
			</dd>

			
			<dt><%= LanguageUtil.get(pageContext, "Tag-your-new-users-optional") %> <a href="#" id="tip1">?</a></dt>
			<dd>
				<textarea id="tagName" name="tagName" cols="20" rows="10" onkeyup="suggestTagsForSearch(this, 'suggestedTagsDiv');" style="height: 100px; width: 300px;" class="tagField"></textarea>
				<div class="callOutBox2 hintBox" style="top:130px;margin-left:320px; _margin-left:20px;">
					<%= LanguageUtil.get(pageContext, "Suggested-Tags") %>: 
					<span id="suggestedTagsDiv"></span>
				</div>
			</dd>
		</dl>
		
		<%  
			boolean isMailingListAdmin = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.isMailingListAdmin(user);
			
			List mailingList = null;
			List roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
			Iterator rolesIt = roles.iterator();
		
			if (isMailingListAdmin)
				mailingList = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.getAllMailingLists();
			else {
				mailingList = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.getMailingListsByUser(user);
				mailingList.add(com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.getUnsubscribersMailingList());
			}
			if (0 < mailingList.size()) {
				Iterator it = mailingList.iterator ();
		%>
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Save-or-append-your-users-to-a-Mailing-List") %>:</dt>
			<dd>
				<input type="radio" dojoType="dijit.form.RadioButton" value="<%= com.liferay.portal.util.Constants.ADD %>" name="actionList" checked="checked" onclick="enableDisable('create');" />
				<%= LanguageUtil.get(pageContext, "New-Mailing-List-Title") %>:
				<input type="text" dojoType="dijit.form.TextBox" name="usermanagerListTitle" id="usermanagerListTitle" tabindex="1" value="" size="20" class="form-text" />
			</dd>
			
			<dd>
				<input type="radio" dojoType="dijit.form.RadioButton" value="<%= com.liferay.portal.util.Constants.SAVE %>" name="actionList" onclick="enableDisable('append');" />
				<%= LanguageUtil.get(pageContext, "Existing-Mailing-List") %>:
				<select dojoType="dijit.form.FilteringSelect" id="usermanagerListInode" name="usermanagerListInode" disabled>
					<option value=""></option>
					<% while (it.hasNext()) {
						com.dotmarketing.portlets.mailinglists.model.MailingList list = (com.dotmarketing.portlets.mailinglists.model.MailingList)it.next();
					%>
						<option value="<%=list.getInode()%>">
							<%=list.getTitle().equals("Do Not Send List")?LanguageUtil.get(pageContext, "message.mailinglists.do_not_send_list_title"):list.getTitle() %>
						</option>
					<% } %>
				</select>
			</dd>
		
			<dt>&nbsp;</dt>
			<dd>
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="allowPublicToSubscribe" />
				<label for="allowPublicToSubscribe"><%= LanguageUtil.get(pageContext, "Allow-Public-to-Subscribe") %></label>
			</dd>
		</dl>
	<% } %>
		
	</div>
	<!-- END Main Tab -->
	
	<!-- START Permissions Tab - DISABLED for 1.9 
	<div id="TabTwo" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
		
		<h2>Put permission stuff here!</h2>
		
	</div>
	END Permissions Tab -->
	
</div>
<!-- END Tabs -->

<div class="clear"></div>

<!-- START Button Row -->
<div class="buttonRow">
	<button dojoType="dijit.form.Button" type="button" name="load" id="loadButton" onClick="doLoadSubmit()" iconClass="uploadIcon">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Load-Users")) %>
	</button>
	<button dojoType="dijit.form.Button" onClick="dijit.byId('sample').show();" iconClass="infoIcon"><%= LanguageUtil.get(pageContext, "help") %></button>
</div>
<!-- END Button Row -->


<!-- START Popup Hint -->
<div id="sample" dojoType="dijit.Dialog" style="display: none">
	<div style="float:right;"><img src="/icon?i=xls" align="absmiddle"> <a href="javascript:downloadCSVTemplate();"><%= LanguageUtil.get(pageContext, "Click-here-to-download-a-csv-sample-file") %></a></div>
	<h3><%= LanguageUtil.get(pageContext, "The-CSV-file-must-be-in-the-following-format") %></h3>
	<p>
		<b><%= LanguageUtil.get(pageContext, "One-line-for-each-record") %></b>
		<b><%= LanguageUtil.get(pageContext, "Fields-separated-by-comma") %></b>
	</p>
	<p>
		<b><%= LanguageUtil.get(pageContext, "Fields-must-be-in-this-order") %>:</b><br/>
		<%= LanguageUtil.get(pageContext, "message.usermanager.fields.order") %><%	for (int j=1; j<=numberGenericVariables; j++) { %>, <%=LanguageUtil.get(pageContext, "user.profile.var"+j)%><%}%>.
	</p>
	
	<p>
		<b><%= LanguageUtil.get(pageContext, "Required") %>:</b><br/>
		<%= LanguageUtil.get(pageContext, "message.usermanager.fields.required") %>
	</p>
	<p>
		<b><%= LanguageUtil.get(pageContext, "Optional-may-be-omitted") %>:</b><br/>
		<%= LanguageUtil.get(pageContext, "message.usermanager.fields.omitted") %><%	for (int j=1; j<=numberGenericVariables; j++) { %>, <%=LanguageUtil.get(pageContext, "user.profile.var"+j)%><%}%>.
	</p>
</div>
<!-- START Popup Hint -->					

<!-- START Hint -->
	<span dojoType="dijit.Tooltip" connectId="tip1" id="one_tooltip">
		<p><%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></p>
		<p><%= LanguageUtil.get(pageContext, "Tags-are-descriptors-that-you-can-assign-to-users-Tags-are-a-little-bit-like-keywords") %></p>
	</span>
<!-- END Hnt -->

<%
String layer = request.getParameter("layer");
layer = (layer == null ? "main" : layer);
%>

</form>
</liferay:box>
