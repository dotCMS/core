<%@ page import="com.dotcms.enterprise.HTMLDiffUtilProxy" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset" %>

<%
    String id = request.getParameter("id");
    long lang = Long.parseLong(request.getParameter("pageLang"));
    User user = APILocator.getUserAPI().getSystemUser();
    String contentId = request.getParameter("contentId");
    Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(id, false, lang, user, false);
    HTMLPageAsset p = null;
    try{
        p = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
    }catch(Exception e){
        //TODO
    }

    HTMLDiffUtilProxy dp = new HTMLDiffUtilProxy();
    String x = dp.htmlDiffPage(p, user, contentId);
%>

<%=x%>