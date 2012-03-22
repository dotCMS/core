<%@page import="com.dotmarketing.business.APILocator" %>

<%

User systemUser =  new User();
try{
    
    systemUser = APILocator.getUserAPI().getSystemUser();
}
catch(Exception e){
    
}


%>

<script type="text/javascript">

   function downloadUsers(){
	   
	    checkFullCommand();
		var users = getCheckboxSelectedUsersValues();
		var fullCommand = dijit.byId("fullCommand").attr('value');
		var condition = "&users=" + users + "&fullCommand=" + fullCommand + "&referrer=<%= redirect %>";
		window.location.href='<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="exportExcel" /></portlet:actionURL>'+condition;
   }
   
   function viewUsersList() {
   		document.getElementById("users_list").style.display = "";
   }
   
   
<%
	referrer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
	redirect = java.net.URLEncoder.encode(referrer + "&cmd=search");
%>

   
   function getCheckboxSelectedUsersValues() {
   		var result = "";
   		var cbUserIdList = document.getElementsByName('users');
   		
   		if ((cbUserIdList != null) || (0 < cbUserIdList.length)) {
   			for (var i = 0; i < cbUserIdList.length; ++i) {
   				if (dijit.byId(cbUserIdList[i].id).attr('value')) {
   					result = result + cbUserIdList[i].value + ",";
   				}
   			}
   		}
   		
   		return result;
   }
	
	function checkUncheckAll() {
		var checkAll = dijit.byId("fullCommand");
		var cbUserIdList = document.getElementsByName('users');
		
		if (checkAll.attr('value')) {
			for (var i = 0; i < cbUserIdList.length; ++i) {
				dijit.byId(cbUserIdList[i].id).attr('value', true);
				selectAllUsersMessage();
			}
		} else {
			for (var i = 0; i < cbUserIdList.length; ++i) {
				dijit.byId(cbUserIdList[i].id).attr('value', false);
				clearAllUsersMessage();
			}
		}
	}

	function messageCheckUncheckAll() {
		var checkAll = dijit.byId("fullCommand");

		if (checkAll.attr('value'))
			checkAll.attr('value', false);
		else
			checkAll.attr('value', true);

		checkUncheckAll();
	}

	function checkFullCommand() {
		if (document.getElementById("fullCommand").value != "true") {
			var checked = false;
			for (var i = 0; i < cbUserIdList.length; ++i) {
   				check = document.getElementById(cbUserIdList[i]);
   				if (check.checked) {
					checked = true;
				}
   			}
			if (!checked) {
				document.getElementById("fullCommand").value = true;
			}
		}
	}


	function selectAllUsersMessage() {
		var message = document.getElementById('message');
		var html = totalUsers + ' <%= LanguageUtil.get(pageContext, "Users") %> <%= LanguageUtil.get(pageContext, "selected") %>. <a href="javascript: void(0);" onclick="messageCheckUncheckAll();"><%= LanguageUtil.get(pageContext, "Uncheck-all") %> ' + totalUsers + ' <%= LanguageUtil.get(pageContext, "matches") %></a>';
		message.innerHTML = html;
	}

	function clearAllUsersMessage() {	
		var message = document.getElementById('message');
		var html = '<a href="javascript: void(0);" onclick="messageCheckUncheckAll();"><%= LanguageUtil.get(pageContext, "Check-all") %> ' + totalUsers + ' <%= LanguageUtil.get(pageContext, "matches") %></a>';
		message.innerHTML = html;
	}

	function checkUncheckUser(element) {
		if (!element.checked) {
			dijit.byId('fullCommand').attr('value', false);
			clearAllUsersMessage();
		}
	}
	
	function doNextSubmit() {
		var form = document.getElementById('<portlet:namespace />fm');
		<% if (viewUserManager) { %>
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="<%=String.valueOf(pageNumber+1)%>" /></portlet:renderURL>';
		<% } else { %>
		form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="<%=String.valueOf(pageNumber+1)%>" /></portlet:actionURL>';
		<% } %>
		form.submit();
	}
	
	function doPreviousSubmit() {
		var form = document.getElementById('<portlet:namespace />fm');
		<% if (viewUserManager) { %>
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="<%=String.valueOf(pageNumber-1)%>" /></portlet:renderURL>';
		<% } else { %>
		form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="<%=String.valueOf(pageNumber-1)%>" /></portlet:actionURL>';
		<% } %>
		form.submit();
	}

	function doSaveSubmit() {
<%
	Object[][] result = (Object[][]) request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLIST);
	if ((result == null) || ((result.length == 0))) {
%>
		if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.confirm.create.empty.mailinglist")) %>'))
			return;
<%
	} else {
		int totalMatchesToShow = 0;
		for (int m = 0; m < result.length; m++) {
			boolean hasReadPermission = "true".equalsIgnoreCase(result[m][13].toString())?true:false;

			if (hasReadPermission)
				totalMatchesToShow++;
		}
		if (totalMatchesToShow == 0) {
%>
		if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.confirm.create.empty.mailinglist")) %>'))
			return;
<%
		}
	}
