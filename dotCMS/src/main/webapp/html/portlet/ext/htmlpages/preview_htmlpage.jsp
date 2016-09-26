<%@ include file="/html/portlet/ext/htmlpages/init.jsp" %>
<%
	request.setAttribute("htmlPage",request.getParameter("inode"));
%>
<%= LanguageUtil.get(pageContext, "Im-HERE-in-Preview-Page") %>