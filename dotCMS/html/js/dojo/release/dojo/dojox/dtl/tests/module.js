if(!dojo._hasResource["dojox.dtl.tests.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tests.module"] = true;
dojo.provide("dojox.dtl.tests.module");

try{
	dojo.require("dojox.dtl.tests.text.filter");
	dojo.require("dojox.dtl.tests.text.tag");
	dojo.require("dojox.dtl.tests.dom.tag");
	dojo.require("dojox.dtl.tests.dom.buffer");
	dojo.require("dojox.dtl.tests.context");
}catch(e){
	doh.debug(e);
}

}
