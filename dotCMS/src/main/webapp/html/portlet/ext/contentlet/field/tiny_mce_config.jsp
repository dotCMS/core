<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="org.apache.velocity.context.Context"%>
<%@page import="com.dotcms.rendering.velocity.util.VelocityUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
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
    <% Context ctx = VelocityUtil.getWebContext(request, response);%>

    var dotCMSHasLicense = <%=dotCMSHasLicense%>;
    var tinyMCEProps = <%=VelocityUtil.eval("#set($dontShowIcon=true)#dotParse('//" + myHost.getHostname() + "/application/wysiwyg/tinymceprops.vtl')", ctx)%>

<%} else if (UtilMethods.isSet(Config.getStringProperty("TINY_MCE_CONFIG_LOCATION", null))) {%>

    <jsp:include page="<%= Config.getStringProperty(\"TINY_MCE_CONFIG_LOCATION\") %>" />

<%} else {%>
    <%@ include file="/html/portlet/ext/contentlet/field/tiny_mce_config_default.jsp" %>
<%}%>