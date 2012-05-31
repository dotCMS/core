<%@page import="org.apache.jasper.tagplugins.jstl.core.Redirect"%>

<%@page import="java.util.Date"%>
<%@ page import="com.dotmarketing.beans.Host" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotcms.publishing.timemachine.TimeMachineConfig" %>
<%@page import="com.dotcms.publishing.timemachine.TimeMachinePublisher" %>
<%@page import="com.dotcms.publishing.PublishStatus"%>
<%@page import="com.dotmarketing.business.TimeMachineSessionBean"%>
<%@page import="com.liferay.portal.model.User"%>

<%
Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
User user = APILocator.getUserAPI().getSystemUser();
%>

<html>
	<body>

<%
		if (request.getParameter("start") != null) {
			String hostID = (String) request.getParameter("host");
			Host host1 = WebAPILocator.getHostWebAPI().find(hostID, user, true);
			PublishStatus status = APILocator.getTimeMachineAPI().startTimeMachineForHostAndDate(host1, new Date());
%>
			<%=status.getStartProgress() %> : <%=status.getEndProgress() %>
			You are done!
<%		
		}
		String bundle = (String) request.getParameter("tmbundle");
		String disconnect = request.getParameter("tm-disconnect");
		if ( disconnect == null && bundle != null) {
			
			TimeMachineSessionBean config1 = new TimeMachineSessionBean();
			config1.setActive(true);
			config1.setDate(new Date(Long.parseLong(bundle)));
			config1.setHost(host);
			config1.setNotFoundGoOnMainSite(true);

			session.setAttribute(TimeMachineSessionBean.SESSION_KEY, config1);
%>
			<p>
			<a href="timemachine.jsp?tm-disconnect=true">close</a>
			</p>
		
			<iframe src="http://demo.michele.com:8080" width="100%" height="100%"></iframe>
<%
		}
		else {
			session.removeAttribute(TimeMachineSessionBean.SESSION_KEY);
%>
<h1>Welcome to the TimeMachine</h1>

<p>
Please select of the available dates

<% for (Host host_ : WebAPILocator.getHostWebAPI().findAll(user, true)) { %>
	<h2>Host: <%=host_.getHostname() %></h2>
	<ul>
	<% for (Date date : APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host_)) { %>
		<li><%= date %>: <a href="timemachine.jsp?tmbundle=<%= date.getTime()%>">Go</a></li>
	<% } %>
	</ul>
<% } %>
</p>

<p>
	Click here to start time machine for a given host
	<ul>
	<% for (Host host_ : WebAPILocator.getHostWebAPI().findAll(user, true)) { %>
		<li>Host: <%=host_.getHostname() %>: <a href="timemachine.jsp?start=true&host=<%=host_.getIdentifier()%>">start</a></li>
	<% } %>
	</ul>
</p>

<% 		
		} 
%>

	</body>
</html>
