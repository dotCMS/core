<%@page import="com.dotmarketing.portlets.mailinglists.factories.MailingListFactory"%>
<%@page import="com.dotmarketing.portlets.mailinglists.model.MailingList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.beans.UserProxy"%>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.util.InodeUtils" %>

<%@ include file="/html/portlet/ext/mailinglists/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.Role"%>

<%

	MailingList ml;

	if (request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_EDIT)!=null) {
		ml = (MailingList) request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_EDIT);
	}
	else {
		ml = new MailingList ();
	}

	List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
	Iterator<Role> rolesIt = roles.iterator();
	boolean isMarketingAdmin = false;
	while (rolesIt.hasNext()) {
	    Role role = (Role) rolesIt.next();
	    if (role.getName().equals(Config.getStringProperty("MAILINGLISTS_ADMIN_ROLE")) || role.getName().equals(Config.getStringProperty("MAILINGLISTS_EDITOR_ROLE"))) {
	        isMarketingAdmin = true;
	        break;
	    }
	}

	boolean mlEditable = true;
	if (InodeUtils.isSet(ml.getInode())
			&& (!ml.getUserId().equals(user.getUserId()) &&
					!isMarketingAdmin)
			&& (!ml.getInode().equalsIgnoreCase(MailingListFactory.getUnsubscribersMailingList().getInode()))) {
		mlEditable = false;
	}

	int perPage = Config.getIntProperty("PER_PAGE");
	

%>


<%@ include file="/html/portlet/ext/mailinglists/edit_mailinglist_js_inc.jsp" %>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<!--liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Mailing-List")) %>' /-->
	
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
	
<!-- START Subscriber Tab -->
	<div id="listInfoTab" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "Subscribers")%>">

		<html:form action="/ext/mailinglists/edit_mailinglist" styleId="fm" enctype="multipart/form-data">
		<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
		<input name="<portlet:namespace />redirect" type="hidden" value="">
		<input name="su" type="hidden" value="add">
		<input type="hidden" name="inode" value="<%=ml.getInode()%>">
		<input type="hidden" name="mailinglists" value="<%=ml.getInode()%>">
		<html:hidden property="subscriberCount" />

			<div class="nameHeader">
				<span style="vertical-align:middle;font-weight:bold;padding-left:10px;"><%=LanguageUtil.get(pageContext, "Title")%>:</span>
				<input type="text" dojoType="dijit.form.TextBox" id="title" name="title" size="30" value="<%= ml.getTitle() %>" />
				&nbsp;&nbsp;
				<input type="checkbox" dojoType="dijit.form.CheckBox" id="publicList" name="publicList" <%= ml.isPublicList() ? "checked" : "" %> />
				<span style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "Allow-Public-to-Subscribe")%>:</span>
			</div>

		</html:form>
	

		<%
		if(InodeUtils.isSet(ml.getInode())) {
			int subscribersCount = MailingListFactory.getMailingListSubscribersCount(ml.getInode(), user.getUserId());
			int unsubscribersCount = MailingListFactory.getMailingListUnsubscribersCount(ml.getInode(), user.getUserId());
			int bouncesCount = MailingListFactory.getMailingListBouncesCount(ml.getInode(), user.getUserId());	
		%>

		<div class="yui-g portlet-toolbar">
			<a name="subscribers"></a>
			<div class="yui-u first">
				<%=LanguageUtil.get(pageContext, "Subscribers")%> (<span id="subscribersCount"></span>)
			</div>
<%
	if (mlEditable) {
%>
			<div class="yui-u" style="text-align:right;">
				<button dojoType="dijit.form.Button" onClick="deleteSubscribers()" iconClass="deleteIcon">
					<%=LanguageUtil.get(pageContext, "delete")%>
				</button>
				<button dojoType="dijit.form.ComboButton" id="comboCreate" optionsTitle="" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="1" /></portlet:renderURL>'" iconClass="plusIcon" title=""> 
					<span><%=LanguageUtil.get(pageContext, "Add-to-List")%></span> 
					<div dojoType="dijit.Menu" id="createMenu" style="display: none;">  
					<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="load" /><portlet:param name="page" value="1" /></portlet:renderURL>'"><%=LanguageUtil.get(pageContext, "Load-Users")%></div> 
				</div> 
			</button>
			</div>
<%
	}
%>
		</div>

		<div class="clear"></div>
		<form action="<%=CTX_PATH%>/ext/mailinglists/edit_mailinglist" id="subscribersForm" name="subscribersForm" method="post">
			<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="deleteSubs">
			<input name="<portlet:namespace />redirect" type="hidden" value="">
			<input type="hidden" name="inode" value="<%=ml.getInode()%>">
			<div id="subscribersMessage" align="center"></div>
			<table class="listingTable">
				<tr>
					<th align="center" width="1%" nowrap="nowrap" id="subscribersControls">
					<% if (mlEditable && (subscribersCount > 0)) { %>
						<input type="checkbox" dojoType="dijit.form.CheckBox" id="subscribersCheckAll" name="subscribersCheckAll" onclick="checkAllSubscribers()" />
					<% } %>
					</th>
					<th width="1%" nowrap="nowrap" align="center"><%=LanguageUtil.get(pageContext, "edit")%></th>
					<th><a href="#subscribers" onclick="javascript: sortSubscribersBy('firstname');"><%=LanguageUtil.get(pageContext, "First-Name")%></a></th>
					<th><a href="#subscribers" onclick="javascript: sortSubscribersBy('lastname');"><%=LanguageUtil.get(pageContext, "Last-Name")%></a></th>
					<th><a href="#subscribers" onclick="javascript: sortSubscribersBy('emailaddress');"><%=LanguageUtil.get(pageContext, "Email")%></a></th>
					<th width="1%" nowrap="nowrap"><%=LanguageUtil.get(pageContext, "Last-Result")%></th>
				</tr>
				<tbody id="subscribersTable"></tbody>
			</table>
		</form>
		<div class="clear"></div>
		<div class="yui-gb buttonRow" id="subscribersPagination"></div>

	</div>
<!-- END Subscriber Tab -->



<!-- START Unsbscriber Tab -->
	<div id="unsubTab" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "Unsubscriptions")%>">
		<form id="unsubscribersForm" name="unsubscribersForm">
			<div id="unsubscribersMessage" align="center"></div>
			<div class="yui-g portlet-toolbar">
				<div class="yui-u first">
					<%=LanguageUtil.get(pageContext, "Unsubscriptions")%> (<span id="unsubscribersCount"></span>)
				</div>
			</div>
			<table class="listingTable">
				<tr>
					<th align="center" width="1%" nowrap="nowrap" id="unsubscribersControls">
					<% if (mlEditable && (unsubscribersCount > 0)) { %>
						<input type="checkbox" dojoType="dijit.form.CheckBox" id="unsubscribersCheckAll" name="unsubscribersCheckAll" onclick="checkAllUnsubscribers()" />
					<% } %>
					</th>
					<th width="1%" nowrap="nowrap" align="center"><%=LanguageUtil.get(pageContext, "edit")%></th>
					<th><a href="#unsubscribers" onclick="javascript: sortUnsubscribersBy('firstname');"><%=LanguageUtil.get(pageContext, "First-Name")%></a></th>
					<th><a href="#unsubscribers" onclick="javascript: sortUnsubscribersBy('lastname');"><%=LanguageUtil.get(pageContext, "Last-Name")%></a></th>
					<th><a href="#unsubscribers" onclick="javascript: sortUnsubscribersBy('emailaddress');"><%=LanguageUtil.get(pageContext, "Email")%></a></th>
					<th width="1%" nowrap="nowrap"><%=LanguageUtil.get(pageContext, "Last-Result")%></th>
				</tr>
				<tbody id="unsubscribersTable"></tbody>
			</table>
		</form>
		<div class="clear"></div>
		<div class="yui-gb buttonRow" id="unsubscribersPagination"></div>
		<div class="clear"></div>
