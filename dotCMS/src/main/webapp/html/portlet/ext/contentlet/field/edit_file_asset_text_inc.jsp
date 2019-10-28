<%String contents =UtilMethods.htmlifyString(FileUtils.readFileToString(fa.getBinary(field.getVelocityVarName()))); %>

<style type="text/css">
    #editor {
        position: relative;
	  	width: 95%;
        height: 95%;
        border:1px solid #C0C0C0;
    }
    .ace_scrollbar {
    	overflow: auto;
	}
	.dijitDialogPaneContent {
	    height: 100% !important;
	}
    .editor-options {
        margin-top: 5px;
    }
</style>

<script type='text/javascript' src='/dwr/interface/FileAssetAjax.js'></script>
<script language="JavaScript">
    var aceEditor;
    var changed=false;
  	function aceAreaParser(parser){
	    aceEditor = ace.edit("<%=field.getVelocityVarName()%>_ACE");
	    aceEditor.setTheme("ace/theme/textmate");
	    console.log(parser);
	    aceEditor.getSession().setMode("ace/mode/"+parser);
  		aceEditor.clearSelection();
  		aceEditor.on("change", function(){
  			changed=true;
  		});
  		aceEditor.on("blur", function(){
  			saveText();
  		});
	 }

     function handleWrapMode(e) {
         aceEditor.getSession().setUseWrapMode(e);
     }

     function loadAce(fileName) {
				var parser = "text";
				switch(fileName) {
					case 'css':
						parser="css";
						break;
					case 'vtl':
						 parser="velocity";
						break;
					case 'html':
						parser="text";
						break;
					case 'htm':
						parser="html";
						break;
					case 'js':
						parser = "jsp";
						break;
					case 'xml':
						parser = "xml";
						break;
					case 'sql':
						parser = "sql";
						break;
				    case 'php':
					    parser = "php";
						break;
				}
			aceAreaParser(parser);
     }


	function saveText(){
		if(!changed) {
			return;
		}
		var text = aceEditor.getValue();
		
		
		FileAssetAjax.saveFileText(contentletInode.value, text, '<%=field.getVelocityVarName()%>', {
			async: false,
			callback:function(data) {
			console.log("savedText");
			console.log(data);
		   }
		});
	}

</script>


<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="java.nio.file.Paths"%>
<%@page import="java.nio.file.Files"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<div id="fileTextEditorDiv">
    <div style="height:600px;max-width:900px;border:1px solid silver" 
        id="<%=field.getVelocityVarName()%>_ACE"><%=contents %></div>
        <div class="editor-options">
			<div class="checkbox">
            	<input id="wrapEditor" name="wrapEditor" data-dojo-type="dijit/form/CheckBox" value="true" onChange="handleWrapMode" />
            	<label for="wrapEditor"><%= LanguageUtil.get(pageContext, "Wrap-Code") %></label>
			</div>
        </div>
</div>

<input type="hidden" id="<%=field.getVelocityVarName()%>_hidden_field" value="<%=contents %>">

<script>
loadAce("<%=UtilMethods.getFileExtension(fa.getBinary(field.getVelocityVarName()).getName())%>")
</script>
