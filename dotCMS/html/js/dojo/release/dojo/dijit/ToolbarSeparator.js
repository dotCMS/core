if(!dojo._hasResource["dijit.ToolbarSeparator"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.ToolbarSeparator"] = true;
dojo.provide("dijit.ToolbarSeparator");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");



dojo.declare("dijit.ToolbarSeparator",
		[ dijit._Widget, dijit._Templated ],
		{
		// summary:
		//		A spacer between two `dijit.Toolbar` items
		templateString: '<div class="dijitToolbarSeparator dijitInline" role="presentation"></div>',
		buildRendering: function(){
			this.inherited(arguments);
			dojo.setSelectable(this.domNode, false);
		},
		isFocusable: function(){
			// summary:
			//		This widget isn't focusable, so pass along that fact.
			// tags:
			//		protected
			return false;
		}

	});

}
