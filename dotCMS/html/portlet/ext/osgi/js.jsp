<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%response.setContentType("text/JavaScript");%>
dojo.require("dojo.hash");
dojo.require("dojox.layout.ContentPane");
dojo.subscribe("/dojo/hashchange", this, function(hash){mainAdmin.refresh();});



dojo.declare("dotcms.dijit.osgi.MainAdmin", null, {
	baseDiv : "osgiBundles",
	constructor : function() {
	},
	show : function(href) {

		var r = Math.floor(Math.random() * 1000000000);
		if (href.indexOf("?") > -1) {
			href = href + "&r=" + r;
		} else {
			href = href + "?r=" + r;
		}
		dojo.hash(encodeURIComponent(href));

	},
	
	refresh : function() {
		var hashValue = decodeURIComponent(dojo.hash());

		if(!hashValue || hashValue.length ==0){
			return;
		}
		var myCp = dijit.byId(this.baseDiv);
		var hanger = dojo.byId("osgiMain");
		if(!hanger){
			return;
		}
		if (myCp) {
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : this.baseDiv
		}).placeAt("osgiMain");

		myCp.attr("href", hashValue);
		dojo.parser.parse("osgiMain");
	
	}
});

dojo.declare("dotcms.dijit.osgi.Bundles", null, {
	baseJsp : "/html/portlet/ext/osgi/bundles.jsp",
	constructor : function() {
		
	},

	show : function() {
		var href = this.baseJsp;
		mainAdmin.show(href);
	},

});

var mainAdmin = new dotcms.dijit.osgi.MainAdmin({});
var bundles = new dotcms.dijit.osgi.Bundles({});

dojo.ready(function() {
	var myHash = decodeURIComponent(dojo.hash());
	if(myHash && myHash.length > 0){
		bundles.show(myHash);
	}else{
		bundles.show();
	}

});