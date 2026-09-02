<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final String categoryInode = request.getParameter("categoryInode");
    if (UtilMethods.isSet(categoryInode)) {
        final Category category = APILocator.getCategoryAPI().find(categoryInode, user, false);
        if (category != null && UtilMethods.isSet(category.getInode())) {
            request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, category);
%>
<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
<%
        }
    }
%>
