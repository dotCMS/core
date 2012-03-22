/*
	Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.demos.widgets.PicasaView"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.data.demos.widgets.PicasaView"] = true;
dojo.provide("dojox.data.demos.widgets.PicasaView");
dojo.require("dijit._Templated");
dojo.require("dijit._Widget");

dojo.declare("dojox.data.demos.widgets.PicasaView", [dijit._Widget, dijit._Templated], {
	//Simple demo widget for representing a view of a Picasa Item.

	templateString: dojo.cache("dojox", "data/demos/widgets/templates/PicasaView.html", "<table class=\"picasaView\">\r\n\t<tbody>\r\n\t\t<tr class=\"picasaTitle\">\r\n\t\t\t<td>\r\n\t\t\t\t<b>\r\n\t\t\t\t\tTitle:\r\n\t\t\t\t</b>\r\n\t\t\t</td>\r\n\t\t\t<td dojoAttachPoint=\"titleNode\">\r\n\t\t\t</td>\r\n\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<td>\r\n\t\t\t\t<b>\r\n\t\t\t\t\tAuthor:\r\n\t\t\t\t</b>\r\n\t\t\t</td>\r\n\t\t\t<td dojoAttachPoint=\"authorNode\">\r\n\t\t\t</td>\r\n\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<td colspan=\"2\">\r\n\t\t\t\t<b>\r\n\t\t\t\t\tSummary:\r\n\t\t\t\t</b>\r\n\t\t\t\t<span class=\"picasaSummary\" dojoAttachPoint=\"descriptionNode\"></span>\r\n\t\t\t</td>\r\n\t\t</tr>\r\n\t\t<tr>\r\n\t\t\t<td dojoAttachPoint=\"imageNode\" colspan=\"2\">\r\n\t\t\t</td>\r\n\t\t</tr>\r\n\t</tbody>\r\n</table>\r\n\r\n"),

	//Attach points for reference.
	titleNode: null, 
	descriptionNode: null,
	imageNode: null,
	authorNode: null,

	title: "",
	author: "",
	imageUrl: "",
	iconUrl: "",

	postCreate: function(){
		this.titleNode.appendChild(document.createTextNode(this.title));
		this.authorNode.appendChild(document.createTextNode(this.author));
		this.descriptionNode.appendChild(document.createTextNode(this.description));
		var href = document.createElement("a");
		href.setAttribute("href", this.imageUrl);
		href.setAttribute("target", "_blank");
        var imageTag = document.createElement("img");
		imageTag.setAttribute("src", this.iconUrl);
		href.appendChild(imageTag);
		this.imageNode.appendChild(href);
	}
});

}
