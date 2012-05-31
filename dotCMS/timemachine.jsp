<%@page import="org.apache.jasper.tagplugins.jstl.core.Redirect"%>

<%@page import="java.util.Date"%>
<%@ page import="com.dotmarketing.beans.Host" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotcms.publishing.timemachine.TimeMachineConfig" %>
<%@page import="com.dotcms.publishing.timemachine.TimeMachinePublisher" %>
<%@page import="com.dotcms.publishing.PublishStatus"%>

<html>
	<body>

<%
		if (request.getParameter("trigger") != null) {
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			PublishStatus status = APILocator.getTimeMachineAPI().startTimeMachineForHostAndDate(host, new Date());
%>
			<%=status.getStartProgress() %> : <%=status.getEndProgress() %>

			You are done!
<%		
		}
		String bundle = (String) request.getParameter("tmbundle");
		String disconnect = request.getParameter("tm-disconnect");
		if ( disconnect == null && bundle != null) {
			session.setAttribute("tmbundle", bundle);
%>
			<p>
			<a href="timemachine.jsp?tm-disconnect=true">close</a>
			</p>
		
			<iframe src="http://demo.michele.com:8080" width="100%" height="100%"></iframe>
<%
		}
		else {
			session.removeAttribute("tmbundle");
%>
<h1>Welcome to the TimeMachine</h1>

<p>
Please select of the available dates
<ul>
<% Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request); %>
<% for (Date date : APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host)) { %>
<li><%= date %>: <a href="timemachine.jsp?tmbundle=<%= date.getTime()%>">Go</a></li>
<% } %>
</ul>
</p>

<p>
	Click here to save time machine
	<a href="timemachine.jsp?trigger=true">Start</a>
</p>

<% 		
		} 
%>

	</body>
</html>
