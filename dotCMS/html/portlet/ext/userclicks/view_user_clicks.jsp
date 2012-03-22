<%@ include file="/html/portlet/ext/userclicks/init.jsp" %>

<%@ page import="com.dotmarketing.util.Config" %>
<%

	int numrows = 0;
	if (request.getAttribute("numrows")!=null) {
		numrows = ((Integer) request.getAttribute("numrows")).intValue();
	}
	
	int pageNumber = 1;

	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	com.liferay.portal.model.User viewUser = (com.liferay.portal.model.User) request.getAttribute("viewUser");

%>
<style type="text/css">
<!--
#pup {position:absolute; visibility:hidden; z-index:200; width:130; }
//-->
</style>
<%
%>

<form id="fm" method="post">


		
<script language="Javascript">

	var myForm = document.getElementById('fm');

	function submitfm(form) {

			form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/userclicks/view_user_clicks" /></portlet:renderURL>';
			submitForm(form);

	}
</script>
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
			<%= LanguageUtil.get(pageContext, "User-Clicks-for") %> <%= viewUser.getFullName()%>
		</td>
		<td width="100%">&nbsp;</td>
		</tr>	
</table>
&nbsp;<BR>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-htmlpage-views-all") %>' />

		<table border="0" cellpadding="4" cellspacing="1" width="90%" align="center" bgcolor="#dddddd">	
		<tr>
			<td><b><%= LanguageUtil.get(pageContext, "Visit") %></font></td>
			<td><b><%= LanguageUtil.get(pageContext, "Date") %></font></td>
			<td><b><%= LanguageUtil.get(pageContext, "Time-Spent") %></font></td>
			<td><b><%= LanguageUtil.get(pageContext, "Clicks") %></font></td>
		</tr>
		<tr>
		
		
<%
		java.util.List clickstreams = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.USER_CLICKS_VIEW);
		if(clickstreams != null){
		java.util.Iterator clickIter = clickstreams.iterator();
		int visit=0;
		while(clickIter.hasNext()){
			com.dotmarketing.beans.Clickstream clickstream = (com.dotmarketing.beans.Clickstream) clickIter.next(); 
%>
	<tr bgcolor="#ffffff">
		<td><%=clickstreams.size() - visit++%></td>
		<td><a href="<portlet:renderURL><portlet:param name="struts_action" value="/ext/userclicks/view_user_clicks" /><portlet:param name="clickstreamId" value="<%= String.valueOf(clickstream.getClickstreamId())%>" /><portlet:param name="user_click_id" value="<%= viewUser.getUserId()%>" /></portlet:renderURL>"><%= com.dotmarketing.util.UtilMethods.dateToHTMLDate(clickstream.getStart()) %>	<%= com.dotmarketing.util.UtilMethods.dateToHTMLTime(clickstream.getStart()) %>		</a></td>
		<td><% long diffMillis = clickstream.getLastRequest().getTime() - clickstream.getStart().getTime();
		diffMillis = diffMillis/1000;
		if(diffMillis > 60){
			out.print((diffMillis/60) + "m " + diffMillis%60 + "s"); 
		}else{
			out.print((diffMillis + 1 ) +"s");
		}
		%></td>
		<td><%= clickstream.getClickstreamRequests().size() %>
		<%--<td><div><% if(clickstream.getInitialReferrer()==null || clickstream.getInitialReferrer().length() < 8){%>Bookmarked or Typed<% }else{ %><a href="<%= clickstream.getInitialReferrer() %>" target="_blank"><% String strReferer =  clickstream.getInitialReferrer().replaceAll("http://","").replaceAll("https://",""); if(strReferer.length() > 20){ out.print(strReferer.substring(0,19) + "..."); }else{out.println(strReferer); }%></a><% } %></div></td>--%>
	</tr>
<% 
		} 
	}
%>
				<% if (minIndex != 0 ||maxIndex < numrows) { %>
					<tr bgcolor="#ffffff">
						<td colspan="2" align=left>
						<% if (minIndex != 0) { %>
							<font class="gamma" size="2">
							<B><button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL><portlet:param name="struts_action" value="/ext/userclicks/view_user_clicks" /><portlet:param name="pageNumber" value="<%=String.valueOf(pageNumber-1)%>" /><portlet:param name="user_click_id" value="<%= viewUser.getUserId()%>" /></portlet:renderURL>'" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %></button></b>
							</font>
						<% } %>
						</td>
						<td colspan="2" align=right>
						
						<% if (maxIndex < numrows) { %>
							<font class="gamma" size="2">
							<B><button dojoType="dijit.form.Button" class="bg" onClick="window.location='<portlet:renderURL><portlet:param name="struts_action" value="/ext/userclicks/view_user_clicks" /><portlet:param name="pageNumber" value="<%=String.valueOf(pageNumber+1)%>" /><portlet:param name="user_click_id" value="<%= viewUser.getUserId()%>" /></portlet:renderURL>'" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "Next") %></button></b>
							</font>
						<% } %>
						</td>
					</tr>
				<% } %>
				<% if (numrows ==0) { %>
					<tr bgcolor="#ffffff">
						<td colspan="4" align=center>
						<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "This-user-has-not-logged-on-to-the-site") %></font>
						</td>
					</tr>
				<% } %>
			</table>		


</liferay:box>

</form>
				<form id=fm2 name=fm2 method="post">
					<input type="hidden" name="p_u_e_a" value="">
				</form>




