<%@ page import="com.dotmarketing.business.IdentifierCache" %>
<%@ page import="com.dotmarketing.cache.LiveCache" %>
<%@ page import="com.dotmarketing.cache.WorkingCache" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/portlet/ext/userclicks/init.jsp" %>
<%
	com.liferay.portal.model.User viewUser = (com.liferay.portal.model.User) request.getAttribute("viewUser");
	com.dotmarketing.beans.Clickstream clickstream = (com.dotmarketing.beans.Clickstream) request.getAttribute("clickstream");
	int pageNumber = 1;
	
	java.util.List clickstreamRequests = com.dotmarketing.factories.ClickstreamRequestFactory.getClickstreamRequestsByClickStream(clickstream);
		com.dotmarketing.beans.ClickstreamRequest initialRequest = (com.dotmarketing.beans.ClickstreamRequest) clickstreamRequests.get(0);
	
%>
<table border="0" cellpadding="2" cellspacing="1" width="100%" align="center">
	<tr>
		<td nowrap>
			<a href="<portlet:actionURL>
			<portlet:param name="struts_action" value="/admin/edit_user_profile" />
			<portlet:param name="p_u_e_a" value="<%=viewUser.getEmailAddress()%>" />
			</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Back-to") %> <%= viewUser.getFirstName()%><%= LanguageUtil.get(pageContext, "s-Profile") %></a>
		</td>
		<td nowrap>&nbsp;&gt;&nbsp;</td>
		<td nowrap>
			<a href="<portlet:renderURL><portlet:param name="struts_action" value="/ext/userclicks/view_user_clicks" /><portlet:param name="pageNumber" value="<%=String.valueOf(pageNumber)%>" /><portlet:param name="user_click_id" value="<%= viewUser.getUserId()%>" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "User-Clicks-for") %> <%= viewUser.getFullName()%></a>
		</td>
		<td nowrap>&nbsp;&gt;&nbsp;</td>
		<td nowrap>
			<%= LanguageUtil.get(pageContext, "Clickstream-Detail") %>
		</td>
		
		<td width="100%">&nbsp;</td>
		</tr>	
</table>
&nbsp;<BR>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "user-click-detail") %>' />

<table border="0" cellpadding="4" cellspacing="0" width="90%">
<tr>
	<td nowrap><B><%= LanguageUtil.get(pageContext, "Server") %></font></td>
	<td><%= initialRequest.getServerName() %></td>
</tr>


<tr>
	<td><B><%= LanguageUtil.get(pageContext, "Date") %></font></td>
	<td><%= com.dotmarketing.util.UtilMethods.dateToHTMLDate(clickstream.getStart()) %>	<%= com.dotmarketing.util.UtilMethods.dateToHTMLTime(clickstream.getStart()) %></td>
</tr>
<tr>
	<td><B><%= LanguageUtil.get(pageContext, "Time-Spent") %></font></td>
	<td><% long diffMillis = clickstream.getLastRequest().getTime() - clickstream.getStart().getTime();
		diffMillis = diffMillis/1000;
		if(diffMillis > 60){
			out.print((diffMillis/60) + "m " + diffMillis%60 + "s"); 
		}else{
			out.print((diffMillis + 1 ) +"s");
		}
		%></td>
</tr>
<tr>
	<td nowrap><B><%= LanguageUtil.get(pageContext, "Initial-Referer") %></font></td>
	<td><%= clickstream.getInitialReferrer() %></td>
</tr>
<tr>
	<td><B><%= LanguageUtil.get(pageContext, "Web-Browser") %></font></td>
	<td><%= clickstream.getUserAgent() %></td>
</tr>
<tr>
	<td><B><%= LanguageUtil.get(pageContext, "Remote-Address") %></font></td>
	<td><%= clickstream.getRemoteAddress() %></td>
</tr>
</table>
</liferay:box>

<p>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "user-clicks") %>' />

<table border="0" cellpadding="4" cellspacing="1" width="90%" align="center" bgcolor="#dddddd">
	<td>
		<b><%= LanguageUtil.get(pageContext, "Click-Number") %></b>
	</td>
	<td>
		<b><%= LanguageUtil.get(pageContext, "Page") %></b>
	</td>
	<td>
		<b><%= LanguageUtil.get(pageContext, "Language") %></b>
	</td>
	<td>
		<b><%= LanguageUtil.get(pageContext, "Time-Spent-on-Page") %></b>
	</td>
</tr>


	<%for(int i=0 ; i < clickstreamRequests.size(); i++){%>
		<%
		com.dotmarketing.beans.ClickstreamRequest clickstreamRequest = (com.dotmarketing.beans.ClickstreamRequest) clickstreamRequests.get(i);
		com.dotmarketing.beans.Host h = APILocator.getHostAPI().findByName(clickstreamRequest.getServerName(), APILocator.getUserAPI().getSystemUser(), false);
		
		
		%>

		<tr bgcolor="#ffffff">
			<td>
				<%=clickstreamRequest.getRequestOrder() %>
			</td>
			<td>
			<%
			String idInode = APILocator.getIdentifierAPI().find(h, clickstreamRequest.getRequestURI()).getInode();%>

			
								
					<a href="<portlet:renderURL>
						<portlet:param name="struts_action" value="/ext/htmlpageviews/view_htmlpage_views" />
						<portlet:param name="pageIdentifier" value="<%=String.valueOf(idInode)%>" />
						</portlet:renderURL>"><%= clickstreamRequest.getRequestURI() %></a>
			
			
			</td>
			<td>
			<%
			 	if(clickstreamRequest.getLanguageId() > 0){%>
			<%String langBrowser=APILocator.getLanguageAPI().getLanguage(clickstreamRequest.getLanguageId()).getLanguage(); %> 
				<%=LanguageUtil.get(pageContext,langBrowser)%> <%}%>
			</td>
			<td>
					<%
					if( i < clickstreamRequests.size() -1 ){
						com.dotmarketing.beans.ClickstreamRequest csr2 = (com.dotmarketing.beans.ClickstreamRequest) clickstreamRequests.get(i +1);%>
						<% long diffMillis = (csr2.getTimestamp().getTime() - clickstreamRequest.getTimestamp().getTime());%>
						
<%
		diffMillis = diffMillis/1000;
		if(diffMillis > 60){
			out.print((diffMillis/60) + "m " + diffMillis%60 + "s"); 
		}else{
			out.print((diffMillis + 1 ) +"s");
		}
		%>
						
					<%}else{%>
					exited
					<%}%>
			</td>
		</tr>
	<% } %>
</table>

</liferay:box>
<form id=fm2 name=fm2 method="post">
	<input type="hidden" name="p_u_e_a" value="">
</form>
<script language="javascript">
 function editUser(userId){
 	document.fm2.p_u_e_a.value=userId;
 	submitForm(document.fm2, '/c/admin/edit_user_profile');
 }
</script>