%>
		var ele = document.getElementsByName("usermanagerListTitle");
		if(ele[0].value.length <1 ){
			ele[0].focus();
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.title")) %>');
			return;
		}

	    var users = getCheckboxSelectedUsersValues();
	    var condition = "&users=" + users + "&usermanagerListTitle=" + ele[0].value + "&allowPublicToSubscribe=" + dijit.byId('allowPublicToSubscribe').attr('value');

		checkFullCommand();
		var form = document.getElementById('<portlet:namespace />fm');
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SAVE%>" /></portlet:actionURL>'+ condition;
		form.submit();
	}

	function doAppendSubmit() {
		var ele = document.getElementsByName("usermanagerListInode");
		if(ele[0].value.length <1 ){
			ele[0].focus();
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.select.mailinglist")) %>');
			return;
		}
		
		var form = document.getElementById('<portlet:namespace />fm');

		var users = getCheckboxSelectedUsersValues();
		var condition = "&users=" + users + "&usermanagerListInode=" + ele[0].value;

		checkFullCommand();
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.UPDATE%>" /></portlet:actionURL>'+ condition;
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.append.list")) %>'))
			form.submit();
	}

	function doRemoveSubmit() {
		var ele = document.getElementsByName("usermanagerListInode");
		if(ele[0].value.length <1 ){
			ele[0].focus();
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.select.mailinglist")) %>');
			return;
		}
		
		var form = document.getElementById('<portlet:namespace />fm');

		var users = getCheckboxSelectedUsersValues();
		var condition = "&users=" + users + "&usermanagerListInode=" + ele[0].value;

		checkFullCommand();
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.EDIT%>" /></portlet:actionURL>'+ condition;
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.remove.list")) %>'))
			form.submit();
	}

<%
	java.util.Hashtable params1 = new java.util.Hashtable ();
	params1.put("struts_action", new String [] {"/ext/mailinglists/view_mailinglists"} );
	
	String referrer1 = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params1);
	String redirect1 = java.net.URLEncoder.encode(referrer1);
%>

	function doSaveFilterSubmit() {
		var ele = document.getElementsByName("userFilterTitle");
		if(ele[0].value.length <1 ){
			ele[0].focus();
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.alert.title.userfilter")) %>');
			return;
		}
		var form = document.getElementById('<portlet:namespace />fm');

		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SAVE%>" /></portlet:actionURL>&referrer=<%= redirect1 %>&userFilterListInode=' + document.getElementById('userFilterListInode').value + '&userFilterTitle=' + ele[0].value;
		form.submit ();
	}

	function doDeleteFilterSubmit() {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.delete.userfilter")) %>')) {
			var form = document.getElementById('<portlet:namespace />fm');
			var url = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" /><portlet:param name="cmd" value="deleteUserFilter" /></portlet:actionURL>';
			url += '&userFilterListInode=' + document.getElementById('userFilterListInode').value + '&redirect=<%=redirect%>';
			form.action = url;
			form.submit();
		}
	}
