<%@ page import="com.dotmarketing.util.Config" %>

<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>

<%if(request.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)==null){%>
   <!--<div class="portlet-wrapper">
        <jsp:include page="/html/portlet/admin/sub_nav.jsp"></jsp:include>
   </div>-->
<%}%>

<%
    //Check in the query string if we want to load some specific tab
    String requestedTab = request.getParameter( "tab" );

    Boolean loadLicense = false;
    if ( requestedTab != null && requestedTab.equals( "licenseTab" ) ) {

        loadLicense = true;
    }
%>

<div class="portlet-main">

    <div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

        <div id="companyTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "configuration_Basic_Config") %>" >
            <div id="companyTabContentDiv"></div>
        </div>

        <div id="licenseTab" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.EXT_LICENSE_MANAGER")%>" >
            <div id="licenseTabContentDiv"></div>
        </div>

        <%  if(Config.getBooleanProperty("ENABLE_SERVER_HEARTBEAT", true)) { %>
            <div id="networkTab" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "Network")%>" >
                <div id="networkTabContentDiv"></div>
            </div>
        <%
            }
        %>

        <div id="remotePublishingTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Publishing_Environment") %>" >
            <div id="remotePublishingTabContentDiv"></div>
        </div>

    </div>

</div>

<script type="text/javascript">

    dojo.ready(function () {

        var tab = dijit.byId("mainTabContainer");
        dojo.connect(tab, 'selectChild', function (evt) {
            selectedTab = tab.selectedChildWidget;

            if (selectedTab.id == "companyTab") {
                loadCompanyTab();
            } else if (selectedTab.id == "remotePublishingTab") {
                loadRemotePublishingTab();
            } else if (selectedTab.id == "networkTab") {
                loadNetworkTab();
            } else if (selectedTab.id == "licenseTab") {
                loadLicenseTab();
            }
        });

        <%if (loadLicense) {%>
            dijit.byId("mainTabContainer").selectChild("licenseTab", true);
        <%}else{%>
            loadCompanyTab();
        <%}%>

    });
</script>