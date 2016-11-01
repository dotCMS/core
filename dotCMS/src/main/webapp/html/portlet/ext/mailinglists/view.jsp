<%@ include file="/html/portlet/ext/mailinglists/init.jsp" %>

<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.portlets.mailinglists.model.MailingList"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.mailinglists.factories.MailingListFactory"%>

<table border="0" cellpadding="0" cellspacing="0" width="100%" height="90" class="listingTable">
	<tr class="header">
		<td>&nbsp;</td>
		<td><%=LanguageUtil.get(pageContext, "Title")%></td>
		<td align="center"><%=LanguageUtil.get(pageContext, "Subscribers")%></td>
		<td align="center"><%=LanguageUtil.get(pageContext, "Public-List")%></td>
	</tr>
	<% 
		List list = (List) request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_VIEW_PORTLET);	
		for (int k=0;k<Config.getIntProperty("MAX_ITEMS_MINIMIZED_VIEW") && k<list.size();k++) {
			String str_style = (k%2==0) ? "class=\"alternate_1\"" : "class=\"alternate_2\"";
			MailingList ml = (MailingList) list.get(k);
			int subscribersCount = MailingListFactory.getMailingListSubscribersCount(ml.getInode(), user.getUserId());
			
		%>
		<tr <%=str_style %> >
			<td width="25" class="icons">
				<a  href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" />
				<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
				<portlet:param name="inode" value="<%= String.valueOf(ml.getInode()) %>" />
				</portlet:actionURL>">
				<span class="editIcon"></span>
				</a>
			</td>
			<td>
   				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" />
				<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
				<portlet:param name="inode" value="<%= String.valueOf(ml.getInode()) %>" /></portlet:actionURL>">
				<%= ml.getTitle().equals("Do Not Send List")?LanguageUtil.get(pageContext, "message.mailinglists.do_not_send_list_title"):ml.getTitle() %></a> 
			</td>
			<td align="center"><%= subscribersCount %></td>
			<td align="center"><%= ml.isPublicList()==(true)?LanguageUtil.get(pageContext, "message.mailinglists.true"):LanguageUtil.get(pageContext, "message.mailinglists.false") %></td>
		</tr>
	<%}%>
	<tr><td height="100%" colspan="4">&nbsp;</td></tr>
</table>

<table align="right">
	<tr>
		<td nowrap>
   				<a class="portletOption" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>">
				   <%=LanguageUtil.get(pageContext, "all1")%></a>
			| 
				<a class="portletOption" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /></portlet:actionURL>"><%=LanguageUtil.get(pageContext, "new1")%></a>&nbsp;&nbsp;
			
		</td>
	</tr>
</table>
