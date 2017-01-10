/**
 * dropDownMenu v0.4 sw edition
 * An easy to implement dropDown Menu for Websites, that may be based on styled list tags
 *
 * Works for IE 5.5+ PC, Mozilla 1+ all Plattforms, Opera 7+
 *
 * Copyright (c) 2004 Knallgrau New Medias Solutions GmbH, Vienna - Austria
 *
 * Original written by Matthias Platzer at http://knallgrau.at
 *
 * Modified by Sven Wappler http://www.wappler.eu
 *
 * Use it as you need it
 * It is distributed under a BSD style license
 */


/**
 * Container Class (Prototype) for the dropDownMenu
 *
 * @param idOrElement     String|HTMLElement  root Node of the menu (ul)
 * @param name            String              name of the variable that stores the result
 *                                            of this constructor function
 * @param customConfigFunction  Function            optional config function to override the default settings
 *                                            for an example see Menu.prototype.config
 */
var Menu = Class.create();
Menu.prototype = {

	initialize: function(idOrElement, name, customConfigFunction) {

		this.name = name;
		this.type = "menu";
		this.closeDelayTimer = null;
		this.closingMenuItem = null;

		this.config();
		if (typeof customConfigFunction == "function") {
			this.customConfig = customConfigFunction;
			this.customConfig();
		}
		this.rootContainer = new MenuContainer(idOrElement, this);
	},

	config: function() {
	  this.collapseBorders = true;
	  this.quickCollapse = true;
	  this.closeDelayTime = 500;
	}

}

var MenuContainer = Class.create();
MenuContainer.prototype = {
	initialize: function(idOrElement, parent) {
		this.type = "menuContainer";
  		this.menuItems = [];
		this.init(idOrElement, parent);
	},

	init: function(idOrElement, parent) {
	  this.element = $(idOrElement);
	  if(this.element == null) return;
	  this.parent = parent;
	  this.parentMenu = (this.type == "menuContainer") ? ((parent) ? parent.parent : null) : parent;
	  this.root = parent instanceof Menu ? parent : parent.root;
	  this.id = this.element.id;

	  if (this.type == "menuContainer") {
	    if (this.element.hasClassName("dropdown")) this.menuType = "dropdown";
	    else if (this.element.hasClassName("flyout")) this.menuType = "flyout";
	    else if (this.element.hasClassName("horizontal")) this.menuType = "horizontal";
	    else this.menuType = "standard";
	    if (this.menuType == "flyout" || this.menuType == "dropdown") {
	      this.isOpen = false;
		  Element.setStyle(this.element,{
	      	position: "absolute",
	      	top: "0px",
	      	left: "0px",
	      	visibility: "hidden"});
	    } else {
	      this.isOpen = true;
	    }
	  } else {
	    this.isOpen = this.parentMenu.isOpen;
	  }

	  var childNodes = this.element.childNodes;
	  if (childNodes == null) return;

	  for (var i = 0; i < childNodes.length; i++) {
	    var node = childNodes[i];
	    if (node.nodeType == 1) {
	      if (this.type == "menuContainer") {
	        if (node.tagName.toLowerCase() == "li") {
	          this.menuItems.push(new MenuItem(node, this));
	        }
	      } else {
	        if (node.tagName.toLowerCase() == "ul") {
	          this.subMenu = new MenuContainer(node, this);
	        }
	      }
	    }
	  }
	},

	getBorders: function(element) {
	  var ltrb = ["Left","Top","Right","Bottom"];
	  var result = {};
	  for (var i = 0; i < ltrb.length; ++i) {
	    if (this.element.currentStyle)
	      var value = parseInt(this.element.currentStyle["border"+ltrb[i]+"Width"]);
	    else if (window.getComputedStyle)
	      var value = parseInt(window.getComputedStyle(this.element, "").getPropertyValue("border-"+ltrb[i].toLowerCase()+"-width"));
	    else
	      var value = parseInt(this.element.style["border"+ltrb[i]]);
	    result[ltrb[i].toLowerCase()] = isNaN(value) ? 0 : value;
	  }
	  return result;
	},

	open: function() {
	  if (this.root.closeDelayTimer) window.clearTimeout(this.root.closeDelayTimer);
	  this.parentMenu.closeAll(this);
	  this.isOpen = true;
	  if (this.menuType == "dropdown") {

			var left = (Position.positionedOffset(this.parent.element)[0]);
			var viewport = dijit.getViewport();
			var  screenWidth= (viewport.w );
			if(left + 230 > screenWidth ){
				left = left - (this.element.offsetWidth - this.parent.element.offsetWidth );
			}


		Element.setStyle(this.element,{
			left: left + "px",
			top: (Position.positionedOffset(this.parent.element)[1] + Element.getHeight(this.parent.element)) + "px"
		});
		
		
		
		

	  } else if (this.menuType == "flyout") {
	    var parentMenuBorders = this.parentMenu ? this.parentMenu.getBorders() : new Object();
	    var thisBorders = this.getBorders();
	    if (
	      (Position.positionedOffset(this.parentMenu.element)[0] + this.parentMenu.element.offsetWidth + this.element.offsetWidth + 20) >
	      (window.innerWidth ? window.innerWidth : document.body.offsetWidth)
	    ) {
			Element.setStyle(this.element,{
	      		left: (- this.element.offsetWidth - (this.root.collapseBorders ?  0 : parentMenuBorders["left"])) + "px"
			});
	    } else {
			Element.setStyle(this.element,{
	    		left: (this.parentMenu.element.offsetWidth - parentMenuBorders["left"] - (this.root.collapseBorders ?  Math.min(parentMenuBorders["right"], thisBorders["left"]) : 0)) + "px"
			});
	    }
		Element.setStyle(this.element,{
	    	top: (this.parent.element.offsetTop - parentMenuBorders["top"] - this.menuItems[0].element.offsetTop) + "px"
		});
	  }
	  Element.setStyle(this.element,{visibility: "visible"});
	},

	close: function() {
		Element.setStyle(this.element,{visibility: "hidden"});
		this.isOpen = false;
		this.closeAll();
	},

	closeAll: function(trigger) {
		for (var i = 0; i < this.menuItems.length; ++i) {
			this.menuItems[i].closeItem(trigger);
		}
	}

}


