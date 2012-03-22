dojo.provide("dotcms.dijit.image.Crop");

dojo.require("dijit._Widget");
dojo.require("dojo.dnd.move");
dojo.require("dotcms.dijit.image.ResizeHandle");
 dojo.require("dojo.number");

	// a simple "wrap" function. availaing in `plugd`
	// warning: node must be in the DOM before being wrapped.

		// some quick aliases for shrinksafe: 

	
dojo.declare("dotcms.dijit.image.Crop", dijit._Widget, {
		// summary: A Behavioral widget adding a Crop pane to any `<img>`
		
		
	 	wrap :function(node, withTag){
		// summary: wrap some node with a tag type.
			var n = this.ie.iframe.dojo.create(withTag, {	"class":"cropItem"});
			this.ie.iframe.dojo.place(n, node, "before");
			this.ie.iframe.dojo.place(node, n, "first");
			return n;
		}, 
		
		
		// the size of the preview window relative to the glassSize
		scale:1,
		
		// unsupported atm:
		withMouseMove:false,
		withDrag:true,
		debug:false,
		
		// moveInterval: Int
		//		Adjust the time the preview redraws from dnd operations (ms)
		moveInterval: 50,
		
		// hoverable: Boolean
		//		Does this image react to hovering to show/hide the dragger
		hoverable: false,
		
		// resizeable: Boolean
		//		Can the glass be resized, and provide zooming of the preview?
		resizeable: true,
		
		// opacity: Float
		//		Opacity value to use in hoverable or non-hoverable cases.
		opacity: 0.35, 
		

		
		postCreate: function(){
			this.ie = window.top._dotImageEditor ;
			
			this.fixedAspect = this.ie.iframe.dijit.byId("constrain").checked;
			//border adds 2px each side
			if(!dojo.isIE){
				this.cropWidth = parseInt(this.ie.iframe.dojo.byId("cropWidth").value) -4;
				this.cropHeight = parseInt(this.ie.iframe.dojo.byId("cropHeight").value) -4;				
			}else{
				this.cropWidth = parseInt(this.ie.iframe.dojo.byId("cropWidth").value);
				this.cropHeight = parseInt(this.ie.iframe.dojo.byId("cropHeight").value);				
			}
			
			
			var gs = this.glassSize, 
				s = this.scale;
			//alert(ie.iframe.dojo.byId("imageViewPort"));
		
			// wrap the target in a div so we can control it with dnd.
			//alert(this.domNode.id);
			//var mb =dojo.marginBox(this.domNode);
			var mb =this.ie.iframe.dojo.marginBox(this.ie.iframe.dojo.byId("imageViewPort"));
			this.currentSize = mb;

			// wrap the domNode in another div so parentDnd works
			this.container = this.wrap(this.domNode, "div");
			this.ie.iframe.dojo.marginBox(this.container, mb);
			
			// create a draggable handle thing over our target
			this.picker =this.ie.iframe.dojo.create('div', {
				"class":"imageDragger",
				id:"myImagePicker",
				style: { 
					opacity: this.hoverable ? 0 : this.opacity, 
					width: this.cropWidth + "px", 
					height: this.cropHeight + "px"
				}
			}, this.domNode, "before");
			
			var csb =this.ie.iframe.dojo.create("div", {id:"cropSizeBox"}, this.picker);
			this.ie.iframe.dojo.create("span", {id:"cropImageWidth"}, csb);
			this.ie.iframe.dojo.create("span", {innerHTML:" x "}, csb);
			this.ie.iframe.dojo.create("span", {id:"cropImageHeight"}, csb);
			
			this._positionPicker();
			
			// setup dnd for the picker:
			this.mover = new  this.ie.iframe.dojo.dnd.move.parentConstrainedMoveable(this.picker,
				// Safari and IE seem to be pickup up paddings and whatnot
				// as container? ugh. 
				{ area: "content", within: true }
			);
			
			

			if(this.resizeable){
				// create the resize handle for the glass:
				this._handle = new dotcms.dijit.image.ResizeHandle({
					
					// the draggable node is the one to resize. 
					// maintain aspect to keep inline with preview
					targetContainer: this.picker,
					fixedAspect: this.fixedAspect,
				
					// rescale the image often, activeResize is true
					intermediateChanges: true,
					activeResize: true,
					onResize: dojo.hitch(this, function(e,f){
						this._whileMoving();
					}),
					
					// constrain to box. 
					constrainMax: true,
					maxWidth: mb.w, 
					maxHeight: mb.h,
				
					// sane default minimums:
					minWidth:40, 
					minHeight:40
				
				}).placeAt(this.picker);
			}
			
			
			
			// setup dnd behavior
			this.ie.iframe.dojo.subscribe("/dnd/move/start", this, "_startDnd");
			this.ie.iframe.dojo.subscribe("/dnd/move/stop", this, "_stopDnd");
			
			
			if(this.hoverable){
				this.connect(this.container, "onmouseenter", "_enter");
				this.connect(this.container, "onmouseleave", "_leave");
			}
			
			setTimeout(this.ie.iframe.dojo.hitch(this, "_positionPicker"), 125);
			
		},
		
		
		
		

		
		
		_positionPicker: function(e){


			// place the preview thinger somewhere relative to the container 
			// we wrapped around the orig image.
			var tc = this.coords =this.ie.iframe.dojo.coords(this.ie.iframe.dojo.byId("imageViewPort"), true);

			this.ie.iframe.dojo.style(this.picker,{
				left: ( tc.l + (tc.w /2)-76) + "px",
				top:  ( tc.t + (tc.h /2)-76) + "px"
			});
			
			
			var xy =this.ie.iframe.dojo.coords(this.picker);
			this.ie.iframe.dojo.byId("cropImageWidth").innerHTML =xy.w  +"";
			this.ie.iframe.dojo.byId("cropImageHeight").innerHTML=xy.h +"";
			
		},
		
		_startDnd: function(n){
			
			// listen for dnd Start, determine if we care:
			if(!this._interval && n && n.node == this.picker){
				// listen to doc onmousemove 
				this._interval = this.connect(this.ie.iframe.dojo.doc, "onmousemove", "_whileMoving");
			}
		},

		_stopDnd: function(){
			// dnd operations are done, remove the timer
			if(this._interval){
				this.disconnect(this._interval);

				
				delete this._interval; // remember to delete it ;)
				if(this.resizeable && this._lastXY){
			
					// update the max size values of our handle to 
					// be based on our current position, only allowing
					// for the glass to be resized to the offset box
					// we're in (eg, @ top 50 with size 50, max next
					// size is 250). if top 0, use orig container size.
					var tc = this.coords;
					this._handle.maxSize = {
					
						h: Math.floor(tc.h - (this._lastXY.t - tc.t)),
						w: Math.floor(tc.w - (this._lastXY.l - tc.l))
					}
					if(this.debug){
						console.log("this._handle.maxSize:"+ this._handle.maxSize );
						console.log(this._handle.maxSize );
					}
				}
							
				
				
			}
		},
		
		_whileMoving: function(){
			this.ie = window.top._dotImageEditor ;
			// while dnd in progress, adjust the backgroundPosition of the preview
			sTop = this.ie.iframe.document.getElementById("imageViewPort").scrollTop;
			sLeft = this.ie.iframe.document.getElementById("imageViewPort").scrollLeft;
			
			// tc is current image
			// xy is glass scaler
			// d is the preview

			var xy = this._lastXY =this.ie.iframe.dojo.coords(this.picker);
			var tc = this.coords; 

			x = Math.floor(((xy.l + sLeft)- tc.l) ), 
			y = Math.floor(((xy.t+ sTop) - tc.t) );
			if(this.debug){
			
			
			
				console.log("--------------:" );
				console.log("cropper left:" + xy.l );
				console.log("cropper top:" + xy.t);
				console.log("cropper w:" + xy.w );
				console.log("cropper h:" + xy.h);

				console.log("sLeft" + sLeft);
				console.log("sTop:" + sTop);
				console.log("image left:" +tc.l);
				console.log("image top:" + tc.t);
			}


			this.ie.iframe.dojo.byId("cropImageWidth").innerHTML =xy.w  + "";
			this.ie.iframe.dojo.byId("cropImageHeight").innerHTML=xy.h + "";
			

		
		},
		
		destroy: function(){
			// destroy our domNodes. this is a behavioral widget.
			// we can .destroy(true) and leave the image in tact
			this.ie.iframe.dojo.place(this.domNode, this.container, "before");
			this.ie.iframe.dojo.forEach(["picker","container"], function(n){
			this.ie.iframe.dojo.destroy(this[n]);
				delete this[n];
			}, this);
			this.inherited(arguments);
		},
		
		imageReady: function(){
			// stub fired anytime the preview.onload happens
		},
		
		_enter: function(e){
			this.showCropBox();
		},
		
		
		showCropBox: function(){
			
			// handler for mouseenter event
			this._anim && this._anim.stop();
			//alert(d.style(this.picker,"opacity"));
			this._anim =this.ie.iframe.dojo.anim(this.picker, { "opacity": this.opacity });
			if(this.ie.iframe.dojo.style(this.picker,"opacity") ==0){
				this.ie.iframe.dojo.style(this.picker,"opacity", this.opacity );
			}
		},
		
		_leave: function(e){
			// handler for mouseleave event
			if(!this._interval && !this._handle._isSizing){
				this._anim && this._anim.stop();
			//	this._anim =this.ie.iframe.dojo.anim(this.picker, { opacity: 0 });
				if(this.ie.iframe.dojo.style(this.picker,"opacity") >0){
				//dojo.style(this.picker,"opacity", 0 );
				}
			}
		}
		
	});
	
