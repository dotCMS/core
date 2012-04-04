<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.portlets.templates.factories.TemplateFactory" %>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.beans.Identifier"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@ page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.business.IdentifierAPI"%>

<%@ include file="/html/portlet/ext/htmlpages/init.jsp" %>

<%
	PermissionAPI perAPI = APILocator.getPermissionAPI();
HTMLPage htmlpage=null;

if (request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT)!=null) {
	htmlpage = (HTMLPage) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT);
}

Identifier identifier=null;
Template htmlTemplate=null;
File templateImgPreview = null;
if(UtilMethods.isSet(htmlpage.getIdentifier())) {
    identifier = APILocator.getIdentifierAPI().find(htmlpage.getIdentifier());
    htmlTemplate = APILocator.getHTMLPageAPI().getTemplateForWorkingHTMLPage(htmlpage);
    templateImgPreview = TemplateFactory.getImageFile(htmlTemplate);
}


String referer = request.getParameter("referer");
if (referer==null || referer.length()==0) {
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/htmlpages/view_htmlpages"});
	referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
}

String cmd = request.getParameter(Constants.CMD);
if( cmd == null && referer != null ) {
	referer = UtilMethods.encodeURL(referer);
}

String parent = (request.getParameter("parent") != null ) ? request.getParameter("parent") : "" ;



int[] monthIds = CalendarUtil.getMonthIds();
String[] months = CalendarUtil.getMonths(locale);
String[] days = CalendarUtil.getDays(locale);
java.util.Date startDate = htmlpage.getStartDate();
java.util.Date endDate = htmlpage.getEndDate();

Role[] roles = (Role[])com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
Folder folder = null;
try{
 folder = APILocator.getFolderAPI().find(htmlpage.getParent(),user,false);
} catch(Exception e){

}


// If this is a new page, show it on menu and
// order it properly
int showOnMenuNumber = 0;
try{
	showOnMenuNumber = APILocator.getFolderAPI().findMenuItems(folder,user,false).size() + 1;

}
catch(Exception e){

}
if(!UtilMethods.isSet(htmlpage.getInode())){
	htmlpage.setShowOnMenu(true);
	htmlpage.setSortOrder(showOnMenuNumber);
}
// Set Host based on page location
Host host = null;

try {
	APILocator.getHostAPI().findParentHost(folder, APILocator.getUserAPI().getSystemUser(), false);
} catch (Exception e) {

}

if(host == null || host.getInode() == null){
	String hostId ="";

	if (session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID) != null)
		hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
	else if(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID) != null)
		hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	else if (request.getParameter("host_id") != null)
		hostId = request.getParameter("host_id");

	host = APILocator.getHostAPI().find(hostId, user, false);
}

//This variable controls the name of the struts action used when the form is submitted
//the normal action is /ext/contentlet/edit_htmlpage but that can be changed
String formAction = request.getParameter("struts_action") == null?"/ext/htmlpages/edit_htmlpage":request.getParameter("struts_action");








