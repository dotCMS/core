if(!dojo._hasResource["dojox.math.tests.math"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.math.tests.math"] = true;
dojo.provide("dojox.math.tests.math");

dojo.require("dojox.math");

(function(){
	tests.register("dojox.math.tests.factorial", [
		function fact0(t){ t.assertEqual(1, dojox.math.factorial(0)); },
		function fact5(t){ t.assertEqual(120, dojox.math.factorial(5)); },
		function factneg(t){ t.assertTrue(isNaN(dojox.math.factorial(-1))); }
	]);
})();

}
