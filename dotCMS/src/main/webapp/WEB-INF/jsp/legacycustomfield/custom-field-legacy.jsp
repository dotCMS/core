<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.contenttype.business.ContentTypeAPI" %>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="com.dotcms.contenttype.model.field.Field" %><%--
  Created by IntelliJ IDEA.
  User: jsanca
  Date: 11/15/23
  Time: 2:57 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Custom Field Legacy</title>
</head>

<%
    String contentTypeVarName = request.getParameter("variable");
    String fieldName = request.getParameter("field");

    if (null != contentTypeVarName && null != fieldName) {

        User user = WebAPILocator.getUserWebAPI().getUser(request);
        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        ContentType contentType = contentTypeAPI.find(contentTypeVarName);
        if (null != contentType) {

            Field field = contentType.fieldMap().get(fieldName);
            if (null != field) {

%>
<body>
        contentType: <%=contentType%> <br/>
        field: <%=field%> <br/>
</body>
<%
            }
        }
    }
%>
</html>