//Permissions variables
boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
boolean ownerHasPubPermission = (hasOwnerRole && perAPI.doesRoleHavePermission(htmlpage, PermissionAPI.PERMISSION_PUBLISH,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
boolean ownerHasWritePermission = (hasOwnerRole && perAPI.doesRoleHavePermission(htmlpage, PermissionAPI.PERMISSION_WRITE,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole()));
boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
boolean canUserWriteToHTMLPage = ownerHasWritePermission || hasAdminRole || perAPI.doesUserHavePermission(htmlpage,PermissionAPI.PERMISSION_WRITE,user);
boolean canUserPublishHTMLPage = ownerHasPubPermission || hasAdminRole || perAPI.doesUserHavePermission(htmlpage,PermissionAPI.PERMISSION_PUBLISH,user);



//if we are a new htmlpage, check folder permissions
if( !InodeUtils.isSet(htmlpage.getInode()) && folder != null && InodeUtils.isSet(folder.getInode())){
	if(perAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)){
	    canUserWriteToHTMLPage=true;
	    if(!canUserPublishHTMLPage){
	        canUserPublishHTMLPage = perAPI.doesUserHaveInheriablePermissions(folder, htmlpage.getPermissionType(), PermissionAPI.PERMISSION_PUBLISH, user);
	    }
	}
}
%>




<%@page import="com.dotmarketing.portlets.folders.business.FolderFactory"%>
<html:form action='/ext/htmlpages/edit_htmlpage' styleId="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
<input name="<portlet:namespace />subcmd" type="hidden" value="">
<input type="hidden" name="userId" value="<%= user.getUserId() %>">
<input type="hidden" name="webStartDate" value="">
<input type="hidden" name="webEndDate" value="">
<input type="hidden" name="submitParent" id="submitParent" value="">
<input type="hidden" name="inode" value="<%=htmlpage.getInode()%>">
					<html:hidden property="selectedparent" styleId="selectedparent" />
					<html:hidden property="selectedparentPath" styleId="selectedparentPath" />
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-htmlpage\") %>" />


<script language="Javascript">
	<%@ include file="/html/portlet/ext/htmlpages/edit_htmlpages_js_inc.jsp" %>
	<liferay:include page="/html/js/calendar/calendar_js.jsp" flush="true">
		<liferay:param name="calendar_num" value="2" />
	</liferay:include>
</script>


<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- START basic properties -->
	<div id="hostPropertiesTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Basic-Properties") %>" onShow="showEditButtonsRow()">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "Page-Menu-Title") %>:</dt>
			<dd>



				<input type="text" dojoType="dijit.form.TextBox" name="title" id="titleField" value="<%=UtilMethods.webifyString(htmlpage.getTitle()) %>" onchange="beLazy()"/>

			</dd>

			<dt><%= LanguageUtil.get(pageContext, "Folder") %>:</dt>
			<dd>
				<% if(!InodeUtils.isSet(htmlpage.getParent())) { %>
					<div id="folder" name="parent" onlySelectFolders="true" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" <%= UtilMethods.isSet(host)?"hostId=\"" + host.getIdentifier() + "\"":"" %>></div>
				<% } else { %>
                    <html:hidden  property="parent" styleId="parent" />
					<html:text readonly="true" style="width:350px;border:0px;" styleClass="form-text" property="selectedparentPath" styleId="selectedparentPath" />

				<% } %>
			</dd>

			<dt><%= LanguageUtil.get(pageContext, "Page-URL") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" name="pageUrl" id="pageUrl" value="<%= UtilMethods.isSet(htmlpage.getPageUrl()) ? htmlpage.getPageUrl() : "" %>" /></dd>
			<dd class="inputCaption">(<%= LanguageUtil.get(pageContext, "page-name") %>)</dd>

			<dt><%= LanguageUtil.get(pageContext, "Template") %>:</dt>
			<dd>
				<span dojoType="dotcms.dojo.data.TemplateReadStore" jsId="templateStore" dojoId="templateStoreDojo" hostId="<%=host.getIdentifier() %>" ></span>
		  		<select id="template"
		  				name="template"
		  				dojoType="dijit.form.FilteringSelect"
		  				style="width:350px;"
		  				onChange="showTemplate()"
		  				store="templateStore"
		  				searchDelay="300"
		  				pageSize="15"
		  				autoComplete="false"
		  				ignoreCase="true"
		  				labelAttr="fullTitle"
		  				searchAttr="fullTitle"
		        	<%= htmlTemplate != null && InodeUtils.isSet(htmlTemplate.getIdentifier())?"value=\"" + htmlTemplate.getIdentifier() + "\"":""  %>
		            invalidMessage="<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
		        </select>
		        <script>
		        dojo.addOnLoad(function(){
		        		//alert(dijit.byId("template").getAttr("fetchProperties"));

		        })
		        </script>
			</dd>
				<%if(templateImgPreview!=null && InodeUtils.isSet(templateImgPreview.getInode())){%>
					<dd><img src="/thumbnail?id=<%=templateImgPreview.getIdentifier() %>&w=250&h=250" id="templateImage" border="0" style="border:1px solid #B6CBEB;"></dd>
				<%}%>
		</dl>
	</div>
<!-- /Basic Properties -->


