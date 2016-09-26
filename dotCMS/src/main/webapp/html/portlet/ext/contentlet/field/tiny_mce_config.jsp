<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>


<%
	if (UtilMethods.isSet(Config.getStringProperty("TINY_MCE_CONFIG_LOCATION"))) {
%>

		<jsp:include page="<%= Config.getStringProperty(\"TINY_MCE_CONFIG_LOCATION\") %>" />

<%
	} else {
%>
	<%@ include file="/html/portlet/ext/contentlet/field/tiny_mce_config_default.jsp" %>

		
<%
	}
%>
