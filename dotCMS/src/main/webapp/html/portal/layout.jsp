<%@page import="com.dotmarketing.util.Parameter"%>
<%@ include file="/html/portal/layout_init.jsp" %>
<%
boolean statePopUp = true;
boolean child = ParamUtil.getString(request, "child").equals("true") ? true : false;

	String curColumnOrder = "w";
	portlets = new Portlet[0];
	int j = 0;

Portlet portlet = PortletManagerUtil.getPortletById(company.getCompanyId(), ParamUtil.getString(request,"p_p_id", layout.getPortletIds().get(0)));
%><%@ include file="/html/portal/view_portlet_inc.jsp" %>