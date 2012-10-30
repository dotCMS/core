<%@ page import="com.liferay.portlet.LiferayWindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>

<script type="text/javascript">

function deletePublishContentlet(objId,referer) {
	if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.archive.selected.contentlet")) %>')) {
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value='/ext/publishQueue/view_publish_queue' /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer;
	}
}

</script>