
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%

	java.util.List<Role> all_roles = APILocator.getRoleAPI().findAllAssignableRoles(false); 

	List<Role> userRoles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
	
	int totalNumberRoles = all_roles.size();
	int roleColumnWidth = 50;
	int separatorColumnWidth = 10;

	if (numberColumnsRoles == 2) {
		roleColumnWidth = 35;
	}

	double rolesPerColumn = java.lang.Math.ceil(((double)(totalNumberRoles)) / ((double)numberColumnsRoles));
	int numberRolesUser = userRoles.size();

	

%>

<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%><script language="JavaScript">
<% 
	String userRolesStr = "";
	for (int j = 0; j < userRoles.size(); j++) {
		Role userRole = (Role) userRoles.get(j); 
		userRolesStr += userRole.getId()+",";
	}
%>
	var userRoles = "<%=userRolesStr%>";

	function checkRead(checkId) {
		if (('' + dijit.byId('readRole_' + checkId).attr('value')) == 'false') {
			if (userRoles.indexOf(checkId) >= 0) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Warning")) %>: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.warning.roles")) %>');
			}
			dijit.byId('writeRole_' + checkId).attr('value', false);
		}
	}
	
	function checkWrite(checkId) {
		if (('' + dijit.byId('writeRole_' + checkId).attr('value')) == 'false') {
			if (userRoles.indexOf(checkId) >= 0) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Warning")) %>: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.warning.roles")) %>');
			}
		} else {
			dijit.byId('readRole_' + checkId).attr('value', checkId);
		}
	}
	
	function checkAllWrite(element) {
<% for (int i=0; i<numberColumnsRoles; i++) { %>
		var allWriteCheck<%=i%> = dijit.byId('allWriteCheck<%=i%>');
		var allReadCheck<%=i%> = dijit.byId('allReadCheck<%=i%>');
<% } %>
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = writeRole.length;
	
		if (element.checked) {
<% for (int i=0; i<numberColumnsRoles; i++) { %>
			allWriteCheck<%=i%>.attr('value', true);
			allReadCheck<%=i%>.attr('value', true);
<%	} %>
		}
		else {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Warning")) %>: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.warning.roles")) %>');
<% for (int i=0; i<numberColumnsRoles; i++) { %>
			allWriteCheck<%=i%>.attr('value', false);
<%	} %>
		}	
	
		for (var i = 0;i < size; i++)
		{
			if (!element.checked) {
				dijit.byId(writeRole[i].id).attr('value', false);
			}
			else {
				dijit.byId(readRole[i].id).attr('value', true);
				dijit.byId(writeRole[i].id).attr('value', true);
			}
		}
	}
	
	function checkAllRead(element) {
<%	for (int i=0; i<numberColumnsRoles; i++) { %>
		var allWriteCheck<%=i%> = dijit.byId('allWriteCheck<%=i%>');
		var allReadCheck<%=i%> = dijit.byId('allReadCheck<%=i%>');
<%	} %>
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = readRole.length;

		if (element.checked) {
<% for (int i=0; i<numberColumnsRoles; i++) { %>
			allReadCheck<%=i%>.attr('value', true);
<%	} %>
		}
		else {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Warning")) %>: <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.warning.roles")) %>');
<% for (int i=0; i<numberColumnsRoles; i++) { %>
			allWriteCheck<%=i%>.attr('value', false);
			allReadCheck<%=i%>.attr('value', false);
<%	} %>
		}
	
		for (var i = 0;i < size; i++)
		{
			if (!element.checked) {
				dijit.byId(readRole[i].id).attr('value', false);
				dijit.byId(writeRole[i].id).attr('value', false);
			}
			else {
				dijit.byId(readRole[i].id).attr('value', true);
			}
		}
	}

</script>
<div style="min-height:200px; height:200px; width:100%, background-color:white; overflow: auto" id="permissionRoles">

<table border="0" cellpadding="0" cellspacing="0"  width="100%" >
	<tr align="center">
<%	
	int rowCount = 0;
	for (int columnIndex = 0; columnIndex < numberColumnsRoles; columnIndex++) {
%>
		<td valign="top" <%=columnIndex%2==0?"":"style=\"padding-left:15px;\""%>  >
			<table class="listingTable">
				<tr>
					<th><%= LanguageUtil.get(pageContext, "role-name") %></th>
					<th style="text-align:center;"><%= LanguageUtil.get(pageContext, "view") %></th>
					<th style="text-align:center;"><%= LanguageUtil.get(pageContext, "modify") %></th>					
				</tr>
				<tr class="selectAll">
				  <td><%= LanguageUtil.get(pageContext, "Check-All-/-Uncheck-All") %></td>
				  <td align="center"><input type="checkbox" dojoType="dijit.form.CheckBox" onClick="checkAllRead(this)" name="allReadCheck<%=columnIndex%>" id="allReadCheck<%=columnIndex%>" value="true" /></td>
				  <td align="center"><input type="checkbox" dojoType="dijit.form.CheckBox" onClick="checkAllWrite(this)" name="allWriteCheck<%=columnIndex%>" id="allWriteCheck<%=columnIndex%>" value="true" /></td>
				</tr>
<%		
		int rowIndex = 0;
		for (; ((rowIndex < rolesPerColumn) && (rowCount < totalNumberRoles)); rowIndex++,rowCount++) { 
			Role role = all_roles.get(rowCount);
			if(role.getName().startsWith("user-")) continue;
			String str_style = "";
			if ((rowIndex % 2) == 0) {
				str_style = "class=alternate_2";
			}
			else{
				str_style = "class=alternate_1";
            }

			boolean checkedRead = false;
			boolean checkedWrite = false;

			if (!InodeUtils.isSet(userFilterInode)) {
				// new user filter, using roles from creator user
				for (int j = 0; j < userRoles.size(); j++) {
					Role userRole = (Role) userRoles.get(j);
					if (userRole.getId().equalsIgnoreCase(role.getId())) {
						checkedRead = true;
						checkedWrite = true;
						break;
					}
				}
			}
			else {
				// existing user filter, using roles from user filter
				if (readRoles.contains(role)) {
					checkedRead = true;
				}
				if (writeRoles.contains(role)) {
					checkedWrite = true;
				}
			}
%>
				<tr <%=str_style%> >
					<td >
						<%=role.getName()%>
					</td>
					<td align="center">
						<input type="checkbox" dojoType="dijit.form.CheckBox" onClick="checkRead('<%= role.getId() %>')" name="readRole" id="readRole_<%= role.getId() %>" value="<%= role.getId() %>" <% if (checkedRead) { %>checked="true"<% } %>> 
					</td>
					<td align="center">
						<input type="checkbox" dojoType="dijit.form.CheckBox" onClick="checkWrite('<%= role.getId() %>')" name="writeRole" id="writeRole_<%= role.getId() %>" value="<%= role.getId() %>" <% if (checkedWrite) { %>checked="true"<% } %>>
					</td>
				</tr>
<%		} %>
			</table>
		</td>
		
<%	
	}
%>

</table>

</div>
<br>
