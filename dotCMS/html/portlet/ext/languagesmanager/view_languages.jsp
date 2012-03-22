<%@ include file="/html/portlet/ext/languagesmanager/init.jsp" %>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LIST);
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/languages_manager/view_languages_manager"});
String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
%>

<script language="Javascript">
function addLanguage(){
	window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"><portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /><portlet:param name="id" value="" /></portlet:actionURL>';
}
function editDefault(){
	window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /></portlet:actionURL>&cmd=edit';
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Languages")) %>' />

<div class="buttonRow" style="text-align:right;">
	<button dojoType="dijit.form.Button" onClick="editDefault" iconClass="editIcon"><%= LanguageUtil.get(pageContext, "Edit-Default-Language-Variables") %></button>
	<button dojoType="dijit.form.Button" onClick="addLanguage" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "Add-New-Language") %></button>
</div>
<table class="listingTable">
	<tr>
		<th colspan="2"><%= LanguageUtil.get(pageContext, "Languages") %></th>
	</tr>

	<%int l = 0;%>
	<% for (int k=0; k < (list.size()+1)/2;k++) { %>

	<tr>
		<% for (int m=0; m < 2  ;m++) { %>
			<td>
				<%if(l< list.size()){%>
					<%long longLanguageId =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getId();%>
					<%String strLanguage =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getLanguage();%>
					<%String strLangCode =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getLanguageCode();%>
					<%String strCountryCode =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(l)).getCountryCode();%>
					<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>">
						<portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
						<portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
						<portlet:param name="referer" value="<%= referer %>" />
						<portlet:param name="<%= Constants.CMD %>" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit")) %>' /></portlet:actionURL>">
						<img src="/html/images/languages/<%=strLangCode%>_<%=strCountryCode%>.gif"  border="0" />
					</a>
                  
                    <%=strLanguage%>:
					<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>">
						<portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" />
						<portlet:param name="id" value="<%= String.valueOf(longLanguageId) %>" />
						<portlet:param name="referer" value="<%= referer %>" />
						<portlet:param name="<%= Constants.CMD %>" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit")) %>' /></portlet:actionURL>">
						 <%= LanguageUtil.get(pageContext, "Edit-Variables") %>
					</a>
					|
					<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>">
						<portlet:param name="struts_action" value="/ext/languages_manager/edit_language" />
						<portlet:param name="id" value="<%=String.valueOf(longLanguageId)%>" />
						<portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>">
						<%= LanguageUtil.get(pageContext, "Edit-Language") %>
					</a>
				 <%}%>
			</td>
			<%l++;%>
		<%}%>
	</tr>
<%}%>
</table>
</liferay:box>
