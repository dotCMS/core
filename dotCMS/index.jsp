<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%
Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
String pointer = null;

if(!ADMIN_MODE && !host.isLive()) {
	//Checking if it has a maintenance virtual link
	pointer = (String) VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cmsMaintenancePage");
	if(pointer == null) {
		try {
			Company company = CompanyUtils.getDefaultCompany();
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, LanguageUtil.get(company.getCompanyId(), company.getLocale(), "server-unavailable-error-message"));
		} catch (LanguageException e) {
			Logger.error(CMSFilter.class, e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		return;
	}
}
	
if (!UtilMethods.isSet(pointer)) {
	pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cmsHomePage");
}
if (!UtilMethods.isSet(pointer)) {
	pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache("/cmsHomePage");
}
if(UtilMethods.isSet(pointer)){
	if (pointer.startsWith("/")) {
		request.getRequestDispatcher(pointer).forward(request, response);
	} else {
		response.sendRedirect(pointer);
	}
}
else{
%>

	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
		"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

	<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.factories.ClickstreamFactory"%>
<%@page import="com.dotmarketing.util.Config"%>

<%@page import="com.dotmarketing.cache.VirtualLinksCache"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.portal.language.LanguageException"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.filters.CMSFilter"%>
<%@page import="com.dotmarketing.util.CompanyUtils"%>
<%@page import="com.liferay.portal.model.Company"%><html>
	<head>
		<title>dotCMS: Congratulations!  You have sucessfully set up the dotCMS system</title>

		<style type="text/css">

			#main {
				width: 600px;
				
				font-family: verdana, helvetica, san-serif;
				font-size: 12px;
				margin-left:auto;
				margin-right:auto;
				border-bottom:1px dotted gray;
				clear: both;
			}
			#footer {
				width: 600px;
				text-align:right;
				font-family: verdana, helvetica, san-serif;
				font-size: 12px;
				margin-left:auto;
				margin-right:auto;
				padding-top:10px;
			}
			h1 {
				font-family: verdana, helvetica, san-serif;
				font-size: 20px;
				text-decoration: none;
				font-weight: normal;
				color: gray;
				padding:0px;
				margin:0px;
			}
			h2{
				font-family: verdana, helvetica, san-serif;
				font-size: 16px;
				text-decoration: none;
				font-weight: normal;
				color: gray;
				padding:0px;
				margin:0px;
			}
			#logo{
				float: right;
			}
			#header{
				float: left;
				padding-top:30px;
				
			}
			#text{
				padding:15px;
				line-height: 18px;
				
			}

			#firstLetter{
				line-height:36px;
				padding-right: 5px;
				margin: 0px;
				font-size: 40px;
				float:left;
				color:gray;
	
			}
			ul{
				
				list-style: square;
			}
			li{
				margin-left: 20px;
				margin-bottom: 10px;
			}
			
			fieldset{
				width: 500px;
				border: solid 1px silver;
				background: #fff;
				margin-left:auto;
				margin-right:auto;
				padding: 20px;
			}
			
			legend{
				padding: 3px;
				border: solid 1px silver;
				background: #fff;
			
			}
		</style>

	</head>
	<body>
	<div id="main">
		<div id="header">
			<h1>Getting Started with dotCMS</h1>
		</div>
		<div id="logo">
			<a href="http://www.dotcms.org"><img src="http://www.dotcms.org/portal/images/dotcms_logo.gif?code=noHomePage" width="241" height="60" hspace="10" border="0" alt="dotCMS content management system" title="dotCMS content management system"  /></a>
		</div>
	
		<br clear="all"/>
		<div style="border-bottom:1px dotted gray;"></div>
		<div id="text">
			<p><div id="firstLetter">C</div>ongratulations!  You have successfully set up dotCMS - the open source content management system that makes sense. 
	
				From here, you have a couple of options: 

				<ul>
					<li>Login to <a href="http://<%=host.getHostname()%>/c">Administrative Console</a></li>
					
					<li>Find <a href="http://dotcms.com/documentation/">Help and Documentation</a></li>
					
					<li><a href="http://dotcms.com/community">Sign up</a> with the dotCMS mailing list.</li>
					<li><a href="http://dotcms.com/community">View</a> latest dotCMS News and Information</li>
				</ul>
			</p>
			<br clear="all"/>
			<fieldset><legend><b>Note:</b> How to set your default home page</legend>
			<p>You are seeting this because you do not have a home page set up.  To set your real homepage, login to the <a href="http://<%=host.getHostname()%>/admin">administrative console</a>, go to the "Website" tab and create a new vanity url called "/cmsHomePage" that points to the page you would like to be your home page, e.g.
			<blockquote>Create New Vanity URL:<br/>&nbsp;<br/>&nbsp;&nbsp;&nbsp;/cmsHomePage &nbsp;&raquo;&nbsp; Your Home Page</blockquote>
			
			
		</fieldset>
	
			<br clear="all"/>
			
			
		</div>
	
	</div>
	
	<div id="footer">&copy; <%=new GregorianCalendar().get(Calendar.YEAR)%>, <a href="http://dotcms.com">dotCMS, Inc.</a></div>


	</body>
	</html>

<%}%>