</script>
<%
	Object[][] matches = (Object[][])request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLIST);
	if (matches == null) matches = new String[0][0];

	int	totalUsers = 0;
	try {
		totalUsers = (Integer) request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLISTCOUNT);
	}catch(Exception e){
		totalUsers = 0;
	}

	int totalMatchesToShow = 0;
	for (int m = 0; m < matches.length; m++) {
		boolean hasReadPermission = "true".equalsIgnoreCase(matches[m][13].toString()) ? true : false;
		boolean isSystemUser = systemUser.getUserId().equals(matches[m][0].toString()) ? true : false;

		if (hasReadPermission && !isSystemUser)
			totalMatchesToShow++;
		else
			totalUsers--;
	}

%>
	<script>
	var totalUsers = '<%=totalUsers%>';
	var pageNumber = '<%=pageNumber%>';
	var perPage = '<%=perPage%>';					
	</script>

<div id="users_list">
	<% if (totalMatchesToShow > 0) { %>
	
		<div id="upReturnTagActions" display: none;">
			<B><span id="upReturnTagMessage"></span></B>
		</div>
		
		<div class="yui-g" style="margin:3px 5px 10px 10px;">
		   	<div id="message" class="yui-u first" style="font-size:85%;padding-top:5px;">
		   		<a href="javascript: void(0);" onclick="messageCheckUncheckAll();"><%= LanguageUtil.get(pageContext, "Check-all") %> <%=totalUsers%> <%= LanguageUtil.get(pageContext, "matches") %></a>
			</div>
		    <div class="yui-u" style="text-align:right;"">
				<button dojoType="dijit.form.ToggleButton" id="downLoadButton" iconClass="excelIcon" onclick="downloadUsers();return false;">
					<%= LanguageUtil.get(pageContext, "Download-to-Excel") %>
				</button>
		    </div>
		</div>
	<%	} %>
	
<!-- Results Scroll Window -->
<div id="resultsWrapper" style="overflow-y:auto;overflow-x:hidden;border-bottom:1px solid #ccc;margin-bottom:5px;">
	<table class="listingTable">
		<tr>
			<th><input type="checkbox" dojoType="dijit.form.CheckBox" name="fullCommand" id="fullCommand" value="true" onclick="checkUncheckAll();" /></th>
			<th><%= LanguageUtil.get(pageContext, "name") %></th>
			<%
	        if (company.getAuthType().equalsIgnoreCase(Company.AUTH_TYPE_ID)) {%>
				<th><%= LanguageUtil.get(pageContext, "User-ID") %></th>
			<%}else{%>
				<th><%= LanguageUtil.get(pageContext, "Email-Address") %></th>
			<%}%>
			<th><%= LanguageUtil.get(pageContext, "Created") %></th>	
		</tr>
	<%
		if (totalMatchesToShow == 0) {
	%>

		<tr class="alternate_1">
			<td colspan="4" align="center"><%= LanguageUtil.get(pageContext, "No-users-found") %></td>
		</tr>
		
	 <% } else {
			int index = 0;

	 		for (int m = 0; m < matches.length; m++) {
	 			Object[] match = matches[m];
	 			boolean hasReadPermission = "true".equalsIgnoreCase(matches[m][13].toString())?true:false;
	 			boolean hasWritePermission = "true".equalsIgnoreCase(matches[m][14].toString())?true:false;
				
	 			if (hasReadPermission && !systemUser.getUserId().equals(match[0].toString())) {
					String str_style = (index%2==0) ? "class=\"alternate_2\"" : "class=\"alternate_1\"";
					index++;
	 %>
	 	<tr <%=str_style%>>
	 		<td>
	 			<input type="checkbox" dojoType="dijit.form.CheckBox" name="users" id="users_<%= com.dotmarketing.util.UtilMethods.webifyString(match[0].toString()) %>" value="<%= com.dotmarketing.util.UtilMethods.webifyString(match[0].toString()) %>" onclick="checkUncheckUser(this);" />
	 			<input type="hidden" name="user_Id" value="<%= match[0]%>">
	 		
	 		</td>
			<td><%=match[1]%> <%=com.dotmarketing.util.UtilMethods.webifyString(match[3].toString())%></td>
			<%
	        if (company.getAuthType().equals(Company.AUTH_TYPE_ID)) {%>
				<td><%=com.dotmarketing.util.UtilMethods.webifyString(match[0].toString())%></td>
			<%}else{%>
				<td><%=com.dotmarketing.util.UtilMethods.webifyString(match[4].toString())%></td>
			<%}%>

			<td><%=com.dotmarketing.util.UtilMethods.webifyString(match[12].toString())%></td>
			
			
		</tr>
		
		
	 <%
	 			}
	 	 	}
	 	} 
    %>
	</table>
 </div>
