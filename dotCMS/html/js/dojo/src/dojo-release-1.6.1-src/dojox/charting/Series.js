dojo.provide("dojox.charting.Series");

dojo.require("dojox.charting.Element");
/*=====
dojox.charting.__SeriesCtorArgs = function(plot){
	//	summary:
	//		An optional arguments object that can be used in the Series constructor.
	//	plot: String?
	//		The plot (by name) that this series belongs to.
	this.plot = plot;
}
=====*/
dojo.declare("dojox.charting.Series", dojox.charting.Element, {
	//	summary:
	//		An object representing a series of data for plotting on a chart.
	constructor: function(chart, data, kwArgs){
		//	summary:
		//		Create a new data series object for use within charting.
		//	chart: dojox.charting.Chart2D
		//		The chart that this series belongs to.
		//	data: Array|Object:
		//		The array of data points (either numbers or objects) that
		//		represents the data to be drawn. Or it can be an object. In
		//		the latter case, it should have a property "data" (an array),
		//		destroy(), and setSeriesObject().
		//	kwArgs: dojox.charting.__SeriesCtorArgs?
		//		An optional keyword arguments object to set details for this series.
		dojo.mixin(this, kwArgs);
		if(typeof this.plot != "string"){ this.plot = "default"; }
		this.update(data);
	},

	clear: function(){
		//	summary:
		//		Clear the calculated additional parameters set on this series.
		this.dyn = {};
	},
	
	update: function(data){
		//	summary:
		//		Set data and make this object dirty, so it can be redrawn.
		//	data: Array|Object:
		//		The array of data points (either numbers or objects) that
		//		represents the data to be drawn. Or it can be an object. In
		//		the latter case, it should have a property "data" (an array),
		//		destroy(), and setSeriesObject().
		if(dojo.isArray(data)){
			this.data = data;
		}else{
			this.source = data;
			this.data = this.source.data;
			if(this.source.setSeriesObject){
				this.source.setSeriesObject(this);
			}
		}
		this.dirty = true;
		this.clear();
	}
});
