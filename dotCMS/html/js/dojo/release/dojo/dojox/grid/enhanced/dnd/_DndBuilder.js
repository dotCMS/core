/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.enhanced.dnd._DndBuilder"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid.enhanced.dnd._DndBuilder"] = true;
dojo.provide("dojox.grid.enhanced.dnd._DndBuilder");
dojo.declare("dojox.grid.enhanced.dnd._DndBuilder", null, {
	//summary:
	//		This class declaration is used to be mixed in to dojox.grid._Builder
	//		to enable the _Builder to handle mouse up event for DND feature
	
	domouseup:function(e){
	//summary:
	//		Handle when there is a mouse up event 
	//e: Event
	//		The mouse up event
		if(this.grid.select.isInSelectingMode("col")) {
			this.grid.nestedSorting ? this.grid.focus.focusSelectColEndingHeader(e) : this.grid.focus.focusHeaderNode(e.cellIndex);
		}
		if (e.cellNode)
			this.grid.onMouseUp(e);
		this.grid.onMouseUpRow(e)
	}
});

dojo.declare("dojox.grid.enhanced.dnd._DndHeaderBuilder", null, {
	//summary:
	//		This class declaration is used to be mixed in to dojox.grid._HeadBuilder
	//		to enable the _HeadBuilder to handle mouse up event for DND feature
	
	domouseup: function(e){
		//summary:
		//		Handle when there is a mouse up event 
		//e: Event
		//		The mouse up event
		if(this.grid.select.isInSelectingMode("col")) {
			this.grid.nestedSorting ? this.grid.focus.focusSelectColEndingHeader(e) : this.grid.focus.focusHeaderNode(e.cellIndex);
		}
		this.grid.onMouseUp(e);
	}
});

}
