/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.demos.widgets.FileView"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.data.demos.widgets.FileView"] = true;
dojo.provide("dojox.data.demos.widgets.FileView");
dojo.require("dijit._Templated");
dojo.require("dijit._Widget");

dojo.declare("dojox.data.demos.widgets.FileView", [dijit._Widget, dijit._Templated], {
	//Simple demo widget for representing a view of a Flickr Item.

	templateString: dojo.cache("dojox", "data/demos/widgets/templates/FileView.html", "<div class=\"fileView\">\r\n\t<div class=\"fileViewTitle\">File Details:</div>\r\n\t<table class=\"fileViewTable\">\r\n\t\t<tbody>\r\n\t\t\t<tr class=\"fileName\">\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tName:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td dojoAttachPoint=\"nameNode\">\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tPath:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td dojoAttachPoint=\"pathNode\">\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tSize:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<span dojoAttachPoint=\"sizeNode\"></span>&nbsp;bytes.\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tIs Directory:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td dojoAttachPoint=\"directoryNode\">\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tParent Directory:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td dojoAttachPoint=\"parentDirNode\">\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t\t<tr>\r\n\t\t\t\t<td>\r\n\t\t\t\t\t<b>\r\n\t\t\t\t\t\tChildren:\r\n\t\t\t\t\t</b>\r\n\t\t\t\t</td>\r\n\t\t\t\t<td dojoAttachPoint=\"childrenNode\">\r\n\t\t\t\t</td>\r\n\t\t\t</tr>\r\n\t\t</tbody>\r\n\t</table>\r\n</div>\r\n"),

	//Attach points for reference.
	titleNode: null, 
	descriptionNode: null,
	imageNode: null,
	authorNode: null,

	name: "",
	path: "",
	size: 0,
	directory: false,
	parentDir: "",
	children: [],

	postCreate: function(){
		this.nameNode.appendChild(document.createTextNode(this.name));
		this.pathNode.appendChild(document.createTextNode(this.path));
		this.sizeNode.appendChild(document.createTextNode(this.size));
		this.directoryNode.appendChild(document.createTextNode(this.directory));
		this.parentDirNode.appendChild(document.createTextNode(this.parentDir));
		if (this.children && this.children.length > 0) {
			var i;
			for (i = 0; i < this.children.length; i++) {
				var tNode = document.createTextNode(this.children[i]);
				this.childrenNode.appendChild(tNode);
				if (i < (this.children.length - 1)) {
					this.childrenNode.appendChild(document.createElement("br"));
				}
			}
		}
	}
});

}
