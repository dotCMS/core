/*
	_testCommon.js - a simple module to be included in dijit test pages to allow
	for easy switching between the many many points of the test-matrix.

	in your test browser, provides a way to switch between available themes,
	and optionally enable RTL (right to left) mode, and/or dijit_a11y (high-
	constrast/image off emulation) ... probably not a genuine test for a11y.

	usage: on any dijit test_* page, press ctrl-f9 to popup links.

	there are currently (3 themes * 4 tests) * (10 variations of supported browsers)
	not including testing individual locale-strings

	you should NOT be using this in a production enviroment. include
	your css and set your classes manually. for test purposes only ...
*/

(function(){
	var d = dojo,
		theme = false,
		testMode = null,
		defTheme = "tundra";

	if(window.location.href.indexOf("?") > -1){
		var str = window.location.href.substr(window.location.href.indexOf("?")+1).split(/#/);
		var ary  = str[0].split(/&/);
		for(var i=0; i<ary.length; i++){
			var split = ary[i].split(/=/),
				key = split[0],
				value = split[1];
			switch(key){
				case "locale":
					// locale string | null
					dojo.config.locale = locale = value;
					break;
				case "dir":
					// rtl | null
					document.getElementsByTagName("html")[0].dir = value;
					break;
				case "theme":
					// tundra | soria | noir | squid | nihilo | null
					theme = value;
					break;
				case "a11y":
					if(value){ testMode = "dijit_a11y"; }
			}
		}
	}

	// always include the default theme files:
	if(theme || testMode){

		if(theme){
			var themeCss = d.moduleUrl("dijit.themes",theme+"/"+theme+".css");
			var themeCssRtl = d.moduleUrl("dijit.themes",theme+"/"+theme+"_rtl.css");
			document.write('<link rel="stylesheet" type="text/css" href="'+themeCss+'">');
			document.write('<link rel="stylesheet" type="text/css" href="'+themeCssRtl+'">');
		}

		if(dojo.config.parseOnLoad){
			dojo.config.parseOnLoad = false;
			dojo.config._deferParsing = true;
		}

		d.addOnLoad(function(){

			// set the classes
			var b = dojo.body();
			if(theme){
					dojo.removeClass(b, defTheme);
					if(!d.hasClass(b, theme)){ d.addClass(b, theme); }
					var n = d.byId("themeStyles");
					if(n){ d.destroy(n); }
			}
			if(testMode){ d.addClass(b, testMode); }
			if(dojo.config._deferParsing){
				// attempt to elimiate race condition introduced by this
				// test helper file.  120ms to allow CSS to finish/process?
				setTimeout(dojo.hitch(d.parser, "parse", b), 120);
			}

		});
	}

})();
