<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
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
		<th><%= LanguageUtil.get(pageContext, "Page") %></th>
		<th><%= LanguageUtil.get(pageContext, "Container") %></th>
		<th><%= LanguageUtil.get(pageContext, "Page-Owner") %></th>
	</tr>
<%
	int Cmod=0;
   	for (Map<String, Object> reference : references) {
    	HTMLPage htmlpageRef = (HTMLPage)reference.get("page");
    	Container containerRef = (Container)reference.get("container");
		Host host = APILocator.getHostAPI().findParentHost(htmlpageRef, APILocator.getUserAPI().getSystemUser(), false);
		
		String str_style2 = "";
		if ((Cmod % 2) == 0) {
			str_style2 = "class=alternate_1";
		}
		else{
			str_style2 = "class=alternate_2";
		}
		Cmod++;
%>
	<tr <%= str_style2 %> >
		<td>			
			<a href="javascript:previewHTMLPage('<%=htmlpageRef.getInode()%>')" class="beta">
				<%= UtilMethods.escapeHTMLSpecialChars(htmlpageRef.getTitle()) %>
			</a>
		</td>
		<td><%= containerRef.getTitle() %></td>
		<td><%= UtilMethods.getUserFullName(htmlpageRef.getModUser()) %></td>
	</tr>
	<%
	}
%>
</table>

<script type="text/javascript">
function previewHTMLPage (objId) 
{
	top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/preview_htmlpage" /><portlet:param name="previewPage" value="1" /></portlet:actionURL>&inode=' + objId;
}
</script>


