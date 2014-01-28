
<%@page import="com.dotcms.rest.RestExamplePortlet"%>
<%@page import="com.dotcms.rest.config.RestServiceUtil"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%if(request.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)==null){%>
    <div class="portlet-wrapper">
        <jsp:include page="/html/portlet/admin/sub_nav.jsp"></jsp:include>
    </div>
<%}%>

<script type="text/javascript" >

var loadClusterTab = function () {

    var url = "/api/network/layout/NetworkPortlet/new_cluster_config";
    var content = dijit.byId("clusterTabContent");

    if (content) {
        content.destroyRecursive(false);
    }
    content = new dojox.layout.ContentPane({
        id: "clusterTabContent",
        preventCache: true
    }).placeAt("clusterTabContentDiv");

    content.attr("href", url);
    content.refresh();
};

</script>

<div class="portlet-wrapper">

    <div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

        <div id="clusterTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "configuration_cluster_status") %>" >
            <div id="clusterTabContentDiv"></div>
        </div>

    </div>

</div>

<script type="text/javascript">

    dojo.ready(function () {

        var tab = dijit.byId("mainTabContainer");
        dojo.connect(tab, 'selectChild', function (evt) {
            selectedTab = tab.selectedChildWidget;

            if (selectedTab.id == "clusterTab") {
                loadClusterTab();
            }
        });

        loadClusterTab();
    });
</script>