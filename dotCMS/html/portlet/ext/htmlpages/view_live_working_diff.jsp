<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory"%>
<%@page import="com.dotcms.enterprise.HTMLDiffUtilProxy" %>


<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%

String id = request.getParameter("id");

String contentId = request.getParameter("contentId");

HTMLPage p = (HTMLPage) InodeFactory.getInode(id, HTMLPage.class);

HTMLDiffUtilProxy dp = new HTMLDiffUtilProxy();


String x = dp.htmlDiffPage(p, APILocator.getUserAPI().getSystemUser(), contentId);

%>
<%=x%>