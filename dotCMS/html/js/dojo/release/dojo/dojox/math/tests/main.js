if(!dojo._hasResource["dojox.math.tests.main"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.math.tests.main"] = true;
dojo.provide("dojox.math.tests.main");

try{
	// functional block
	dojo.require("dojox.math.tests.math");
	dojo.require("dojox.math.tests.stats");
	dojo.require("dojox.math.tests.round");
	dojo.require("dojox.math.tests.BigInteger");
	dojo.require("dojox.math.tests.random");
}catch(e){
	doh.debug(e);
}

}
