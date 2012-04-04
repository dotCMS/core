<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="java.util.*" %>
<%@page import="com.dotmarketing.beans.*" %>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.IdentifierFactory" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.factories.WebAssetFactory" %>
<%

	PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	boolean canEdit = true;
 	Versionable v = (Versionable)request.getAttribute(com.dotmarketing.util.WebKeys.VERSIONS_INODE_EDIT);
	if(v instanceof Permissionable) {
		canEdit = permissionAPI.doesUserHavePermission((Permissionable)v, PermissionAPI.PERMISSION_EDIT, user);
	}

	List<Versionable> versions = new ArrayList<Versionable>();


	Identifier iden = APILocator.getIdentifierAPI().find(v);
	File workingFile = (File) APILocator.getVersionableAPI().findWorkingVersion(iden, user, false);
	boolean isImage = UtilMethods.isImage(workingFile.getFileName());
	versions = WebAssetFactory.getAssetVersionsandLive(workingFile);


%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.business.Versionable"%>

<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%><script language="JavaScript">
</script>

<table class="listingTable">
	<tr>
		<th width="5%" nowrap><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th width="10%" nowrap><%= LanguageUtil.get(pageContext, "Action") %></th>
		<th width="45%"><%= LanguageUtil.get(pageContext, "Title") %></th>
		<th width="20%"><%= LanguageUtil.get(pageContext, "Author") %></th>
		<%if(isImage){ %>
			<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Image") %></th>
		<%} %>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Modified-Date") %></th>
	</tr>
<%
	int show = 10;
	boolean showMoreThan = (request.getParameter("showMoreThan") != null);
	if(showMoreThan){
		show = 1000;
	}
	Iterator<Versionable> versionsIt = versions.iterator();
	int kmod = 0;
	int i=0;
	while (versionsIt.hasNext()) {
		i++;
		File ver = (File) versionsIt.next();
		boolean working = ver.isWorking();
		boolean live = ver.isLive();
		String vinode = ver.getInode();
		String title = ver.getTitle();
		String modUser = ver.getModUser();
		Date modDate = ver.getModDate();
		String statusIcon = com.dotmarketing.util.UtilHTML.getStatusIcons(live,working,ver.isArchived(),ver.isLocked(),COMMON_IMG, false);
		
		
		String str_style = "";
		if ((kmod % 2) == 0) {
			str_style = "class='alternate_1'";
		}
		else{
			str_style = "class='alternate_2'";
		}
		kmod++;
		
%>
	<tr  <%=str_style%>>
		<td nowrap="nowrap" width="50">
				<%= statusIcon %>
		</td>
		<td nowrap="nowrap">
		<% if (!working) {  %>
			<% if(canEdit) {  %>
				<% if (!live) { %>
					<a  href="javascript: deleteVersion('<%= vinode%>');"><%= LanguageUtil.get(pageContext, "Delete") %></a> - 
				<% } %>
				<a  href="javascript: selectVersion('<%= vinode %>');"><%= LanguageUtil.get(pageContext, "Bring-Back") %></a>
			<% } %>
		<% } else { %>
			<%= LanguageUtil.get(pageContext, "Working-Version") %>
		<% } %>
		</td>
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
		<%= Calendar.getInstance().getTimeZone().getID() %> 
		 -->
		 <%if(isImage){ %>
			 <td align="center">
			 	<% if (!working && canEdit && !live) { %>
			 		<a  href="javascript: selectVersion('<%= vinode %>');">
			 	<%} %>
			 	<img src="/contentAsset/image/<%=vinode %>/?byInode=true&filter=Thumbnail&thumbnail_h=125&thumbnail_w=125" style="width:150px;height:150px;border:1px solid silver;padding:3px;"></a>
				
			 </td>
		 <%} %>
		 
		<td nowrap="nowrap" style="text-align:center;"><%= UtilMethods.dateToHTMLDate(modDate) %> - <%= UtilMethods.dateToHTMLTime(modDate) %></td>
	</tr>

	<%  if (! showMoreThan && i > show) { %>
		<tr>
			<td colspan="6" align="center">
				<a href="javascript:window.location=window.location + '&showMoreThan=1';"><%= LanguageUtil.get(pageContext, "show-all") %> <%=versions.size() %></div>
			</td>
		</tr>
		<% break;} %>
	<%} %>
	
	<%if (versions.size() == 0) { %>
		<tr>
			<td colspan="6">
				<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Versions-Found") %></div>
			</td>
		</tr>
	<% } %>

</table>