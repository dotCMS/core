<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page import="com.dotmarketing.db.DbConnectionFactory"%>
<html>
    <head>

    <script>
        function showError(){
            var ele = document.getElementById("error");
            if(ele.style.display=="none"){
                ele.style.display="";
            }
            else{
                ele.style.display="none";
            }
        
        }
        
        
    </script>
    
        <title>dotCMS: No License Error</title>

        <style type="text/css">
			body{
				font-family: verdana, helvetica, san-serif;
				padding:20px;
			}
			#main {
				width: 400px;
				font-family: verdana, helvetica, san-serif;
				font-size: 12px;
				margin-left:auto;
				margin-right:auto;
			}
			#footer {
				text-align:center;
				font-family: verdana, helvetica, san-serif;
				font-size: 12px;
			}
			h1 {
				font-family: verdana, helvetica, san-serif;
				font-size: 20px;
				text-decoration: none;
				font-weight: normal;
			}
			#logo{
				float: left;
			}
			#text{
				float: left;
			}
        </style>

    </head>
    <body>
        <div id="main">
            <div id="logo">
                <a href="http://dotcms.com"><img src="/html/images/skin/logo.gif?code=500"  height="50" hspace="10" border="0" alt="dotCMS content management system" title="dotCMS content management system"  /></a>
            </div>
            <div id="text">
	
                <h1>Invalid License</h1>
		
                This server does not have valid dotCMS license.  
                If you are the administrator for this site,  please contact 
                <a href="http://dotcms.com/contact-us">dotCMS</a> for 
                more information or request a trial license via the 
                license manager admin tool.
                
            </div>
        </div>
        <br clear="all"/>&nbsp;<br clear="all"/>
        <div id="footer">&copy; 
<div id="footer">&copy; <script>var d = new Date();document.write(d.getFullYear());</script>, <a href="http://dotcms.com">DM Web, Corp.</a></div>
        <br clear="all"/>&nbsp;<br clear="all"/>
<div id="error" style="display: none;border: 1px #cccccc solid; padding:10px; margin:10px;width:80%">

	
<% 
	DbConnectionFactory.closeConnection();
%>

