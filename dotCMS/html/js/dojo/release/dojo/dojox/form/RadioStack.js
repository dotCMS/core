if(!dojo._hasResource["dojox.form.RadioStack"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.form.RadioStack"] = true;
dojo.provide("dojox.form.RadioStack");

dojo.require("dojox.form.CheckedMultiSelect");
dojo.require("dojox.form._SelectStackMixin");

dojo.declare("dojox.form.RadioStack",
	[ dojox.form.CheckedMultiSelect, dojox.form._SelectStackMixin ], {
	// summary: A radio-based select stack.
});

}
