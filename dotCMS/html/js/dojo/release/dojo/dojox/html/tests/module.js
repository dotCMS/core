if(!dojo._hasResource["dojox.html.tests.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.html.tests.module"] = true;
dojo.provide("dojox.html.tests.module");
try{
	dojo.requireIf(dojo.isBrowser, "dojox.html.tests.entities");
	dojo.requireIf(dojo.isBrowser, "dojox.html.tests.format");
}catch(e){
	doh.debug(e);
}


}
