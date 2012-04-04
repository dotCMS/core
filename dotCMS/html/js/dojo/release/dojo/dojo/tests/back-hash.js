if(!dojo._hasResource["tests.back-hash"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["tests.back-hash"] = true;
dojo.provide("tests.back-hash");

dojo.require("dojo.back");

(function(){
	tests.register("tests.back.hash", [
		function getAndSet(t) {
			var cases = [
				"test",
				"test with spaces",
				"test%20with%20encoded",
				"test+with+pluses",
				" leading",
				"trailing ",
				"under_score",
				"extra#mark",
				"extra?instring",
				"extra&instring",
				"#leadinghash"
			];
			var b = dojo.back;
			function verify(s){
				dojo.back.setHash(s);
				t.is(s, dojo.back.getHash(s));
			}
			dojo.forEach(cases, verify);
		}
	]);
})();

}
