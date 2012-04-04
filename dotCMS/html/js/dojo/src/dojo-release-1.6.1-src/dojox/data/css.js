define("dojox/data/css", ["dojo", "dojox"], function(dojo, dojox) {

dojox.data.css.rules = {};
		
dojox.data.css.rules.forEach = function(fn,ctx,context){
	if(context){
		var _processSS = function(styleSheet){
			//iterate across rules in the stylesheet
			dojo.forEach(styleSheet[styleSheet.cssRules?"cssRules":"rules"], function(rule){
				if(!rule.type || rule.type !== 3){// apply fn to current rule with approp ctx. rule is arg (all browsers)
					var href = "";
					if(styleSheet && styleSheet.href){
						href = styleSheet.href;
					}
					fn.call(ctx?ctx:this,rule, styleSheet, href);
				}
			});
			//process any child stylesheets
		};
		dojo.forEach(context,_processSS);
	}
};
dojox.data.css.findStyleSheets = function(sheets){
	// Takes an array of stylesheet paths and finds the currently loaded StyleSheet objects matching
	// those names
	var sheetObjects = [];
	var _processSS = function(styleSheet){
		var s = dojox.data.css.findStyleSheet(styleSheet);
		if(s){
			dojo.forEach(s, function(sheet){
				if(dojo.indexOf(sheetObjects, sheet) === -1){
					sheetObjects.push(sheet);
				}
			});
		}
	};
	dojo.forEach(sheets, _processSS);
	return sheetObjects;
};
dojox.data.css.findStyleSheet = function(sheet){
	// Takes a stylesheet path and finds the currently loaded StyleSheet objects matching
	// those names (and it's parent(s), if it is imported from another)
	var sheetObjects = [];
	if(sheet.charAt(0) === '.'){
		sheet = sheet.substring(1);
	}
	var _processSS = function(styleSheet){
		if(styleSheet.href && styleSheet.href.match(sheet)){
			sheetObjects.push(styleSheet);
			return true;
		}
		if(styleSheet.imports){
			return dojo.some(styleSheet.imports, function(importedSS){ //IE stylesheet has imports[] containing @import'ed rules
				//console.debug("Processing IE @import rule",importedSS);
				return _processSS(importedSS);
			});
		}
		//iterate across rules in the stylesheet
		return dojo.some(styleSheet[styleSheet.cssRules?"cssRules":"rules"], function(rule){
			if(rule.type && rule.type === 3 && _processSS(rule.styleSheet)){// CSSImportRule (firefox)
				//sheetObjects.push(styleSheet);
				return true;
			}
			return false;
		});
	};
	dojo.some(document.styleSheets, _processSS);
	return sheetObjects;
};
dojox.data.css.determineContext = function(initialStylesheets){
	// Takes an array of stylesheet paths and returns an array of all stylesheets that fall in the
	// given context.  If no paths are given, all stylesheets are returned.
	var ret = [];
	if(initialStylesheets && initialStylesheets.length > 0){
		initialStylesheets = dojox.data.css.findStyleSheets(initialStylesheets);
	}else{
		initialStylesheets = document.styleSheets;
	}
	var _processSS = function(styleSheet){
		ret.push(styleSheet);
		if(styleSheet.imports){
			dojo.forEach(styleSheet.imports, function(importedSS){ //IE stylesheet has imports[] containing @import'ed rules
				//console.debug("Processing IE @import rule",importedSS);
				_processSS(importedSS);
			});
		}
		//iterate across rules in the stylesheet
		dojo.forEach(styleSheet[styleSheet.cssRules?"cssRules":"rules"], function(rule){
			if(rule.type && rule.type === 3){// CSSImportRule (firefox)
				_processSS(rule.styleSheet);
			}
		});
	};
	dojo.forEach(initialStylesheets,_processSS);
	return ret;
};

return dojox.data.css;

});
