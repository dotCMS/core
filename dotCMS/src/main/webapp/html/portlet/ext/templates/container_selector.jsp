<%@page import="com.dotmarketing.util.UtilMethods"%>


<%if(request.getParameter("inode") != null){%>
	
	<%com.dotmarketing.portlets.containers.model.Container container = (com.dotmarketing.portlets.containers.model.Container) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"), com.dotmarketing.portlets.containers.model.Container.class);%>
	<%
		com.dotmarketing.beans.Identifier i = com.dotmarketing.business.APILocator.getIdentifierAPI().find(container);
	%>

	<script>
			opener.insertAtCursor("\n##<%=com.dotmarketing.util.UtilMethods.javaScriptify(container.getTitle()).replace("\"", "\\\"")%>\n#parseContainer('<%=i.getInode()%>')");
			window.close();
	</script>

<%return;}%>

<%@ include file="/html/portlet/ext/templates/init.jsp" %>
<html>
<head>
	<title>Select Container</title>
	<script language="JavaScript">

		function browseContainers() {
			popup = "true";
		    view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted = " + com.dotmarketing.db.DbConnectionFactory.getDBFalse() + ")","UTF-8") %>";
  			pictureWindow = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/view_containers_popup" /></portlet:actionURL>&view=' + view + '&popup=inode&child=true&page_width=900', "newwin", 'width=1010,height=400,scrollbars=yes,resizable=yes');
   		}
   		
		function submitFM(form) {
			var inode = document.getElementById("inode").value;
			window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="<%= com.dotmarketing.util.Constants.TEMPLATE_ADD_CONTAINER%>" /></portlet:actionURL>&child=true&inode=' + inode;
		}
		
		function onactivatewindow(){
			var inode = document.getElementById("inode").value;
			if (!isInodeSet(inode))	
				dijit.byId("insertB").setAttribute("disabled", true);
			else
				dijit.byId("insertB").setAttribute("disabled", false);
		}
	</script>
</head>


<body bgcolor="Silver" leftmargin="0" marginheight="0" marginwidth="0" rightmargin="0" topmargin="0" onLoad="self.focus();" onFocus="onactivatewindow();">

<table border="0" cellpadding="10" cellspacing="0" width="100%">

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>" id="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="<%=com.dotmarketing.util.Constants.TEMPLATE_ADD_CONTAINER%>">

<tr>
	<td>
		<fieldset>
			<legend><font face="MS Sans Serif" size="1"><%= LanguageUtil.get(pageContext, "Container-Information") %></font></legend>

			<table border="0" cellpadding="8" cellspacing="0">
			<tr>
				<td>
					<table border="0" cellpadding="0" cellspacing="0">
					<tr>
						<td>
							<font face="MS Sans Serif" size="1"><%= LanguageUtil.get(pageContext, "Container") %>:</font>
						</td>
						<td>&nbsp;
							
						</td>
						<td nowrap>
						<input type="hidden" name="inode" id="inode"><br>
						<input type="text" name="selectedinode" style="width:150px" id="selectedinode">
                        <button dojoType="dijit.form.Button" onClick="browseContainers()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "browse-for-container")) %>
                        </button>
						</td>
					</tr>
					</table>
				</td>
			</tr>
			
			</table>
		</fieldset>

		<table border="0" cellpadding="8" cellspacing="0" width="100%">
		<tr>
			<td>
			</td>
			<td align="right" nowrap>
                <button dojoType="dijit.form.Button" onClick="self.close();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %></button>
                &nbsp;
                <button dojoType="dijit.form.Button"  onClick="submitFM(document.getElementById('fm'))" id="insertB" disabled="true">
				   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Insert")) %>
                </button>
			</td>
		</tr>
		</table>
	</td>
</tr>

</form>

</table>

</body>

</html>
