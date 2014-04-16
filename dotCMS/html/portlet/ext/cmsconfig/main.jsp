<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>

<%if(request.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)==null){%>
    <div class="portlet-wrapper">
        <jsp:include page="/html/portlet/admin/sub_nav.jsp"></jsp:include>
    </div>
<%}%>

<div class="portlet-wrapper">

    <div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

        <div id="companyTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "configuration_Basic_Config") %>" >
            <div id="companyTabContentDiv"></div>
        </div>

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
            }
        });

        loadCompanyTab();
    });
</script>