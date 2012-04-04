dojo.provide("dojo._base._loader.loader_debug");

//Override dojo.provide, so we can trigger the next
//script tag for the next local module. We can only add one
//at a time because there are browsers that execute script tags
//in the order that the code is received, and not in the DOM order.
dojo.nonDebugProvide = dojo.provide;

dojo.provide = function(resourceName){
	var dbgQueue = dojo["_xdDebugQueue"];
	if(dbgQueue && dbgQueue.length > 0 && resourceName == dbgQueue["currentResourceName"]){
		//Set a timeout so the module can be executed into existence. Normally the
		//dojo.provide call in a module is the first line. Don't want to risk attaching
		//another script tag until the current one finishes executing.
		if(dojo.isAIR){
			window.setTimeout(function(){dojo._xdDebugFileLoaded(resourceName);}, 1);
		}else{
			window.setTimeout(dojo._scopeName + "._xdDebugFileLoaded('" + resourceName + "')", 1);
		}
	}

	return dojo.nonDebugProvide.apply(dojo, arguments);
}

dojo._xdDebugFileLoaded = function(resourceName){

	if(!dojo._xdDebugScopeChecked){
		//If using a scoped dojo, we need to expose dojo as a real global
		//for the debugAtAllCosts stuff to work.
		if(dojo._scopeName != "dojo"){
			window.dojo = window[dojo.config.scopeMap[0][1]];
			window.dijit = window[dojo.config.scopeMap[1][1]];
			window.dojox = window[dojo.config.scopeMap[2][1]];
		}

		dojo._xdDebugScopeChecked = true;
	}
	
	var dbgQueue = dojo._xdDebugQueue;
	
	if(resourceName && resourceName == dbgQueue.currentResourceName){
		dbgQueue.shift();
	}

	if(dbgQueue.length == 0){
		//Check for more modules that need debug loading.
		//dojo._xdWatchInFlight will add more things to the debug
		//queue if they just recently loaded but it was not detected
		//between the dojo._xdWatchInFlight intervals.
		dojo._xdWatchInFlight();
	}

	if(dbgQueue.length == 0){
		dbgQueue.currentResourceName = null;

		//Make sure nothing else is in flight.
		//If something is still in flight, then it still
		//needs to be added to debug queue after it loads.
		for(var param in dojo._xdInFlight){
			if(dojo._xdInFlight[param] === true){
				return;
			}
		}

		dojo._xdNotifyLoaded();
	}else{
		if(resourceName == dbgQueue.currentResourceName){
			dbgQueue.currentResourceName = dbgQueue[0].resourceName;
			var element = document.createElement("script");
			element.type = "text/javascript";
			element.src = dbgQueue[0].resourcePath;
			document.getElementsByTagName("head")[0].appendChild(element);
		}
	}
}
