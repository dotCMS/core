<%@ page import="com.dotcms.util.HttpRequestDataUtil" %>
<%@ page import="java.net.InetAddress" %>
<%
    InetAddress address = HttpRequestDataUtil.getIpAddress(request);
    out.print(address);
%>
