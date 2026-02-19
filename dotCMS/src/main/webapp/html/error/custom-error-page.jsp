<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.util.Optional"%>
<%@page import="com.dotmarketing.util.PageMode"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page isErrorPage="true" %>
<%@page import="com.dotcms.vanityurl.model.CachedVanityUrl"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@page import="com.dotmarketing.factories.ClickstreamFactory"%>
<%@page import="com.dotmarketing.filters.CMSUrlUtil"%>
<%@page import="com.dotmarketing.filters.Constants"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="static com.dotcms.filters.interceptor.saml.SamlWebInterceptor.REFERRER_PARAMETER_KEY" %>
<%
out.clear();
if(PageMode.get(request).isAdmin && Config.getBooleanProperty("SIMPLE_ERROR_PAGES_FOR_BACKEND", true)){
    out.append(String.valueOf(response.getStatus()));
    return;
}






  final int status = response.getStatus();
  final String title = LanguageUtil.get(pageContext, status + "-page-title");
  final String body = LanguageUtil.get(pageContext, status + "-body1");
  try {
    final long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
    final boolean isAPICall = pageContext.getErrorData().getRequestURI().startsWith("/api/");
    if (isAPICall) {
      if (status == 500) {
        if (null != request.getAttribute("javax.servlet.error.message")) {
          final String err = request.getAttribute("javax.servlet.error.message") + " on " + request.getRequestURI();
          Logger.warn(this.getClass(),err);
        }
      }
      return; // empty response is better than an HTML response to a REST API call
    }

    if (status == 401) {

        final String referrer = (null != session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN))
                ? (String) session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)
                : (null != request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI))
                ? (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) : request.getRequestURI();

        if (null == session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)){
          session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, referrer);
        }

        final String forwardQueryString = (String) request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING);

        session.setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, forwardQueryString);
    }

    final String errorPage = "/cms" + status + "Page";
    final Host site = WebAPILocator.getHostWebAPI().getCurrentHost(request);
    final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
    final Optional<CachedVanityUrl> vanityurl = APILocator.getVanityUrlAPI().resolveVanityUrl(errorPage, site, language);
    if (vanityurl.isPresent()) {
      
      final String uri = vanityurl.get().forwardTo;
      if (uri.contains("://")) {
        response.setStatus(301);
        response.setHeader("Location", uri);
      } else {
        Logger.debug(this, errorPage + " path is: " + uri);
        request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, uri);
        request.getRequestDispatcher("/servlets/VelocityServlet").forward(request, response);
      }
      return;
    }
    if (status == 401) {
      final String conMap = (String)request.getAttribute(com.dotmarketing.util.WebKeys.WIKI_CONTENTLET_URL);
      final String referrer = (null != session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN))
          ? (String) session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)
                : (null != request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI))
              ? (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) : request.getRequestURI();
      if (!UtilMethods.isSet(conMap) && UtilMethods.isSet(referrer)) {
        response.sendRedirect("/dotCMS/login?referrer=" + referrer);
      } else {
        if (UtilMethods.isSet(conMap)) {
          request.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, conMap);
        }
      request.getRequestDispatcher("/dotCMS/login").forward(request, response);
      }
    } else if (status == 404) {
      ClickstreamFactory.add404Request(request, response, site);
    } else if (status == 500) {
      if (null != request.getAttribute("javax.servlet.error.message")) {
        final String err = request.getAttribute("javax.servlet.error.message") + " on " + request.getRequestURI();
        Logger.warn(this.getClass(),err);
      }
    }
  } finally {
    DbConnectionFactory.closeSilently();
  }
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
<head>
<title><%= title %></title>

<style type="text/css">
body {
  font-family: helvetica, san-serif;
  padding: 20px;
  margin-top: 0px;
}

#main {
  width: 400px;
}

#footer {
  text-align: center;
}

h1 {
  font-size: 20px;
}

#logo {
  float: left;
}

#text {
  float: left;
}
</style>



</head>
<body>
  <div id="main">
    <div id="text">

      <h1><%= title %></h1>

      <p><%= body %></p>

    </div>
  </div>
</body>
</html>