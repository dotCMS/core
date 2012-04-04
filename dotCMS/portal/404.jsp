<%@ page import="java.util.*" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.lang.Exception" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@ page import="com.dotmarketing.business.CacheLocator"%>
<%@ page import="com.dotmarketing.util.Logger"%>
<%@ page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%try{		

	Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
	
	// Save 404 request
	com.dotmarketing.factories.ClickstreamFactory.add404Request(request,response,host);


	
	String ep_originatingHost = host.getHostname();
	String ep_errorCode = "404";
	
	// Get 404 from virtual link
	String pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cms404Page");
	if (!UtilMethods.isSet(pointer)) {
		pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache("/cms404Page");
	}
	
	Logger.debug(this, "cms404Page path is: " + pointer);
	
	// if we have a virtual link, see if the page exists.  pointer will be set to null if not
	if (UtilMethods.isSet(pointer)) {
		if (pointer.startsWith("/")) {
		// if the virtual link is a relative path, the path is validated within the current host
			pointer = com.dotmarketing.cache.LiveCache.getPathFromCache(pointer, host);	
			Logger.debug(this, "cms404Page relative path is: " + pointer + " - host: " + host.getHostname() + " and pointer: " + pointer);
		} else {
		// if virtual link points to a host or alias in dotCMS server, the path needs to be validated.
		// Otherwise, the original external pointer is kept for the redirect

			try {
				URL errorPageUrl = new URL(pointer);
				String errorPageHost = errorPageUrl.getHost();
				String errorPagePath = errorPageUrl.getPath();
				
				Logger.debug(this, "cms404Page - errorPageHost: " + errorPageHost + " and errorPagePath: " + errorPagePath);
				
				Host internalHost = WebAPILocator.getHostWebAPI().findByName(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
				Host internalAlias = WebAPILocator.getHostWebAPI().findByAlias(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
				
				// 404 Virtual Link is pointing to a host in dotCMS
				if ( internalHost != null) {				
					String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalHost);
					if (absPointer == null) {
						pointer = null;
					}
					Logger.debug(this, "cms404Page absolute internal path is: " + pointer + " - internalHost: " + internalHost.getHostname() + " and errorPagePath: " + errorPagePath);
				
				// 404 Virtual Link is poiting to an alias in dotCMS
				} else if ( internalAlias != null) {
					String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalAlias);
					if (absPointer == null) {
						pointer = null;
					}
					Logger.debug(this, "cms404Page absolute internal path is: " + pointer + " - internalAlias: " + internalAlias.getHostname() + " and errorPagePath: " + errorPagePath);
				
				// 404 Virtual Link is pointing to an external page
				} else {
					Logger.debug(this, "cms404Page absolute external path is: " + pointer);
				}
					
			} catch (Exception e){
				Logger.error(this, "cms404Page path is incorrect: " + pointer + e.getMessage(), e);
				pointer = null;
			}
		}
	}
	
	// if we have virtual link and page exists, redirect or forward
	if(UtilMethods.isSet(pointer) ){
		if (pointer.startsWith("/")) {
			Logger.debug(this, "cms404Page forwarding to relative path: " + pointer);			
			request.getRequestDispatcher(pointer).forward(request, response);
		} else {
			pointer = pointer + "?ep_originatingHost="+ep_originatingHost+"&ep_errorCode="+ep_errorCode;
			Logger.debug(this, "cms404Page redirecting to absolute path: " + pointer);
			response.sendRedirect(pointer);
		}
		return;
	}

	%>
	
	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<%@page import="com.dotmarketing.cache.LiveCache"%>
	<html>
		<head>
			<link rel="shortcut icon" href="//www.dotcms.org/global/favicon.ico" type="image/x-icon">
		    <script>
		        function showError(){
		            var ele = document.getElementById("error");
		            if(ele.style.display=="none"){
		                ele.style.display="";
		            }
		            else{
		                ele.style.display="none";
		            }
		        
		        }
		        
		        
		    </script>
			<title>dotCMS: 404 page not found</title>
		
	<style type="text/css">
		body{
			font-family: verdana, helvetica, san-serif;
			padding:20px;
		}
		#main {
			width: 400px;
			font-family: verdana, helvetica, san-serif;
			font-size: 12px;
			margin-left:auto;
			margin-right:auto;
		}
		#footer {
			text-align:center;
			font-family: verdana, helvetica, san-serif;
			font-size: 12px;
		}
		h1 {
			font-family: verdana, helvetica, san-serif;
			font-size: 20px;
			text-decoration: none;
			font-weight: normal;
		}
		#logo{
			float: left;
		}
		#text{
			float: left;
		}
	</style>
		
		</head>
		<body>
		<div id="main">
			<div id="logo">
				<a href="http://dotcms.com"><img src="/html/images/skin/logo.gif" width="140"  hspace="10" border="0" alt="dotCMS content management system" title="dotCMS content management system"  /></a>
			</div>
			<div id="text">
			
				<h1>Page not found (404 error)</h1>
				
				<p>The page or file you were looking for was not found. 
				<BR/>Please make sure that you have typed the correct URL</a>. </p>
				<p>If the problem persists, you can always return to the <a href="/">home page</a>.</p>
			</div>
		</div>
		<br clear="all"/>&nbsp;<br clear="all"/>
		<div id="footer">&copy; <script>var d = new Date();document.write(d.getFullYear());</script>, <a href="http://dotcms.com">DM Web, Corp.</a></div>
		<br clear="all"/>&nbsp;<br clear="all"/>
		

<%} catch( Exception e){
	Logger.error(this, "cms404Page cant display " + e.getMessage(), e);	%>
404	
	
	
<% }finally{
	DbConnectionFactory.closeConnection();
}%>

