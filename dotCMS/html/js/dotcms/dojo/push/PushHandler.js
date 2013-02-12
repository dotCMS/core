 

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
	remoteUnPublish : function(assetId) {
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/unPublish",
			content: {
				'assetIdentifier' : assetId
			},
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
	},
	remotePublish : function(){
		
		// BEGIN: PUSH PUBLISHING ACTIONLET		
		
		var publishDate = (dijit.byId("wfPublishDateAux") && dijit.byId("wfPublishDateAux")!='undefined')			
			? dojo.date.locale.format(dijit.byId("wfPublishDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
				: (dojo.byId("wfPublishDateAux") && dojo.byId("wfPublishDateAux")!='undefined')	
					? dojo.date.locale.format(dojo.byId("wfPublishDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"})
							: "";

		var publishTime = (dijit.byId("wfPublishTimeAux"))			
			? dojo.date.locale.format(dijit.byId("wfPublishTimeAux").getValue(),{timePattern: "H-m", selector: "time"})
				: (dojo.byId("wfPublishTimeAux"))	
					? dojo.date.locale.format(dojo.byId("wfPublishTimeAux").value,{timePattern: "H-m", selector: "time"})
							: "";
		
					
		var expireDate = (dijit.byId("wfExpireDateAux"))			
			? dijit.byId("wfExpireDateAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
				: (dojo.byId("wfExpireDateAux"))	
					? dojo.byId("wfExpireDateAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
							: "";
		
		var expireTime = (dijit.byId("wfExpireTimeAux"))			
			? dijit.byId("wfExpireTimeAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireTimeAux").getValue(),{timePattern: "H-m", selector: "time"}) : ""
				: (dojo.byId("wfExpireTimeAux"))	
					? dojo.byId("wfExpireTimeAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireTimeAux").value,{timePattern: "H-m", selector: "time"}) : ""
							: "";			
		var neverExpire = (dijit.byId("wfNeverExpire"))			
			? dijit.byId("wfNeverExpire").getValue()
				: (dojo.byId("wfNeverExpire"))	
					? dojo.byId("wfNeverExpire").value
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
				dialog.hide();
			},
			error: function(error){
				alert(error);
				dialog.hide();
			}
		}

		var deferred = dojo.xhrPost(xhrArgs);	
		
		

	}

})