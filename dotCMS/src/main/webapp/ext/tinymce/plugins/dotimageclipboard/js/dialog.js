// TinyMCE 8: the parent editor is reached via `postMessage` to the dialog host window
// (`windowManager.openUrl`'s `onMessage` callback in ../plugin.min.js), not the removed
// `compat3x` `tinyMCEPopup` bridge this file used to depend on.
function dotPasteClipboard(val, img) {
	var html = "<img src='" + val + "'  width='" + img.width + "'  height='" + img.height + "' class='wysiwygImage' />";

	window.parent.postMessage({ mceAction: 'dotImageClipboardInsert', html: html }, window.location.origin);
}

var DotImageClipboard = {
	insertContent : function(val) {
		var img = new Image();
		// set our onload before loading...
		img.onload = function(){dotPasteClipboard(val, img)};
		img.src = val;
	}
};
