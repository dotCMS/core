if(!dojo._hasResource["dojox.date.tests.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.date.tests.module"] = true;
dojo.provide("dojox.date.tests.module");

try{
	dojo.require("dojox.date.tests.relative");
	dojo.require("dojox.date.tests.hebrew.Date");
	dojo.require("dojox.date.tests.islamic.Date");
	dojo.require("dojox.date.tests.buddhist.Date");
	dojo.require("dojox.date.tests.posix");
}catch(e){
	doh.debug(e);
}


}