var MenuItem = Class.create();

Object.extend(Object.extend(MenuItem.prototype, MenuContainer.prototype), {
	initialize: function(idOrElement, parent) {
		var menuItem = this;
		this.type = "menuItem";
		this.subMenu;
		this.init(idOrElement, parent);
		if (this.subMenu) {
			this.element.onmouseover = function() {
				menuItem.subMenu.open();
			}
		} else {
		if (this.root.quickCollapse) {
		  this.element.onmouseover = function() {
			menuItem.parentMenu.closeAll();
		  }
		}
		  }
		  var linkTag = this.element.getElementsByTagName("A")[0];
		  if (linkTag) {
		 linkTag.onfocus = this.element.onmouseover;
		 this.link = linkTag;
		 this.text = linkTag.text;
		  }
		  if (this.subMenu) {
		this.element.onmouseout = function() {
		  if (menuItem.root.openDelayTimer) window.clearTimeout(menuItem.root.openDelayTimer);
		  if (menuItem.root.closeDelayTimer) window.clearTimeout(menuItem.root.closeDelayTimer);
		  eval(menuItem.root.name + ".closingMenuItem = menuItem");
		  menuItem.root.closeDelayTimer = window.setTimeout(menuItem.root.name + ".closingMenuItem.subMenu.close()", menuItem.root.closeDelayTime);
		}
		  }
	},

	openItem: function() {
	  this.isOpen = true;
	  if (this.subMenu) { this.subMenu.open(); }
	},

	closeItem: function(trigger) {
	  this.isOpen = false;
	  if (this.subMenu) {
	    if (this.subMenu != trigger) this.subMenu.close();
	  }
	}
});


var menu;


function configMenu() {
  this.closeDelayTime = 300;
}

function initMenu() {
  menu = new Menu('root', 'menu', configMenu);
}


try{
  Event.observe(window, 'load', initMenu, false);
}catch(ex){}
