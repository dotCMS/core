
<%@page import="com.dotcms.rest.RestExamplePortlet"%>
<%@page import="com.dotcms.rest.config.RestServiceUtil"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%-- <%if(request.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)==null){%> --%>
<!--     <div class="portlet-wrapper"> -->
<%--         <jsp:include page="/html/portlet/admin/sub_nav.jsp"></jsp:include> --%>
<!--     </div> -->
<%-- <%}%> --%>

<script type="text/javascript" >
dojo.require("dijit.layout.TabContainer");
dojo.require("dijit.layout.ContentPane");

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

dojo.ready(function () {

	dotAjaxNav.resetCrumbTrail();
	dotAjaxNav.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Network")%>", "/api/network/layout/NetworkPortlet/new_cluster_config.jsp");
	dotAjaxNav.refreshCrumbtrail();

    loadClusterTab();
});

</script>

<div class="portlet-wrapper" style="min-height: 600px">
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "Network")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>

	<div class="yui-g portlet-toolbar" style="margin:0 0 5px 10px;">
		<div class="yui-u first" style="white-space: nowrap">
			<b><%=LanguageUtil.get(pageContext, "Network")%></b>
		</div>
	</div>

    <div id="clusterTabContentDiv"></div>

</div>
