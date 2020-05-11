<%@ page import="com.dotcms.enterprise.HTMLDiffUtilProxy" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage" %>

<%
    String id = request.getParameter("id");
    long lang = Long.parseLong(request.getParameter("pageLang"));
    User user = APILocator.getUserAPI().getSystemUser();
    //String contentId = request.getParameter("contentId");
    IHTMLPage htmlPage = null;

    try {
        htmlPage = APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(id, lang, false, user, false);
    } catch(final Exception e){
        Logger.error(this.getClass(), e.getMessage(), e);
    }

    final HTMLDiffUtilProxy proxyUtil = new HTMLDiffUtilProxy();
    final String diff = proxyUtil.htmlDiffPage(htmlPage, user, request, response);
%>

<%=diff%>