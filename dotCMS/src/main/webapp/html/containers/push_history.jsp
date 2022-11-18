<%@page import="javax.servlet.http.HttpServletRequest"%>
<%@ page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotcms.contenttype.business.ContentTypeAPI"%>
<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.util.ReleaseInfo"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>


<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>

<%
    final HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
    final String containerId = httpServletRequest.getParameter("containerId");
    Container container =  APILocator.getContainerAPI().getWorkingContainerById(containerId, user, true);
    request.setAttribute(com.dotmarketing.util.WebKeys.VERSIONS_INODE_EDIT, container);
    request.setAttribute("hideBringBack", true);
%>
	<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>

<script language='javascript' type='text/javascript'>

	function deleteVersion(inode){
		if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.container.delete.version"))%>')){
			fetch(`/api/v1/versionables/${inode}`, {method:'DELETE'} )
			.then(response => response.json())
			.then((response)=> { location.reload(); });
		}
	}

	function editVersion(inode) {
		var customEvent = document.createEvent("CustomEvent");
		customEvent.initCustomEvent("ng-event", false, false,  {
			name: "edit-container",
			data: {
				id: '<%=container.getIdentifier()%>',
				inode: inode,
			}
		});
		document.dispatchEvent(customEvent);
	}
</script>
