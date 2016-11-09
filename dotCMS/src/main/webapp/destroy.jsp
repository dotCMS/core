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
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}
}
%>

Session: <%= sessionId%> destroyed <br />
<%for(Cookie c : al){ %>
	Cookie: <%=c.getName() %>  destroyed <br />
<%} %>



