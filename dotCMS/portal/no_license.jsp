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
    
        <title>No License Error</title>

	<style type="text/css">
		body{
			font-family: helvetica, san-serif;
			padding:20px;
			margin-top:0px;
		}
		#main {
			width: 400px;
		}
		#footer {
			text-align:center;
		}
		h1 {
			font-size: 20px;

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

            <div id="text">
	
                <h1>Invalid License</h1>
		
                This server does not have valid  license.

                
            </div>
        </div>




	
<% 
	DbConnectionFactory.closeConnection();
%>

