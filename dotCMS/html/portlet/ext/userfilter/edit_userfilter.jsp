<%@ include file="/html/portlet/ext/userfilter/init.jsp" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.util.InodeUtils"%>
<%

    String	pageNumberStr = (String)request.getAttribute("page");
	int	pageNumber = 1;
	try {
		pageNumber = Integer.parseInt(pageNumberStr);
	}catch(Exception e){
		pageNumber = 1;
	}
	int perPage = com.dotmarketing.util.Config.getIntProperty("USERMANAGER_PER_PAGE");

	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/userfilter/view_userfilterlist"} );
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
	String adminPortletCode = "_9_";

	String redirect = java.net.URLEncoder.encode(referrer + "&cmd=search&emailAddress=");

	String userFilterTitle = (String)request.getAttribute(com.dotmarketing.util.WebKeys.USER_FILTER_LIST_TITLE);
	String userFilterInode = (String)request.getAttribute(com.dotmarketing.util.WebKeys.USER_FILTER_LIST_INODE);

	UserFilter uf = new UserFilter();
	Set<Role> readRoles = new HashSet<Role>();
	Set<Role> writeRoles = new HashSet<Role>();

	PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	if (InodeUtils.isSet(userFilterInode)) {
		uf = UserFilterFactory.getUserFilter(userFilterInode);
		readRoles = perAPI.getReadRoles(uf);
		writeRoles = perAPI.getWriteRoles(uf);
	}

	boolean viewUserManager = false;
	
	UserManagerListSearchForm form = (UserManagerListSearchForm)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLISTFORM);
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<!--liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "edit-user-filter-box-title") %>' /-->

<style type="text/css">
<!--
.toggleDiv {z-index: 99;  display: none;}
linkToggle { font: bold italic }
-->
</style>


<!-- SCRIPT FOR AJAX MANAGEMENT -->
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<!-- END SCRIPT FOR AJAX MANAGEMENT -->

<script type="text/javascript">
var userId = '<%= user.getUserId() %>';
</script>

<div id="hintTag" style="position: absolute; display: none; background-color: #FFFFE7; border-color: #000000; border-style: solid; border-width: 1px;"><font style="font-family: Verdana, Arial,Helvetica; color: #000000; font-style: italic;"><%= LanguageUtil.get(pageContext, "Tags-are-descriptors-that-you-can-assign-to-users-Tags-are-a-little-bit-like-keywords") %></font></div>

	<form method="post" name="<portlet:namespace />fm" id="<portlet:namespace />fm" enctype="multipart/form-data">

<div class="yui-g">
	<div class="yui-u first">
		<div dojoType="dijit.layout.ContentPane">
			<%@ include file="/html/portlet/ext/usermanager/search_form_inc.jsp" %>
		</div>
	</div>
	<div class="yui-u">
		<div dojoType="dijit.layout.ContentPane">
			<%@ include file="/html/portlet/ext/usermanager/search_results_inc.jsp" %>
		</div>
	</div>
</div>
	
	</form>

</liferay:box>
