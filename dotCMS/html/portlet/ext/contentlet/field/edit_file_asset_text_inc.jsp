


<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script  type="text/javascript" src="/html/js/codemirror/js/codemirror.js"></script>
<script type='text/javascript' src='/dwr/interface/FileAssetAjax.js'></script>

<script language="JavaScript">
    var codeMirrorEditor;
    var iAmOpen = false;
    var editorText;
    var saveOrCancel = false;
  	function codeMirrorAreaParser(parser,file){

  		var cmHieght = (window.innerHeight * .90 ) -150;
  		
      codeMirrorEditor = CodeMirror.fromTextArea("file_text", {
  	  width: "95%",
  	  height:cmHieght +"px",
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
		 editorText= codeMirrorEditor.getCode();
	 }
 
	dojo.declare("dotcms.file.EditTextManager", null, {

		fileInode: '',
		
		editText: function (fileInode) {
		    this.fileInode = fileInode;
			FileAssetAjax.getWorkingTextFile(this.fileInode, dojo.hitch(this, this.loadTextCallback));
		},

		loadTextCallback: function(file) {
			debugger
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
			debugger
			codeMirrorAreaParser(parser, file);			
			dijit.byId('editTextDialog').show();	
			
			dijit.byId('editTextButton').setAttribute('disabled',false);	
			
		},


		save: function() {
			var text = codeMirrorEditor.getCode();
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
			if(saveOrCancel){
				editorText= codeMirrorEditor.getCode();
			}
			if (dojo.isIE) {//DOTCMS-5038
    			var node = dojo.query('.CodeMirror-wrapping')[0];
    			node.parentNode.removeChild(node);
			} else {
				dojo.query('.CodeMirror-wrapping')[0].remove();
			}
			dojo.query('#file_text').style({display:''});
			dojo.query('#file_text')[0].value=editorText;	
			saveOrCancel = false;
			dijit.byId('editTextDialog').hide();
		}
		
	});

	var editTextManager = new dotcms.file.EditTextManager();
	function saveText(){
		saveOrCancel = true;
		dijit.byId('editTextButton').setAttribute('disabled',true);
		editTextManager.save();
		
	}
	window.onresize = function() {
				
		dojo.query(".CodeMirror-wrapping").forEach(function(node, index, nodelist){
			var cmHieght = (window.innerHeight * .90 ) -150;

		    dojo.style(node, "height", cmHieght + "px");
		});
		
		
	};
	
</script>

<div dojoType="dijit.Dialog" id="editTextDialog" style="top:5%;left:5%;right:5%;bottom:5%;" onCancel="javascript:editTextManager.close();">

	 	<div>
			<h3><%= LanguageUtil.get(pageContext, "text-editor") %></h3>
	  	</div>
		<form name="fm" id="fm" method="post" action="">
			<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
			<input type="hidden" name="<portlet:namespace />referer" value="<%= request.getParameter("referer") %>">
			<input type="hidden" name="<portlet:namespace />cmd" value="">
			<div style="padding:10px;" id="file_text">
				
			</div>
			<div class="buttonRow">
		           <button id="editTextButton" dojoType="dijit.form.Button" iconClass="saveIcon" onClick="javascript:saveText();"><%= LanguageUtil.get(pageContext, "Save") %></button>&nbsp; &nbsp; 
		           <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:editTextManager.close();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp; 
			</div>
		</form>

</div>

