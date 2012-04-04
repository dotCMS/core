if(!dojo._hasResource["tests.regexp"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["tests.regexp"] = true;
dojo.provide("tests.regexp");

dojo.require("dojo.regexp");

tests.register("tests.regexp", 
	[
		function test_regexp_escape(t){
			t.assertTrue(new RegExp(dojo.regexp.escapeString("\f\b\n\t\r+.$?*|{}()[]\\/^")).test("TEST\f\b\n\t\r+.$?*|{}()[]\\/^TEST"));
			t.assertTrue(new RegExp(dojo.regexp.escapeString("\f\b\n\t\r+.$?*|{}()[]\\/^", ".")).test("TEST\f\b\n\t\r+X$?*|{}()[]\\/^TEST"));
		}
	]
);

}
