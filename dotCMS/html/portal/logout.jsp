<%@page import="java.util.ArrayList"%>
<%
String sessionId = session.getId();
session.invalidate();

ArrayList<Cookie> al = new ArrayList<Cookie>();
Cookie[] cookies = request.getCookies();
if(cookies != null){
	for(int i=0; i<cookies.length; i++){
		Cookie cookie = cookies[i];
		al.add(cookie);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}
}
%>

Session: <%= sessionId%> destroyed <br />
<%for(Cookie c : al){ %>
	Cookie: <%=c.getName() %>  destroyed <br />
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;value: <%=c.getValue() %>   <br />
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;path: <%=c.getPath() %>   <br />
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;getDomain: <%=c.getDomain() %>   <br />
<%} %>


<html>
<script>
	//window.location="/c";
</script>
</html>