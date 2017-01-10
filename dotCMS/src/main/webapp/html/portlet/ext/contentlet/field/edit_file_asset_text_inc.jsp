
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

<%@page import="com.liferay.portal.language.LanguageUtil"%>

<script type='text/javascript' src='/dwr/interface/FileAssetAjax.js'></script>
<script type='text/javascript' src='/html/js/scriptaculous/prototype.js'></script>


<script language="JavaScript">
    var aceEditor;
    var iAmOpen = false;
    var editorText;
    var saveOrCancel = false;
  	function aceAreaParser(parser,file){

  		if(iAmOpen){
	    	aceEditor.getSelection().selectFileStart();
	    	aceEditor.clearSelection();
  			return;
  		}
	    aceEditor = ace.edit('editor');
	    aceEditor.setTheme("ace/theme/textmate");
	    aceEditor.getSession().setMode("ace/mode/"+parser);
  		aceEditor.setValue(file.text);
  		editorText= aceEditor.getValue();
  		aceEditor.clearSelection();
  		iAmOpen = true;
	 }

     function handleWrapMode(e) {
         aceEditor.getSession().setUseWrapMode(e);
     }

	dojo.declare("dotcms.file.EditTextManager", null, {

		fileInode: '',

		editText: function (fileInode) {
		    this.fileInode = fileInode;
			FileAssetAjax.getWorkingTextFile(this.fileInode, dojo.hitch(this, this.loadTextCallback));
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
					var parser = "xml";
					break;
				case 'sql':
					var parser = "sql";
					break;
			    case 'php':
				    var parser = "php";
					break;
			}
			aceAreaParser(parser, file);
			dijit.byId('editTextDialog').show();

			dijit.byId('editTextButton').setAttribute('disabled',false);

		},


		save: function() {
			var text = aceEditor.getValue();
			FileAssetAjax.saveFileText(this.fileInode, text, {
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

<div dojoType="dijit.Dialog" id="editTextDialog" style="top:5%;left:5%;right:5%;bottom:5%;padding-top:15px; height: 80%" onCancel="javascript:editTextManager.close();">
 	<h3><%= LanguageUtil.get(pageContext, "text-editor") %></h3>
	<form name="fm" id="fm" method="post" action="" style="height: 100%">
		<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
		<input type="hidden" name="<portlet:namespace />referer" value="<%= request.getParameter("referer") %>">
		<input type="hidden" name="<portlet:namespace />cmd" value="">

		<div id="editor" style="padding-bottom: 5px; height: 75%"></div>
        <div class="editor-options">
            <input id="wrapEditor" name="wrapEditor" data-dojo-type="dijit/form/CheckBox" value="true" onChange="handleWrapMode" />
            <label for="wrapEditor"><%= LanguageUtil.get(pageContext, "Wrap-Code") %></label>
        </div>
		<div class="buttonRow">
	           <button id="editTextButton" dojoType="dijit.form.Button" iconClass="saveIcon" onClick="javascript:saveText();"><%= LanguageUtil.get(pageContext, "Save") %></button>&nbsp; &nbsp;
	           <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:editTextManager.close();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp;
		</div>
	</form>

</div>
