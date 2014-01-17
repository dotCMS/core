<%@ page import="com.dotmarketing.util.Config" %>
<%@ include file="/html/common/init.jsp" %>
<portlet:defineObjects />
<%@ include file="/html/common/messages_inc.jsp" %>

    <div id="main">
    	<%= request.getAttribute("hello") %> I am in Maximized View
    </div>
