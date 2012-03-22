<%@page import="com.dotmarketing.util.WebKeys"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%String dojoPath = Config.getStringProperty("path.to.dojo");%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	
<html>  
<head> 
<link rel="shortcut icon" href="//www.dotcms.com/global/favicon.ico" type="image/x-icon">
<title>dotCMS: <%= LanguageUtil.get(pageContext, "404-page-not-found") %></title>

<style media="all" type="text/css">
	@import url(/html/css/reset-min.css);
	@import url(/html/css/grids-min.css);
	@import url(/html/css/base.css);
    @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
</style>

<script language="javascript" SRC="<%=dojoPath%>/dojo/dojo.js" djConfig="parseOnLoad: true"></script>
<script>
	dojo.require("dijit.form.Button");
</script>

<script language="javascript">
	function addPage() {
		var form = document.getElementById("frm");
	    form.action = "<%= session.getAttribute(WebKeys.DIRECTOR_URL) %>&cmd=newHTMLPage&url=<%=request.getParameter("url")%>&hostId=<%=request.getParameter("hostId")%>" ;
		form.submit();
	}
</script>

<style>
	.shadowBox{background-color:#fff;width:750px;margin:20px auto;padding:10px;-moz-box-shadow:0px 0px 5px #ccc;-webkit-box-shadow:0px 0px 5px #ccc;-moz-border-radius: 5px;-webkit-border-radius: 5px;border: 1px solid #d0d0d0;}
	.callOutBox{background-color:#fff;margin:20px;padding:10px;-moz-border-radius: 5px;-webkit-border-radius: 5px;border: 1px solid #d0d0d0;}
</style>

</head>

<body class="dmundra" style="background-image:none;">
	
<div id="doc3">


<div class="shadowBox">
	<div>
			<a href="http://www.dotcms.org"><img src="/html/images/skin/logo.gif?code=404" width="208" height="63" hspace="10" border="0" alt="dotCMS content management system" title="dotCMS content management system"  /></a>
	</div>
	<div class="callOutBox">
		<h2><%= LanguageUtil.get(pageContext, "CMS-Page-not-found-Create-it-now") %></h2>
		<p><%= LanguageUtil.get(pageContext, "The-page-or-file-you-were-looking-for-was-not-found-in-dotCMS") %></p>
	</div>
	<div style="text-align:center;padding:5px 8px;">
        <form method="post" action="/ext/htmlpages/edit_htmlpage" id="frm">
        	<button dojoType="dijit.form.Button" iconClass="previousIcon" onClick="history.go(-1)"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Back")) %></button>
            <button dojoType="dijit.form.Button" iconClass="plusIcon" onClick="addPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Create-This-Page-Now")) %></button>
		</form>
	</div>
	<div style="text-align:right;padding:10px 20px;font-size:77%;">
		&copy;
		<script>
			var d = new Date();
			document.write(d.getFullYear());
		</script>, 
		<a href="http://www.dotmarketing.com">dotCMS Inc.</a> All rights reserved.
	</div>
</div>

</div>

</body>
</html>

