if(!dojo._hasResource["dijit.tests.module"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.tests.module"] = true;
dojo.provide("dijit.tests.module");

try{
	dojo.require("dijit.tests._base.module");
	dojo.require("dijit.tests.infrastructure-module");

	dojo.require("dijit.tests.general-module");
	dojo.require("dijit.tests.tree.module");
	dojo.require("dijit.tests.editor.module");
	dojo.require("dijit.tests.form.module");
	dojo.require("dijit.tests.layout.module");
}catch(e){
	doh.debug(e);
}



}
