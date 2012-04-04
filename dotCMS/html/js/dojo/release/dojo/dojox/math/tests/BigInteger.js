if(!dojo._hasResource["dojox.math.tests.BigInteger"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.math.tests.BigInteger"] = true;
dojo.provide("dojox.math.tests.BigInteger");

dojo.require("dojox.math.BigInteger");

tests.register("dojox.math.tests.BigInteger",
	[
		function sanity_check(t){
			var x = new dojox.math.BigInteger("abcd1234", 16),
				y = new dojox.math.BigInteger("beef", 16),
				z = x.mod(y);
			t.is("b60c", z.toString(16));
		}
	]
);

}
