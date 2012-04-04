if(!dojo._hasResource["dojox.encoding.tests.digests._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.encoding.tests.digests._base"] = true;
dojo.provide("dojox.encoding.tests.digests._base");
dojo.require("dojox.encoding.digests._base");

try{
	dojo.require("dojox.encoding.tests.digests.MD5");
	dojo.require("dojox.encoding.tests.digests.SHA1");
}catch(e){
	doh.debug(e);
}

}
