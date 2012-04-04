dojo.provide("dojox.charting.themes.PlotKit.purple");
dojo.require("dojox.charting.themes.PlotKit.base");

(function(){
	var dc = dojox.charting, pk = dc.themes.PlotKit;

	pk.purple = pk.base.clone();
	pk.purple.chart.fill = pk.purple.plotarea.fill = "#eee6f5";
	pk.purple.colors = dc.Theme.defineColors({hue: 271, saturation: 60, low: 40, high: 88});
})();
