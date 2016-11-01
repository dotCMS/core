<%@ include file="/html/common/init.jsp" %><%

// General variables


String title = ParamUtil.get(request, "box_title", "");

// Portlet specific variables

String portletId = ParamUtil.get(request, "box_portlet_id", "");


boolean stateMin = false;

%>