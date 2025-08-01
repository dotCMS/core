<%@page import="com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="java.util.*" %>
<%@page import="com.dotmarketing.beans.*" %>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.IdentifierFactory" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.factories.WebAssetFactory" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.business.Versionable"%>

<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@ page import="com.dotcms.rendering.velocity.viewtools.WebAPI" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator" %>
<%@ page import="com.dotcms.variant.model.Variant" %>
<%@ page import="com.dotcms.variant.VariantAPI" %>

<%

	String id=request.getParameter("contentletId");
	Identifier ident = APILocator.getIdentifierAPI().find(id);
	boolean isImage = UtilMethods.isImage(ident.getAssetName());

	final String variantNameParameter = request.getParameter("variantName");
	final Variant variant = variantNameParameter == null || VariantAPI.DEFAULT_VARIANT.equals(variantNameParameter) ?
			VariantAPI.DEFAULT_VARIANT :
			APILocator.getVariantAPI().get(variantNameParameter).orElse(VariantAPI.DEFAULT_VARIANT);

	List<Contentlet> versions = APILocator.getContentletAPI().findAllVersions(ident,  variant, user, false);
	boolean canEdit  = false;

	if(versions.size() > 0){
		canEdit  = APILocator.getPermissionAPI().doesUserHavePermission(versions.get(0), PermissionAPI.PERMISSION_EDIT, user);
	}


%>




<div class="contentIdentifier" style="margin: 0px 18px 18px 18px;">
<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=ident.getId() %>
	<style>
		.hoverable-box {
			float: right;
			padding: 4px 8px;
			border: 1px solid #ccc;
			cursor: pointer;
			border-radius: 3px;
			text-align: center;
			transition: background 0.3s;
		}
		.hoverable-box:hover {
			background: #e6f3ff; /* Light blue */
		}
	</style>
	<div class="hoverable-box">

		<a href="/api/v1/content/<%=ident.getId() %>" style="text-decoration: none;font-weight: normal" target="_blank">API</a>
	</div>
</div>
<table class="listingTable">
	<tr>
		<th width="5%" nowrap><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th width="10%" nowrap><%= LanguageUtil.get(pageContext, "Action") %></th>
		<th style="min-width:120px;" nowrap>&nbsp;</th>
		<th width="45%"><%= LanguageUtil.get(pageContext, "Title") %></th>
		<th width="20%"><%= LanguageUtil.get(pageContext, "Author") %></th>
		<%if(isImage){ %>
			<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Image") %></th>
		<%} %>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Modified-Date") %></th>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Inode") %></th>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Actions") %></th>
	</tr>
<%
    Iterator<Contentlet> versionsIt = versions.iterator();
	boolean isAlreadyLocked = false;
	while (versionsIt.hasNext()) {
		Contentlet ver = versionsIt.next();
		String vinode = ver.getInode();
		String title = ver.getTitle();
		String modUser = ver.getModUser();
		Date modDate = ver.getModDate();
		Optional<ContentletVersionInfo> verinfo=APILocator.getVersionableAPI().getContentletVersionInfo(id, ver.getLanguageId());
		if(!verinfo.isPresent()){
			continue;
		}
		Language langV=APILocator.getLanguageAPI().getLanguage(ver.getLanguageId());
		boolean working = ver.getInode().equals(verinfo.get().getWorkingInode());
		boolean live = ver.getInode().equals(verinfo.get().getLiveInode());

%>
	<tr>
		<td nowrap="nowrap" width="50" align="center">
			<%=UtilHTML.getVersionStatusIcons(ver) %>
		</td>
		<td nowrap="nowrap">
		<% if (!working) {  %>
			<% if(canEdit) {  %>
				<% if (!live) { %>
					<a  href="javascript:void(0);" onclick="deleteVersion('<%= vinode%>');return false;"><%= LanguageUtil.get(pageContext, "Delete") %></a> -

				<% } %>
				<a  href="javascript: selectVersion('<%= vinode %>');"><%= LanguageUtil.get(pageContext, "Bring-Back") %></a>
			<% } %>
		<% } else { %>
			<%= LanguageUtil.get(pageContext, "Working-Version") %>
		<% } %>
		</td>
		<td> <img src="/html/images/languages/<%= LanguageUtil.getLiteralLocale(langV.getLanguageCode(), langV.getCountryCode()) %>.gif"/>&nbsp;<%=langV.getLanguage()+"&nbsp;"+(UtilMethods.isSet(langV.getCountryCode()) ? ("(" + langV.getCountryCode() + ")&nbsp;") : StringPool.BLANK) %></td>
		<td><a  href="javascript: editVersion ('<%= vinode %>');"><%= title %></a></td>
<%
	String modUserName = "";
	if(UtilMethods.isSet(modUser)){
		try{
			modUserName = APILocator.getUserAPI().loadUserById(modUser,APILocator.getUserAPI().getSystemUser(),false) != null ? APILocator.getUserAPI().loadUserById(modUser,APILocator.getUserAPI().getSystemUser(),false).getFullName(): "";
		}catch(Exception e){Logger.debug(this,"No User Found");}
	}
%>
		 <td><%= modUserName %></td>
		<!-- DOTCMS-3813  -->
		<!-- }  -->

		<!-- Timezone
		<%= APILocator.systemTimeZone().getID() %>
		 -->
		 <%if(isImage){ %>
			 <td align="center">
			 	<% if (!working && canEdit && !live) { %>
			 		<a  href="javascript: selectVersion('<%= vinode %>');">
			 	<%} %>

				<%if(!ident.getAssetName().toLowerCase().endsWith(".svg")){%>
			 		<img src="/contentAsset/image/<%=vinode %>/fileAsset/?byInode=1&filter=Thumbnail&thumbnail_h=125&thumbnail_w=125" style="width:150px;height:150px;border:1px solid silver;padding:3px;"></a>
				<%}else{%>
				 <img src="/contentAsset/image/<%=vinode %>/fileAsset/?byInode=1" style="width:150px;height:150px;border:1px solid silver;padding:3px;"></a>
				<%}%>
			 </td>
		 <%} %>
		<td nowrap="nowrap" style="text-align:center;"><%= UtilMethods.dateToHTMLDate(modDate) %> - <%= UtilMethods.dateToHTMLTime(modDate) %></td>
		<td nowrap="nowrap"><%= vinode %></td>
		<td nowrap="nowrap">
			<% if (!working) { %>
				<button dojoType="dijit.form.Button" class="dijitButton" onclick="emmitCompareEvent('<%=vinode%>', '<%=ident.getId()%>', '<%=langV.getLanguageCode() %>-<%=langV.getCountryCode().toLowerCase()%>');return false;"><%= LanguageUtil.get(pageContext, "compare") %></button>
			<% } %>
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
