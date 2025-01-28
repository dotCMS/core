<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.struts.MultiMessageResources"%>
<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", PortletID.CONFIGURATION.toString()); %>
<%@ include file="/html/common/uservalidation.jsp"%>


<%@page import="com.dotcms.enterprise.LicenseUtil"%>




<style type="text/css">

    .license-manager {
        width: 900px; /* Adjust the width as needed */
        margin: 0 auto;
        padding: 30px 60px;
    }
</style>



<div class="license-manager">
    <h2 style="padding-bottom:40px;text-align: center">dotCMS is Licensed under the dotCMS Business Source License</h2>
    <div style="background: black;color:white;border-radius: 8px;padding:40px;overflow: auto">
    <pre style="margin:auto;max-width: 800px;white-space: pre-wrap;"
    ><%= LicenseUtil.getLicenseText() %>
    </pre>
    </div>



</div>
