<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="java.util.Enumeration"%>

<%
User user = null;
try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);
} catch (Exception e) {
	Logger.warn(this.getClass(), "no user found");
} 
if(user ==null || "100".equals(LicenseUtil.getLevel())){
	response.getWriter().println("Unauthorized");
	return;
}

List<String> clipboard = (List<String>) request.getSession().getAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD);
if(clipboard ==null){
	clipboard = new ArrayList<String>();
}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<style>

#imageContainer{
	width:520px;
	height:400px;
	overflow: auto;
	background: white;
	border:1px solid gray;
	margin: auto;
}
.thumbContainer{
	cursor:pointer;
	width:150px;
	overflow: hidden;
	height:170px;
	margin:5px;
	border:2px dotted silver;
	padding:1px;
	float:left;
}
.thumbContainer:hover{

	border:2px dotted blue;

}
.thumbInfo{
	text-align:right;
	color:gray;
}
</style>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title><%=LanguageUtil.get(pageContext, "dotCMS-Image-Clipboard") %></title>
	<script type="text/javascript" src="../../tiny_mce_popup.js"></script>
	<script type="text/javascript" src="js/dialog.js"></script>
</head>
<body>
<h2><%=LanguageUtil.get(pageContext, "Clipboard") %></h2>
<%=LanguageUtil.get(pageContext, "Select-an-image-below") %>



<div id="imageContainer">

	<%if(user ==null || "100".equals(System.getProperty("dotcms_level"))){%>
		<div style="text-align:center;padding-top:140px;">
			<h3 style="color:silver"><%=LanguageUtil.get(pageContext, "dotCMS-Enterprise-comes-with-an-advanced-Image-Editor-tool") %></h3>
		</div>
	<%} else if(clipboard.size() ==0){ %>
		<div style="text-align:center;padding-top:140px;">
			<h2 style="color:silver"><%=LanguageUtil.get(pageContext, "Your-clipboard-is-empty") %></h2>
		</div>
	<%} else {%>
		<%for(int i=0;i<clipboard.size();i++){ 
			String url = clipboard.get(i); 
			String thumbUrl = url;

			// if thumbnail is already in filter
	   		if(thumbUrl.indexOf("filter=") > -1){
	   			String beforeFilter = thumbUrl.substring(0, thumbUrl.indexOf("filter="));
	   			String afterFilter = thumbUrl.substring(thumbUrl.indexOf("filter=") + 7, thumbUrl.length());
	   			String filter = afterFilter.substring(0,afterFilter.indexOf("&"));
	   			afterFilter = afterFilter.substring(afterFilter.indexOf("&"),afterFilter.length());

	 			thumbUrl = beforeFilter + "&filter=" + filter + ",Thumbnail&" + afterFilter +"&thumbnail_w=150&thumbnail_h=150";       
			}
			else{
				if(thumbUrl.indexOf("?") <0){
					thumbUrl+="?";
				}
				thumbUrl+= "&filter=Thumbnail&thumbnail_w=150&thumbnail_h=150";
			}
			
			
			
			
			
			
			
			%>
			<div class="thumbContainer" onclick="DotImageClipboard.insert('<%=url%>')">
				<img id="clipThumb<%=i %>" src="<%=thumbUrl %>" width="150" height="150"/>
				<div class='thumbInfo' id="clipThumbInfo<%=i %>"></div>
			</div>
		<%} %>
	<%} %>
	
	
	
<script>
	function getDim(x){
		var ele = document.getElementById("clipThumbInfo" + x);
		ele.innerHTML = eval("img" + x + ".width") + "x" + eval("img" + x + ".height")
		
		
	}

	<%for(int i=0;i<clipboard.size();i++){ %>
		<%String url = clipboard.get(i); %>
		var img<%=i%> = new Image();;
		img<%=i%>.onload =function(){getDim(<%=i%>)};
		img<%=i%>.src="<%=url%>";
	<%}%>
</script>
	
	
	
</div>

</body>
</html>