<!-- Advanced Properties -->
	<div id="fileAdvancedTab" refreshOnShow="true" preload="true"  dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Advanced-Properties") %>" onShow="showEditButtonsRow()">
		<dl>
			<%if(identifier!=null && InodeUtils.isSet(identifier.getInode())){%>
				<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
				<dd><%= identifier.getInode() %></dd>
			<%}%>

			<dt><%= LanguageUtil.get(pageContext, "Show-on-Menu") %>:</dt>
			<dd><input type="checkbox" dojoType="dijit.form.CheckBox" name="showOnMenu" id="showOnMenu" onclick="disableOrder(this)" <%= htmlpage.isShowOnMenu() ? "checked" : "" %>/></dd>

			<dt><%= LanguageUtil.get(pageContext, "Menu-Sort-Order") %>:</dt>
			<dd><input type="text"  dojoType="dijit.form.NumberTextBox" name="sortOrder" id="sortOrder" constraints="{min:-5000,max:5000,places:0}" style="width: 70px;" value="<%= htmlpage.getSortOrder() %>" /></dd>

			<dt><%= LanguageUtil.get(pageContext, "Friendly-Name") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px;" name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(htmlpage.getFriendlyName()) ? htmlpage.getFriendlyName() : "" %>" /></dd>
			<dd class="inputCaption">(<%= LanguageUtil.get(pageContext, "can-be-used-for-SEO-friendly-page-title") %>)</dd>


		<div style="display:none">
			<dt><%= LanguageUtil.get(pageContext, "Start-Date") %>:</dt>
			<dd>
				<%
					Calendar startDateCal = new GregorianCalendar();
					startDateCal.setTime(startDate);
				%>
				<input type="text" dojoType="dijit.form.DateTextBox" validate='return false;' invalidMessage="" id="calendar_0" name="calendar_0" value="<%= startDateCal.get(Calendar.YEAR) + "-" + (startDateCal.get(Calendar.MONTH) < 9 ? "0" : "") + (startDateCal.get(Calendar.MONTH) + 1) + "-" + (startDateCal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + startDateCal.get(Calendar.DAY_OF_MONTH) %>" onchange="dateSelected('calendar_0');" />
				<input type="hidden" name="calendar_0_month" id="calendar_0_month" value="<%= startDateCal.get(Calendar.MONTH) %>" />
				<input type="hidden" name="calendar_0_day" id="calendar_0_day" value="<%= startDateCal.get(Calendar.DATE) %>" />
				<input type="hidden" name="calendar_0_year" id="calendar_0_year" value="<%= startDateCal.get(Calendar.YEAR) %>" />
			</dd>

			<dt><%= LanguageUtil.get(pageContext, "End-Date") %>:</dt>
			<dd>
				<%
					Calendar endDateCal = new GregorianCalendar();
					endDateCal.setTime(endDate);
				%>
				<input type="text" dojoType="dijit.form.DateTextBox" validate='return false;' invalidMessage="" id="calendar_1" name="calendar_1" value="<%= endDateCal.get(Calendar.YEAR) + "-" + (endDateCal.get(Calendar.MONTH) < 9 ? "0" : "") + (endDateCal.get(Calendar.MONTH) + 1) + "-" + (endDateCal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + endDateCal.get(Calendar.DAY_OF_MONTH) %>" onchange="dateSelected('calendar_1');" />
				<input type="hidden" name="calendar_1_month" id="calendar_1_month" value="<%= endDateCal.get(Calendar.MONTH) %>" />
				<input type="hidden" name="calendar_1_day" id="calendar_1_day" value="<%= endDateCal.get(Calendar.DATE) %>" />
				<input type="hidden" name="calendar_1_year" id="calendar_1_year" value="<%= endDateCal.get(Calendar.YEAR) %>" />
			</dd>
		</div>

			<dt><%= LanguageUtil.get(pageContext, "Cache-TTL") %>:</dt>
			<dd>
				<input type="text" onchange="showCacheTime()" dojoType="dijit.form.NumberTextBox" name="cacheTTL" constraints="{min:0,max:30758400,places:0}"  id="cacheTTL" value="<%= htmlpage.getCacheTTL() %>" style="width:100px" />   <span id="showCacheTime"></span></dd>




			<dt><%= LanguageUtil.get(pageContext, "Redirect") %>:</dt>
			<dd>
				<input type="text" dojoType="dijit.form.TextBox" style="width:275px;" id="redirect" name="redirect" value="<%= UtilMethods.isSet(htmlpage.getRedirect()) ? htmlpage.getRedirect() : "" %>" />
				<button dojoType="dijit.form.Button" onClick="browsePage();return false;" iconClass="linkIcon" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "select-link")) %>
				</button>
			</dd>

			<dt><%= LanguageUtil.get(pageContext, "HTTPS-Required") %>:</dt>
			<dd><input type="checkbox" dojoType="dijit.form.CheckBox" name="httpsRequired" <%= htmlpage.isHttpsRequired() ? "checked" : "" %> /></dd>

			<dt><%= LanguageUtil.get(pageContext, "SEO-Description") %>:</dt>
			<dd>
				<textarea dojoType="dijit.form.Textarea" wrap="off" style="width:340px; min-height:100px;; font-size: 11px"  cols="40" rows="10" name="seoDescription" ><%= UtilMethods.xmlEscape(UtilMethods.webifyString(htmlpage.getSeoDescription()))%></textarea>
			</dd>

			<dt><%= LanguageUtil.get(pageContext, "SEO-Keywords") %>:</dt>
			<dd>
				<textarea dojoType="dijit.form.Textarea" wrap="off" style="width:340px; min-height:100px;; font-size: 11px"  cols="40" rows="10" name="seoKeywords" ><%=UtilMethods.xmlEscape(UtilMethods.webifyString( htmlpage.getSeoKeywords()))%></textarea>
			</dd>

			<dt>
				<%= LanguageUtil.get(pageContext, "Page Metadata") %>:
			</dt>
			<dd>
				<textarea dojoType="dijit.form.Textarea" wrap="off" style="width:340px; min-height:100px;; font-size: 11px"  cols="40" rows="10" name="metadata" ><%= UtilMethods.xmlEscape(htmlpage.getMetadata())%></textarea>
			</dd>
		</dl>
 	</div>
