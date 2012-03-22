<%
	String fileId = request.getParameter("file_id");
	String fileText = (String) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT_TEXT);
	String fileExt = (String) request.getAttribute(com.dotmarketing.util.WebKeys.FILE_EDIT_TEXT_FILE_EXT);
%>


<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script  type="text/javascript" src="/html/js/codemirror/js/codemirror.js"></script>
<script type='text/javascript' src='/dwr/interface/FileAjax.js'></script>

<script language="JavaScript">
    var codeMirrorEditor;
    var iAmOpen = false;
  	function codeMirrorArea(parser,file){
      codeMirrorEditor = CodeMirror.fromTextArea("file_text", {
  	  width: "700px",
  	  height:"350px",
		parserfile: ["parsedummy.js","parsexml.js", "parsecss.js", "tokenizejavascript.js", "parsejavascript.js","../contrib/php/js/tokenizephp.js", "../contrib/php/js/parsephp.js","../contrib/php/js/parsephphtmlmixed.js","parsehtmlmixed.js"],
		stylesheet: ["/html/js/codemirror/css/xmlcolors.css", "/html/js/codemirror/css/jscolors.css", "/html/js/codemirror/css/csscolors.css", "/html/js/codemirror/contrib/php/css/phpcolors.css"],
		path: "/html/js/codemirror/js/",
	     initCallback:function() {
		  changeParser(parser,file);
		  }
	    });	
	 }
	 function changeParser(parser, file){
		if (parser != 'DummyParser') {
			codeMirrorEditor.setParser(parser);
		}
		
	   	if(iAmOpen){
  			return;
  		}
  		iAmOpen = true;
		 codeMirrorEditor.setCode(file.text);
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
					var parser="CSSParser";
					break;
				case 'vtl':
					 var parser="DummyParser";
					break;
				case 'html':
					var parser="DummyParser";
					break;
				case 'htm':
					var parser="HTMLMixedParser";
					break;
				case 'js':
					var parser = "JSParser";
					break;
				case 'xml':
					var parser = "XMLParser";
					break;
				case 'sql':
					var parser = "DummyParser";
					break;
			    case 'php':
				    var parser = "PHPHTMLMixedParser";
					break;
			}
			codeMirrorArea(parser, file);
			dijit.byId('editTextDialog').show();
			dijit.byId('editTextButton').setAttribute('disabled',false);		 
		},


		save: function() {
			var text = codeMirrorEditor.getCode();
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
			var editorText= codeMirrorEditor.getCode();
			if (dojo.isIE) {//DOTCMS-5038
    			var node = dojo.query('.CodeMirror-wrapping')[0];
    			node.parentNode.removeChild(node);
			} else {
				dojo.query('.CodeMirror-wrapping')[0].remove();
			}
			dojo.query('#file_text').style({display:''});
			dojo.query('#file_text')[0].value=editorText;
			dijit.byId('editTextDialog').hide();
		}
		
	});

	var editTextManager = new dotcms.file.EditTextManager();
	function saveText(){

		dijit.byId('editTextButton').setAttribute('disabled',true);
		editTextManager.save();
		
	}

	
</script>

<div dojoType="dijit.Dialog" id="editTextDialog" onCancel="javascript:editTextManager.close();">

 	<div>
		<h3><%= LanguageUtil.get(pageContext, "text-editor") %></h3>
  	</div>
	<form name="fm" id="fm" method="post" action="">
		<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
		<input type="hidden" name="<portlet:namespace />referer" value="<%= request.getParameter("referer") %>">
		<input type="hidden" name="<portlet:namespace />cmd" value="">
		<div style="padding:10px;">
			<textarea id="file_text" value="<%=fileText%>" style="font-size: 12px; height:350px;width:700px;"></textarea>
		</div>
		<div class="buttonRow">
	           <button id="editTextButton" dojoType="dijit.form.Button" iconClass="saveIcon" onClick="javascript:saveText();"><%= LanguageUtil.get(pageContext, "Save") %></button>&nbsp; &nbsp; 
	           <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:editTextManager.close();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp; 
		</div>
	</form>

</div>

