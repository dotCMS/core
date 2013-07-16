<%@page import="org.apache.jasper.tagplugins.jstl.core.ForEach"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.util.*" %>
<%@page import="com.dotmarketing.beans.*" %>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.IdentifierFactory" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.factories.WebAssetFactory" %>
<%@page import="com.dotcms.publisher.assets.bean.PushedAsset" %>

<%

	Contentlet cver = null;
	boolean isContentlet = false;
 	Versionable v = (Versionable)request.getAttribute(com.dotmarketing.util.WebKeys.VERSIONS_INODE_EDIT);
	if(v instanceof Contentlet){
		cver = (Contentlet)v;
		isContentlet = true;
	}

	PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	boolean canEdit = true;

	if(v instanceof Permissionable) {
		canEdit = permissionAPI.doesUserHavePermission((Permissionable)v, PermissionAPI.PERMISSION_EDIT, user);
	}

	List<Versionable> versions = new ArrayList<Versionable>();
	Identifier ident = null;
	if (isContentlet && InodeUtils.isSet(cver.getInode())) {
		ident = APILocator.getIdentifierAPI().find(cver);
		versions.addAll(APILocator.getContentletAPI().findAllVersions(ident,user,false));
		Contentlet working = null;
		try{
			if(InodeUtils.isSet(ident.getInode()))
			{
				working = APILocator.getContentletAPI().findContentletByIdentifier(ident.getInode(),false,cver.getLanguageId(),user,false);
			}
		}catch(DotContentletStateException cse){
			//Case of a new language version of the content
			Logger.debug(this,LanguageUtil.get(pageContext, "Working-Contentlet-could-not-be-found-for-identifier")+" = "+ ident.getInode());
		}catch(Exception e){
			Logger.warn(this,LanguageUtil.get(pageContext, "Unable-to-get-working-contentlet")+ " : " + LanguageUtil.get(pageContext, "Usually-not-a-problem-it-is-probably-because-the-contentlet-is-new"));
		}
		//if(working != null)
			//versions.add(0,working);
	}else if(InodeUtils.isSet(v.getInode())){
		ident = APILocator.getIdentifierAPI().find(v);
		WebAsset working = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(ident, user, false);
		versions = WebAssetFactory.getAssetVersionsandLive(working);
	}

	List<PushedAsset> pushedAssets = APILocator.getPushedAssetsAPI().getPushedAssets(ident!=null?ident.getId():"");

%>

<script>

function deletePushHistory(roleid) {
	var roleNode;

	var xhrArgs = {
		url : "/api/bundle/deletepushhistory/assetid/" + <%=(ident!=null?ident.getId():"")%>,
		handleAs : "json",
		sync: false,
		load : function(data) {
			roleNode = data;
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	var deferred = dojo.xhrGet(xhrArgs);
	return roleNode;
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
		<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=(ident!=null?ident.getId():"") %>
	</div>

	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.Button" onClick="deletePushHistory();" iconClass="plusIcon">
			<%= LanguageUtil.get(pageContext, "publisher_delete_asset_history") %>
		</button>
	</div>
</div>

<table class="listingTable">
	<tr>
		<th width="5%" nowrap><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th width="10%" nowrap><%= LanguageUtil.get(pageContext, "publisher_Identifier") %></th>
		<th width="45%"><%= LanguageUtil.get(pageContext, "publisher_push_date") %></th>
		<th width="20%"><%= LanguageUtil.get(pageContext, "publisher_Environment") %></th>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Actions") %></th>
	</tr>
<%
	for(PushedAsset asset: pushedAssets) {

	Environment env = APILocator.getEnvironmentAPI().findEnvironmentById(asset.getEnvironmentId());


%>
	<tr  >
		<td nowrap="nowrap" width="50" align="center">
		</td>
		<td nowrap="nowrap">
			<%= asset.getBundleId() %>
		</td>
		<td><%= asset.getPushDate() %></td>
		 <td><%= env.getName() %></td>
		<td nowrap="nowrap" style="text-align:center;">
			<button dojoType="dijit.form.Button" onClick="forceResend();" iconClass="plusIcon">
				<%= LanguageUtil.get(pageContext, "publisher_delete_asset_history") %>
			</button>
		</td>
	</tr>
<% } if (versions.size() == 0) { %>
	<tr>
		<td colspan="5">
			<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Versions-Found") %></div>
		</td>
	</tr>
<% } %>

</table>


