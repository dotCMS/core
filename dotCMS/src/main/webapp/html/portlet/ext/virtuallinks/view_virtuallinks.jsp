<%@ include file="/html/portlet/ext/virtuallinks/init.jsp" %>
<%@ page import="com.dotcms.contenttype.model.type.BaseContentType" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>

<%
	List<ContentType> vanityURLContentTypes = APILocator.getContentTypeAPI(APILocator.systemUser()).findByType(BaseContentType.VANITY_URL);
	
	if(!vanityURLContentTypes.isEmpty()) {
%>
	<script>
	
	window.location.href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
		<portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' />
			<portlet:param name='baseType' value='<%=String.valueOf(BaseContentType.VANITY_URL.getType())%>' />
			</portlet:actionURL>";
	</script>
<%} else {%>

<div>
	<div class="noResultsMessage"><%=LanguageUtil.get(pageContext, "no.vanity.url.content.types")%></div>
</div>

<% } %>