 

dojo.provide("dotcms.dijit.RemotePublisherDialog");

dojo.require("dijit._Widget");

dojo.require("dijit.Dialog");



dojo.declare("dotcms.dijit.RemotePublisherDialog", null, {
	myId:"remotePublisherDia",
	title:"",
	show : function(){
		
		var dia = dijit.byId(this.myId);
		if(dia){
			dia.destroyRecursive();
			
		}
		dia = new dijit.Dialog({
			id			: this.myId,
			title		: this.title,
			href 		: "/html/portlet/ext/remotepublish/remote_publish_dialog.jsp?contentletId=",
			
		});
		

		
		
		dia.show();
		
		
	},
	hide : function(){
		
		var dia = dijit.byId(this.myId);
		if(dia){
			dia.hide();
			dia.destroyRecursive();
		}
	}

})