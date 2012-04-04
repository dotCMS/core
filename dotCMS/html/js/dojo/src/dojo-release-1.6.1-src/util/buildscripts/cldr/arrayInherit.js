/**
 *  The CLDR represents some lists, like month names, as separate entries, and our JSON uses arrays to express them.
 *  For some variants, the best our XSLT can do is translate this to a sparse array with 'undefined' entries.
 *  These entries need to be picked up from the parent locale(s) and copied into the array as necessary(only when the item
 *  is not the source of a locale alias mapping, like 'months-format-abbr' should be ignored if it is the source of
 *  locale alias mapping like 'months-format-abbr@localeAlias' :{'target':"months-format-wide",'bundle':"gregorian"}).
 *  So, this script is responsible for taking all the generated JSON files, and for values which are of type
 *  array and not the source of locale alias mapping, mixing in the parent values with the undefined ones, recursing
 *  all the way to the 'root' locale,and replacing the contents of the file.
 *
 *  this script traverses all locales dir in the given root dir
 *
 *   E.g.(Just for example, the contents are not applicable)
 *   parent locale - "en":
 *    // generated from ldml/main/ *.xml, xpath: ldml/calendars/calendar-ethiopic
 *    ({
 *    	'months-format-abbr':["1","2","3","4","5","6","7","8","9","10","11","12"],
 *    	'dateFormat-long': "yyyy MMMM d",
 *    	'dateTimeFormat': "{1} {0}"
 *    })
 *
 *   child locale - "en-us":
 *    // generated from ldml/main/ *.xml, xpath: ldml/calendars/calendar-ethiopic
 *    ({
 *    	'months-format-abbr':[undefined,undefined,"March",undefined,undefined,"June"],
 *    	'dateFormat-long': "yyyy-MMMM-d"
 *    })
 *
 *   After process, the result will be:
 *    child locale - "en-us":
 *    // generated from ldml/main/ *.xml, xpath: ldml/calendars/calendar-ethiopic
 *    ({
 *    	'months-format-abbr':["1","2","March","4","5","June","7","8","9","10","11","12"],
 *    	'dateFormat-long': "yyyy-MMMM-d"
 *    })
 */

djConfig={baseUrl: "../../../dojo/"};

load("../../../dojo/dojo.js");
load("../jslib/logger.js");
load("../jslib/fileUtil.js");
load("../jslib/buildUtil.js");
load("cldrUtil.js");

dojo.require("dojo.i18n");

var dir = arguments[0];
var logDir = arguments[1];
var logStr = "";

print('arrayInherit.js...');

// limit search to gregorian.js files, which are the only ones to use Array as data type
var fileList = fileUtil.getFilteredFileList(dir, /\/gregorian\.js$/, true);

for(var i= 0; i < fileList.length; i++){
	//Use new String so we get a JS string and not a Java string.
	var jsFileName = new String(fileList[i]);
	var data = null;
	var jsPath = jsFileName.split("/");
	var localeIndex = jsPath.length-2;
	var locale = jsPath[localeIndex];
	if(locale=="nls"){continue;} // don't run on ROOT resource
	var hasChanged = false;
	
	try{
		dojo.i18n._requireLocalization('dojo.cldr', 'gregorian', locale);
		var bundle = dojo.i18n.getLocalization('dojo.cldr', 'gregorian', locale); //flattened bundle
	}catch(e){print(e);/* simply ignore if no bundle found*/}
	
	dojo.i18n._searchLocalePath(locale, true, function(variant){
		var isComplete = false;
		var path = jsPath;
		if(variant=="ROOT"){
			path = path.slice(0, localeIndex);
			path.push(jsPath[localeIndex+1]);
		}else{
			path[localeIndex] = variant;
		}
		var contents;
		try{
			contents = new String(readFile(path.join("/"), "utf-8"));
		}catch(e){
			print(e); //TODO: should we be catching this?
			return false;
		}
		var variantData = dojo.fromJson(contents);
		if(!data){
			data = variantData;
		}else{
			isComplete = true;
			//logStr += locale + "===============================================\n";
			for(prop in data){
				if(dojo.isArray(data[prop])){
					//ignore if the property is an alias source, for alias.js and specialLocale.js
					if(isLocaleAliasSrc(prop, bundle)){
						//logStr += prop + " is alias, ignored\n";
						continue;
					}

					var variantArray = variantData[prop];
					dojo.forEach(data[prop], function(element, index, list){
						if(element === undefined && dojo.isArray(variantArray)){
							list[index] = variantArray[index];
							//logStr += prop + "[" + index + "] undefined, is replaced with " + list[index] + "\n";
							hasChanged = true;
							if(!("index" in list)){
								isComplete = false;
							}
						}
					});
					if(dojo.isArray(variantArray) && variantArray.length > data[prop].length){
						data[prop] = data[prop].concat(variantArray.slice(data[prop].length));
						hasChanged = true;
					}
				}
			}
			//logStr += "\n";
		}
		return isComplete;
	});
	if(hasChanged){
		fileUtil.saveUtf8File(jsFileName, "(" + dojo.toJson(data, true) + ")");
	}
}

//fileUtil.saveUtf8File(logDir + '/arrayInherit.log',logStr+'\n');