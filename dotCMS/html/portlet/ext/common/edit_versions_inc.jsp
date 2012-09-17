<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.util.*" %>
<%@page import="com.dotmarketing.beans.*" %>
<%@page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.IdentifierFactory" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.factories.WebAssetFactory" %>


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

%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.DotContentletStateException"%>
<%@page import="com.dotmarketing.business.Versionable"%>

<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>



<div class="buttonRow" style="text-align: left;padding-left:20px;">
<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=(ident!=null?ident.getId():"") %>
</div>
<table class="listingTable">
	<tr>
		<th width="5%" nowrap><%= LanguageUtil.get(pageContext, "Status") %></th>
		<th width="10%" nowrap><%= LanguageUtil.get(pageContext, "Action") %></th>
		<% if(isContentlet){ %>
			<th width="1%" nowrap>&nbsp;</th>
		<% } %>
		<th width="45%"><%= LanguageUtil.get(pageContext, "Title") %></th>
		<th width="20%"><%= LanguageUtil.get(pageContext, "Author") %></th>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Modified-Date") %></th>
		<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Inode") %></th>
	</tr>
<%
	Iterator<Versionable> versionsIt = versions.iterator();
	int kmod = 0;
	boolean isAlreadyLocked = false;
	while (versionsIt.hasNext()) {
		Versionable ver = versionsIt.next();
		boolean working = ver.isWorking();
		boolean live = ver.isLive();
		Language langV = APILocator.getLanguageAPI().getDefaultLanguage();
		if(isContentlet){
			langV = APILocator.getLanguageAPI().getLanguage(((Contentlet)ver).getLanguageId());
		}
		String vinode = ver.getInode();
		String title = ver.getTitle();
		String modUser = ver.getModUser();
		Date modDate = ver.getModDate();
		String statusIcon = com.dotmarketing.util.UtilHTML.getVersionStatusIcons(ver);
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
		<%=statusIcon%>
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
		<% if(isContentlet){ %>
			<td> <img src="/html/images/languages/<%=langV.getLanguageCode()+"_"+langV.getCountryCode() %>.gif"/></td>		
		<% } %>
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


