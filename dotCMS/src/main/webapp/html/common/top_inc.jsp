<%--

THE INTENT OF THIS FILE IS A COMMON HEADER THAT SHOULD
BE ABLE TO BE INCLUDED IN ALL JSP PAGES, EVEN "BLANK ONES"
PLEASE KEEP ALL PORTAL SPECIFIC CODE, JS AND MARKUP OUT OF
THIS FILE AND ITS INCLUDES

--%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

	String dojoPath = Config.getStringProperty("path.to.dojo");
	if(!UtilMethods.isSet(dojoPath)){
		// Change dojopath in dotmarketing-config.properties!
		response.sendError(500, "No dojo path variable (path.to.dojo) set in the property file");
	}
	String agent = request.getHeader("User-Agent");
	response.setHeader("Cache-Control","no-store");
	response.setHeader("Pragma","no-cache");
	response.setHeader("Expires","01 Jan 2000 00:00:00 GMT");

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:bi="urn:bi" xmlns:csp="urn:csp">
<head>
	<script src="/html/js/dragula-3.7.2/dragula.min.js"></script>
	<meta http-equiv="x-ua-compatible" content="IE=edge" >
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

	<meta content="no-cache" http-equiv="Cache-Control" />
	<meta content="no-cache" http-equiv="Pragma" />
	<meta content="0" http-equiv="Expires" />
	<meta name="Expire" content="Now" />

	<link rel="shortcut icon" href="//dotcms.com/favicon.ico" type="image/x-icon">
	<title>dotCMS : <%= LanguageUtil.get(pageContext, "Enterprise-Web-Content-Management") %></title>
    
    <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dijit.css">
    <link rel="stylesheet" type="text/css" href="/html/css/dijit-dotcms/dotcms.css?b=<%= ReleaseInfo.getVersion() %>">


	<!--[if IE]>
		<link rel="stylesheet" type="text/css" href="/html/css/iehacks.css" />
	<![endif]-->

	<%
	String dojoLocaleConfig = "locale:'en-us'";
	if(locale != null){
		dojoLocaleConfig = "locale:'"+locale.getLanguage() + "-" + locale.getCountry().toLowerCase() + "',";
	}
	%>

   	<script type="text/javascript">
	   	djConfig={
			parseOnLoad: true,
			i18n: "<%=dojoPath%>/custom-build/build/",
			useXDomain: false,
			isDebug: false,
			<%=dojoLocaleConfig%>
			modulePaths: { dotcms: "/html/js/dotcms" }
	   };

	   	function isInodeSet(x){
			return (x && x != undefined && x!="" && x.length>15);
		}
		// Polyfill IE11
		if (typeof String.prototype.endsWith !== 'function') {
			String.prototype.endsWith = function(suffix) {
				return this.indexOf(suffix, this.length - suffix.length) !== -1;
			};
		}
   	</script>

	<script type="text/javascript" src="/html/js/log4js/log4javascript.js"></script>
	<script type="text/javascript" src="/html/js/log4js/dotcms-log4js.js"></script>
	<script type="text/javascript" src="/html/js/dojo/custom-build/dojo/dojo.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/html/js/dojo/custom-build/build/build.js?b=<%= ReleaseInfo.getVersion() %>"></script>
  	<script type="text/javascript" src="/html/common/javascript.jsp?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/engine.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/util.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/TemplateAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/HostAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/ContainerAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/RoleAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/BrowserAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/UserAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/InodeAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>


	<script type="text/javascript">
		dojo.require("dojo.data.ItemFileReadStore");

		dojo.require("dotcms.dijit.image.ImageEditor");
		dojo.require("dotcms.dojo.data.UsersReadStore");

		dojo.addOnLoad(function () {
			dojo.global.DWRUtil = dwr.util;
			dojo.global.DWREngine = dwr.engine;
			dwr.engine.setErrorHandler(DWRErrorHandler);
			dwr.engine.setWarningHandler(DWRErrorHandler);
		});

		function DWRErrorHandler(msg, e) {
			console.log(msg, e);
		}
		var dojoDom=dojo.require("dojo.dom");
		var dojoDomGeometry=dojo.require("dojo.dom-geometry");
		var dojoStyle=dojo.require("dojo.dom-style");
		dojo.coords = function(elem,xx) {
			var mb=dojoDomGeometry.getMarginBox(elem,dojoStyle.getComputedStyle(elem));
			var abs=dojoDomGeometry.position(elem,xx);
			mb.x=abs.x;
			mb.y=abs.y;
			mb.w=abs.w;
			mb.h=abs.h;
			return mb;
		};
