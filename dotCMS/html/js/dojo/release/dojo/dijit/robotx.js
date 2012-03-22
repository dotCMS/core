if(!dojo._hasResource["dijit.robotx"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.robotx"] = true;
dojo.provide("dijit.robotx");
dojo.require("dijit.robot");
dojo.require("dojo.robotx");



//WARNING: This module depends on GLOBAL dijit being set for v1.5 code; therefore the lexical variable that
//references "dijit" has been renamed to "dijit_"

dojo.experimental("dijit.robotx");
(function(){
var __updateDocument = doh.robot._updateDocument;

dojo.mixin(doh.robot,{
	_updateDocument: function(){
		__updateDocument();
		var win = dojo.global;
		if(win["dijit"]){
			window.dijit = win.dijit; // window reference needed for IE
		}
	}
});

})();

}
