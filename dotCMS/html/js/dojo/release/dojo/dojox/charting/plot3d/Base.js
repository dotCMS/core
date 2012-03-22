if(!dojo._hasResource["dojox.charting.plot3d.Base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.plot3d.Base"] = true;
dojo.provide("dojox.charting.plot3d.Base");

dojo.require("dojox.charting.Chart3D");

dojo.declare("dojox.charting.plot3d.Base", null, {
	constructor: function(width, height, kwArgs){
		this.width  = width;
		this.height = height;
	},
	setData: function(data){
		this.data = data ? data : [];
		return this;
	},
	getDepth: function(){
		return this.depth;
	},
	generate: function(chart, creator){
	}
});


}
