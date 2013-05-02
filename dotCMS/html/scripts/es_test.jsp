<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<html>
<body>
<%
List<ContentletSearch> cs = APILocator.getContentletAPI().searchIndex(
		"structureName:webpagecontent", 20, 0, "", APILocator.getUserAPI().getAnonymousUser(), true);
for(ContentletSearch cc : cs) {
	out.println(cc.getIdentifier());
	out.println("<br/>");
}

%>
</body>
</html>