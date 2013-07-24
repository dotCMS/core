<%@page import="org.apache.jasper.tagplugins.jstl.core.ForEach"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.util.*" %>
<%@page import="com.dotmarketing.beans.*" %>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.IdentifierFactory" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.factories.WebAssetFactory" %>
<%@page import="com.dotcms.publisher.assets.bean.PushedAsset" %>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage" %>

<%
	Inode asset = (Inode) request.getAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT);

	List<PushedAsset> pushedAssets = APILocator.getPushedAssetsAPI().getPushedAssets(asset.getIdentifier());

%>

<script>

function deletePushHistory() {

	var xhrArgs = {
		url : '/api/bundle/deletepushhistory/assetid/<%=(asset.getIdentifier())%>',
		handleAs : "json",
		sync: false,
		load : function(data) {

		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	var deferred = dojo.xhrGet(xhrArgs);
	document.location.reload(true);
}
</script>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.business.Versionable"%>

<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>


<div class="yui-g portlet-toolbar">
	<div class="yui-u first">
		<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=(asset.getIdentifier()) %>
	</div>

	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="deletePushHistory();" iconClass="deleteIcon" disabled='<%=pushedAssets.isEmpty()%>'>
			<%= LanguageUtil.get(pageContext, "publisher_delete_asset_history") %>
		</button>
	</div>
</div>

<table class="listingTable">
	<tr>
		<th width="10%" nowrap><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></th>
		<th width="45%"><%= LanguageUtil.get(pageContext, "publisher_push_date") %></th>
		<th width="20%"><%= LanguageUtil.get(pageContext, "publisher_Environment") %></th>
	</tr>
<%
	for(PushedAsset pushedAsset: pushedAssets) {

	Environment env = APILocator.getEnvironmentAPI().findEnvironmentById(pushedAsset.getEnvironmentId());


%>
	<tr  >
		<td nowrap="nowrap">
			<%= pushedAsset.getBundleId() %>
		</td>
		<td><%= pushedAsset.getPushDate() %></td>
		 <td><%= (env != null) ? env.getName() : LanguageUtil.get(pageContext, "deleted") %></td>
	</tr>
<% } if (pushedAssets.size() == 0) { %>
	<tr>
		<td colspan="5">
			<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "publisher_status_no_entries") %></div>
		</td>
	</tr>
<% } %>

</table>


