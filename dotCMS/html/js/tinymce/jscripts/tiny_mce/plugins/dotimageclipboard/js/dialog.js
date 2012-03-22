tinyMCEPopup.requireLangPack();


function dotPasteClipboard(val, tinyMCEPopup, img){


	val = "<img src='" + val + "'  width='" + img.width + "'  height='" + img.height + "' class='wysiwygImage' />";
	tinyMCEPopup.editor.execCommand('mceInsertRawHTML', false, val);
	tinyMCEPopup.close();
	
}

var DotImageClipboard = {
	init : function() {

	},

	insert : function(val) {

		
		
		var img = new Image();
		// set our onload before loading...
		var editor = tinyMCEPopup.editor;
		
		
		img.onload =function(){dotPasteClipboard(val, tinyMCEPopup, img)};
		
		img.src=val;
		
		

	}
};

tinyMCEPopup.onInit.add(DotImageClipboard.init, DotImageClipboard);
