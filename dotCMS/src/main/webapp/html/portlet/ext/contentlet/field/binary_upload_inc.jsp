<!-- http://jira.dotmarketing.net/browse/DOTCMS-1073  -->
<%@ page import="java.util.LinkedList"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.Config"%>

<%String dojoPath = Config.getStringProperty("path.to.dojo");
if(!UtilMethods.isSet(dojoPath)){
	throw new Exception("No dojo path variable (path.to.dojo) set in the property file");
}
%>

<html>

	<head>
  		<title></title>
  		<style type="text/css">
			@import "/html/css/dot_admin.css";
        	@import "<%=dojoPath%>/dojo/resources/dojo.css"
	        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
    	</style>
  		
  		<link rel="STYLESHEET" type="text/css" href="/html/css/dot_admin.css">
	  	<style>
		  .prog-border {
		  height: 15px;
		  width: 205px;
		  background: #fff;
		  border: 1px solid #000;
		  margin: 0;
		  padding: 0;
		  }
		
		  .prog-bar {
		  height: 11px;
		  margin: 2px;
		  padding: 0px;
		  background:#488AC7;
		  }  
		</style>
		  
	  <script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
		  
	  <script type="text/javascript">
     	djConfig={
                parseOnLoad: true,
                isDebug: false,
                modulePaths: { dotcms: "/html/js/dotcms", dwr: '/dwr' }
        };
	  </script>
	  <script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js"></script>
      <script type="text/javascript">
    	dojo.require("dijit.form.Button");
      	dojo.require("dojo.parser");
      </script>
	  

	</head>

	<body onLoad="setRemoveButton()" class="dmundra">
	
		  <script type="text/javascript" language="JavaScript">
	  
	  var updater = null;
	
	  function startStatusCheck(){

	   	$('submitButton').disabled = true;	
		$('status').innerHTML = '<b><%= LanguageUtil.get(pageContext, "Uploading") %>...</b>';
	    
	    updater = new Ajax.PeriodicalUpdater(
	                                'status',
	                                '/servlets/ajax_file_upload',
	                                {asynchronous:true, frequency:1,
	                                method: 'get',
	                                parameters: 'c=status&f=importFile', 
	                                onFailure: reportError});
		
		window.parent.isAjaxFileUploading = "true";
		
	    return true;	    
	  }
	
	  function reportError(request){
	  
	  	$('submitButton').disabled = false;
	    $('status').innerHTML = '<div class="error"><b><%= LanguageUtil.get(pageContext, "Error-communicating-with-server-Please-try-again") %></b></div>';
	    window.parent.isAjaxFileUploading = "";	    
	  }
	
	  function killUpdate(message){
	  
	 	$('submitButton').disabled = false;
	    updater.stop();    
	    
	    if(message != ''){    
	    	$('status').innerHTML = '<div class="error"><b><%= LanguageUtil.get(pageContext, "Error-processing-results") %>: ' + message + '</b></div>';
	      	window.parent.isAjaxFileUploading = "";
	    }else{
	      new Ajax.Updater('status',
	                     '/servlets/ajax_file_upload',
	                     {asynchronous:true, method: 'get', 
	                     parameters: 'c=status&f=importFile', 
	                     onFailure: reportError});
	    }	    
	  }
	    
	  function doFileUpload(){
	  
	  	var fileVal = $('importFile').value;
		
		if(fileVal.length > 0 ){
	  		startStatusCheck();
	  		$('upForm').submit();
	  	}	  	
	  }
	  
	  function fillComplete(fileName){
	  
	  	window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>').value=fileName;  
	  	setRemoveButton();
	    hideUploadElement();
	    window.parent.isAjaxFileUploading = "";
	    
	    if(window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>BinaryFile')){
				window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>BinaryFile').style.display = "none";
		}
		
	  }  
	  
	  function setRemoveButton(){
	  
	  if(window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>').value.length > 0 
	  		&& window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>').value != 'null') {

	  		window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>FieldArea').style.display = "";	  		 	
		  	hideUploadElement();
		  	
	  	}else{
	  		window.parent.document.getElementById('<%=request.getParameter("fieldContentlet")%>FieldArea').style.display = "none";	  		 	
	  	}	  	
	  }
	  
	  
	  var showUploadElement = function(){
		  dojo.byId('fileDivImportFile').style.display = '';
		  dojo.byId('fileDivReupload').style.display = 'none';
	  } 
	  
	  function hideUploadElement(){
		  dojo.byId('fileDivImportFile').style.display = 'none';
		  dojo.byId('fileDivReupload').style.display = '';
	  }
	  
	  </script>

  	<!-- This iframe is used as a place for the post to load -->
  	<iframe id='target_upload' name='target_upload' src='' style='display: none'></iframe>

  	<form enctype="multipart/form-data" 
  		name="upForm" 
  		method="post" 
  		action="/servlets/ajax_file_upload" 
  		onsubmit="return startStatusCheck();" 
  		target="target_upload" 
  		id="upForm">
  		  
		<div id="fileDiv" align="left" >
		
			<div id="fileDivImportFile">
				<input id="importFile" 
						name="importFile" 
						type="file" 
						onchange="doFileUpload();" 
						class="form-text"			
						size="30">
			</div>
			<div id="fileDivReupload" style="display: none;">
				<button dojoType="dijit.form.Button" id="reupload" onclick="showUploadElement();">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "re-upload")) %>
				</button>
			</div>
		</div>
		
	  	<div style="display: none;"> 
           <button dojoType="dijit.form.Button" type="submit" id="submitButton">Submit</button>
        </div>	
	  	
  	</form>
  
  	<!-- This is the upload status area -->
  	<div id="status"></div>

	</body>
</html>
