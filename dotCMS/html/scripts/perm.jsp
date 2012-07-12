<%@page import="com.dotmarketing.business.PermissionAPITest"%>
<%@page import="org.junit.runner.notification.Failure"%>
<%@page import="org.junit.runner.Result"%>
<%@page import="org.junit.runner.JUnitCore"%>
<%

Result result = JUnitCore.runClasses(PermissionAPITest.class);
%>

<html>
<body>
<ul>
<%for (Failure failure : result.getFailures()) {%>
    <li> <%= failure.toString() %> <br/> 
         <pre><%= failure.getTrace() %></pre>
    </li>
<%}%>
</ul>
</body>
</html>