<!-- /Advanced Properties -->

<!-- Permissions Tab -->
<%
	boolean canEditAsset = perAPI.doesUserHavePermission(htmlpage, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="filePermissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
		<% request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, htmlpage); %>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
	</div>
<%
	}
%>
<!-- /Permissions Tab  -->

<!-- Versions Tab -->
	<%if(htmlpage != null && InodeUtils.isSet(htmlpage.getInode())){ %>
		<div id="fileVersionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>" onShow="showEditButtonsRow()">
			<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp"%>
		</div>
	<%}%>
<!-- /Versions Tab -->

</div>
<!-- /TabContainer-->
<div class="clear"></div>
<!-- Button Row --->
<div class="buttonRow" id="editHtmlPageButtonRow">

	<%--check permissions to display the save and publish button or not--%>
	<% if (InodeUtils.isSet(htmlpage.getInode())) { %>
             <button dojoType="dijit.form.Button" onClick="previewHTMLPage()" target="_new" iconClass="previewIcon" type="button">
                 <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "preview")) %>
             </button>
	<% } %>

	<% if (!InodeUtils.isSet(htmlpage.getInode()) || htmlpage.isLive() || htmlpage.isWorking()) { %>
		<% if (canUserWriteToHTMLPage) { %>
			<% if (!InodeUtils.isSet(htmlpage.getInode()) || htmlpage.isLive() || htmlpage.isWorking()) { %>
				<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'')" iconClass="saveIcon" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
				</button>
			<% } else if (InodeUtils.isSet(htmlpage.getInode())) { %>
		<% } %>
	<% } %>

	<% if (canUserPublishHTMLPage) { %>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'publish')" iconClass="publishIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
		</button>
	<% } %>
	<% } else { %>
		<button dojoType="dijit.form.Button"  onClick="selectVersion(<%=htmlpage.getInode()%>, '<%=referer%>')" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this--version")) %>
		</button>
	<% } %>

	<% if (InodeUtils.isSet(htmlpage.getInode()) && htmlpage.isDeleted()) { %>
		<button dojoType="dijit.form.Button" onClick="submitfmDelete()" iconClass="deleteIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-page")) %>
		</button>
	<% } %>

	<button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon" type="button">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	</button>
</div>

<script language="Javascript"><!--
	//displays the image for this template
	dojo.addOnLoad(function() {
		disableOrder(document.getElementById("showOnMenu"));
	});

	<%
	if(!UtilMethods.isSet(htmlpage.getInode())){ %>
	dojo.addOnLoad(function() {
		 //dojo.byId("template").value = "<%= host.getHostname()%>";
		// dijit.byId("template")._startSearchFromInput();
	});
    <%}%>
--></script>

</liferay:box>
</html:form>

<script language="Javascript">
	//DOTCMS-5268
	dojo.require('dotcms.dijit.FileBrowserDialog');
	function browsePage() {
		pageSelector.show();
	}
	function pageSelected(page) {
		dojo.byId('redirect').value = page.pageURI;
	}
</script>

<div dojoAttachPoint="fileBrowser" jsId="pageSelector" onFileSelected="pageSelected" mimeTypes="application/dotpage"
	dojoType="dotcms.dijit.FileBrowserDialog">
</div>
