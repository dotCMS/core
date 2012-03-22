<%@ include file="/html/common/init.jsp" %>
<tiles:useAttribute id="tilesContent" name="content" classname="java.lang.String" />
<tiles:useAttribute id="tilesPortletSubNav" name="portlet_sub_nav" classname="java.lang.String" />
<%
	boolean inPortal = (request.getAttribute("org.dotcms.variables.inPortlets") != null);
	boolean inPopupIFrame = UtilMethods.isSet(ParamUtil.getString(request, "popup")) || UtilMethods.isSet(ParamUtil.getString(request, "in_frame"));

	request.setAttribute("org.dotcms.variables.inPortlets", "true"); 
	
%>

<%if(inPortal ) {%>
	<c:if test="<%= Validator.isNotNull(tilesPortletSubNav) %>">
		<div class="portlet-wrapper" >
			<liferay:include page="<%= Constants.TEXT_HTML_DIR + tilesPortletSubNav %>" flush="true" />
		</div>
	</c:if>
	<div class="portlet-wrapper" >
		<jsp:include page="<%= Constants.TEXT_HTML_DIR + tilesContent %>"></jsp:include>
	</div>
	
<%}else if(inPopupIFrame) { %>
	<%@ include file="/html/common/top_inc.jsp" %>
	<style>
		body{
			background: white;
		}
	</style>
	<%@ include file="/html/common/messages_inc.jsp" %>
	<jsp:include page="<%= Constants.TEXT_HTML_DIR + tilesContent %>"></jsp:include>
	<%@ include file="/html/common/bottom_inc.jsp" %>
<%}else{ %>

	<%@ include file="/html/common/top_inc.jsp" %>
	
	<div id="doc3" class="yui-t7">
		<div id="hd">
			<%@ include file="/html/common/nav_main_inc.jsp" %>
			<%@ include file="/html/common/nav_sub_inc.jsp" %>
			<%@ include file="/html/common/messages_inc.jsp" %>
		</div>
		
		<div id="bd">
			<jsp:include page="<%= Constants.TEXT_HTML_DIR + tilesContent %>"></jsp:include>
		</div>
		
		<div>
			<%@ include file="/html/common/bottom_portal_inc.jsp" %>
		</div>
	</div>
	<%@ include file="/html/common/bottom_inc.jsp" %>
<%} %>

