<%@ page import="java.io.File" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="org.apache.commons.io.FileUtils" %>

<%
	final File file = contentlet.getBinary(field.getVelocityVarName());
	String contents = "";
	String fileExtension = "txt";
	if (null != file) {
		contents = UtilMethods.htmlifyString(FileUtils.readFileToString(file));
		fileExtension = UtilMethods.getFileExtension(file.getName());
	}
%>

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

	let tempFileId = "new";

	/**
	 * For text File Assets, creates a temporary file with the changes that the user made. Once the User saves/publishes
	 * the Contentlet, these new changes will overwrite the existing file so that they can be persisted.
	 */
	function saveText(){
		if(!changed) {
			return;
		}
		let text = aceEditor.getValue();

		if (contentletInode.value == '') {
			let fileName = dojo.byId("fileName").value;
			if (fileName) {
				let fileExtension = fileName.split('.').pop();
				if (fileExtension) {
					loadAce(fileExtension);
				}
				let data = JSON.stringify({
					"fileName": fileName,
					"fileContent": text
				});
				let xhr = new XMLHttpRequest();

				xhr.addEventListener("readystatechange", function() {
					if(this.readyState === 4) {
						console.log(this.responseText);
					}
				});

				xhr.onload = function() {
					let jsonData = JSON.parse(xhr.response);
					tempFileId = jsonData.tempFiles[0].id;
					let elements = document.getElementsByName("<%= field.getFieldContentlet() %>");
					for (let i = 0; i < elements.length; i++) {
						if (elements[i].tagName.toLowerCase() == "input") {
							elements[i].value = tempFileId;
						}
					}
				};

				xhr.open("PUT", "/api/v1/temp/id/" + tempFileId);
				xhr.setRequestHeader("Content-Type", "application/json");
				xhr.send(data);
			}
		} else {
			FileAssetAjax.saveFileText(contentletInode.value, text, '<%=field.getVelocityVarName()%>', {
				async: false,
				callback: function (data) {
					console.log("savedText");
					console.log(data);
				}
			});
		}
	}

</script>

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
loadAce("<%= fileExtension %>")
</script>
