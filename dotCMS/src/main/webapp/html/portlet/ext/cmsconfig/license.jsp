<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.struts.MultiMessageResources"%>
<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", PortletID.CONFIGURATION.toString()); %>
<%@ include file="/html/common/uservalidation.jsp"%>


<%@page import="com.dotcms.enterprise.LicenseUtil"%>







<div class="license-manager">
    <h2 style="padding-bottom:40px;text-align: center">dotCMS is Licensed under the dotCMS Business Source License</h2>
    <div style="margin:auto;background:#292929;width:1000px;color:white;border-radius:20px;padding:50px 100px;overflow: auto">
    <pre style='margin:auto;width:100%;border:0px solid red;white-space: pre-wrap;font-family:  Menlo, Consolas, "Courier New", monospace;'
    ><%= LicenseUtil.getLicenseText() %>
    </pre>
    </div>



</div>
