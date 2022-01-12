<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="org.apache.velocity.context.Context"%>
<%@page import="com.dotcms.rendering.velocity.util.VelocityUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.util.Logger" %>
<%
Host myHost =  WebAPILocator.getHostWebAPI().getCurrentHost(request); 
Identifier id = APILocator.getIdentifierAPI().find(myHost, "/application/wysiwyg/tinymceprops.vtl");
boolean hasDefaultConfig = id!=null && UtilMethods.isSet(id.getId());
if(!hasDefaultConfig){
    myHost =  WebAPILocator.getHostWebAPI().findDefaultHost(APILocator.systemUser(), false);
    id = APILocator.getIdentifierAPI().find(myHost, "/application/wysiwyg/tinymceprops.vtl");
    hasDefaultConfig = id!=null && UtilMethods.isSet(id.getId());
}

boolean dotCMSHasLicense = LicenseUtil.getLevel() > 100;

if (hasDefaultConfig) {%>
    <%
        Context ctx = VelocityUtil.getWebContext(request, response);
        String props = VelocityUtil.eval("#set($dontShowIcon=true)#dotParse('//" + myHost.getHostname() + "/application/wysiwyg/tinymceprops.vtl')", ctx);
        if(UtilMethods.isNotSet(props)){
            props = "{}";
            Logger.warn(this.getClass()," Unable to set props on TinyMCE! No tinyMCE default props were loaded. check that the following file exist on your site.`/application/wysiwyg/tinymceprops.vtl`.");
        }
    %>
  
    var dotCMSHasLicense = <%=dotCMSHasLicense%>;
    var tinyMCEProps = <%=props%>

<%} else if (UtilMethods.isSet(Config.getStringProperty("TINY_MCE_CONFIG_LOCATION", null))) {%>

    <jsp:include page="<%= Config.getStringProperty(\"TINY_MCE_CONFIG_LOCATION\") %>" />

<%} else {%>
    <%@ include file="/html/portlet/ext/contentlet/field/tiny_mce_config_default.jsp" %>
<%}%>
