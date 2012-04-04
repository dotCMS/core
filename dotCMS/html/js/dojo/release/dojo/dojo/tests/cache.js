if(!dojo._hasResource["tests.cache"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["tests.cache"] = true;
dojo.provide("tests.cache");

dojo.require("dojo.cache");

tests.register("tests.cache", 
	[
		{
			runTest: function(t){
				var expected = "<h1>Hello World</h1>";

				t.is(expected, dojo.trim(dojo.cache("dojo.tests.cache", "regular.html", "<h1>Hello World</h1>\r\n")));
				t.is(expected, dojo.trim(dojo.cache("dojo.tests.cache", "sanitized.html", {value: "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\r\n\t\"http://www.w3.org/TR/html4/loose.dtd\">\r\n<html>\r\n\t<head>\r\n\t\t<script type=\"text/javascript\" src=\"../../dojo.js\"></script>\r\n\t\t<script type=\"text/javascript\" src=\"../../cache.js\"></script>\r\n\t</head>\r\n\t<body class=\"tundra\">\r\n\t\t<h1>Hello World</h1>\r\n\t</body>\r\n</html>\r\n",sanitize: true})));
				
				//Test object variant for module.
				var objPath = dojo.moduleUrl("dojo.tests.cache", "object.html").toString();
				t.is(expected, dojo.trim(dojo.cache(new dojo._Url(objPath), {sanitize: true})));

				//Just a couple of other passes just to make sure on manual inspection that the
				//files are loaded over the network only once.
				t.is(expected, dojo.trim(dojo.cache("dojo.tests.cache", "regular.html", "<h1>Hello World</h1>\r\n")));
				t.is(expected, dojo.trim(dojo.cache("dojo.tests.cache", "sanitized.html", {value: "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\r\n\t\"http://www.w3.org/TR/html4/loose.dtd\">\r\n<html>\r\n\t<head>\r\n\t\t<script type=\"text/javascript\" src=\"../../dojo.js\"></script>\r\n\t\t<script type=\"text/javascript\" src=\"../../cache.js\"></script>\r\n\t</head>\r\n\t<body class=\"tundra\">\r\n\t\t<h1>Hello World</h1>\r\n\t</body>\r\n</html>\r\n",sanitize: true})));
				t.is(expected, dojo.trim(dojo.cache(new dojo._Url(objPath), {sanitize: true})));
			}
		}
	]
);

}