</div>
	


	<!-- Start Pagination -->
				<div class="yui-gb buttonRow" style="text-align:center;">
					<div class="yui-u first" style="text-align:left;">
						<div id="previousDiv" style="<%=  (1 < pageNumber)?"":"display: none;"%>">
							<button dojoType="dijit.form.Button" name="previous" id="previous" iconClass="previousIcon" onClick="doPreviousSubmit()">
			                 <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Previous")) %>
		                    </button>
						</div>&nbsp;
					</div>
					<div class="yui-u">
						<div id="matchingResultsBottomDiv"></div>
					</div>
					<div class="yui-u" style="text-align:right;">
						<div id="nextDiv" style="<%=(pageNumber < ((totalUsers/perPage) + (totalUsers%perPage*0.001)))?"":"display: none;"%>">
							<button dojoType="dijit.form.Button" name="next" id="next" iconClass="nextIcon" onClick="doNextSubmit()">
			                  <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Next")) %>
		                  </button>
						</div>&nbsp;
					</div>
				</div>
			<!-- END Pagination -->

	
<div id="users_groups" style="display: none;">
	<table border="0" cellpadding="4" cellspacing="1" width="95%" bgcolor="#eeeeee">
		<tr class="beta">
			<td align="center"><strong><%= LanguageUtil.get(pageContext, "Assign-Groups-to-User-List-this-will-remove-all-currently-assigned-Groups") %></strong></td>
		</tr>
		<tr>
			<td align="center">
				<button dojoType="dijit.form.Button"  name="view_users_list" onClick="viewUsersList()" >
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Users-List")) %>
                </button>

			</td>
		</tr>
	</table>
</div>
	
<div id="users_roles" style="display: none;">
	<table border="0" cellpadding="4" cellspacing="1" width="95%" bgcolor="#eeeeee">
		<tr class="beta">
			<td align="center"><strong><%= LanguageUtil.get(pageContext, "Assign-Roles-to-User-List-this-will-remove-all-currently-assigned-Roles") %></strong></td>
		</tr>
		
		<tr>
			<td align="center">
				<button dojoType="dijit.form.Button"  name="view_users_list"  onClick="viewUsersList()">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Users-List")) %>
                </button>
               
		  	</td>
		</tr>
	</table>
</div>
<!-- Results Scroll Window -->

<div id="downReturnTagActions" style="z-index: 99;  display: none; height: 0px;">
	<table cellpadding="4" cellspacing="1" width="95%" bgcolor="#eeeeee">
		<tr><td align="center"><font face="Arial" size="2" color="#ff0000"><B><span id="downReturnTagMessage"></span></B></font></td></tr>
	</table>
</div>

<%
	Iterator it = null;
	if (viewUserManager) {
		boolean isMailingListAdmin = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.isMailingListAdmin(user);
		boolean isMailingListEditor = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.isMailingListEditor(user);
		boolean isCMSAdmin = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
		
		if (isMailingListAdmin || isMailingListEditor || isCMSAdmin) {
			List mailingList = (List)request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_VIEW);
			it = mailingList.iterator ();
%>
<div style="text-align:center;">
	<div dojoType="dijit.form.DropDownButton" iconClass="saveIcon">
		<span class="saveIcon"><%= LanguageUtil.get(pageContext, "save") %></span> 
		<div dojoType="dijit.Menu" id="saveMenu">	
			<div dojotype="dijit.MenuItem" id="saveAsNewList" onclick="dijit.byId('newMailingList').show();"><%= LanguageUtil.get(pageContext, "as-a-new-list") %></div>
			<div dojotype="dijit.MenuItem" id="saveToExistingList" onclick="dijit.byId('existingMailingList').show();"><%= LanguageUtil.get(pageContext, "to-an-existing-list") %></div>
			<div dojotype="dijit.MenuItem" id="saveAsDinamicList" onclick="dijit.byId('newDinamicList').show();"><%= LanguageUtil.get(pageContext, "as-a-dynamic-list") %></div>
		</div>
	</div>
</div>
<%
		}
	}
