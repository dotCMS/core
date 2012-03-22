if(!dojo._hasResource["dojox.encoding.crypto._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.encoding.crypto._base"] = true;
dojo.provide("dojox.encoding.crypto._base");


dojo.getObject("encoding.crypto", true, dojox);

(function(){
	var c=dojox.encoding.crypto;
	c.cipherModes={
		//	summary
		//	Enumeration for various cipher modes.
		ECB:0, CBC:1, PCBC:2, CFB:3, OFB:4, CTR:5
	};
	c.outputTypes={
		//	summary
		//	Enumeration for input and output encodings.
		Base64:0, Hex:1, String:2, Raw:3
	};
})();

}
