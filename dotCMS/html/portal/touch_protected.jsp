<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.util.Logger"%>
<%
boolean validate = false;
try{
	String userId = (String) session.getAttribute(WebKeys.USER_ID);
	if(UtilMethods.isSet(userId)){
		User myUser = UserLocalManagerUtil.getUserById(userId);
		if(myUser != null && myUser.isActive() ){
			validate = true;
		}
	}
}
catch(Exception e){

}

if(!validate){
	session.removeAttribute(WebKeys.USER_ID);
	return;
}
 %>

<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<html>
	<head>
		<link rel="shortcut icon" href="//www.dotcms.com/global/favicon.ico" type="image/x-icon">
		<meta content="no-cache" http-equiv="Cache-Control">
		<meta content="no-cache" http-equiv="Pragma">
		<meta content="0" http-equiv="Expires">
	</head>
	<body onload="top.location = '<%= CTX_PATH %>/';">
	
		<table border="0" cellpadding="0" cellspacing="0" height="100%" width="100%">
			<tr>
				<td align="center" valign="middle">
					<font face="Verdana, Tahoma, Arial" size="3">
						<b><%= LanguageUtil.get(pageContext, "processing-login") %>...</b>
					</font>
				</td>
			</tr>
		</table>
	</body>
</html>
