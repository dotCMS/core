 

dojo.provide("dotcms.dojo.push.PushHandler");

dojo.require("dijit._Widget");

dojo.require("dijit.Dialog");

dojo.require("dotcms.dijit.RemotePublisherDialog");


dojo.declare("dotcms.dojo.push.PushHandler", null, {
	assetIdentifier:"", 
	dialog:null,
	title:"",
	constructor: function(title){
		this.title = title;
	},
	showDialog: function(assetId){
		this.assetIdentifier = assetId;
		dialog = new dotcms.dijit.RemotePublisherDialog();
		dialog.title = this.title;
		dialog.show();
	}, 
	remotePublish : function(){
		
		// BEGIN: PUSH PUBLISHING ACTIONLET		
		
		var publishDate = (dijit.byId("publishDate") && dijit.byId("publishDate")!='undefined')			
			? dojo.date.locale.format(dijit.byId("publishDate").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
				: (dojo.byId("publishDate") && dojo.byId("publishDate")!='undefined')	
					? dojo.date.locale.format(dojo.byId("publishDate").value,{datePattern: "yyyy-MM-dd", selector: "date"})
							: "";

		var publishTime = (dijit.byId("publishTime"))			
			? dojo.date.locale.format(dijit.byId("publishTime").getValue(),{timePattern: "H-m", selector: "time"})
				: (dojo.byId("publishTime"))	
					? dojo.date.locale.format(dojo.byId("publishTime").value,{timePattern: "H-m", selector: "time"})
							: "";
		
					
		var expireDate = (dijit.byId("expireDate"))			
			? dijit.byId("expireDate").getValue()!=null ? dojo.date.locale.format(dijit.byId("expireDate").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
				: (dojo.byId("expireDate"))	
					? dojo.byId("expireDate").value!=null ? dojo.date.locale.format(dojo.byId("expireDate").value,{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
							: "";
		
		var expireTime = (dijit.byId("expireTime"))			
			? dijit.byId("expireTime").getValue()!=null ? dojo.date.locale.format(dijit.byId("expireTime").getValue(),{timePattern: "H-m", selector: "time"}) : ""
				: (dojo.byId("expireTime"))	
					? dojo.byId("expireTime").value!=null ? dojo.date.locale.format(dojo.byId("expireTime").value,{timePattern: "H-m", selector: "time"}) : ""
							: "";			
		var neverExpire = (dijit.byId("neverExpire"))			
			? dijit.byId("neverExpire").getValue()
				: (dojo.byId("neverExpire"))	
					? dojo.byId("neverExpire").value
							: "";
					
		// END: PUSH PUBLISHING ACTIONLET
		
		
		// BEGIN: PUSH PUBLISHING ACTIONLET
		dojo.byId("assetIdentifier").value=this.assetIdentifier;
		dojo.byId("remotePublishDate").value=publishDate;
		dojo.byId("remotePublishTime").value=publishTime;
		dojo.byId("remotePublishExpireDate").value=expireDate;
		dojo.byId("remotePublishExpireTime").value=expireTime;
		dojo.byId("remotePublishNeverExpire").value=neverExpire;
		// END: PUSH PUBLISHING ACTIONLET
		
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish",
			form: dojo.byId("remotePublishForm"),
			handleAs: "text",
			load: function(data){
				if(data.indexOf("FAILURE") > -1){
					
					alert(data);
				}
			},
			error: function(error){
				alert(error);
				
			}
		}

		var deferred = dojo.xhrPost(xhrArgs);	
		
		dialog.hide();

	}

})