dojo.provide("dojox.charting.themes.PlotKit.green");
dojo.require("dojox.charting.themes.PlotKit.base");

(function(){
	var dc = dojox.charting, pk = dc.themes.PlotKit;

	pk.green = pk.base.clone();
	pk.green.chart.fill = pk.green.plotarea.fill = "#eff5e6";
	pk.green.colors = dc.Theme.defineColors({hue: 82, saturation: 60, low: 40, high: 88});
})();
