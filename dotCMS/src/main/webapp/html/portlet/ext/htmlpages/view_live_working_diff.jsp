<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory"%>
<%@page import="com.dotcms.enterprise.HTMLDiffUtilProxy" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.liferay.portal.model.User"%>
<%

String id = request.getParameter("id");
long lang = Long.parseLong(request.getParameter("pageLang"));
User user = APILocator.getUserAPI().getSystemUser();
String contentId = request.getParameter("contentId");
//Contentlet contentlet = APILocator.getContentletAPI().find(id,user,false);
Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(id, false, lang, user, false);
IHTMLPage p = null;
try{
	if(contentlet != null)
		p = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
	else
		p = (IHTMLPage) InodeFactory.getInode(id, HTMLPage.class);
}catch(Exception e){
	//
}

HTMLDiffUtilProxy dp = new HTMLDiffUtilProxy();

String x = dp.htmlDiffPage(p, user, contentId);
%>
<%=x%>