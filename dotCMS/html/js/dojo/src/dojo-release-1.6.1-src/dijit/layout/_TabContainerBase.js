define("dijit/layout/_TabContainerBase", ["dojo", "dijit", "text!dijit/layout/templates/TabContainer.html", "dijit/layout/StackContainer", "dijit/_Templated"], function(dojo, dijit) {

dojo.declare("dijit.layout._TabContainerBase",
	[dijit.layout.StackContainer, dijit._Templated],
	{
	// summary:
	//		Abstract base class for TabContainer.   Must define _makeController() to instantiate
	//		and return the widget that displays the tab labels
	// description:
	//		A TabContainer is a container that has multiple panes, but shows only
	//		one pane at a time.  There are a set of tabs corresponding to each pane,
	//		where each tab has the name (aka title) of the pane, and optionally a close button.

	// tabPosition: String
	//		Defines where tabs go relative to tab content.
	//		"top", "bottom", "left-h", "right-h"
	tabPosition: "top",

	baseClass: "dijitTabContainer",

	// tabStrip: [const] Boolean
	//		Defines whether the tablist gets an extra class for layouting, putting a border/shading
	//		around the set of tabs.   Not supported by claro theme.
	tabStrip: false,

	// nested: [const] Boolean
	//		If true, use styling for a TabContainer nested inside another TabContainer.
	//		For tundra etc., makes tabs look like links, and hides the outer
	//		border since the outer TabContainer already has a border.
	nested: false,

	templateString: dojo.cache("dijit.layout", "templates/TabContainer.html"),

	postMixInProperties: function(){
		// set class name according to tab position, ex: dijitTabContainerTop
		this.baseClass += this.tabPosition.charAt(0).toUpperCase() + this.tabPosition.substr(1).replace(/-.*/, "");

		this.srcNodeRef && dojo.style(this.srcNodeRef, "visibility", "hidden");

		this.inherited(arguments);
	},

	buildRendering: function(){
		this.inherited(arguments);

		// Create the tab list that will have a tab (a.k.a. tab button) for each tab panel
		this.tablist = this._makeController(this.tablistNode);

		if(!this.doLayout){ dojo.addClass(this.domNode, "dijitTabContainerNoLayout"); }

		if(this.nested){
			/* workaround IE's lack of support for "a > b" selectors by
			 * tagging each node in the template.
			 */
			dojo.addClass(this.domNode, "dijitTabContainerNested");
			dojo.addClass(this.tablist.containerNode, "dijitTabContainerTabListNested");
			dojo.addClass(this.tablistSpacer, "dijitTabContainerSpacerNested");
			dojo.addClass(this.containerNode, "dijitTabPaneWrapperNested");
		}else{
			dojo.addClass(this.domNode, "tabStrip-" + (this.tabStrip ? "enabled" : "disabled"));
		}
	},

	_setupChild: function(/*dijit._Widget*/ tab){
		// Overrides StackContainer._setupChild().
		dojo.addClass(tab.domNode, "dijitTabPane");
		this.inherited(arguments);
	},

	startup: function(){
		if(this._started){ return; }

		// wire up the tablist and its tabs
		this.tablist.startup();

		this.inherited(arguments);
	},

	layout: function(){
		// Overrides StackContainer.layout().
		// Configure the content pane to take up all the space except for where the tabs are

		if(!this._contentBox || typeof(this._contentBox.l) == "undefined"){return;}

		var sc = this.selectedChildWidget;

		if(this.doLayout){
			// position and size the titles and the container node
			var titleAlign = this.tabPosition.replace(/-h/, "");
			this.tablist.layoutAlign = titleAlign;
			var children = [this.tablist, {
				domNode: this.tablistSpacer,
				layoutAlign: titleAlign
			}, {
				domNode: this.containerNode,
				layoutAlign: "client"
			}];
			dijit.layout.layoutChildren(this.domNode, this._contentBox, children);

			// Compute size to make each of my children.
			// children[2] is the margin-box size of this.containerNode, set by layoutChildren() call above
			this._containerContentBox = dijit.layout.marginBox2contentBox(this.containerNode, children[2]);

			if(sc && sc.resize){
				sc.resize(this._containerContentBox);
			}
		}else{
			// just layout the tab controller, so it can position left/right buttons etc.
			if(this.tablist.resize){
				//make the tabs zero width so that they don't interfere with width calc, then reset
				var s = this.tablist.domNode.style;
				s.width="0";
				var width = dojo.contentBox(this.domNode).w;
				s.width="";
				this.tablist.resize({w: width});
			}

			// and call resize() on the selected pane just to tell it that it's been made visible
			if(sc && sc.resize){
				sc.resize();
			}
		}
	},

	destroy: function(){
		if(this.tablist){
			this.tablist.destroy();
		}
		this.inherited(arguments);
	}
});


return dijit.layout._TabContainerBase;
});
