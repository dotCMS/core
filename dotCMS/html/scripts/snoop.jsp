<%@ include file="/html/scripts/auth.jsp" %><%@ page import="javax.servlet.http.HttpUtils,java.util.Enumeration" %>
<HTML>
<HEAD>
	<TITLE>JSP snoop page</TITLE>
	
</HEAD>
<BODY>

<H1>JSP Snoop page</H1>

<H2>Context information</H2>
<TABLE border=1>
<TR>
	<TH align=right>Server Info:</TH>
	<TD><%= application.getServerInfo() %></TD>
</TR>
</TABLE>

<H2>Request information</H2>

<TABLE border=1>
<TR>
	<TH align=right>Requested URL:</TH>
	<TD><%= HttpUtils.getRequestURL(request) %></TD>
</TR>
<TR>
	<TH align=right>Request method:</TH>
	<TD><%= request.getMethod() %></TD>
</TR>
<TR>
	<TH align=right>Request URI:</TH>
	<TD><%= request.getRequestURI() %></TD>
</TR>
<TR>
	<TH align=right>Request protocol:</TH>
	<TD><%= request.getProtocol() %></TD>
</TR>
<TR>
	<TH align=right>Servlet path:</TH>
	<TD><%= request.getServletPath() %></TD>
</TR>
<TR>
	<TH align=right>Path info:</TH>
	<TD><%= request.getPathInfo() %></TD>
</TR>
<TR>
	<TH align=right>Path translated:</TH>
	<TD><%= request.getPathTranslated() %></TD>
</TR>
<TR>
	<TH align=right>Query string:</TH>
	<TD><%= request.getQueryString() %></TD>
</TR>
<TR>
	<TH align=right>Content length:</TH>
	<TD><%= request.getContentLength() %></TD>
</TR>
<TR>
	<TH align=right>Content type:</TH>
	<TD><%= request.getContentType() %></TD>
<TR>
<TR>
	<TH align=right>Server name:</TH>
	<TD><%= request.getServerName() %></TD>
<TR>
<TR>
	<TH align=right>Server port:</TH>
	<TD><%= request.getServerPort() %></TD>
<TR>
<TR>
	<TH align=right>Remote user:</TH>
	<TD><%= request.getRemoteUser() %></TD>
<TR>
<TR>
	<TH align=right>Remote address:</TH>
	<TD><%= request.getRemoteAddr() %></TD>
<TR>
<TR>
	<TH align=right>Remote host:</TH>
	<TD><%= request.getRemoteHost() %></TD>
<TR>
<TR>
	<TH align=right>Authorization scheme:</TH>
	<TD><%= request.getAuthType() %></TD>
<TR>
</TABLE>

<%
	Enumeration e = request.getHeaderNames();
	if(e != null && e.hasMoreElements()) {
%>
<H2>Request headers</H2>
<TABLE border=1>
<TR>
	<TH align=left>Header:</TH>
	<TH align=left>Value:</TH>
</TR>
<%
		while(e.hasMoreElements()) {
			String k = (String) e.nextElement();
%>
<TR>
	<TD><%= k %></TD>
	<TD><%= request.getHeader(k) %></TD>
</TR>
<%
		}
%>
</TABLE>
<%
	}
%>


<%
	e = request.getParameterNames();
	if(e != null && e.hasMoreElements()) {
%>
<H2>Request parameters</H2>
<TABLE border=1>
<TR valign=top>
	<TH align=left>Parameter:</TH>
	<TH align=left>Value:</TH>
	<TH align=left>Multiple values:</TH>
</TR>
<%
		while(e.hasMoreElements()) {
			String k = (String) e.nextElement();
			String val = request.getParameter(k);
			String vals[] = request.getParameterValues(k);
%>
<TR valign=top>
	<TD><%= k %></TD>
	<TD><%= val %></TD>
	<TD><%
			for(int i = 0; i < vals.length; i++) {
				if(i > 0)
					out.print("<BR>");
				out.print(vals[i]);
			}
		%></TD>
</TR>
<%
		}
%>
</TABLE>
<%
	}
%>

<%
	e = getServletConfig().getInitParameterNames();
	if(e != null && e.hasMoreElements()) {
%>
	<H2>Init parameters</H2>
	<TABLE border=1>
	<TR valign=top>
		<TH align=left>Parameter:</TH>
		<TH align=left>Value:</TH>
	</TR>
	<%
			while(e.hasMoreElements()) {
				String k = (String) e.nextElement();
				String val = getServletConfig().getInitParameter(k);
	%>
	<TR valign=top>
		<TD><%= k %></TD>
		<TD><%= val %></TD>
	</TR>
<%}%>
<%}%>
</TABLE>

<%
	Cookie[] cookies =request.getCookies();
	
%>
<H2>Cookies</H2>
<TABLE border=1>
<TR valign=top>
	<TH align=left>Name:</TH>
	<TH align=left>Value:</TH>
	<TH align=left>Domain:</TH>
	<TH align=left>Domain:</TH>
	<TH align=left>Comment:</TH>
	<TH align=left>Max Age:</TH>
</TR>
<%for(Cookie c : cookies){%>
<TR valign=top>
	<TD><%= c.getName() %></TD>
	<TD><%= c.getValue() %></TD>
	<TD><%= c.getDomain() %></TD>
	<TD><%= c.getValue() %></TD>
	<TD><%= c.getComment() %></TD>
	<TD><%= c.getMaxAge() %></TD>
</TR>
<%}%>

</TABLE>




</BODY>
</HTML>
