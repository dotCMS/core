<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.io.File"%>
<%@page import="java.util.List"%>
<%
File d = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/undeployed"));
String[] a = d.list();
%>
{
	"identifier":"value", 
	"label":"label",
	"items":[
		<% for(String b : a){ %>
		{"value":"<%= b %>","label":"<%= b %>"},
		<%} %>
	]
}