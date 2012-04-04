if(!dojo._hasResource["dojox.rpc.tests.libraryTests"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.rpc.tests.libraryTests"] = true;
dojo.provide("dojox.rpc.tests.libraryTests");

try{
	dojo.require("dojox.rpc.tests.Yahoo");
	dojo.require("dojox.rpc.tests.Geonames");
	dojo.require("dojox.rpc.tests.Wikipedia");
	dojo.require("dojox.rpc.tests.FriendFeed");
}catch(e){
	doh.debug(e);
}


}
