dojo.provide("dojox.charting.plot2d.Lines");

dojo.require("dojox.charting.plot2d.Default");

dojo.declare("dojox.charting.plot2d.Lines", dojox.charting.plot2d.Default, {
	//	summary:
	//		A convenience constructor to create a typical line chart.
	constructor: function(){
		//	summary:
		//		Preset our default plot to be line-based.
		this.opt.lines = true;
	}
});
