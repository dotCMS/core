if(!dojo._hasResource["tests.currency"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["tests.currency"] = true;
dojo.provide("tests.currency");

dojo.require("dojo.currency");

tests.register("tests.currency", 
	[
		{
			// Test formatting and parsing of currencies in various locales pre-built in dojo.cldr
			// NOTE: we can't set djConfig.extraLocale before bootstrapping unit tests, so directly
			// load resources here for specific locales:

			name: "currency",
			setUp: function(){
				var partLocaleList = ["en-us", "en-ca", "de-de"];
				for(var i = 0 ; i < partLocaleList.length; i ++){
					dojo.requireLocalization("dojo.cldr","currency",partLocaleList[i], "ROOT,ar,ca,cs,da,de,el,en,en-au,en-ca,en-us,es,fi,fr,he,hu,it,ja,ko,nb,nl,pl,pt,ru,sk,sl,sv,th,tr,zh,zh-tw");
					dojo.requireLocalization("dojo.cldr","number",partLocaleList[i], "ROOT,ar,ca,cs,da,de,de-de,el,en,en-au,en-gb,en-us,es,es-es,fi,fr,he,hu,it,ja,ja-jp,ko,ko-kr,nb,nl,pl,pt,pt-pt,ru,sk,sl,sv,th,tr,zh,zh-cn,zh-tw");
				}
			},
			runTest: function(t){
				t.is("\u20ac123.45", dojo.currency.format(123.45, {currency: "EUR", locale: "en-us"}));
				t.is("$123.45", dojo.currency.format(123.45, {currency: "USD", locale: "en-us"}));
				t.is("$1,234.56", dojo.currency.format(1234.56, {currency: "USD", locale: "en-us"}));
				t.is("US$123.45", dojo.currency.format(123.45, {currency: "USD", locale: "en-ca"}));
				t.is("$123.45", dojo.currency.format(123.45, {currency: "CAD", locale: "en-ca"}));
				t.is("CA$123.45", dojo.currency.format(123.45, {currency: "CAD", locale: "en-us"}));
				t.is("123,45 \u20ac", dojo.currency.format(123.45, {currency: "EUR", locale: "de-de"}));
				t.is("1.234,56 \u20ac", dojo.currency.format(1234.56, {currency: "EUR", locale: "de-de"}));
				// There is no special currency symbol for ADP, so expect the ISO code instead
				t.is("ADP123", dojo.currency.format(123, {currency: "ADP", locale: "en-us"}));
				t.is("$1,234", dojo.currency.format(1234, {currency: "USD", fractional: false, locale: "en-us"}));

				t.is(123.45, dojo.currency.parse("$123.45", {currency: "USD", locale: "en-us"}));
				t.is(1234.56, dojo.currency.parse("$1,234.56", {currency: "USD", locale: "en-us"}));
				t.is(123.45, dojo.currency.parse("123,45 \u20ac", {currency: "EUR", locale: "de-de"}));
				t.is(1234.56, dojo.currency.parse("1.234,56 \u20ac", {currency: "EUR", locale: "de-de"}));
				t.is(1234.56, dojo.currency.parse("1.234,56\u20ac", {currency: "EUR", locale: "de-de"}));

				t.is(1234, dojo.currency.parse("$1,234", {currency: "USD", locale: "en-us"}));
				t.is(1234, dojo.currency.parse("$1,234", {currency: "USD", fractional: false, locale: "en-us"}));
				t.t(isNaN(dojo.currency.parse("$1,234", {currency: "USD", fractional: true, locale: "en-us"})));
			}
		}
	]
);

}
