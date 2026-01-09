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
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="com.fasterxml.jackson.datatype.jdk8.Jdk8Module" %>
<%@ page import="com.dotcms.rest.api.v1.DotObjectMapperProvider" %>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.fasterxml.jackson.databind.JsonNode" %>
<%@ page import="com.fasterxml.jackson.databind.node.ObjectNode" %>

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
<%
    // Cache busting for development - use timestamp
    String cacheBuster = String.valueOf(System.currentTimeMillis());
%>

<script type="text/javascript" src="/html/js/util.js?v=<%=  ReleaseInfo.getVersion() %>"></script>
<script type="text/javascript" src="/html/legacy_custom_field/shared-logger.js?v=<%=  cacheBuster %>"></script>
<script type="text/javascript" src="/html/legacy_custom_field/iframe-height-manager.js?v=<%=  cacheBuster %>"></script>
<script type="text/javascript" src="/html/legacy_custom_field/field-interceptors.js?v=<%=  cacheBuster %>"></script>

<script type="text/javascript">
    dojo.require("dojo.data.ItemFileReadStore");

    dojo.require("dotcms.dijit.image.ImageEditor");
    dojo.require("dotcms.dojo.data.UsersReadStore");
    dojo.require("dijit.form.TextBox");

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

    .dijitTextBox, .dijitSelect{
        max-width: initial;
    }

  </style>
</head>

<%
    String contentTypeVarName = request.getParameter("variable");
    String fieldName = request.getParameter("field");
    String isModal = request.getParameter("modal"); // Check if opened as modal
    boolean modalMode = "true".equals(isModal);

    // Use DotObjectMapperProvider to get properly configured ObjectMapper
    ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
%>


<script>
/**
 * @namespace DotCustomFieldApi
 * @description Bridge API for DotCMS Custom Fields that enables communication between the custom field iframe and the parent form.
 * This API allows custom fields to get/set values and listen to changes in the parent form.
 */
(function() {
    window.DotCustomFieldApi = {
        ready(callback) {
            if (window.DotCustomFieldApi.get) {
                callback(window.DotCustomFieldApi);
                return;
            }

            window.addEventListener('message', function onMessage(event) {
                if (event.data.type === 'dotcms:form:loaded') {
                    window.removeEventListener('message', onMessage);
                    callback(window.DotCustomFieldApi);
                }
            });
        }
    };
})();
</script>

<%

    if (null != contentTypeVarName && null != fieldName) {

        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        ContentType contentType = contentTypeAPI.find(contentTypeVarName);

        // GET CURRENT CONTENTLET OBJECT
        String inode = request.getParameter("inode") != null ? request.getParameter("inode") : null;
        ContentletAPI conAPI = APILocator.getContentletAPI();
        Contentlet contentlet =  (inode!=null) ? conAPI.find(inode,user,false) : new Contentlet();
        String contentletObj = "{}";

        if(contentlet != null){
            try{
                Map con = contentlet.getMap();
                contentletObj = mapper.writeValueAsString(con);
            } catch(Exception e){
                Logger.error("legacy-custom-field.jsp", "Error serializing contentlet: " + e.getMessage(), e);
            }
        }

        if (null != contentType) {

            Field field = contentType.fieldMap().get(fieldName);

            // Serialize fieldMap to JSON and remove 'values' attribute from each field
            // to prevent JavaScript parsing errors from HTML/CSS/JS content in 'values'
            String fieldJson;
            try {
                JsonNode fieldMapNode = mapper.valueToTree(contentType.fieldMap());
                if (fieldMapNode.isObject()) {
                    ObjectNode fieldMapObject = (ObjectNode) fieldMapNode;
                    // Remove 'values' attribute from each field in the map
                    fieldMapObject.fields().forEachRemaining(entry -> {
                        JsonNode fieldNode = entry.getValue();
                        if (fieldNode.isObject()) {
                            ((ObjectNode) fieldNode).remove("values");
                        }
                    });
                }
                fieldJson = mapper.writeValueAsString(fieldMapNode);
            } catch (Exception e) {
                Logger.error("legacy-custom-field.jsp", "Error serializing fieldMap without 'values': " + e.getMessage(), e);
                // Fallback to original serialization if something goes wrong
                fieldJson = mapper.writeValueAsString(contentType.fieldMap());
            }


            if (null != field) {

                String HTMLString = "";
                Object value = null;
                String defaultValue = field.defaultValue() != null ? field.defaultValue().trim() : "";
                String textValue = field.values();

                if(UtilMethods.isSet(textValue)){
                    org.apache.velocity.context.Context velocityContext =  com.dotmarketing.util.web.VelocityWebUtil.getVelocityContext(request,response);
                    // Set velocity variable if not already set
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
                    <body id="legacy-custom-field-body">
                        <%= HTMLString %>
                    </body>

                    <script>
                        /**
                         * Legacy Custom Field Integration Module
                         *
                         * This module handles the integration between legacy custom fields in iframes
                         * and the parent Angular application. It provides:
                         * 1. Automatic iframe height adjustment (via external module)
                         * 2. Two-way data binding between Angular and legacy fields
                         * 3. Support for dynamic field creation and updates
                         *
                         * === INTEGRATION MODULE ===
                         * Handles iframe integration and field synchronization automatically
                         */
                        (() => {
                            // Initialize the height manager using the external module
                            const iframeId = '<%= field.variable() %>';
                            const inode = '<%= inode != null ? inode : "new" %>';
                            const modalMode = <%= modalMode %>;

                            if (window.DotIframeHeightManager) {
                                window.DotIframeHeightManager.initializeHeightManager(iframeId, inode, modalMode);
                            }


                            const allFields = Object.values(<%= fieldJson %>)
                                .filter(field => field.dataType !== 'SYSTEM');


                            const contentlet = <%= contentletObj %>;

                            if (window.DotFieldInterceptors) {
                                window.DotFieldInterceptors.initializeFieldInterceptors(allFields, contentlet);
                            }

                        })();
                    </script>
<%
            }
        }
    }
%>
</html>
