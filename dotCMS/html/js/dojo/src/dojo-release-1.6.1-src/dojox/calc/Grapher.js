define("dojox/calc/Grapher", ["dojo", "dijit/_Templated", "dojox/math/_base", "dijit/dijit", "dijit/form/DropDownButton", "dijit/TooltipDialog", "dijit/form/TextBox", "dijit/form/Button", "dijit/form/ComboBox", "dijit/form/Select", "dijit/form/CheckBox", "dijit/ColorPalette", "dojox/charting/Chart2D", "dojox/charting/themes/Tufte", "dojo/colors"], function(dojo) {

dojo.experimental("dojox.calc.Grapher");

dojo.declare(
	"dojox.calc.Grapher",
	[dijit._Widget, dijit._Templated],
{
	// summary:
	//		The dialog layout for making graphs
	//
	templateString: dojo.cache("dojox.calc", "templates/Grapher.html"),

	widgetsInTemplate:true,

	addXYAxes: function(chart){
		// summary:
		//		add or re-add the default x/y axes to the Chart2D provided
		// params:
		//	chart is an instance of dojox.charting.Chart2D

		return chart.addAxis("x", {
			max: parseInt(this.graphMaxX.get("value")),
			min: parseInt(this.graphMinX.get("value")),
			majorLabels: true,
			minorLabels: true,
			//includeZero: true,
			minorTicks: false,
			microTicks: false,
			//majorTickStep: 1,
			htmlLabels: true,
			labelFunc: function(value){
				return value;
			},
			maxLabelSize: 30,
			fixUpper: "major", fixLower: "major",
			majorTick: { length: 3 }
		}).
		addAxis("y", {
			max: parseInt(this.graphMaxY.get("value")),
			min: parseInt(this.graphMinY.get("value")),
			labelFunc: function(value){
				return value;
			},
			maxLabelSize: 50,
			vertical: true,
			// htmlLabels: false,
			microTicks: false,
			minorTicks: true,
			majorTick: { stroke: "black", length: 3 }
		});
	},
	selectAll: function(){
		// summary
		//	select all checkboxes inside the function table
		for(var i = 0; i < this.rowCount; i++){
			this.array[i][this.checkboxIndex].set("checked", true);
		}
	},
	deselectAll: function(){
		// summary
		//	deselect all checkboxes inside the function table
		for(var i = 0; i < this.rowCount; i++){
			this.array[i][this.checkboxIndex].set("checked", false);
		}
	},
	drawOne: function(i){
		// i is a the index to this.array
		// override me
	},
	onDraw: function(){
		console.log("Draw was pressed");
		// override me
	},
	erase: function(i){
		// summary:
		//	erase the chart inside this.array with the index i
		// params:
		//	i is the integer index to this.array that represents the current row number in the table
		var nameNum = 0;
		var name = "Series "+this.array[i][this.funcNumberIndex]+"_"+nameNum;
		while(name in this.array[i][this.chartIndex].runs){
			this.array[i][this.chartIndex].removeSeries(name);
			nameNum++;
			name = "Series "+this.array[i][this.funcNumberIndex]+"_"+nameNum;
		}
		this.array[i][this.chartIndex].render();
		this.setStatus(i, "Hidden");
	},
	onErase: function(){
		// summary:
		//	the erase button's onClick method
		//	it see's if the checkbox is checked and then erases it if it is.
		for(var i = 0; i < this.rowCount; i++){
			if(this.array[i][this.checkboxIndex].get("checked")){
				this.erase(i);
			}
		}
	},
	onDelete: function(){
		// summary:
		//	the delete button's onClick method
		//	delete all of the selected rows
		for(var i = 0; i < this.rowCount; i++){
			if(this.array[i][this.checkboxIndex].get("checked")){
				this.erase(i);
				for(var k = 0; k < this.functionRef; k++){
					if(this.array[i][k] && this.array[i][k]["destroy"]){
						this.array[i][k].destroy();
					}
				}
				this.graphTable.deleteRow(i);
				this.array.splice(i, 1);
				this.rowCount--;
				i--;
			}
		}
	},
	// attributes to name the indices of this.array
	checkboxIndex:		0,
	functionMode:		1,
	expressionIndex:	2,
	colorIndex:		3,
	dropDownIndex:		4,
	tooltipIndex:		5,
	colorBoxFieldsetIndex:	6,
	statusIndex:		7,
	chartIndex:		8,
	funcNumberIndex:	9,
	evaluatedExpression:	10,
	functionRef:		11,

	createFunction: function(){
		// summary:
		//	create a new row in the table with all of the dojo objects.

		var tr = this.graphTable.insertRow(-1);
		this.array[tr.rowIndex] = [];
		var td = tr.insertCell(-1);
		var d = dojo.create('div');
		td.appendChild(d);
		var checkBox = new dijit.form.CheckBox({}, d);
		this.array[tr.rowIndex][this.checkboxIndex] = checkBox;
		dojo.addClass(d, "dojoxCalcCheckBox");

		td = tr.insertCell(-1);
		var funcMode = this.funcMode.get("value");
		d = dojo.doc.createTextNode(funcMode);
		td.appendChild(d);
		this.array[tr.rowIndex][this.functionMode] = funcMode;
		//dojo.addClass(d, "dojoxCalcFunctionMode");// cannot use text nodes

		td = tr.insertCell(-1);
		d = dojo.create('div');
		td.appendChild(d);
		var expression = new dijit.form.TextBox({}, d);
		this.array[tr.rowIndex][this.expressionIndex] = expression;
		dojo.addClass(d, "dojoxCalcExpressionBox");

		var b = dojo.create('div');
		var color = new dijit.ColorPalette({changedColor:this.changedColor}, b);
		dojo.addClass(b, "dojoxCalcColorPalette");

		this.array[tr.rowIndex][this.colorIndex] = color;

		var c = dojo.create('div');
		var dialog = new dijit.TooltipDialog({content:color}, c);
		this.array[tr.rowIndex][this.tooltipIndex] = dialog;
		dojo.addClass(c, "dojoxCalcContainerOfColor");

		td = tr.insertCell(-1);
		d = dojo.create('div');
		td.appendChild(d);

		var colorBoxFieldset = dojo.create('fieldset');
		dojo.style(colorBoxFieldset, {backgroundColor: "black", width: "1em", height: "1em", display: "inline"});
		this.array[tr.rowIndex][this.colorBoxFieldsetIndex] = colorBoxFieldset;

		var drop = new dijit.form.DropDownButton({label:"Color ", dropDown:dialog}, d);
		drop.containerNode.appendChild(colorBoxFieldset);
		this.array[tr.rowIndex][this.dropDownIndex] = drop;
		dojo.addClass(d, "dojoxCalcDropDownForColor");

		/*td = tr.insertCell(-1);
		d = dojo.create('div');
		td.appendChild(d);
		var status = new dijit.form.TextBox({style:"width:50px", value:"Hidden", readOnly:true}, d);//hidden, drawn, or error
		this.array[tr.rowIndex][this.statusIndex] = status;
		dojo.addClass(d, "dojoxCalcStatusBox");*/

		td = tr.insertCell(-1);
		d = dojo.create('fieldset');
		d.innerHTML = "Hidden";
		this.array[tr.rowIndex][this.statusIndex] = d;
		dojo.addClass(d, "dojoxCalcStatusBox");
		td.appendChild(d);

		d = dojo.create('div');
		dojo.style(d, {position:"absolute", left:"0px", top:"0px"})
		this.chartsParent.appendChild(d);
		this.array[tr.rowIndex][this.chartNodeIndex] = d;
		dojo.addClass(d, "dojoxCalcChart");
		var chart = new dojox.charting.Chart2D(d).setTheme(dojox.charting.themes.Tufte).
			addPlot("default", { type: "Lines", shadow:  {dx: 1, dy: 1, width: 2, color: [0, 0, 0, 0.3]} });
		this.addXYAxes(chart);
		this.array[tr.rowIndex][this.chartIndex] = chart;
		color.set("chart", chart);
		color.set("colorBox", colorBoxFieldset);
		color.set("onChange", dojo.hitch(color, 'changedColor'));

		this.array[tr.rowIndex][this.funcNumberIndex] = this.funcNumber++;
		this.rowCount++;
	},
	setStatus: function(i, status){
		// summary:
		//	set the status of the row i to be status
		// params:
		//	i is an integer index of this.array as well as a row index
		//	status is a String, it is either Error, Hidden, or Drawn
		this.array[i][this.statusIndex].innerHTML = status; //this.array[i][this.statusIndex].set("value", status);
	},
	changedColor: function(){
		// summary:
		//	make the color of the chart the new color
		//	the context is changed to the colorPalette, and a reference to chart was added to it a an attribute
		var chart = this.get("chart");
		var colorBoxFieldset = this.get("colorBox");
		for(var i = 0; i < chart.series.length; i++){
			if(chart.series[i]["stroke"]){
				if(chart.series[i].stroke["color"]){
					chart.series[i]["stroke"].color = this.get("value");
					chart.dirty = true;
				}
			}
		}
		chart.render();
		dojo.style(colorBoxFieldset, {backgroundColor:this.get("value")});
	},
	makeDirty: function(){
		// summary:
		//	if something in the window options is changed, this is called
		this.dirty = true;
	},
	checkDirty1: function(){
		// summary:
		//	to stay in sync with onChange, checkDirty is called with a timeout
		setTimeout(dojo.hitch(this, 'checkDirty'), 0);
	},
	checkDirty: function(){
		// summary:
		//	adjust all charts in this.array according to any changes in window options
		if(this.dirty){
			// change the axes of all charts if it is dirty
			for(var i = 0; i < this.rowCount; i++){
				this.array[i][this.chartIndex].removeAxis("x");
				this.array[i][this.chartIndex].removeAxis("y");
				this.addXYAxes(this.array[i][this.chartIndex]);
			}
			this.onDraw();
		}
		this.dirty = false;
	},
	postCreate: function(){
		// summary
		//	add Event handlers, some additional attributes, etc
		this.inherited(arguments);// this is super class postCreate
		this.createFunc.set("onClick", dojo.hitch(this, 'createFunction'));

		this.selectAllButton.set("onClick", dojo.hitch(this, 'selectAll'));
		this.deselectAllButton.set("onClick", dojo.hitch(this, 'deselectAll'));

		this.drawButton.set("onClick", dojo.hitch(this, 'onDraw'));
		this.eraseButton.set("onClick", dojo.hitch(this, 'onErase'));
		this.deleteButton.set("onClick", dojo.hitch(this, 'onDelete'));

		this.dirty = false;
		this.graphWidth.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.graphHeight.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.graphMaxX.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.graphMinX.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.graphMaxY.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.graphMinY.set("onChange", dojo.hitch(this, 'makeDirty'));
		this.windowOptionsInside.set("onClose", dojo.hitch(this, 'checkDirty1'));

		this.funcNumber = 0;
		this.rowCount = 0;
		this.array = [];

	},
	startup: function(){
		// summary
		//	make sure the parent has a close button if it needs to be able to close
		this.inherited(arguments);// this is super class startup
		// close is only valid if the parent is a widget with a close function
		var parent = dijit.getEnclosingWidget(this.domNode.parentNode);
		if(parent && typeof parent.close == "function"){
			this.closeButton.set("onClick", dojo.hitch(parent, 'close'));
		}else{
			dojo.style(this.closeButton.domNode, "display", "none"); // hide the button
		}
		// add one row at the start
		this.createFunction();

		// make the graph bounds appear initially
		this.array[0][this.checkboxIndex].set("checked", true);
		this.onDraw();
		this.erase(0);
		this.array[0][this.expressionIndex].value = "";
	}
});

(function(){
	// summary
	//	provide static functions for Grapher
	var
		epsilon = 1e-15 / 9,
		bigNumber = 1e200,
		log2 = Math.log(2),
		defaultParams = {graphNumber:0, fOfX:true, color:{stroke:"black"}};

	dojox.calc.Grapher.draw = function(/*Chart2D*/ chart, /*Function*/ functionToGraph, params){
		// summary
		//	graph a chart with the given function.
		// params
		//	chart is a dojox.charting.Chart2D object, functionToGraph is a function with one numeric parameter (x or y typically)
		//	and params is an Object the can contain the number of the graph in the chart it is (an integer), a boolean saying if the functionToGraph is a function of x (otherwise y)
		//	and the color, which is an object with a stroke with a color's name eg: color:{stroke:"black"}

		params = dojo.mixin({}, defaultParams, params);
		chart.fullGeometry();
		var x;
		var y;
		var points;
		if(params.fOfX==true){
			x = 'x';
			y = 'y';
			points = dojox.calc.Grapher.generatePoints(functionToGraph, x, y, chart.axes.x.scaler.bounds.span, chart.axes.x.scaler.bounds.lower, chart.axes.x.scaler.bounds.upper, chart.axes.y.scaler.bounds.lower, chart.axes.y.scaler.bounds.upper);
		}else{
			x = 'y';
			y = 'x';
			points = dojox.calc.Grapher.generatePoints(functionToGraph, x, y, chart.axes.y.scaler.bounds.span, chart.axes.y.scaler.bounds.lower, chart.axes.y.scaler.bounds.upper, chart.axes.x.scaler.bounds.lower, chart.axes.x.scaler.bounds.upper);
		}

		var i = 0;

		if(points.length > 0){
			for(; i < points.length; i++){
				if(points[i].length>0){
					chart.addSeries("Series "+params.graphNumber+"_"+i, points[i], params.color);
				}
			}
		}
		// you only need to remove the excess i's
		var name = "Series "+params.graphNumber+"_"+i;
		while(name in chart.runs){
			chart.removeSeries(name);
			i++;
			name = "Series "+params.graphNumber+"_"+i;
		}
		chart.render();
		return points;
	}

	dojox.calc.Grapher.generatePoints = function(/*Function*/ funcToGraph, /*String*/ x, /*String*/ y, /*Number*/ width, /*Number*/ minX, /*Number*/ maxX, /*Number*/ minY, /*Number*/ maxY){
		// summary:
		//	create the points with information about the graph.
		// params:
		//	funcToGraph is a function with one numeric parameter (x or y typically)
		//	x and y are Strings which always have the values of "x" or "y".  If y="x" and x="y" then it is creating points for the function as though it was a function of y
		//	Number minX, Number maxX, Number minY, Number maxY are all bounds of the chart.  If x="y" then maxY should be the maximum bound of x rather than y
		//	Number width is the pixel width of the chart
		// output:
		//	an array of arrays of points
		var pow2 = (1 << Math.ceil(Math.log(width) / log2));
		var
			dx = (maxX - minX) / pow2, // divide by 2^n instead of width to avoid loss of precision
			points = [], // [{x:value, y:value2},...]
			series = 0,
			slopeTrend,
			slopeTrendTemp;

		points[series] = [];

		var i = minX, k, p;
		for(var counter = 0; counter <= pow2; i += dx, counter++){
			p = {};
			p[x] = i;
			p[y] = funcToGraph({_name:x, _value:i, _graphing:true});//funcToGraph(i);
			if(p[x] == null || p[y] == null){
				return {};// someone pushed cancel in the val code
			}
			if(isNaN(p[y]) || isNaN(p[x])){
				continue;
			}
			points[series].push(p);

			if(points[series].length == 3){
				slopeTrend = getSlopePairTrend(slope(points[series][points[series].length - 3], points[series][points[series].length-2]), slope(points[series][points[series].length-2], points[series][points[series].length-1]));
				continue;
			}
			if(points[series].length < 4){
				continue;
			}

			slopeTrendTemp = getSlopePairTrend(slope(points[series][points[series].length - 3], points[series][points[series].length-2]), slope(points[series][points[series].length-2], points[series][points[series].length-1]));
			if(slopeTrend.inc != slopeTrendTemp.inc || slopeTrend.pos != slopeTrendTemp.pos){
				// var a = asymptoteSearch(funcToGraph, points[series][points[series].length - 2], points[series][points[series].length-1]);
				var a = asymptoteSearch(funcToGraph, points[series][points[series].length - 3], points[series][points[series].length-1]);
				p = points[series].pop();
				// this pop was added after changing the var a line above
				points[series].pop();
				for(var j = 0; j < a[0].length; j++){
					points[series].push(a[0][j]);
				}
				for(k = 1; k < a.length; k++){
					points[++series] = a.pop();
				}
				points[series].push(p);
				slopeTrend = slopeTrendTemp;
			}
		}
		while(points.length > 1){
			for(k = 0; k < points[1].length; k++){
				if(points[0][points[0].length - 1][x] == points[1][k][x]){
					continue;
				}
				points[0].push(points[1][k]);
			}
			points.splice(1, 1);
		}
		points = points[0];

		// make new series when it goes off the graph
		var s = 0;
		var points2 = [ [] ];
		for(k = 0; k < points.length; k++){
			var x1, y1, b, slope1;
			if(isNaN(points[k][y]) || isNaN(points[k][x])){
				while(isNaN(points[k][y]) || isNaN(points[k][x])){
					points.splice(k, 1);
				}
				points2[++s] = [];
				k--;
			}else if(points[k][y] > maxY || points[k][y] < minY){
				// make the last point's y equal maxY and find a matching x
				if(k > 0 && points[k - 1].y!=minY && points[k - 1].y!=maxY){
					slope1 = slope(points[k - 1], points[k]);
					if(slope1 > bigNumber){
						slope1 = bigNumber;
					}else if(slope1 < -bigNumber){
						slope1 = -bigNumber;
					}
					if(points[k][y] > maxY){
						y1 = maxY;
					}else{
						y1 = minY;
					}
					b = points[k][y] - slope1 * points[k][x];
					x1 = (y1 - b) / slope1;

					p = {};
					p[x] = x1;
					p[y] = funcToGraph(x1);//y1;//

					if(p[y]!=y1){
						p = findMinOrMaxY(funcToGraph, points[k - 1], points[k], y1);
					}

					points2[s].push(p);
					// setup the next series
					points2[++s] = []
				}
				var startK = k;
				while(k < points.length && (points[k][y] > maxY || points[k][y] < minY)){
					k++;
				}
				if(k >= points.length){
					if(points2[s].length == 0){
						points2.splice(s, 1);
					}
					break;
				}
				// connect the end graph
				if(k > 0 && points[k].y != minY && points[k].y != maxY){
					slope1 = slope(points[k - 1], points[k]);
					if(slope1 > bigNumber){
						slope1 = bigNumber;
					}else if(slope1 < -bigNumber){
						slope1 = -bigNumber;
					}
					if(points[k - 1][y] > maxY){
						y1 = maxY;
					}else{
						y1 = minY;
					}
					b = points[k][y] - slope1 * points[k][x];
					x1 = (y1 - b) / slope1;

					p = {};
					p[x] = x1;
					p[y] = funcToGraph(x1);//y1;//
					if(p[y]!=y1){
						p = findMinOrMaxY(funcToGraph, points[k - 1], points[k], y1);
					}
					points2[s].push(p);
					points2[s].push(points[k]);
				}
			}else{
				points2[s].push(points[k]);
			}
		}
		return points2;

		function findMinOrMaxY(funcToGraph, left, right, minMaxY){

			while(left<=right){
				var midX = (left[x]+right[x])/2;
				var mid = {};
				mid[x] = midX;
				mid[y] = funcToGraph(mid[x]);

				if(minMaxY==mid[y]||mid[x]==right[x]||mid[x]==left[x]){
					return mid;
				}

				var moveTowardsLarger = true;
				if(minMaxY<mid[y]){
					moveTowardsLarger = false;
				}

				if(mid[y]<right[y]){
					if(moveTowardsLarger){
						left = mid;
					}else{
						right = mid;
					}
				}else if(mid[y]<left[y]){
					if(!moveTowardsLarger){
						left = mid;
					}else{
						right = mid;
					}
				}
			}
			return NaN;
		}

		function asymptoteSearch(funcToGraph, pointStart, pointStop){
			var
				pointTemp = [ [], [] ],
				left = pointStart,
				right = pointStop,
				midpoint;


			while(left[x] <= right[x]){
				var midX = (left[x] + right[x]) / 2;

				midpoint = {};
				midpoint[x] = midX;
				midpoint[y] = funcToGraph(midX);

				var rx = nextNumber(midpoint[x]);
				var rightPoint = {};
				rightPoint[x] = rx;
				rightPoint[y] = funcToGraph(rx);

				if(Math.abs(rightPoint[y]) >= Math.abs(midpoint[y])){
					pointTemp[0].push(midpoint);
					left = rightPoint;
				}else{
					pointTemp[1].unshift(midpoint);
					if(right[x] == midpoint[x]){
						break;
					}
					right = midpoint;
				}
					
			}
			return pointTemp;
		}

		function getSlopePairTrend(slope1, slope2){
			var
				isInc = false,
				isPos = false;

			if (slope1 < slope2){
				isInc = true;
			}
			if (slope2 > 0){
				isPos = true;
			}
			return { inc: isInc, pos: isPos };
		}

		function nextNumber(v){
			var delta;
			if(v > -1 && v < 1){
				if(v < 0){ // special handling as we approach 0
					if(v >= -epsilon){
						delta = -v; // always stop at 0
					}else{
						delta = v / Math.ceil(v / epsilon); // divide distance to 0 into equal tiny chunks
					}
				}else{
					delta = epsilon;
				}
			}else{
				delta = Math.abs(v) * epsilon;
			}
			return v + delta;
		}

		function slope(p1, p2){
			return (p2[y] - p1[y]) / (p2[x] - p1[x]);
		}
	};
})();


return dojox.calc.Grapher;
});
