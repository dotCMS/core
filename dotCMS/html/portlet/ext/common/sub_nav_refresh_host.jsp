<%@page import="java.util.StringTokenizer"%>
<%
	session.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, request.getParameter("host_id"));
	String referer = request.getParameter("referer");
	if(referer.indexOf("host_id=")>-1){
		StringTokenizer st = new StringTokenizer(referer, "?&", true);
		StringBuffer sb = new StringBuffer();
		while(st.hasMoreTokens()){
			String token = st.nextToken();
			if(!token.startsWith("host_id=")){
				sb.append(token);
			}
		}
		sb.append("host_id=" + request.getParameter("host_id"));
		response.sendRedirect(sb.toString());
	}
	else{
		response.sendRedirect(referer + "&host_id=" + request.getParameter("host_id"));
	}
%>