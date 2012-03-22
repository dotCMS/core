if(!dojo._hasResource["dijit._base.window"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit._base.window"] = true;
dojo.provide("dijit._base.window");
dojo.require("dojo.window");



dijit.getDocumentWindow = function(doc){
	return dojo.window.get(doc);
};

}
