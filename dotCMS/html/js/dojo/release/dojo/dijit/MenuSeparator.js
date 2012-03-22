if(!dojo._hasResource["dijit.MenuSeparator"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.MenuSeparator"] = true;
dojo.provide("dijit.MenuSeparator");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit._Contained");



dojo.declare("dijit.MenuSeparator",
		[dijit._Widget, dijit._Templated, dijit._Contained],
		{
		// summary:
		//		A line between two menu items

		templateString: dojo.cache("dijit", "templates/MenuSeparator.html"),

		buildRendering: function(){
			this.inherited(arguments);
			dojo.setSelectable(this.domNode, false);
		},

		isFocusable: function(){
			// summary:
			//		Override to always return false
			// tags:
			//		protected

			return false; // Boolean
		}
	});

}
