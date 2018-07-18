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
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%
  out.clear();
  int status = response.getStatus();
  String title = LanguageUtil.get(pageContext, status + "-page-title");
  String body = LanguageUtil.get(pageContext, status + "-body1");
  try {
    long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
    boolean isAPICall = pageContext.getErrorData().getRequestURI().startsWith("/api/");

    if(isAPICall) {
      if(status == 500) {
        if(request.getAttribute("javax.servlet.error.message")!=null){    
          String err = request.getAttribute("javax.servlet.error.message") + " on " + request.getRequestURI();
          Logger.warn(this.getClass(),err);
        }
      }
      return; // empty response is better than an HTML response to a REST API call
    }
    
    String errorPage = "/cms" + status + "Page";
    Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
    // Get from virtual link
      if (CMSUrlUtil.getInstance().isVanityUrl(errorPage, host, languageId)) {
        CachedVanityUrl vanityurl = APILocator.getVanityUrlAPI().getLiveCachedVanityUrl(errorPage, host, languageId, APILocator.systemUser());
        String uri = vanityurl.getForwardTo();

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
      String referer = (session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN) != null)
          ? (String) session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)
          : (request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null)
              ? (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) : request.getRequestURI();


      request.getSession().setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, referer);
      request.getRequestDispatcher("/dotCMS/login").forward(request, response);
    } else if (status == 404) {
      ClickstreamFactory.add404Request(request, response, host);
    }
    else if (status == 500) {
      if(request.getAttribute("javax.servlet.error.message")!=null){
        
        String err = request.getAttribute("javax.servlet.error.message") + " on " + request.getRequestURI();
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
<title><%=title%></title>

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

      <h1><%=title%></h1>

      <p><%=body%></p>

    </div>
  </div>
</body>
</html>