%>

<!-- START New List Pop-up -->
	<div id="newMailingList" dojoType="dijit.Dialog" style="display: none;width:450px;">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "New-Mailing-List-Title") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" class="form-text" name="usermanagerListTitle" id="usermanagerListTitle" size="20" value=""/></dd>
			<dt>&nbsp;</dt>
			<dd>
				<input type="checkbox" dojoType="dijit.form.CheckBox" name="allowPublicToSubscribe" id="allowPublicToSubscribe" value="yes"/>
				<label for="allowPublicToSubscribe"><%= LanguageUtil.get(pageContext, "Allow-Public-to-Subscribe") %></label>
			</dd>
		</dl>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" onclick="doSaveSubmit()" iconClass="saveIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-results-as-New-List")) %>
			</button>
		</div>
	</div>
<!-- End New List Pop-up -->

<!-- START Existing List Pop-up -->
	<div id="existingMailingList" dojoType="dijit.Dialog" style="display:none; width:450px;">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Existing-Mailing-List") %>:</dt>
			<dd>
				<select dojoType="dijit.form.FilteringSelect" autocomplete="false" name="usermanagerListInode" id="usermanagerListInode">
					<option value=""></option>
					<% if (it != null) { %>
						<% while (it.hasNext()) { 
								com.dotmarketing.portlets.mailinglists.model.MailingList list = (com.dotmarketing.portlets.mailinglists.model.MailingList)it.next();
						%>
							<option value="<%=list.getInode()%>"><%= ((list.getTitle() != null) && list.getTitle().equals("Do Not Send List")) ? LanguageUtil.get(pageContext, "message.mailinglists.do_not_send_list_title") : list.getTitle()%></option>
						<% } %>
					<% } %>
				</select>
			</dd>
		</dl>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button"  onClick="doAppendSubmit()" iconClass="plusIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Append-results-to-a-list")) %>
			</button>
			<button dojoType="dijit.form.Button" onClick="doRemoveSubmit()" iconClass="deleteIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remove-results-from-list")) %>            
			</button>
		</div>
	</div>
<!-- END Existing List Pop-up -->

<!-- START As a Dynamic List Pop-up -->
	<div id="newDinamicList" dojoType="dijit.Dialog" style="display: none">
		<%= LanguageUtil.get(pageContext, "Add-Role-Permissions-to-Search-Criteria") %>
		<div class="buttonRow">
			<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "User-Filter-Title") %>:</span>
			<input type="text" class="form-text" name="userFilterTitle" id="userFilterTitle" size="30" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getUserFilterTitle()) ? form.getUserFilterTitle() : "" %>" />
	        <button dojoType="dijit.form.Button" onclick="doSaveFilterSubmit()" iconClass="saveIcon">
	             <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Criteria-as-Filter")) %>
	        </button>
		</div>
		<div class="clear"></div>
		<% if ((form.getUserFilterListInode() != null) && (InodeUtils.isSet(form.getUserFilterListInode()))) { %>
			<div class="buttonRow">
				<input type="hidden" name="userFilterListInode" id="userFilterListInode" value="<%= form.getUserFilterListInode() %>" />
		        <button dojoType="dijit.form.Button" onclick="doDeleteFilterSubmit()" iconClass="deleteIcon">
		             <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Filter")) %>   
		        </button>
			</div>
		<% } else { %>
			<input type="hidden" name="userFilterListInode" id="userFilterListInode" value="" />
		<% } %>
				
		<table border="0" cellpadding="2" cellspacing="0" align="center" width="100%" class="portletBox">
			
			<tr>
				<td>
					<% int numberColumnsRoles = 1; %>
					<% String width = "400px"; %>
					<%@ include file="/html/portlet/ext/usermanager/select_permissions_inc.jsp" %>
				</td>
			</tr>
		</table>
	</div>
<!-- END As a Dynamic List Pop-up -->