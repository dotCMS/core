<%@page import="javax.servlet.http.HttpServletRequest"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotcms.contenttype.business.ContentTypeAPI"%>
<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.util.ReleaseInfo"%>

<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
    final String contentTypeId = httpServletRequest.getParameter("contentTypeId");
    Structure structure = APILocator.getStructureAPI().find(contentTypeId, user);

    request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, structure);
%>

<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>

