<%@page import="com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.beans.Host"%>

<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

<%
	List<Map<String, Object>> references = (List<Map<String, Object>>) request.getAttribute("references");
%>


<%@page import="com.dotmarketing.business.APILocator"%><br/>

<table width="100" class="listingTable">
	<tr>
		<td colspan="3" align="center">
		  <strong><%= LanguageUtil.get(pageContext, "This-piece-of-content-is-referenced-by-the-following-Pages-Containers") %>:</strong>
		</td>
	</tr>
	<tr class="header">
		<th><%= LanguageUtil.get(pageContext, "Host") %></th>
		<th><%= LanguageUtil.get(pageContext, "Page") %></th>
		<th><%= LanguageUtil.get(pageContext, "Container") %></th>
		<th><%= LanguageUtil.get(pageContext, "Page-Owner") %></th>
		<th><%= LanguageUtil.get(pageContext, "Persona") %></th>
	</tr>
<%
	int Cmod=0;
   	for (Map<String, Object> reference : references) {
    	IHTMLPage htmlpageRef = (IHTMLPage)reference.get("page");
    	Container containerRef = (Container)reference.get("container");
    	String persona = (String)reference.get("persona");
		Host host = APILocator.getHTMLPageAssetAPI().getParentHost(htmlpageRef);
		
		String str_style2 = "";
		if ((Cmod % 2) == 0) {
			str_style2 = "class=alternate_1";
		}
		else{
			str_style2 = "class=alternate_2";
		}
		Cmod++;
		String urlWithLang = htmlpageRef.getURI() + "&language_id=" + htmlpageRef.getLanguageId();
%>
	<tr <%= str_style2 %> >
		<td><%= host.getName() %></td>
		<td>
			<a href="/dotAdmin/#/edit-page/content?url=<%=urlWithLang%>" class="beta" target="_top">
				<%= UtilMethods.escapeHTMLSpecialChars(htmlpageRef.getTitle()) %>
			</a>
		</td>
		<td><%= containerRef.getTitle() %></td>
		<td><%= UtilMethods.getUserFullName(htmlpageRef.getModUser()) %></td>
		<td><%= persona %></td>
	</tr>
	<%
	}
%>
</table>


