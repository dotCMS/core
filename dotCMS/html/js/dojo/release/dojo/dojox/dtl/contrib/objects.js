if(!dojo._hasResource["dojox.dtl.contrib.objects"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.contrib.objects"] = true;
dojo.provide("dojox.dtl.contrib.objects");

dojo.mixin(dojox.dtl.contrib.objects, {
	key: function(value, arg){
		return value[arg];
	}
});

dojox.dtl.register.filters("dojox.dtl.contrib", {
	"objects": ["key"]
});

}
