if(!dojo._hasResource["dojox.lang.tests.main"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.lang.tests.main"] = true;
dojo.provide("dojox.lang.tests.main");

try{
	// functional block
	dojo.require("dojox.lang.tests.listcomp");
	dojo.require("dojox.lang.tests.lambda");
	dojo.require("dojox.lang.tests.fold");
	dojo.require("dojox.lang.tests.curry");
	dojo.require("dojox.lang.tests.misc");
	dojo.require("dojox.lang.tests.array");
	dojo.require("dojox.lang.tests.object");
	dojo.require("dojox.lang.tests.oo_mixin");
	dojo.require("dojox.lang.tests.recomb");
	dojo.require("dojox.lang.tests.observable");
	dojo.require("dojox.lang.tests.docs");
	dojo.require("dojox.lang.tests.typed");
}catch(e){
	doh.debug(e);
}

}
