var dotCMSImpl = function () {
}

var dotCMSLightboxImpl = function () {
	
	var boxDiv;
	
	return {
		showLightBox: function (div, options) {
		
			var animate = options && options.animate?options.animate:true;
			
			if(div == null)
				return;
				
			this.boxDiv = Ext.get(div);
		
			var overlay = Ext.get('dotCMS_lightbox_overlay');
			
			if(overlay == null) {
				var body = Ext.get(document.body);
				body.insertHtml('beforeEnd', '<div id="dotCMS_lightbox_overlay"></div>');
				overlay = Ext.get('dotCMS_lightbox_overlay');
			}
			
			var body = Ext.get(document.body);
			var viewSize = body.getViewSize();
			overlayWidth = viewSize.width > body.getWidth()?viewSize.width:body.getWidth();
			overlayHeight = viewSize.height > body.getHeight()?viewSize.height:body.getHeight();
			overlay.setTop(0);
			overlay.setLeft(0);
			overlay.setWidth(overlayWidth);
			overlay.setHeight(overlayHeight);
			overlay.applyStyles("z-index: 1000");
			var boxDiv = Ext.get(boxDiv)
			this.boxDiv.position('absolute', 1100, (viewSize.width / 2) - (this.boxDiv.getComputedWidth() / 2), 
				(viewSize.height / 2) - (this.boxDiv.getComputedHeight() / 2));
			Ext.get(overlay).setVisibilityMode(Ext.Element.VISIBILITY);
			Ext.get(overlay).show(animate);
			this.boxDiv.show(true);
		},

		hideLightBox: function () {
			var overlay = Ext.get('dotCMS_lightbox_overlay');
			if(overlay != null && overlay.isVisible()) {
				if(Ext.get(this.boxDiv) != null) {
					Ext.get(this.boxDiv).hide();
				}
				Ext.get(overlay).hide();
			}
		}
	}		
}

dotCMSImpl.prototype.lightbox = new dotCMSLightboxImpl();

var dotCMS = new dotCMSImpl();
		
