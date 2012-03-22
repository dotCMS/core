<%@ include file="/html/portlet/ext/mailinglists/init.jsp" %>

<%
	
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = 20;
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/mailinglists/view_mailinglists"});
	
	String query = (request.getParameter("query")!=null) ? request.getParameter("query") : "";
	String direction = (request.getParameter("direction")!=null) ? request.getParameter("direction") : " desc";

	
	String orderby = request.getParameter("orderby");
	if (!com.dotmarketing.util.UtilMethods.isSet(orderby)) orderby = "title";
		
%>

<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.mailinglists.model.MailingList"%>
<%@page import="com.dotmarketing.portlets.mailinglists.factories.MailingListFactory"%>

<%@page import="com.dotmarketing.portlets.userfilter.model.UserFilter"%>
<%@page import="com.dotmarketing.portlets.userfilter.factories.UserFilterFactory"%>
<%@page import="com.dotmarketing.portlets.campaigns.model.Campaign"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<script language="JavaScript">

	function resetSearch() {
		form = document.getElementById('fm');
		form.query.value = '';
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>';
		submitForm(form);
	}

	function submitSearch() {
		form = document.getElementById('fm');
		form.pageNumber.value = 1;
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>';
		submitForm(form);
	}

	function checkUncheckAll() {
		var checkAllValue = dijit.byId('checkAll').attr('value');
		var cbMailingIdList = document.getElementsByName('mailing_list');
		
		if (checkAllValue) {
			for (var i = 0; i < cbMailingIdList.length; ++i) {
				dijit.byId(cbMailingIdList[i].id).attr('value', true);
			}
		} else {
			for (var i = 0; i < cbMailingIdList.length; ++i) {
				dijit.byId(cbMailingIdList[i].id).attr('value', false);
			}
		}
    }

	function getSelectedListValues() {
   		var result = [];
		var cbMailingIdList = document.getElementsByName('mailing_list');

		for (var i = 0; i < cbMailingIdList.length; ++i) {
			if (dijit.byId(cbMailingIdList[i].id).attr('value')) {
				result.push(cbMailingIdList[i].value);
			}
		}
				
   		return result;
    }

	function deleteSelectedLists() {
	    if(confirm('<%=LanguageUtil.get(pageContext, "Are-you-sure-you-would-like-to-delete-these-mailing-lists")%>')) {
		    var mailingListIds = getSelectedListValues();
	        var mailingLists = "&mailinglists=" + mailingListIds;
	        
			form = document.getElementById('fm');
			form.<portlet:namespace />cmd.value = '<%=com.dotmarketing.util.Constants.DELETE_LIST%>';
			form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /><portlet:param name="orderby" value="<%=orderby%>" /></portlet:renderURL>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /></portlet:actionURL>' + mailingLists;
			submitForm(form);
	    }
	}
	
	function createNewList(){
		var href = "<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/usermanager/view_usermanagerlist' />";
		href = href + "<portlet:param name='cmd' value='<%=com.liferay.portal.util.Constants.SEARCH%>' />";
		href = href + "<portlet:param name='page' value='1' />"
		href = href + "</portlet:renderURL>";
		document.location.href = href;
	}
	
	function uploadNewList(){
		var href = "<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/usermanager/view_usermanagerlist' />";
		href = href + "<portlet:param name='cmd' value='load' />";
		href = href + "<portlet:param name='page' value='1' />";
		href = href + "</portlet:renderURL>";
		document.location.href = href;
	}


</script>
<%--liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%=LanguageUtil.get(pageContext, "Viewing-Mailing-Lists")%>' /--%>
	
<form id="fm" method="post" action="" onsubmit="return false;">
<div class="yui-gc portlet-toolbar">
	<div class="yui-u first">
		<span style="vertical-align:middle;"><%=LanguageUtil.get(pageContext, "Mailing-List-Title")%>:</span>
		<span style="vertical-align:middle;"><input type="text" dojoType="dijit.form.TextBox" name="query" id="query" value="<%=com.dotmarketing.util.UtilMethods.isSet(query) ? query : ""%>"></span>
		<span style="vertical-align:middle;">
			<button dojoType="dijit.form.Button" onClick="submitSearch()" iconClass="searchIcon">
				<%=LanguageUtil.get(pageContext, "Search")%>
			</button>
		</span>
	</div>