<%
	if (mlEditable) {
%>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" onClick="deleteUnsubscribers()" iconClass="deleteIcon">
				<%=LanguageUtil.get(pageContext, "delete")%>
			</button>
		</div>
<%
	}
%>
	</div>
<!--  END unsubscribers Tab -->
	


<!--  START bounces Tab -->
	
		<div id="bounceTab" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "Bounces")%>">			
			<div class="yui-g portlet-toolbar">
				<div class="yui-u first">
					<%=LanguageUtil.get(pageContext, "Bounces/Errors")%> (<span id="bouncesCount"></span>)
				</div>
			</div>			
			<form id="bouncesForm" name="bouncesForm">
				<div id="bouncesMessage" align="center"></div>
				<table class="listingTable">
					<tr>
						<th align="center" width="1%" nowrap="nowrap" id="bouncesControls">
						<% if (mlEditable && (bouncesCount > 0)) { %>
							<input type="checkbox" dojoType="dijit.form.CheckBox" id="bouncesCheckAll" name="bouncesCheckAll" onclick="checkAllBounces()" />
						<% } %>
						</th>
						<th width="1%" nowrap="nowrap" align="center"><%=LanguageUtil.get(pageContext, "edit")%></th>
						<th><a href="#bounces" onclick="javascript: sortBouncesBy('firstname');"><%=LanguageUtil.get(pageContext, "First-Name")%></a></th>
						<th><a href="#bounces" onclick="javascript: sortBouncesBy('lastname');"><%=LanguageUtil.get(pageContext, "Last-Name")%></a></th>
						<th><a href="#bounces" onclick="javascript: sortBouncesBy('emailaddress');"><%=LanguageUtil.get(pageContext, "Email")%></a></th>
						<th width="1%" nowrap="nowrap"><%=LanguageUtil.get(pageContext, "Last-Result")%></th>
					</tr>
					<tbody id="bouncesTable"></tbody>
				</table>
			</form>
			<div class="clear"></div>
			<div class="yui-gb buttonRow" id="bouncesPagination"></div>
			<div class="clear"></div>
<%
	if (mlEditable) {
%>
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" onClick="deleteBounces()" iconClass="deleteIcon">
					<%=LanguageUtil.get(pageContext, "delete")%>
				</button>
			</div>
<%
	}
%>
		</div>
	
<!--  END bounces list -->
	
</div>
<div class="clear"></div>
<div class="buttonRow">
	<% if (mlEditable) { %>
		<button dojoType="dijit.form.Button" onClick="deleteList(document.getElementById('fm'))" iconClass="deleteIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-list")) %>
		</button>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="saveIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
		</button>
	<% } %>
	<button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	</button>
</div>

<script type="text/javascript">
	onload = loadLists;
</script>
	
<% } %>

</liferay:box>

