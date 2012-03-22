if(!dojo._hasResource["dojox.widget.DialogSimple"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.widget.DialogSimple"] = true;
dojo.provide("dojox.widget.DialogSimple");

dojo.require('dijit.Dialog');
dojo.require("dojox.layout.ContentPane");

dojo.declare("dojox.widget.DialogSimple", [dojox.layout.ContentPane, dijit._DialogBase], {
	// summary: A Simple Dialog Mixing the `dojox.layout.ContentPane` functionality over
	//		top of a vanilla `dijit.Dialog`. See `dojox.widget.Dialog` for a more flexible
	//		dialog option allowing animations and different styles/theme support.
});

}
