<%@page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="com.dotmarketing.beans.Clickstream"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.userclicks.factories.UserClickFactory"%>
<%@page import="com.dotmarketing.business.web.UserWebAPI"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.liferay.portal.model.User"%>

<% 
	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
	User currentUser = userWebAPI.getLoggedInUser(request);
	
	String userId = request.getParameter("userId");
	UserAPI userAPI = APILocator.getUserAPI();
	User user = userAPI.loadUserById(userId, currentUser, false);

	List listClicks = UserClickFactory.getAllUserClicks(userId);

	String initialReferer = "unknown";
	String initialIP = "";
	if(listClicks.size() > 0){
		Clickstream clickstream = (Clickstream) listClicks.get(listClicks.size() -1);
		initialIP = clickstream.getRemoteAddress();
		initialReferer = clickstream.getInitialReferrer()==null?"unknown":clickstream.getInitialReferrer();
	} 
	if(listClicks.size() ==0) { 
%>


<b><%= LanguageUtil.get(pageContext, "User-has-no-visitor-data-available") %></b>
<%
	} else { 
%>
<table class="listingTable">
    <tr class="alternate_1">
        <td nowrap width="90">
            <%= LanguageUtil.get(pageContext, "Initial-Referer") %>:
        </td>
        <% if("unknown".equals(initialReferer)){ %>
        <td>
            <%= initialReferer %>
        </td>
        <% }else{ %>
        <td>
            <a href="<%= initialReferer%>" target="_blank"><%= UtilMethods.truncatify(initialReferer, 50) %></a>
        </td>
        <% } %>
    </tr>
    <tr class="alternate_2">
        <td nowrap width="90">
            <%= LanguageUtil.get(pageContext, "Initial-IP") %>:
        </td>
        <td>
            <a href="http://www.ip2location.com/free.asp?ipaddresses=<%= initialIP%>" target="_blank"><%= initialIP %></a>
        </td>
    </tr>
</table>
<br/>
<fieldset>
	<legend><%= LanguageUtil.get(pageContext, "ClickTrails") %></legend>
	<table class="listingTable">
	    <tr>
	        <th>
	            <font class="bg">
	                <b><%= LanguageUtil.get(pageContext, "Date") %></b>
	            </font>
	        </th>
	        <th>
	            <font class="bg">
	                <b><%= LanguageUtil.get(pageContext, "Time-Spent") %></b>
	            </font>
	        </th>
	        <th>
	            <font class="bg">
	                <b><%= LanguageUtil.get(pageContext, "Clicks") %></b>
	            </font>
	        </th>
	    </tr>
	    <%
	    	if(listClicks == null || listClicks.size() < 1) { 
		%>
	    <tr class="alternate_1">
	        <td colspan=4>
	            <%= LanguageUtil.get(pageContext, "This-user-has-not-logged-in-to-the-site") %>
	        </td>
	    </tr>
	    <%	
			} else {
	    		Iterator clickIter = listClicks.iterator();
	    		int i = 0;
	    		while(clickIter.hasNext()) {
	    			Clickstream clickstream = (Clickstream) clickIter.next(); 
	    	    	String className = i % 2 == 0?"alternate_2":"alternate_1";
		%>
	    <tr class="<%=className%>">
	        <td>
	            <a href="javascript: viewClickstreamDetails('<%= clickstream.getClickstreamId() %>', '<%= currentUser.getUserId() %>')"><%= com.dotmarketing.util.UtilMethods.dateToHTMLDate(clickstream.getStart()) %><%= com.dotmarketing.util.UtilMethods.dateToHTMLTime(clickstream.getStart()) %></a>
	        </td>
	        <td>
	            <% 
					long diffMillis = clickstream.getLastRequest().getTime() - clickstream.getStart().getTime();
	            	diffMillis = diffMillis / 1000;
	            	if(diffMillis > 60){
	            		out.print((diffMillis/60) +" "+ UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "minutes")));
	            	} else{
	            		out.print(diffMillis +" "+UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "seconds")));
	            	} 
				%>
	        </td>
	        <td>
	    	    <%= clickstream.getClickstreamRequests().size() %>
		    </td>
	    </tr>
	    <% 
	    			i++;
				}
	    	} 
		%>
	</table>
</fieldset>
<%
	} 
%>