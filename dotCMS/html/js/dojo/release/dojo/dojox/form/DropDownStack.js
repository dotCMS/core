if(!dojo._hasResource["dojox.form.DropDownStack"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.form.DropDownStack"] = true;
dojo.provide("dojox.form.DropDownStack");

dojo.require("dijit.form.Select");
dojo.require("dojox.form._SelectStackMixin");

dojo.declare("dojox.form.DropDownStack",
	[ dijit.form.Select, dojox.form._SelectStackMixin ], {
	// summary: A dropdown-based select stack.
	
});

}
