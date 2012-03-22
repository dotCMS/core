<%@ include file="/html/portal/init.jsp" %>
<%@page import="java.util.Enumeration"%>
<portlet:defineObjects />

<%
RenderResponseImpl renderResponseImpl = (RenderResponseImpl)renderResponse;

String portletTitle = renderResponseImpl.getTitle();
%>

<table border="0" cellpadding="4" cellspacing="0" width="100%" height="300">
<tr>
	<td align="center">
		<table border="0" cellpadding="8" cellspacing="0">
		<tr>
			<td>
			<center>
			
				<%= LanguageUtil.get(pageContext, "oops-sorry-this-caused-a-problem") %>
		
			<br>&nbsp;<br>
		
				<button dojoType="dijit.form.Button" onclick="history.back();" iconClass="cancelIcon">
					<%= LanguageUtil.get(pageContext, "try-again") %>
				</button>
		

			</td>
		</tr>
		</table>
	</td>
</tr>
</table>