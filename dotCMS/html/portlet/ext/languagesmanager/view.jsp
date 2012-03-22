<%@ include file="/html/portlet/ext/languagesmanager/init.jsp" %>
<%
java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LIST);

%>

<%@page import="com.dotmarketing.util.Config"%>
<table border="0" cellpadding="5" cellspacing="0" width="95%" height=20>


<% for (int k=0; k < list.size()  ;k++) { %>
	<%long longLanguageId =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(k)).getId();%>
	<%String strLanguage =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(k)).getLanguage();%>
	<%String strLangCode =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(k)).getLanguageCode();%>
	<%String strCountryCode =((com.dotmarketing.portlets.languagesmanager.model.Language)list.get(k)).getCountryCode();%>
<tr>
		<td >
			<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /><portlet:param name="id" value="<%=String.valueOf(longLanguageId)%>" /><portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>">
				<img src="/html/images/languages/<%=strLangCode%>_<%=strCountryCode%>.gif"  border="0" />
			</a>
		</td>
		<td>

		<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /><portlet:param name="id" value="<%=String.valueOf(longLanguageId)%>" /><portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>">
			<%=strLanguage%>  <%= LanguageUtil.get(pageContext, "Variables") %>
		</a> 	
		</td>
		<td>	
			<a  href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /><portlet:param name="id" value="<%=String.valueOf(longLanguageId)%>" /><portlet:param name="<%= Constants.CMD %>" value="edit" /></portlet:actionURL>"> 
				(<%= LanguageUtil.get(pageContext, "properties") %>)
			</a>
		</td>
	</tr>
<%}%>
	<tr>
	 <td colspan="3" align="center">
			<font class="gamma" size="2">
   				
   				<a  class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "All-Languages") %></a>
				|
				<a  class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /><portlet:param name="id" value="" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Add-Language") %></a>
   				
			</font>
		</td>
	</tr>
</table>
		