<%
	if(UtilMethods.isSet(request.getParameter(WebKeys.IN_FRAME)) && UtilMethods.isSet(request.getParameter(WebKeys.FRAME))){
		boolean inFrame = Boolean.valueOf(request.getParameter(WebKeys.IN_FRAME));
		
		if(inFrame){
			  request.getSession().setAttribute(WebKeys.IN_FRAME,inFrame);
	    	  request.getSession().setAttribute(WebKeys.FRAME,request.getParameter(WebKeys.FRAME));
		}else{
			  request.getSession().removeAttribute(WebKeys.IN_FRAME);
	  	      request.getSession().removeAttribute(WebKeys.FRAME);
		}
	}
%>
  </script>
  

  <script type="module" src="/dotcms-webcomponents/dotcms-webcomponents.esm.js"></script>
  <script nomodule="" src="/dotcms-webcomponents/dotcms-webcomponents.js"></script>

  <style>
    :root {
        --color-background: #3a3847;
        --color-main: #c336e5;
        --color-main_mod: #d369ec;
        --color-main_rgb: 195, 54, 229;
        --color-sec: #54428e;
        --color-sec_rgb: 84, 66, 142;
        --color-white: #fff;
        --color-white_rgb: 255, 255, 255;
​
        /* Basics */
        --border-radius: 2px;
        --basic-padding: 8px;
        --basic-padding-2: 16px;
        --basic-padding-3: 24px;
​
        /* Typography */
        --font-size-xx-large: 24px;
        --font-size-x-large: 18px;
        --font-size-large: 16px;
        --font-size-medium: 14px;
        --font-size-small: 12px;
        --font-size-x-small: 10px;
        --font-weight-semi-bold: 500;
​
        --body-text: Roboto, 'Helvetica Neue', Helvetica, Arial, 'Lucida Grande', sans-serif;
        --body-font-size-base: --font-size-medium;
        --body-font-color: --black;
​
        /* MD */
        --md-shadow-1: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
        --md-shadow-2: 0 3px 6px rgba(0, 0, 0, 0.16), 0 3px 6px rgba(0, 0, 0, 0.23);
        --md-shadow-3: 0 10px 24px 0 rgba(0, 0, 0, 0.2);
        --md-shadow-4: 0 2px 4px 0 rgba(0, 0, 0, 0.1);
        --md-shadow-5: 0 10px 20px 0 rgba(0, 0, 0, 0.15);
​
        /* ANIMATION */
        --basic-speed: 150ms;
    }
  </style>

  <link href="https://fonts.googleapis.com/css?family=Material+Icons&display=block" rel="stylesheet" />

	<% String dotBackImage = (!UtilMethods.isSet(company.getHomeURL()) || "localhost".equals(company.getHomeURL())) ? "/html/images/backgrounds/bg-3.jpg" : company.getHomeURL();%>
	<style>
		.imageBG{background-color:<%= company.getSize() %>;background-image:url(<%= dotBackImage %>);background-repeat:no-repeat;background-position:top center;background-size:100% auto;height:75px;position:absolute;top:0;left:0;width:100%;z-index:-2;}
	</style>


</head>
<%if(UtilMethods.isSet(request.getParameter("popup")) || UtilMethods.isSet(request.getAttribute("popup")) ){%>
<body class="dotcms" style="background:white">
<%}else{ %>
<body class="dotcms" style="visibility:hidden;background:white">
<%} %>
