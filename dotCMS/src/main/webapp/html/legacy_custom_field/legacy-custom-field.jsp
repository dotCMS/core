<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.contenttype.business.ContentTypeAPI" %>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="com.dotcms.contenttype.model.field.Field" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>

<%@page import="java.util.Locale"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.util.ReleaseInfo"%>
<%@page import="com.dotmarketing.cms.factories.PublicCompanyFactory"%>
<%@page import="com.liferay.portal.model.*" %>


<%@page import="com.dotmarketing.util.Config"%>
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

<html>
<head>
    <title>Custom Field Legacy</title>

  <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dijit.css">
  <link rel="stylesheet" type="text/css" href="/html/css/dijit-dotcms/dotcms.css?b=<%= ReleaseInfo.getVersion() %>">
  
  <%
	HttpSession sess = request.getSession(false);
	Locale locale = null;
	User user = PortalUtil.getUser(request);
	

	if(sess != null){
	 	locale = (Locale) sess.getAttribute(com.dotcms.repackage.org.apache.struts.Globals.LOCALE_KEY);
	}
	if (locale == null && user != null) {
		// Locale should never be null except when the TCK tests invalidate the session
		locale = user.getLocale();
	}
	if(locale ==null){
		locale = PublicCompanyFactory.getDefaultCompany().getLocale();
	}

    Company company = PortalUtil.getCompany(request);
%>

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
     modulePaths: {
         dotcms: "/html/js/dotcms",
     }
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

<script type="text/javascript" src="/html/js/dojo/custom-build/dojo/dojo.js?b=<%= ReleaseInfo.getVersion() %>"></script>
<script type="text/javascript" src="/html/js/dojo/custom-build/build/build.js?b=<%= ReleaseInfo.getVersion() %>"></script>

<script type="text/javascript" src="/dwr/engine.js?b=<%= ReleaseInfo.getVersion() %>"></script>
<script type="text/javascript" src="/dwr/util.js?b=<%= ReleaseInfo.getVersion() %>"></script>
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
</script>

<style>
    :root {
			--color-background: #3a3847;
        --color-main: #426bf0;
        --color-main_mod: #4656ba;
        --color-main_rgb: 74, 144, 226;
        --color-sec: #9747ff;
        --color-sec_rgb: 151, 71, 255;
        --color-white: #fff;
        --color-white_rgb: 255, 255, 255;
        --empty-message: '';

      /* Basics */
      --border-radius: 2px;
      --basic-padding: 8px;
      --basic-padding-2: 16px;
      --basic-padding-3: 24px;

      /* Typography */
      --font-size-xx-large: 24px;
      --font-size-x-large: 18px;
      --font-size-large: 16px;
      --font-size-medium: 14px;
      --font-size-small: 12px;
      --font-size-x-small: 10px;
      --font-weight-semi-bold: 500;

      --body-text: Roboto, 'Helvetica Neue', Helvetica, Arial, 'Lucida Grande', sans-serif;
      --body-font-size-base: --font-size-medium;
      --body-font-color: --black;

      /* MD */
      --md-shadow-1: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
      --md-shadow-2: 0 3px 6px rgba(0, 0, 0, 0.16), 0 3px 6px rgba(0, 0, 0, 0.23);
      --md-shadow-3: 0 10px 24px 0 rgba(0, 0, 0, 0.2);
      --md-shadow-4: 0 2px 4px 0 rgba(0, 0, 0, 0.1);
      --md-shadow-5: 0 10px 20px 0 rgba(0, 0, 0, 0.15);

      /* ANIMATION */
      --basic-speed: 150ms;
    }

	/* fallback */
	@font-face {
	font-family: 'Material Icons';
	font-style: normal;
	font-weight: 400;
	font-display: swap;
	src: url('/dotAdmin/assets/MaterialIcons-Regular.ttf') format('truetype');
	}
	.material-icons {
	font-family: 'Material Icons';
	font-weight: normal;
	font-style: normal;
	font-size: 24px;
	line-height: 1;
	letter-spacing: normal;
	text-transform: none;
	display: inline-block;
	white-space: nowrap;
	word-wrap: normal;
	direction: ltr;
	-webkit-font-feature-settings: 'liga';
	-webkit-font-smoothing: antialiased;
	}

    body {background: white}

  </style>

</head>

<%
    String contentTypeVarName = request.getParameter("variable");
    String fieldName = request.getParameter("field");

    if (null != contentTypeVarName && null != fieldName) {

        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        ContentType contentType = contentTypeAPI.find(contentTypeVarName);

        if (null != contentType) {

            Field field = contentType.fieldMap().get(fieldName);
            System.out.println(field);
            if (null != field) {
               
                String HTMLString = "";
                Object value = null; //TODO: Investigate how to set this value
                String defaultValue = field.defaultValue() != null ? field.defaultValue().trim() : "";
                String textValue = field.values();

                if(UtilMethods.isSet(textValue)){
                    org.apache.velocity.context.Context velocityContext =  com.dotmarketing.util.web.VelocityWebUtil.getVelocityContext(request,response);
                    // set the velocity variable for use in the code (if it has not already been set
                    if(!UtilMethods.isSet(velocityContext.get(field.variable()))){
                        if(UtilMethods.isSet(value)){
                            velocityContext.put(field.variable(), value);
                        }
                        else{
                            velocityContext.put(field.variable(), defaultValue);
                        }
                    }
                    HTMLString = new VelocityUtil().parseVelocity(textValue,velocityContext);
                }
%>
                    <body>
                        <%= HTMLString %>
                    </body>
<%
            }
        }
    }
%>
</html>
