/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.wire.demos.WidgetRepeater"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.wire.demos.WidgetRepeater"] = true;
dojo.provide("dojox.wire.demos.WidgetRepeater")
		
dojo.require("dojo.parser");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit._Container");

dojo.declare("dojox.wire.demos.WidgetRepeater", [ dijit._Widget, dijit._Templated, dijit._Container ], {
	//	summary:
	//		Simple widget that does generation of widgets repetatively, based on calls to 
	//		the createNew function and contains them as child widgets.
	templateString: "<div class='WidgetRepeater' dojoAttachPoint='repeaterNode'></div>",
	widget: null,
	repeater: null,
	createNew: function(obj){
		//	summary:
		//		Function to handle the creation of a new widget and appending it into the widget tree.
		//	obj:	
		//		The parameters to pass to the widget.
		try{
			if(dojo.isString(this.widget)){
				dojo.require(this.widget);
				this.widget = dojo.getObject(this.widget);
			}
			this.addChild(new this.widget(obj));
			this.repeaterNode.appendChild(document.createElement("br"));
		}catch(e){ console.debug(e); }
	}
});

}
