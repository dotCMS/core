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

<%
	
	String id=request.getParameter("contentletId");
	Identifier ident = APILocator.getIdentifierAPI().find(id);
	boolean isImage = UtilMethods.isImage(ident.getAssetName());
	List<Contentlet> versions = APILocator.getContentletAPI().findAllVersions(ident, user, false);
	
	boolean canEdit  = false;

	if(versions.size() > 0){
		canEdit  = APILocator.getPermissionAPI().doesUserHavePermission(versions.get(0), PermissionAPI.PERMISSION_EDIT, user);
	}


%>




<div class="buttonRow" style="text-align: left;padding-left:20px;">
<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=ident.getId() %>
</div>
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
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Inode") %></th>
	</tr>
<%
	Iterator<Contentlet> versionsIt = versions.iterator();
	int kmod = 0;
	boolean isAlreadyLocked = false;
	while (versionsIt.hasNext()) {
		Contentlet ver = versionsIt.next();
		Contentlet c = (Contentlet) ver;
		boolean working = ver.isWorking();
		boolean live = ver.isLive();
		String vinode = ver.getInode();
		String title = ver.getTitle();
		String modUser = ver.getModUser();
		Date modDate = ver.getModDate();

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
		<td nowrap="nowrap" width="50" align="center">
		<%if(ver.isWorking()){%>
			<img src="/html/images/icons/status-away.png" />
		<%} %>
		<%if(ver.isLive()){%>
			<img src="/html/images/icons/status.png" />
		<%} %>
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
			 	<img src="/contentAsset/image/<%=vinode %>/fileAsset/?byInode=1&filter=Thumbnail&thumbnail_h=125&thumbnail_w=125" style="width:150px;height:150px;border:1px solid silver;padding:3px;"></a>
				
			 </td>
		 <%} %>
		<td nowrap="nowrap" style="text-align:center;"><%= UtilMethods.dateToHTMLDate(modDate) %> - <%= UtilMethods.dateToHTMLTime(modDate) %></td>
		<td nowrap="nowrap"><%= vinode %></td>
	</tr>
	<% } if (versions.size() == 0) { %>
		<tr>
			<td colspan="5">
				<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Versions-Found") %></div>
			</td>
		</tr>
	<% } %>

</table>


