if(!dojo._hasResource["dojox.charting.themes.PlotKit.cyan"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.themes.PlotKit.cyan"] = true;
dojo.provide("dojox.charting.themes.PlotKit.cyan");
dojo.require("dojox.charting.themes.PlotKit.base");

(function(){
	var dc = dojox.charting, pk = dc.themes.PlotKit;

	pk.cyan = pk.base.clone();
	pk.cyan.chart.fill = pk.cyan.plotarea.fill = "#e6f1f5";
	pk.cyan.colors = dc.Theme.defineColors({hue: 194, saturation: 60, low: 40, high: 88});
})();

}