<%
	boolean isCMSAdmin = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
	boolean isMailingListAdmin = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.isMailingListAdmin(user);
	boolean isMailingListEditor = com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.isMailingListEditor(user);
	
	if (isCMSAdmin || isMailingListAdmin || isMailingListEditor) {
%>
	<div class="yui-u" style="text-align:right;">
		<button dojoType="dijit.form.ComboButton" id="comboCreate" optionsTitle="" iconClass="plusIcon" onClick="createNewList()"> 
			<span><%=LanguageUtil.get(pageContext, "Create-List")%></span> 
			<div dojoType="dijit.Menu" id="createMenu" style="display: none;"> 
				<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="createNewList()"><%=LanguageUtil.get(pageContext, "Add-New-Mailing-List")%></div> 
				<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="uploadNewList()"><%=LanguageUtil.get(pageContext, "Load-Users")%></div> 
			</div> 
		</button>
	</div>
<%
	}
%>
</div>


<input type="hidden" name="<portlet:namespace />cmd" value="">
<input type="hidden" name="<portlet:namespace />redirect" value="">
<input type="hidden" name="inode" value="">
<input type="hidden" name="pageNumber" value="<%=pageNumber%>">

<!-- START Table Results -->	
<table class="listingTable" >
	<tr>
		<th>
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="checkAll" id="checkAll" onclick="checkUncheckAll()">
		</th>
		<th>
			<a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
				<portlet:param name='struts_action' value='/ext/mailinglists/view_mailinglists' />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query : "") %>' />
				<portlet:param name='orderby' value='title' />
				<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
				</portlet:renderURL>"><%=LanguageUtil.get(pageContext, "Title")%>
			</a>
		</th>
		<th>
			<!--a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
				<portlet:param name='struts_action' value='/ext/mailinglists/view_mailinglists' />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query : "") %>' />
				<portlet:param name='orderby' value='subscriber_count' />
				<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
				</portlet:renderURL>"--><%=LanguageUtil.get(pageContext, "Subscribers")%>
			<!--/a-->
		</th>
		<th>
			<!--a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
				<portlet:param name='struts_action' value='/ext/mailinglists/view_mailinglists' />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query : "") %>' />
				<portlet:param name='orderby' value='subscriber_count' />
				<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
				</portlet:renderURL>"--><%=LanguageUtil.get(pageContext, "Last-Campaign")%>
			<!--/a-->
		</th>
		<th>
			<!--a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>
				<portlet:param name='struts_action' value='/ext/mailinglists/view_mailinglists' />
				<portlet:param name="query" value='<%= (com.dotmarketing.util.UtilMethods.isSet(query)? query : "") %>' />
				<portlet:param name='orderby' value='public_list' />
				<portlet:param name='direction' value='<%=(direction.equals(" asc")?" desc": " asc")%>' />
				</portlet:renderURL>"--><%=LanguageUtil.get(pageContext, "List-Type")%>
			<!--/a-->
		</th>
	</tr>
	<% 
		List lists = (List) request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_VIEW);
		MailingList ml;
		UserFilter uf;
		String str_style;
		int subscribersCount;
		List<Campaign> campaigns;
		
		for (int k=minIndex;k<maxIndex && k<lists.size();k++) {
			if (lists.get(k) instanceof MailingList) {
				ml = (MailingList) lists.get(k);
				uf = null;
				str_style =  ((k%2)==0)  ? "class=\"alternate_1\"" : "class=\"alternate_2\"";
				subscribersCount = MailingListFactory.getMailingListSubscribersCount(ml.getInode(), user.getUserId());
				campaigns = InodeFactory.getParentsOfClass(ml, Campaign.class, "campaign.start_date desc");
			} else {
				ml = null;
				uf = (UserFilter) lists.get(k);
				str_style =  ((k%2)==0)  ? "class=\"alternate_1\"" : "class=\"alternate_2\"";
				subscribersCount = UserFilterFactory.getUserProxiesFromFilter(uf).size();
				campaigns = InodeFactory.getParentsOfClass(uf, Campaign.class, "campaign.start_date desc");
			}
	%>
	<tr <%= str_style %>>
		<%--td width="30" nowrap align="center">
			<a  href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /><portlet:param name="inode" value="<%= String.valueOf(ml.getInode()) %>" /></portlet:actionURL>">
			<span class="targetIcon"></span>
		</td--%>
		<td width="30">
<%
		if (ml != null) {
			if ((ml.getTitle() != null) && !ml.getTitle().equals("Do Not Send List")) {
%>
			<input type="checkbox" dojoType="dijit.form.CheckBox" value="<%=String.valueOf(ml.getInode())%>" id="ml_<%=String.valueOf(ml.getInode())%>" name="mailing_list"/>
<%
			}
		} else {
%>
			<input type="checkbox" dojoType="dijit.form.CheckBox" value="<%=String.valueOf(uf.getInode())%>" id="ml_<%=String.valueOf(uf.getInode())%>" name="mailing_list"/>
<%
		}
%>
		</td>
		<td>
<%
		if (ml != null) {
%>
			<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /><portlet:param name="inode" value="<%= String.valueOf(ml.getInode()) %>" /></portlet:actionURL>">
				<%= ((ml.getTitle() != null) && ml.getTitle().equals("Do Not Send List")) ? LanguageUtil.get(pageContext, "message.mailinglists.do_not_send_list_title") : ml.getTitle() %>
			</a>
<%
		} else {
%>
			<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /><portlet:param name="inode" value="<%= String.valueOf(uf.getInode()) %>" /></portlet:actionURL>">
				<%= uf.getUserFilterTitle() %>
			</a>
<%
		}
%> 
		</td>
		<td>
			<%= subscribersCount %>
		</td>
		<td>
<%
		if (ml != null) {
			if ((campaigns != null) && (0 < campaigns.size())) {
%>
			<%= UtilMethods.dateToHTMLDate(campaigns.get(0).getCStartDate()) %>
<%
			} else {
%>
			-
<%
			}
		} else {
			if ((campaigns != null) && (0 < campaigns.size())) {
%>
			<%= UtilMethods.dateToHTMLDate(campaigns.get(0).getCStartDate()) %>
<%
			} else {
%>
			-
<%
			}
		}
%>
		</td>
		<td>
			<font class="gamma" size="2">
<%
		if (ml != null) {
%>
				<%= ml.isPublicList()==(true)?LanguageUtil.get(pageContext, "message.mailinglists.public.subscription"):LanguageUtil.get(pageContext, "message.mailinglists.private.subscription") %>
<%
		} else {
%>
				<%= LanguageUtil.get(pageContext, "Dynamic") %>
<%
		}
%>
			</font>
		</td>
	</tr>
	<%}%>
</table>
<!-- END Table Results -->
<div class="clear"></div>
<!-- START Pagination -->
<div class="yui-g buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<% if (minIndex != 0) { %>
				<button dojoType="dijit.form.Button" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /></portlet:renderURL>'" iconClass="previousIcon"><%=LanguageUtil.get(pageContext, "Previous")%></button>
		<% } %>
	</div>
	
	<div class="yui-u first" style="text-align:right;">
		<% if (maxIndex < lists.size()) { %>
			<button dojoType="dijit.form.Button" onClick="window.location='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /></portlet:renderURL>'" iconClass="nextIcon"><%=LanguageUtil.get(pageContext, "Next")%></button>
		<% } %>
	</div>
</div>
<!-- END Pagination -->
<div class="clear"></div>
<!-- START Button Row -->
<%
	if (isCMSAdmin || isMailingListAdmin) {
%>
<div class="buttonRow">
	<button dojoType="dijit.form.Button" onClick="deleteSelectedLists()" iconClass="deleteIcon">
		<%=LanguageUtil.get(pageContext, "Delete-Selected")%>
	</button>
</div>
<%
	}
%>
<!-- END Button Row -->

</form>
<%--/liferay:box--%>