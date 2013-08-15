<%
	String fileId = request.getParameter("file_id");
	String fileText = (String) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT_TEXT);
	String fileExt = (String) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT_TEXT_FILE_EXT);
%>


<%@page import="com.liferay.portal.language.LanguageUtil"%>
<style type="text/css">
    #editor { 
        position: relative;
	  	width: 600px;
        height: 330px;
        border:1px solid #C0C0C0;
    }
    .ace_scrollbar {
    	overflow: auto;
	}
</style>
<script src="/html/js/ace-builds-1.1.01/src-noconflict/ace.js" type="text/javascript"></script>
<script type='text/javascript' src='/dwr/interface/FileAjax.js'></script>

<script language="JavaScript">
    var aceEditor;
    var iAmOpen = false;
    var editorText;
    var saveOrCancel = false;
  	function aceArea(parser,file){
  		if(iAmOpen){
	    	aceEditor.getSelection().selectFileStart();
	    	aceEditor.clearSelection();
  			return;
  		}
	    aceEditor = ace.edit('editor');
	    aceEditor.setTheme("ace/theme/textmate");
	    aceEditor.getSession().setMode("ace/mode/"+parser);
  		aceEditor.getSession().setUseWrapMode(true);
  		aceEditor.setValue(file.text);
  		editorText= aceEditor.getValue();
  		aceEditor.clearSelection();
  		iAmOpen = true;
	 }
 
	dojo.declare("dotcms.file.EditTextManager", null, {

		fileInode: '',
		fileId: '',
	  	
		editText: function (fileInode,fileId) {
		    this.fileInode = fileInode;
			this.fileId = fileId;
			FileAjax.getWorkingTextFile(this.fileInode, dojo.hitch(this, this.loadTextCallback));
		},

		loadTextCallback: function(file) {
			switch(file.extension) {
				case 'css':
					var parser="css";
					break;
				case 'vtl':
					 var parser="velocity";
					break;
				case 'html':
					var parser="text";
					break;
				case 'htm':
					var parser="html";
					break;
				case 'js':
					var parser = "jsp";
					break;
				case 'xml':
					var parser = "XMLPxml
				case 'sql':
					var parser = "sql";
					break;
			    case 'php':
				    var parser = "php";
					break;
			}
			aceArea(parser, file);
			dijit.byId('editTextDialog').show();
			dijit.byId('editTextButton').setAttribute('disabled',false);		 
		},


		save: function() {
			var text = aceEditor.getValue();
			FileAjax.saveFileText(this.fileId, text, {
				async: false,	
				callback:function() {
				editTextManager.close();
			   }
		     }
		   );
		 },

		saveTextCallback: function() {
			this.close();
		},
		
		close: function() {
			if(!saveOrCancel){
				aceEditor.setValue(editorText);
			}	
			saveOrCancel = false;
			dijit.byId('editTextDialog').hide();
		}
		
	});

	var editTextManager = new dotcms.file.EditTextManager();
	function saveText(){
		saveOrCancel = true;
		editorText= aceEditor.getValue();
		dijit.byId('editTextButton').setAttribute('disabled',true);
		editTextManager.save();
		
	}

	
</script>

<div dojoType="dijit.Dialog" id="editTextDialog" style="height:450px;width:650px;padding-top:15px\9;" onCancel="javascript:editTextManager.close();">

 	<div>
		<h3><%= LanguageUtil.get(pageContext, "text-editor") %></h3>
  	</div>
	<form name="fm" id="fm" method="post" action="">
		<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
		<input type="hidden" name="<portlet:namespace />referer" value="<%= request.getParameter("referer") %>">
		<input type="hidden" name="<portlet:namespace />cmd" value="">
		<div id="editor" style="padding-bottom: 5px\15;"></div>
		<div class="buttonRow">
	           <button id="editTextButton" dojoType="dijit.form.Button" iconClass="saveIcon" onClick="javascript:saveText();"><%= LanguageUtil.get(pageContext, "Save") %></button>&nbsp; &nbsp; 
	           <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:editTextManager.close();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp; 
		</div>
	</form>

</div>

