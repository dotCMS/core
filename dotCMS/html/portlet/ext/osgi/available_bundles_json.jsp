<%@page import="com.liferay.util.FileUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.io.File"%>
<%@page import="java.util.List"%>
<%
    String f = FileUtil.getRealPath( "/WEB-INF/felix/undeployed" );
    if ( f == null ) {
        return;
    }

    File d = new File( f );
    String[] a = d.list();
    if ( a == null ) {
        a = new String[0];
    }
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