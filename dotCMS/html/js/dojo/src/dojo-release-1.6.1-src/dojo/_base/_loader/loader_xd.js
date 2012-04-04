//Cross-domain resource loader.
dojo.provide("dojo._base._loader.loader_xd");

dojo._xdReset = function(){
	//summary: Internal xd loader function. Resets the xd state.

	//This flag indicates where or not we have crossed into xdomain territory. Once any resource says
	//it is cross domain, then the rest of the resources have to be treated as xdomain because we need
	//to evaluate resources in order. If there is a xdomain resource followed by a xhr resource, we can't load
	//the xhr resource until the one before it finishes loading. The text of the xhr resource will be converted
	//to match the format for a xd resource and put in the xd load queue.
	dojo._isXDomain = dojo.config.useXDomain || false;

	dojo._xdClearInterval();
	dojo._xdInFlight = {};
	dojo._xdOrderedReqs = [];
	dojo._xdDepMap = {};
	dojo._xdContents = [];
	dojo._xdDefList = [];
}

dojo._xdClearInterval = function(){
	//summary: Internal xd loader function.
	//Clears the interval timer used to check on the
	//status of in-flight xd module resource requests.
	if(dojo._xdTimer){
		clearInterval(dojo._xdTimer);
		dojo._xdTimer = 0;
	}
}


//Call reset immediately to set the state.
dojo._xdReset();

dojo._xdCreateResource = function(/*String*/contents, /*String*/resourceName, /*String*/resourcePath){
	//summary: Internal xd loader function. Creates an xd module source given an
	//non-xd module contents.

	//Remove comments. Not perfect, but good enough for dependency resolution.
	var depContents = contents.replace(/(\/\*([\s\S]*?)\*\/|\/\/(.*)$)/mg , "");

	//Find dependencies.
	var deps = [];
    var depRegExp = /dojo.(require|requireIf|provide|requireAfterIf|platformRequire|requireLocalization)\s*\(([\w\W]*?)\)/mg;
    var match;
	while((match = depRegExp.exec(depContents)) != null){
		if(match[1] == "requireLocalization"){
			//Need to load the local bundles asap, since they are not
			//part of the list of modules watched for loading.
			eval(match[0]);
		}else{
			deps.push('"' + match[1] + '", ' + match[2]);
		}
	}

	//Create resource object and the call to _xdResourceLoaded.
	var output = [];
	output.push(dojo._scopeName + "._xdResourceLoaded(function(" + dojo._scopePrefixArgs + "){\n");

	//See if there are any dojo.loadInit calls
	var loadInitCalls = dojo._xdExtractLoadInits(contents);
	if(loadInitCalls){
		//Adjust fileContents since extractLoadInits removed something.
		contents = loadInitCalls[0];
		
		//Add any loadInit calls to the top of the xd file.
		for(var i = 1; i < loadInitCalls.length; i++){
			output.push(loadInitCalls[i] + ";\n");
		}
	}

	output.push("return {");

	//Add dependencies
	if(deps.length > 0){
		output.push("depends: [");
		for(i = 0; i < deps.length; i++){
			if(i > 0){
				output.push(",\n");
			}
			output.push("[" + deps[i] + "]");
		}
		output.push("],");
	}

	//Add the contents of the file inside a function.
	//Pass in scope arguments so we can support multiple versions of the
	//same module on a page.
	output.push("\ndefineResource: function(" + dojo._scopePrefixArgs + "){");

	//Don't put in the contents in the debugAtAllCosts case
	//since the contents may have syntax errors. Let those
	//get pushed up when the script tags are added to the page
	//in the debugAtAllCosts case.
	if(!dojo.config["debugAtAllCosts"] || resourceName == "dojo._base._loader.loader_debug"){
		output.push(contents);
	}
	//Add isLocal property so we know if we have to do something different
	//in debugAtAllCosts situations.
	output.push("\n}, resourceName: '" + resourceName + "', resourcePath: '" + resourcePath + "'};});");
	
	return output.join(""); //String
}

