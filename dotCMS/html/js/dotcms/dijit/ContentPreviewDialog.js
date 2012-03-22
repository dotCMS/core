 

dojo.provide("dotcms.dijit.ContentPreviewDialog");

dojo.require("dijit._Widget");

dojo.require("dijit.Dialog");



dojo.declare("dotcms.dijit.ContentPreviewDialog", null, {
	myId:"contentPreviewDia",
	contentletId:"",
	show : function(){
		
		var dia = dijit.byId(this.myId);
		if(dia){
			dia.destroyRecursive();
			
		}
		dia = new dijit.Dialog({
			id			: this.myId,
			title		: '<%=LanguageUtil.get(pageContext, "Content-Preview")%>',
			href 		: "/html/portlet/ext/contentlet/view_contentlet_popup_inc.jsp?contentletId=" + this.contentletId,
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