package com.dotcms.filters.interceptor.dotcms;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.PortalUtil;

/**
 * Interceptor created to validate referers for incoming requests. This will reject any calls to
 * URIs that do not include a valid referer or Origin header and will help prevent XSS attacks
 */
public class XSSPreventionWebInterceptor implements WebInterceptor {

  private static final long serialVersionUID = 1L;

  private static final String XSS_PROTECTED_PATHS = "XSS_PROTECTED_PATHS";
  private static String[] protectedPaths = null;

  // \A -> The beginning of the input
  // All paths needs to be in lower case as the URI is lowercase before to be evaluated
  private static final String[] XSS_PROTECTED_PATHS_DEFAULT ={
          "\\A/html/", 
          "\\A/c/", 
          "\\A/servlets", 
          "\\A/servlet/",
          "\\A/dottaillogservlet", 
          "\\A/dwr/",
          "\\A/dotajaxdirector", 
          "\\A/dotscheduledjobs", 
          "\\A/jsontags/", 
          "\\A/edit/"
      };

  private final SecurityUtils securityUtils;
  
  public XSSPreventionWebInterceptor() {
    this(new SecurityUtils());
  }
  
  public XSSPreventionWebInterceptor(SecurityUtils securityUtils) {
    this.securityUtils=securityUtils;
  }

  @Override
  public String[] getFilters() {

    return protectedPaths;
  }

  @Override
  public void init() {
    protectedPaths = Config.getStringArrayProperty(XSS_PROTECTED_PATHS, XSS_PROTECTED_PATHS_DEFAULT);
  }

  @Override
  public Result intercept(final HttpServletRequest request, HttpServletResponse response) throws IOException {

    Result result = Result.NEXT;
    if(!securityUtils.validateReferer(request)) {
      
      if(PortalUtil.getUser(request)!=null && request.getRequestURI().startsWith("/c/")) {
        try(PrintWriter writer = response.getWriter()){
          response.setContentType("text/html");
          writer.append("<html><head>");
          writer.append("<script>");
          writer.append("top.location.href='/dotAdmin/';");
          writer.append("</script>");
          writer.append("</head></html>");
        }
      }else {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/html");
        try(PrintWriter writer = response.getWriter()){
          writer.print(unauthorizedHtmlResponse());
        }
      }
      result = Result.SKIP_NO_CHAIN;
    }
   

    return result; // if it is log in, continue!
  }

  /**
   * HTML response that will be mainly use for angular in order to identify we have a 401. Basically
   * from angular there is not a simpler way to identify the status of the requested URL by an iframe
   * but angular can check things like the title of the iframe and handle according to that.
   */
  private String unauthorizedHtmlResponse() {

    return "" + "<html>\n" + " <head>\n" + "     <title>401</title>\n" + " </head>\n" + " <body>" + "     <h1>401 / Unauthorized</h1>\n";

  }

}