dojo._xdExtractLoadInits = function(/*String*/fileContents){
	//Extracts
	var regexp = /dojo.loadInit\s*\(/g;
	regexp.lastIndex = 0;

	var parenRe = /[\(\)]/g;
	parenRe.lastIndex = 0;

	var results = [];
	var matches;
	while((matches = regexp.exec(fileContents))){
		//Find end of the call by finding the matching end paren
		parenRe.lastIndex = regexp.lastIndex;
		var matchCount = 1;
		var parenMatch;
		while((parenMatch = parenRe.exec(fileContents))){
			if(parenMatch[0] == ")"){
				matchCount -= 1;
			}else{
				matchCount += 1;
			}
			if(matchCount == 0){
				break;
			}
		}
		
		if(matchCount != 0){
			throw "unmatched paren around character " + parenRe.lastIndex + " in: " + fileContents;
		}

		//Put the master matching string in the results.
		var startIndex = regexp.lastIndex - matches[0].length;
		results.push(fileContents.substring(startIndex, parenRe.lastIndex));

		//Remove the matching section.
		var remLength = parenRe.lastIndex - startIndex;
		fileContents = fileContents.substring(0, startIndex) + fileContents.substring(parenRe.lastIndex, fileContents.length);

		//Move the master regexp past the last matching paren point.
		regexp.lastIndex = parenRe.lastIndex - remLength;

		regexp.lastIndex = parenRe.lastIndex;
	}

	if(results.length > 0){
		results.unshift(fileContents);
	}

	return (results.length ? results : null);
}

dojo._xdIsXDomainPath = function(/*string*/relpath) {
    //summary: Figure out whether the path is local or x-domain
	//If there is a colon before the first / then, we have a URL with a protocol.
    
	var colonIndex = relpath.indexOf(":");
	var slashIndex = relpath.indexOf("/");

	if(colonIndex > 0 && colonIndex < slashIndex || relpath.indexOf("//") === 0){
		return true;
	}else{
		//Is the base script URI-based URL a cross domain URL?
		//If so, then the relpath will be evaluated relative to
		//baseUrl, and therefore qualify as xdomain.
		//Only treat it as xdomain if the page does not have a
		//host (file:// url), if the baseUrl does not match the
		//current window's domain, or if the baseUrl starts with //.
		//If baseUrl starts with // then it probably means that xdomain
		//is wanted since it is such a specific path request. This is not completely robust,
		//but something more robust would require normalizing the protocol on baseUrl and on the location
		//to see if they differ. However, that requires more code, and // as a start path is unusual.
		var url = dojo.baseUrl;
		colonIndex = url.indexOf(":");
		slashIndex = url.indexOf("/");
		if(url.indexOf("//") === 0 || (colonIndex > 0 && colonIndex < slashIndex && (!location.host || url.indexOf("http://" + location.host) != 0))){
			return true;
		}
	}
    return false;
}

dojo._loadPath = function(/*String*/relpath, /*String?*/module, /*Function?*/cb){
	//summary: Internal xd loader function. Overrides loadPath() from loader.js.
	//xd loading requires slightly different behavior from loadPath().

	var currentIsXDomain = dojo._xdIsXDomainPath(relpath);
    dojo._isXDomain |= currentIsXDomain;

	var uri = ((relpath.charAt(0) == '/' || relpath.match(/^\w+:/)) ? "" : dojo.baseUrl) + relpath;

	try{
		return ((!module || dojo._isXDomain) ? dojo._loadUri(uri, cb, currentIsXDomain, module) : dojo._loadUriAndCheck(uri, module, cb)); //Boolean
	}catch(e){
		console.error(e);
		return false; //Boolean
	}
}

dojo._xdCharSet = "utf-8";

dojo._loadUri = function(/*String*/uri, /*Function?*/cb, /*boolean*/currentIsXDomain, /*String?*/module){
	//summary: Internal xd loader function. Overrides loadUri() from loader.js.
	//		xd loading requires slightly different behavior from loadPath().
	//description: Wanted to override getText(), but it is used by
	//		the widget code in too many, synchronous ways right now.
	if(dojo._loadedUrls[uri]){
		return 1; //Boolean
	}

	//Add the module (resource) to the list of modules.
	//Only do this work if we have a modlue name. Otherwise,
	//it is a non-xd i18n bundle, which can load immediately and does not
	//need to be tracked. Also, don't track dojo.i18n, since it is a prerequisite
	//and will be loaded correctly if we load it right away: it has no dependencies.
	if(dojo._isXDomain && module && module != "dojo.i18n"){
		dojo._xdOrderedReqs.push(module);

		//Add to waiting resources if it is an xdomain resource.
		//Don't add non-xdomain i18n bundles, those get evaled immediately.
		if(currentIsXDomain || uri.indexOf("/nls/") == -1){
			dojo._xdInFlight[module] = true;

			//Increment inFlightCount
			//This will stop the modulesLoaded from firing all the way.
			dojo._inFlightCount++;
		}

		//Start timer
		if(!dojo._xdTimer){
			if(dojo.isAIR){
				dojo._xdTimer = setInterval(function(){dojo._xdWatchInFlight();}, 100);
			}else{
				dojo._xdTimer = setInterval(dojo._scopeName + "._xdWatchInFlight();", 100);
			}
		}
		dojo._xdStartTime = (new Date()).getTime();
	}

	if (currentIsXDomain){
		//Fix name to be a .xd.fileextension name.
		var lastIndex = uri.lastIndexOf('.');
		if(lastIndex <= 0){
			lastIndex = uri.length - 1;
		}

		var xdUri = uri.substring(0, lastIndex) + ".xd";
		if(lastIndex != uri.length - 1){
			xdUri += uri.substring(lastIndex, uri.length);
		}

		if (dojo.isAIR){
			xdUri = xdUri.replace("app:/", "/");
		}

		//Add to script src
		var element = document.createElement("script");
		element.type = "text/javascript";
		if(dojo._xdCharSet){
			element.charset = dojo._xdCharSet;
		}
		element.src = xdUri;
		if(!dojo.headElement){
			dojo._headElement = document.getElementsByTagName("head")[0];

			//Head element may not exist, particularly in html
			//html 4 or tag soup cases where the page does not
			//have a head tag in it. Use html element, since that will exist.
			//Seems to be an issue mostly with Opera 9 and to lesser extent Safari 2
			if(!dojo._headElement){
				dojo._headElement = document.getElementsByTagName("html")[0];
			}
		}
		dojo._headElement.appendChild(element);
	}else{
		var contents = dojo._getText(uri, null, true);
		if(contents == null){ return 0; /*boolean*/}
		
		//If this is not xdomain, or if loading a i18n resource bundle, then send it down
		//the normal eval/callback path.
		if(dojo._isXDomain
			&& uri.indexOf("/nls/") == -1
			&& module != "dojo.i18n"){
			var res = dojo._xdCreateResource(contents, module, uri);
			dojo.eval(res);
		}else{
			if(cb){
				contents = '('+contents+')';
			}else{
				//Only do the scoping if no callback. If a callback is specified,
				//it is most likely the i18n bundle stuff.
				contents = dojo._scopePrefix + contents + dojo._scopeSuffix;
			}
			var value = dojo["eval"](contents+"\r\n//@ sourceURL="+uri);
			if(cb){
				cb(value);
			}
		}
	}

	//These steps are done in the non-xd loader version of this function.
	//Maintain these steps to fit in with the existing system.
	dojo._loadedUrls[uri] = true;
	dojo._loadedUrls.push(uri);
	return true; //Boolean
}

dojo._xdResourceLoaded = function(/*Object*/res){
	//summary: Internal xd loader function. Called by an xd module resource when
	//it has been loaded via a script tag.
	
	//Evaluate the function with scopeArgs for multiversion support.
	res = res.apply(dojo.global, dojo._scopeArgs);

	//Work through dependencies.
	var deps = res.depends;
	var requireList = null;
	var requireAfterList = null;
	var provideList = [];
	if(deps && deps.length > 0){
		var dep = null;
		var insertHint = 0;
		var attachedResource = false;
		for(var i = 0; i < deps.length; i++){
			dep = deps[i];

			//Look for specific dependency indicators.
			if (dep[0] == "provide"){
				provideList.push(dep[1]);
			}else{
				if(!requireList){
					requireList = [];
				}
				if(!requireAfterList){
					requireAfterList = [];
				}

				var unpackedDeps = dojo._xdUnpackDependency(dep);
				if(unpackedDeps.requires){
					requireList = requireList.concat(unpackedDeps.requires);
				}
				if(unpackedDeps.requiresAfter){
					requireAfterList = requireAfterList.concat(unpackedDeps.requiresAfter);
				}
			}

			//Call the dependency indicator to allow for the normal dojo setup.
			//Only allow for one dot reference, for the i18n._preloadLocalizations calls
			//(and maybe future, one-dot things).
			var depType = dep[0];
			var objPath = depType.split(".");
			if(objPath.length == 2){
				dojo[objPath[0]][objPath[1]].apply(dojo[objPath[0]], dep.slice(1));
			}else{
				dojo[depType].apply(dojo, dep.slice(1));
			}
		}


		//If loading the debugAtAllCosts module, eval it right away since we need
		//its functions to properly load the other modules.
		if(provideList.length == 1 && provideList[0] == "dojo._base._loader.loader_debug"){
			res.defineResource(dojo);
		}else{
			//Save off the resource contents for definition later.
			var contentIndex = dojo._xdContents.push({
					content: res.defineResource,
					resourceName: res["resourceName"],
					resourcePath: res["resourcePath"],
					isDefined: false
				}) - 1;
	
			//Add provide/requires to dependency map.
			for(i = 0; i < provideList.length; i++){
				dojo._xdDepMap[provideList[i]] = { requires: requireList, requiresAfter: requireAfterList, contentIndex: contentIndex };
			}
		}

		//Now update the inflight status for any provided resources in this loaded resource.
		//Do this at the very end (in a *separate* for loop) to avoid shutting down the
		//inflight timer check too soon.
		for(i = 0; i < provideList.length; i++){
			dojo._xdInFlight[provideList[i]] = false;
		}
	}
}

dojo._xdLoadFlattenedBundle = function(/*String*/moduleName, /*String*/bundleName, /*String?*/locale, /*Object*/bundleData){
	//summary: Internal xd loader function. Used when loading
	//a flattened localized bundle via a script tag.
	locale = locale || "root";
	var jsLoc = dojo.i18n.normalizeLocale(locale).replace('-', '_');
 	var bundleResource = [moduleName, "nls", bundleName].join(".");
	var bundle = dojo["provide"](bundleResource);
	bundle[jsLoc] = bundleData;
	
	//Assign the bundle for the original locale(s) we wanted.
	var mapName = [moduleName, jsLoc, bundleName].join(".");
	var bundleMap = dojo._xdBundleMap[mapName];
	if(bundleMap){
		for(var param in bundleMap){
			bundle[param] = bundleData;
		}
	}
};


dojo._xdInitExtraLocales = function(){
	// Simulate the extra locale work that dojo.requireLocalization does.

	var extra = dojo.config.extraLocale;
	if(extra){
		if(!extra instanceof Array){
			extra = [extra];
		}

		dojo._xdReqLoc = dojo.xdRequireLocalization;
		dojo.xdRequireLocalization = function(m, b, locale, fLocales){
			dojo._xdReqLoc(m,b,locale, fLocales);
			if(locale){return;}
			for(var i=0; i<extra.length; i++){
				dojo._xdReqLoc(m,b,extra[i], fLocales);
			}
		};
	}
}

dojo._xdBundleMap = {};

dojo.xdRequireLocalization = function(/*String*/moduleName, /*String*/bundleName, /*String?*/locale, /*String*/availableFlatLocales){
	//summary: Internal xd loader function. The xd version of dojo.requireLocalization.
	

	//Account for allowing multiple extra locales. Do this here inside the function
	//since dojo._xdInitExtraLocales() depends on djConfig being set up, but that only
	//happens after hostenv_browser runs. loader_xd has to come before hostenv_browser
	//though since hostenv_browser can do a dojo.require for the debug module.
	if(dojo._xdInitExtraLocales){
		dojo._xdInitExtraLocales();
		dojo._xdInitExtraLocales = null;
		dojo.xdRequireLocalization.apply(dojo, arguments);
		return;
	}

	var locales = availableFlatLocales.split(",");
	
	//Find the best-match locale to load.
	//Assumes dojo.i18n has already been loaded. This is true for xdomain builds,
	//since it is included in dojo.xd.js.
	var jsLoc = dojo.i18n.normalizeLocale(locale);

	var bestLocale = "";
	for(var i = 0; i < locales.length; i++){
		//Locale must match from start of string.
		if(jsLoc.indexOf(locales[i]) == 0){
			if(locales[i].length > bestLocale.length){
				bestLocale = locales[i];
			}
		}
	}

	var fixedBestLocale = bestLocale.replace('-', '_');
	//See if the bundle we are going to use is already loaded.
 	var bundleResource = dojo.getObject([moduleName, "nls", bundleName].join("."));
	if(!bundleResource || !bundleResource[fixedBestLocale]){
		//Need to remember what locale we wanted and which one we actually use.
		//Then when we load the one we are actually using, use that bundle for the one
		//we originally wanted.
		var mapName = [moduleName, (fixedBestLocale||"root"), bundleName].join(".");
		var bundleMap = dojo._xdBundleMap[mapName];
		if(!bundleMap){
			bundleMap = dojo._xdBundleMap[mapName] = {};
		}
		bundleMap[jsLoc.replace('-', '_')] = true;
		
		//Do just a normal dojo.require so the resource tracking stuff works as usual.
		dojo.require(moduleName + ".nls" + (bestLocale ? "." + bestLocale : "") + "." + bundleName);
	}
}

// Replace dojo.requireLocalization with a wrapper
dojo._xdRealRequireLocalization = dojo.requireLocalization;
dojo.requireLocalization = function(/*String*/moduleName, /*String*/bundleName, /*String?*/locale, /*String*/availableFlatLocales){
    // summary: loads a bundle intelligently based on whether the module is
    // local or xd. Overrides the local-case implementation.
    
    var modulePath = dojo.moduleUrl(moduleName).toString();
    if (dojo._xdIsXDomainPath(modulePath)) {
        // call cross-domain loader
        return dojo.xdRequireLocalization.apply(dojo, arguments);
    } else {
        // call local-loader
        return dojo._xdRealRequireLocalization.apply(dojo, arguments);
    }
}

//This is a bit brittle: it has to know about the dojo methods that deal with dependencies
//It would be ideal to intercept the actual methods and do something fancy at that point,
//but I have concern about knowing which provide to match to the dependency in that case,
//since scripts can load whenever they want, and trigger new calls to dojo._xdResourceLoaded().
dojo._xdUnpackDependency = function(/*Array*/dep){
	//summary: Internal xd loader function. Determines what to do with a dependency
	//that was listed in an xd version of a module contents.

	//Extract the dependency(ies).
	var newDeps = null;
	var newAfterDeps = null;
	switch(dep[0]){
		case "requireIf":
		case "requireAfterIf":
			//First arg (dep[1]) is the test. Depedency is dep[2].
			if(dep[1] === true){
				newDeps = [{name: dep[2], content: null}];
			}
			break;
		case "platformRequire":
			var modMap = dep[1];
			var common = modMap["common"]||[];
			newDeps = (modMap[dojo.hostenv.name_]) ? common.concat(modMap[dojo.hostenv.name_]||[]) : common.concat(modMap["default"]||[]);
			//Flatten the array of arrays into a one-level deep array.
			//Each result could be an array of 3 elements  (the 3 arguments to dojo.require).
			//We only need the first one.
			if(newDeps){
				for(var i = 0; i < newDeps.length; i++){
					if(newDeps[i] instanceof Array){
						newDeps[i] = {name: newDeps[i][0], content: null};
					}else{
						newDeps[i] = {name: newDeps[i], content: null};
					}
				}
			}
			break;
		case "require":
			//Just worry about dep[1]
			newDeps = [{name: dep[1], content: null}];
			break;
		case "i18n._preloadLocalizations":
			//We can eval these immediately, since they load i18n bundles.
			//Since i18n bundles have no dependencies, whenever they are loaded
			//in a script tag, they are evaluated immediately, so we do not have to
			//treat them has an explicit dependency for the dependency mapping.
			//We can call it immediately since dojo.i18n is part of dojo.xd.js.
			dojo.i18n._preloadLocalizations.apply(dojo.i18n._preloadLocalizations, dep.slice(1));
			break;
	}

	//The requireIf and requireAfterIf needs to be evaluated after the current resource is evaluated.
	if(dep[0] == "requireAfterIf" || dep[0] == "requireIf"){
		newAfterDeps = newDeps;
		newDeps = null;
	}
	return {requires: newDeps, requiresAfter: newAfterDeps}; //Object
}

dojo._xdWalkReqs = function(){
	//summary: Internal xd loader function.
	//Walks the requires and evaluates module resource contents in
	//the right order.
	var reqChain = null;
	var req;
	for(var i = 0; i < dojo._xdOrderedReqs.length; i++){
		req = dojo._xdOrderedReqs[i];
		if(dojo._xdDepMap[req]){
			reqChain = [req];
			reqChain[req] = true; //Allow for fast lookup of the req in the array
			dojo._xdEvalReqs(reqChain);
		}
	}
}

dojo._xdEvalReqs = function(/*Array*/reqChain){
	//summary: Internal xd loader function.
	//Does a depth first, breadth second search and eval of required modules.
	while(reqChain.length > 0){
		var req = reqChain[reqChain.length - 1];
		var res = dojo._xdDepMap[req];
		var i, reqs, nextReq;
		if(res){
			//Trace down any requires for this resource.
			//START dojo._xdTraceReqs() inlining for small Safari 2.0 call stack
			reqs = res.requires;
			if(reqs && reqs.length > 0){
				for(i = 0; i < reqs.length; i++){
					nextReq = reqs[i].name;
					if(nextReq && !reqChain[nextReq]){
						//New req depedency. Follow it down.
						reqChain.push(nextReq);
						reqChain[nextReq] = true;
						dojo._xdEvalReqs(reqChain);
					}
				}
			}
			//END dojo._xdTraceReqs() inlining for small Safari 2.0 call stack

			//Evaluate the resource.
			var contents = dojo._xdContents[res.contentIndex];
			if(!contents.isDefined){
				var content = contents.content;
				content["resourceName"] = contents["resourceName"];
				content["resourcePath"] = contents["resourcePath"];
				dojo._xdDefList.push(content);
				contents.isDefined = true;
			}
			dojo._xdDepMap[req] = null;

			//Trace down any requireAfters for this resource.
			//START dojo._xdTraceReqs() inlining for small Safari 2.0 call stack
			reqs = res.requiresAfter;
			if(reqs && reqs.length > 0){
				for(i = 0; i < reqs.length; i++){
					nextReq = reqs[i].name;
					if(nextReq && !reqChain[nextReq]){
						//New req depedency. Follow it down.
						reqChain.push(nextReq);
						reqChain[nextReq] = true;
						dojo._xdEvalReqs(reqChain);
					}
				}
			}
			//END dojo._xdTraceReqs() inlining for small Safari 2.0 call stack
		}

		//Done with that require. Remove it and go to the next one.
		reqChain.pop();
	}
}

dojo._xdWatchInFlight = function(){
	//summary: Internal xd loader function.
	//Monitors in-flight requests for xd module resources.

	var noLoads = "";
	var waitInterval = (dojo.config.xdWaitSeconds || 15) * 1000;
	var expired = (dojo._xdStartTime + waitInterval) < (new Date()).getTime();

	//If any xdInFlight are true, then still waiting for something to load.
	//Come back later. If we timed out, report the things that did not load.
	for(var param in dojo._xdInFlight){
		if(dojo._xdInFlight[param] === true){
			if(expired){
				noLoads += param + " ";
			}else{
				return;
			}
		}
	}

	//All done. Clean up and notify.
	dojo._xdClearInterval();

	if(expired){
		throw "Could not load cross-domain resources: " + noLoads;
	}

	dojo._xdWalkReqs();
	
	var defLength = dojo._xdDefList.length;
	for(var i= 0; i < defLength; i++){
		var content = dojo._xdDefList[i];
		if(dojo.config["debugAtAllCosts"] && content["resourceName"]){
			if(!dojo["_xdDebugQueue"]){
				dojo._xdDebugQueue = [];
			}
			dojo._xdDebugQueue.push({resourceName: content.resourceName, resourcePath: content.resourcePath});
		}else{
			//Evaluate the resource to bring it into being.
			//Pass in scope args to allow multiple versions of modules in a page.
			content.apply(dojo.global, dojo._scopeArgs);
		}
	}

	//Evaluate any resources that were not evaled before.
	//This normally shouldn't happen with proper dojo.provide and dojo.require
	//usage, but providing it just in case. Note that these may not be executed
	//in the original order that the developer intended.
	for(i = 0; i < dojo._xdContents.length; i++){
		var current = dojo._xdContents[i];
		if(current.content && !current.isDefined){
			//Pass in scope args to allow multiple versions of modules in a page.
			current.content.apply(dojo.global, dojo._scopeArgs);
		}
	}

	//Clean up for the next round of xd loading.
	dojo._xdReset();

	if(dojo["_xdDebugQueue"] && dojo._xdDebugQueue.length > 0){
		dojo._xdDebugFileLoaded();
	}else{
		dojo._xdNotifyLoaded();
	}
}

dojo._xdNotifyLoaded = function(){
	//Clear inflight count so we will finally do finish work.

	//Just having a legitimate status (true or false) for an inflight item
	//means that it is still being processed. Do the typeof test
	//to avoid bad JavaScript that might tinker with Object.prototype.
	for(var prop in dojo._xdInFlight){
		if(typeof dojo._xdInFlight[prop] == "boolean"){
			return;
		}
	}

	dojo._inFlightCount = 0;

	//Only trigger call loaded if dj_load_init has run.
	if(dojo._initFired && !dojo._loadNotifying){
		dojo._callLoaded();
	}
}
