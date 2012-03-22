<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.StringTokenizer"%>
<%@page import="com.liferay.portal.model.User"%>

<%String identifiers =  request.getParameter("identifiers");%>
<%
User user= com.liferay.portal.util.PortalUtil.getUser(request);
boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
if(hasAdminRole){
	if (!UtilMethods.isSet(identifiers)) {%>



<%@page import="com.dotmarketing.business.CacheLocator"%>
<html>
<head></head>
<body>
<form method="post"><textarea wrap="off"  name="identifiers" cols="60" rows="30"></textarea>
<br />


<input type="submit"></form>



</body>

</html>


	<%} else {
	
			StringTokenizer st = new StringTokenizer(identifiers, "\n,\r");
	
			out.println("found: " + st.countTokens());
			out.println("<hr>");
			int i =0;
			while(st.hasMoreTokens()){
				String x = st.nextToken();
				if(!UtilMethods.isSet(x)){
					continue;
				}
				x =x.trim();
				
				//LuceneUtils.removeDocByIdenToCurrentIndex(x);
				
				
				
				i++;
				out.println(i + ":" + x);
				out.println("<br>");
			}
		}
	}else{%>
		<html>
			<head></head>
			<body>
        		You don't have the necessary permissions to run this script!
			</body>
		</html>
	<%} 
%>



