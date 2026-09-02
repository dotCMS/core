<%@page import="javax.servlet.http.HttpServletRequest"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.model.User"%>

<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
    final String contentletId = httpServletRequest.getParameter("contentletId");
    final String languageIdParam = httpServletRequest.getParameter("languageId");

    if (UtilMethods.isSet(contentletId) && UtilMethods.isSet(languageIdParam)) {
        final Long languageId = Long.parseLong(languageIdParam);
        Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(contentletId, false, languageId, user, false);

        if (contentlet != null && UtilMethods.isSet(contentlet.getIdentifier())) {
            request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);
            request.setAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT, contentlet);
%>

<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>

<%
        }
    }
%>
