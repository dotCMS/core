/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.enhanced.dnd._DndRowSelector"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid.enhanced.dnd._DndRowSelector"] = true;
dojo.provide("dojox.grid.enhanced.dnd._DndRowSelector");
dojo.declare("dojox.grid.enhanced.dnd._DndRowSelector", null, {
	//summary:
	//		This class declaration is used to be mixed in to dojox.grid._RowSelector
	//		to enable DND feature in _RowSelector.

	domousedown: function(e){
	//summary:
	//		Handle when there is a mouse down event 
	//e: Event
	//		The mouse down event
		this.grid.onMouseDown(e);
	},
	
	domouseup: function(e){
	//summary:
	//		Handle when there is a mouse up event 
	//e: Event
	//		The mouse up event
		this.grid.onMouseUp(e);
	},
	
	dofocus:function(e){
	//summary:
	//		Handle when there is a focus event 
	//e: Event
	//		The focus event
		e.cellNode.style.border = "solid 1px";
	}
});

}
