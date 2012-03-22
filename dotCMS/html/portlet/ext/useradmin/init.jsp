<%@ include file="/html/common/init.jsp" %>
<%@ page import="com.dotmarketing.util.CompanyUtils" %>

<%
	Company c = CompanyUtils.getDefaultCompany();
	boolean authByEmail = false;
	if(c.getAuthType().equals(Company.AUTH_TYPE_EA)) {
		authByEmail = true;
	}
%>

<portlet:defineObjects />
