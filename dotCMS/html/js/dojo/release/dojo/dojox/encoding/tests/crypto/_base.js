if(!dojo._hasResource["dojox.encoding.tests.crypto._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.encoding.tests.crypto._base"] = true;
dojo.provide("dojox.encoding.tests.crypto._base");
dojo.require("dojox.encoding.crypto.Blowfish");

try{
	dojo.require("dojox.encoding.tests.crypto.Blowfish");
	dojo.require("dojox.encoding.tests.crypto.SimpleAES");
	dojo.require("dojox.encoding.tests.crypto.RSA");
}catch(e){
	doh.debug(e);
}

}
