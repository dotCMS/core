dojo.provide("dotcms.dijit.image.ImageEditor");
dojo.require("dotcms.dijit.image.Crop");
dojo.require("dijit.Dialog");


function _rand(){
     return Math.floor(Math.random()*100000000);
}

dojo.declare("dotcms.dijit.image.ImageEditor", dijit._Widget,{

    // cssUrl:'/html/js/dotcms/dijit/image/image-tools.css',
    imageToolJsp:'/html/js/dotcms/dijit/image/image_tool.jsp',
    baseFilterUrl:"/contentAsset/image",
    // changes to show the value in the toolbar
    currentUrl:"/contentAsset/image",
    ajaxUrl:"/servlet/dotImageToolAjax",

    resizeFilter:false,
    zoomValue:0,
    inode:'0',
    tempId:null,
    inited: false,
    painting : false,
    thumbnailWidth:500,
    thumbnailHeight:250,
    thumbDivId:'',
    fieldName:'fileAsset',
    saveAsIncrement:1,
    editImageText:"Edit Image",
    binaryFieldId:'',
    fieldContentletId:'',
    saveAsFileName:'',
    compression:'none',
    compressionValue:65,
    fileSize:0,
    execute: null,
	activeEditor: undefined,

    postCreate: function(){
        window.top._dotImageEditor = this;
        this.execute = this.createImageWindow;
        // this._loadCss();
        this.filters = new Array();

        var where= (this.parentNode != undefined) ? dojo.byId(this.parentNode) : this.domNode;
        // create divs

        this.thumbnailDiv = dojo.create('div' ,{
            style:"margin-bottom:5px;max-width:1040px;min-width:150px;display:inline-block;height:" + (this.thumbnailHeight+4) +"px"},where);

            dojo.attr(this.thumbnailDiv, "class", "thumbnailDiv");


        this.editFileWindow = dojo.create('div' ,{
            innerHTML:this.editImageText},this.thumbnailDiv);

            dojo.attr(this.editFileWindow, "class", "editImageText");


        this.thumbnailImage = dojo.create("img" , {
            style:"max-width:100%;"},this.thumbnailDiv);





        this._initBaseFilterUrl();
        this.setThumbnail();

        dojo.connect(this.thumbnailDiv , "onclick", this, "createImageWindow");
    },

    _initBaseFilterUrl: function() {
        var shorty = this.inode  ;

        if(this.tempId){
            shorty=this.tempId;
        }
        
        
        
        this.baseFilterUrl+= "/" + shorty;

        if(this.fieldName != undefined){
            this.baseFilterUrl+= "/" + this.fieldName;
        }

        this.currentUrl = this.baseFilterUrl;
    },
    
    createImageWindow: function(){
        this.tabindex = 0;
        this.thumbnailDiv.tabindex=0;


        var newerInode = window.contentAdmin.contentletInode;

        var id=this.inode;
        if(newerInode && newerInode!= this.inode){
            this.inode = newerInode;
            id=newerInode;
            this.tempId=null;
        }else if(this.tempId){
            id = this.tempId;
        }
        
        


        this.baseFilterUrl= "/contentAsset/image/" + id;
        if(this.fieldName != undefined){
            this.baseFilterUrl+= "/" + this.fieldName;
        }
        this.currentUrl = this.baseFilterUrl;
        



        // clean up any old image editors laying around
        this._cleanUpImageEditor();
        window.top._dotImageEditor = this;
        var url = this.imageToolJsp + "?id=" + id;
    
        url = url + "&fieldName="+ this.fieldName;

        
        
        this.imageEditor = document.createElement('div');
        this.imageEditor.id = 'dotImageDialog';
        this.imageEditor.innerHTML="<iframe scrolling='yes' src='" + url+ "' id='imageToolIframe' frameborder='0' style='width:100%;height:100%;overflow:hidden;box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.3), 0 6px 20px 0 rgba(0, 0, 0, 0.19);'></iframe>";
        this.imageEditor.style="position:absolute;top:10px;bottom:20px;left:20px;right:20px;padding:0;margin:0;border:1px silver solid;background:white;z-index: 99999;";
        document.body.insertBefore(this.imageEditor, document.body.firstChild);

    },

    /***************************************************************************
     *
     * Thumbnail preview
     *
     **************************************************************************/




    /****
     * Returns URL representation for given image
     * This will always be a png file
     */
    _getThumbRendition:function(thumbUrl){
        // if thumbnail is already in filter
        if(thumbUrl.indexOf("filter/") > -1){
            var beforeFilter = thumbUrl.substring(0, thumbUrl.indexOf("filter/"));
            var afterFilter = thumbUrl.substring(thumbUrl.indexOf("filter/") + 7, thumbUrl.length);
            var filter = afterFilter.substring(0,afterFilter.indexOf("/"));
            var afterFilter = afterFilter.substring(afterFilter.indexOf("/"),afterFilter.length);

            thumbUrl = beforeFilter + "filter/" + filter + ",Thumbnail/" + afterFilter + "/thumbnail_h/" + this.thumbnailHeight;
        }
        else{
            if(!this.saveAsFileName.toLowerCase().endsWith('.svg')) {
                thumbUrl+= "/filter/Thumbnail/thumbnail_h/"+ this.thumbnailHeight;
            }else{
                return thumbUrl;
            }
        }


        return this.cleanUrl(thumbUrl + "/r/" + _rand());
    },

    cleanUrl: function(x){
    	while(x.indexOf("//")>-1){
    		x= x.replace("//","/");
    	}
    	return x;



    },


    setThumbnail: function(){
        var thumbUrl = this._getThumbRendition(this.currentUrl)  ;
        dojo.style(this.thumbnailImage, "display", "none");
        dojo.addClass(this.thumbnailDiv, "loader");
        this.thumbnailImage.src.onload=new function(){
            var ctx = window.top._dotImageEditor ;
            dojo.style(ctx.thumbnailImage, "display", "block");
            dojo.removeClass(ctx.thumbnailDiv, "loader");

        };

        //this.thumbnailImage.src =  "#"; // thumbUrl;
        this.thumbnailImage.src =  thumbUrl ;

    },




    /**
     * This function runs on pageload from the image_tool.jsp page. It loads the
     * underlying images to get w/h info from them.
     *
     */
    initViewport: function(){
        // make the iframe all big
        var frame=dojo.byId("imageToolIframe");




        //if we are a File Asset
        if(this.binaryFieldId == ''){
            this.iframe.dojo.style(this.iframe.dojo.byId("saveAsSpan"), "display", "inline");

        }


        if(this.inited) return;
        this.imagesLoaded=false;
        this.inited = true;
        var viewPort = this.iframe.dojo.byId("imageViewPort");
        var showImage = this.iframe.dojo.byId("me");
        var baseImage = this.iframe.dojo.byId("baseImage");
        var sic = dojo.coords(showImage);

        if(baseImage.complete){
            var bic = dojo.coords(baseImage);
            this.iframe.dojo.byId("baseImageWidth").innerHTML = bic.w;
            this.iframe.dojo.byId("baseImageHeight").innerHTML = bic.h;
            this.baseAndShowLoaded();
        }else{
            baseImage.onload= function (){
                var ctx = window.top._dotImageEditor ;
                var bic = dojo.coords(ctx.iframe.dojo.byId("baseImage"));
                // set the img dimentions so user can see
                ctx.iframe.dojo.byId("baseImageWidth").innerHTML = bic.w;
                ctx.iframe.dojo.byId("baseImageHeight").innerHTML = bic.h;
                ctx.baseAndShowLoaded();
            };
        }
        if(showImage.complete){
            // set the img dimentions so user can see

            this.iframe.dojo.byId("displayImageWidth").value = bic.w;
            this.iframe.dojo.byId("displayImageHeight").value = bic.h;
            this.baseAndShowLoaded();
        }
        else{
            showImage.onload= function (){
                var ctx = window.top._dotImageEditor ;
                var sic = dojo.coords(ctx.iframe.dojo.byId("me"));
                // set the img dimentions so user can see
                ctx.iframe.dojo.byId("displayImageWidth").value = sic.w;
                ctx.iframe.dojo.byId("displayImageHeight").value = sic.h;
                ctx.baseAndShowLoaded();
            };
        }
        dojo.attr(this.iframe.dojo.byId("imageViewPort"), "class", "");

    },


    baseAndShowLoaded : function(){
        var showImage = this.iframe.dojo.byId("me");
        var baseImage = this.iframe.dojo.byId("baseImage");

        if(showImage.complete && baseImage.complete && !this.imagesLoaded){
            this.imagesLoaded=true;


            var sic = dojo.coords(showImage);
            var bic = dojo.coords(baseImage);

            var x = Math.round(sic.w / bic.w *100);
            this.zoomValue = 0
            if(!isNaN(x)){
                this.zoomValue =x;
            }

            var x =this.iframe.dijit.byId("showScaleSlider");

            x.attr("value", this.zoomValue);
            this._updateZoomFactor();
        }

    },

    closeImageWindow : function(e){
        this._cleanUpImageEditor();
        
    },

    /**
     * cleans up old references, resets imageEditor
     */
    _cleanUpImageEditor : function (e){
        window.top._dotImageEditor = null;
        window.parent._dotImageEditor = null;
        
        var myEditor = (this.imageEditor) ? this.imageEditor : document.getElementById("dotImageDialog");
        
        // if we have an Image Editor
        if(myEditor){
            
            while (this.imageEditor.firstChild) {
                this.imageEditor.removeChild(this.imageEditor.firstChild);
            }
            myEditor.parentNode.removeChild(myEditor)
        }

        this.imageEditor=null;
        this.iframe =null;
        this.inited=false;
        this.painting =false;
        this.filters=new Array();
        this.saveAsIncrement=1;
        this.imagesLoaded=false;
        this.resizeFilter=false;
        
    },

    /***************************************************************************
     *
     * Button Actions
     *
     **************************************************************************/

    addToClipboard : function(){
        if(this.currentUrl.indexOf("/temp_")>-1){
            alert("You cannot clip a temp or altered file.  Please save the content and reopen the image editor to clip it");
            return;
        }
        var fileUrl = this.cleanUrl(this.currentUrl);
        var url = (fileUrl.indexOf("?")>-1) ? fileUrl + "&"  : fileUrl + "?_imageToolClipboard=true";
        //alert(url);
        var target = this.iframe.dojo.byId('me');
        dojo.style(target, "opacity", 0);
        dojo.xhrGet({
            url:url,
            target:target,
             preventCache:true,
             sync:true,
             load:function(data){
                dojo.fadeIn({node:target}).play();

             },
             error:function(data){
                dojo.style(target, "opacity", 100);
             }

        });

    },

    /**
     * Save as passes a var:_imageToolSaveFile to the binary servlet with all the other
     * params.  This saves the file handle in the users session.  We look for this
     * file and save it when the user checks the file or content in
     */

    saveImage : function(){
        if(this.binaryFieldId != null && this.binaryFieldId.length > 0){
            this.saveBinaryImage(this.activeEditor);
        }
        else{
            this.saveFileImage();
        }
    },


    _isValidURL: function (url) {
        let elem = document.createElement("input");
        elem.setAttribute("type", "url");
		elem.value = url;
		return elem.validity.valid;
    },

    /**
     * Sends an image back to the tinymce WYSIWYG editor from the "Edit Image" dialog.
     * 
     * @param {object} activeEditor - Instance of the active editor
     * @param {string} url - Image URL
     */
    _sendImageToEditor: function(activeEditor, url) {
        let newUrl;
        newUrl = this._isValidURL(url) ? new URL(url).pathname : url;

        const asset = `
            <img 
                src="${newUrl}" 
                alt="${this.fieldName}"
                data-field-name="${this.fieldName}"
                data-inode="${this.inode}"
                data-identifier="${this.fieldContentletId}"
                data-saveas="${this.saveAsFileName}"
            />`;
        activeEditor.execCommand("mceReplaceContent", false, asset);
    },
    /**
     * This saves an image
     * that lives on a contentlet
     *
     */
    saveBinaryImage: function(activeEditor){
        var field = this.binaryFieldId;
        if(this.fieldContentletId.length>0) {
            field=this.fieldContentletId;
        }

        let url = this.cleanUrl(this.currentUrl);

        if (activeEditor) { 
            this._sendImageToEditor(activeEditor, url)
        } else {
            url = url.indexOf("?") > -1 ? url + "&" : url + "?";
            url += field.length > 0 ? "&binaryFieldId=" + field : "";
            url += "&_imageToolSaveFile=true";
            var xhr = new XMLHttpRequest();
            xhr.onload = (self => {
                return () => {
                    if (xhr.status == 200) {
                        var dataJson = JSON.parse(xhr.responseText);
                        self.tempId=dataJson.id;
                        if(window.document.getElementById(self.binaryFieldId + "ValueField")){
                            window.document.getElementById(self.binaryFieldId + "ValueField").value=dataJson.id; 
                        }
                        
                    } else {
                        alert("Error! Upload failed");
                    }
                };
            })(this);
            xhr.open("GET", url, true);
            xhr.send();
            this.setThumbnail();
        }
        // close without wiping out the saved value
        this.closeImageWindow();
    },



    closeEditorWhenRedrawFinish: function() {
        if (this.painting) {
            setTimeout("window.top._dotImageEditor.closeEditorWhenRedrawFinish()", 500);
        } else {
            this.closeImageWindow();
        }
    },

    doDownload: function(){
        var url =this.cleanUrl(this.currentUrl);
        
        var aj =this.iframe.dojo.byId("actionJackson");
 
        url = (x.indexOf("?")>-1) ? x + "&"  : x + "?";
        url = url + "r=" +_rand()+ "&force_download=true";
        aj.src=url;

    },


    closeSaveAsDia : function(){
        saveAsDia = this.iframe.dijit.byId("saveAsDialog").hide();
        this.iframe.dojo.byId("saveAsName").value = "";


    },

    /***************************************************************************
     *
     * UI Controls
     *
     **************************************************************************/




    // Start Crop work ------------------------------------------------
    toggleCrop: function(){
        if(this.crop || this.crop != undefined){
            this.doCrop();
        }
        else{
            this._cropOn();
        }
    },



    doConstrain : function(){

        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;
        dojo.attr(this.iframe.dojo.byId("cropWidth"),"value", Math.floor(width/2));
        dojo.attr(this.iframe.dojo.byId("cropHeight"),"value", Math.floor(height/2));


    },
    setCropHeightFromWidth : function(){

        var cw = dojo.attr(this.iframe.dojo.byId("cropWidth"),"value");
        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;

        dojo.attr(this.iframe.dojo.byId("cropHeight"),"value", Math.floor(height * (cw/width) ));
    },

    setCropWidthFromHeight: function(){

        var ch = dojo.attr(this.iframe.dojo.byId("cropHeight"),"value");
        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;

        dojo.attr(this.iframe.dojo.byId("cropWidth"),"value", Math.floor(width * (ch/height) ));
    },





    // turns cropping on
    _cropOn: function(){

        dojo.style(this.iframe.dojo.byId("cropBtn"), "border", "red 1px solid");
        if(this.crop && this.crop != undefined){
            return;
        }


        this.crop = new dotcms.dijit.image.Crop({
            hoverable:true
        }, this.iframe.dojo.byId("me"));
        this.crop.showCropBox();

    },
    _cropOff: function(){

        if(!this.crop || this.crop ==undefined){
            return;
        }
        dojo.style(this.iframe.dojo.byId("cropBtn"), "border", "red 0px dotted");

        var img = this.iframe.dojo.byId("me");

        this.crop.destroy();
        this.crop=null;
        this.iframe.dojo.byId("imageViewPort").appendChild(img);
    },

    doCrop: function(){
        this.iframe.dojo.byId("cropBtn").blur();
        dojo.attr(this.iframe.dijit.byId('cropBtn'), "disabled", true);

        var showImage = this.iframe.dojo.byId("me");
        var baseImage = this.iframe.dojo.byId("baseImage");

        pc = dojo.coords(this.crop.picker);
        vp = dojo.coords(this.iframe.dojo.byId("imageViewPort"));
        sTop = this.iframe.dojo.byId("imageViewPort").scrollTop;
        sLeft = this.iframe.dojo.byId("imageViewPort").scrollLeft;

        var sw = parseInt(this.iframe.dojo.byId("displayImageWidth").value);
        var sh = parseInt(this.iframe.dojo.byId("displayImageHeight").value);       
                
        var x = 0;
        var y = 0;
        var w = pc.w;
        var h = pc.h;
        
        if(pc.l < 0){
        	w = pc.w + pc.l;
        }
        if(pc.t < 0){
        	h = pc.h + pc.t;
        }        
        
        if(pc.l > 0 && (pc.l+pc.w) < sw){
        	x = pc.l;
        	w = pc.w;
        }
        if(pc.t > 0 && (pc.t+pc.h) < sh){
        	y = pc.t;
        	h = pc.h;
        }
        
        if((pc.l+pc.w) > sw){
        	x = pc.l;
        	w = (sw - pc.l) - 1;
        }
        if((pc.t+pc.h) > sh){
        	y = pc.t;
        	h = (sh - pc.t) - 1;
        }
        
        var val ="";
        val+="/crop_w/" + parseInt(w);
        val+="/crop_h/" + parseInt(h) ;
        val+="/crop_x/" + parseInt(x);
        val+="/crop_y/"+ parseInt(y) ;


        this._addFilter("Crop", val)
        this._redrawImage();

    },

    // End Crop work ------------------------------------------------



    // Start Resize work ------------------------------------------------



    updateSlider: function(){
        this._updateZoomFactor();
        this._updateWidthHieghtByZoom();
    },


    // slider updates the zoom info
    _updateZoomFactor: function(){
        var x =this.iframe.dijit.byId("showScaleSlider");
        zoomValue = Math.round(x.value);
        this.iframe.dojo.byId("zoomInfo").innerHTML = zoomValue + "%";
    },

    _updateWidthHieghtByZoom: function(){
        var baseImage = this.iframe.dojo.byId("baseImage");
        var bic = dojo.coords(baseImage);
        var w = Math.round(bic.w * (zoomValue / 100));
        if(!this.resizeFilter)
        	this.iframe.dojo.byId("displayImageWidth").value = w;

        this.resizeFilter= false;
        this.setHieghtFromWidth();
    },





    /**
     * adjusts slider after someone types in a width and height
     */

    _setSilderFromWidth: function(){
        var baseImage = this.iframe.dojo.byId("baseImage");
        var bic = dojo.coords(baseImage);
        var w = Math.round(bic.w);
        var tw = this.iframe.dojo.byId("displayImageWidth").value;
        var x =this.iframe.dijit.byId("showScaleSlider");
        x.setValue(Math.round((tw/w) * 100));
    },



    setHieghtFromWidth: function(){
        var showImage = this.iframe.dojo.byId("me");
        var showImageCoords = dojo.coords(showImage);
        var val = parseInt(this.iframe.dojo.byId("displayImageWidth").value);
        if(isNaN(val) || val <1 || val > 9999){
            //alert("Resize value too large,  > 9999px");
            val = showImageCoords.w;
            this.iframe.dojo.byId("displayImageWidth").value = val;
        }
        this.iframe.dojo.byId("displayImageHeight").value = Math.round(val * showImageCoords.h / showImageCoords.w)

    },



    setWidthFromHeight: function(){
        var showImage = this.iframe.dojo.byId("me");
        var showImageCoords = dojo.coords(showImage);
        var val = parseInt(this.iframe.dojo.byId("displayImageHeight").value);
        if(isNaN(val) || val <1 || val > 9999){
            alert("Resize value too large,  > 9999px");
            val = showImageCoords.h;
            this.iframe.dojo.byId("displayImageHeight").value = val;
        }

        this.iframe.dojo.byId("displayImageWidth").value = Math.round((val * showImageCoords.w ) /showImageCoords.h  )

    },


    allowNumbers: function(evt){
        var charCode = evt.keyCode;
       // alert(charCode);
        if(charCode ==8 || charCode ==46 || charCode ==9){
            return true;
        }
        if (charCode < 48 || (charCode > 58 &&charCode < 96) || charCode > 105){
            return false;
        }
        return true;
    },




    /**
     * This function takes a width and handles all resizes
     */
    _doResize: function(width){
        var args=  "/resize_w/" + (parseInt(width)) ;
        this._addFilter("Resize", args);
        this._redrawImage();
    },



    /**
     * Called when someone moves the slider (onchange)
     */
    doSliderResize: function(){
        var width =parseInt(this.iframe.dojo.byId("displayImageWidth").value);
        if(!isNaN(width)){
            this._doResize(width);
        }
    },

    /**
     * Called when someone types in a new width
     */

    resizeBtnClick: function(action){
    	if(action == 'resize')
    		this.resizeFilter = true;

        var width =parseInt(this.iframe.dojo.byId("displayImageWidth").value);
        if(!isNaN(width)){
            this._doResize(width);
        }
        this._setSilderFromWidth();
    },



    // End Resize work ------------------------------------------------





    doRotate: function(){
        var x = this.iframe.dijit.byId("rotate");
        if(x.isValid()){

            this._addFilter("Rotate", "/rotate_a/" + x.value + ".0");
            this._redrawImage();
        }
        else{
            alert("invalid angle");
        }
    },






    toggleFlip: function(){
        var x = this.iframe.dijit.byId("flip");

        if(x.checked){
            this._addFilter("Flip", "/flip_flip/1");
        }
        else{

            this._removeFilter("Flip");
        }
        this._redrawImage();
    },

    
    updateCompressionValue: function(){
        var x = this.iframe.dijit.byId("compressionValue").getValue();
        this.iframe.dojo.byId("compressionValueSpan").innerHTML=x + "%";
        this.compressionValue=x;
    },
    
    
    
    
    
    
    
    toggleCompression: function(){
        var x = this.iframe.dijit.byId("compression").getValue();

        this._removeFilter("Jpeg");
        this._removeFilter("WebP");
        this._removeFilter("Quality");
        
        this.iframe.dijit.byId("compressionValue").set("value", this.compressionValue, false);
        this.iframe.dojo.byId("compressionValueSpan").innerHTML=this.compressionValue + "%";
        
        if(!x || x=="none"){
            this.iframe.dojo.query("#controlTable .compressTd").forEach(function(node, index, arr){
                node.style.display="none";
            });
        }
        if(x=="jpeg"){
            this.iframe.dojo.query("#controlTable .compressTd").forEach(function(node, index, arr){
                node.style.display="table-cell";
            });

            this._addFilter("Jpeg", "/jpeg_q/" + this.compressionValue);
            this.iframe.dijit.byId("compression").set("value", "jpeg", false);
        }
        else if(x=="webp"){
            this.iframe.dojo.query("#controlTable .compressTd").forEach(function(node, index, arr){
                node.style.display="table-cell";
            });
            this._addFilter("WebP", "/webp_q/" + this.compressionValue);
            this.iframe.dijit.byId("compression").set("value", "webp", false);
            
        }
        else if(x=="auto"){
            this.iframe.dojo.query("#controlTable .compressTd").forEach(function(node, index, arr){
                node.style.display="table-cell";
            });
            this._addFilter("Quality", "/quality_q/" + this.compressionValue);
            this.iframe.dijit.byId("compression").set("value", "auto", false);
            
        }
        
        this._redrawImage();
    },

    toggleGrayscale: function(){
        var x = this.iframe.dijit.byId("grayscale");

        if(x.checked){
            this._addFilter("Grayscale", "/grayscale/1");
        }
        else{
            this._removeFilter("Grayscale");
        }
        this._redrawImage();
    },




    applyHSB : function(){
        var x = this.iframe.dijit.byId("hsbDialog");
        x.hide();
        var h = parseInt(this.iframe.dijit.byId("hueSlider").value);
        var s = parseInt(this.iframe.dijit.byId("satSlider").value);
        var b = parseInt(this.iframe.dijit.byId("brightSlider").value);

        var hs = (h % 100 == 0) ? (h / 100) + ".00" : h/100 +"";
        var ss = (s % 100 == 0) ? (s / 100) + ".00" : s/100 +"";
        var bs = (b % 100 == 0) ? (b / 100) + ".00" : b/100 +"";
        var args =  "/hsb_h/" + (hs) + "/hsb_s/" + (ss)  +"/hsb_b/" + (bs) ;
        this._addFilter("Hsb",args);
        this._redrawImage();
    },


    changeHSB: function(){
        var h = parseInt(this.iframe.dijit.byId("hueSlider").value);
        var s = parseInt(this.iframe.dijit.byId("satSlider").value);
        var b = parseInt(this.iframe.dijit.byId("brightSlider").value);
        this.iframe.dojo.byId("hueSpan").innerHTML = h;
        this.iframe.dojo.byId("satSpan").innerHTML = s;
        this.iframe.dojo.byId("brightSpan").innerHTML = b;

    },

    selectAllUrl : function(){
        this.iframe.dojo.byId("viewingUrl").select();
    },

    toggleHSB: function (){
        var x = this.iframe.dijit.byId("hsbDialog");
        x.show();
    },




    /***************************************************************************
     *
     * Filters and Image Drawing
     *
     **************************************************************************/


    // We only add filters once
     _addFilter:function(filterName, parameters){
        var filter = [filterName, parameters];
        var newFilters = new Array();
        var isResize = (filterName == "Resize");
        var hasJpeg= (filterName == "Jpeg") ? true : false;
        var hasWebP= (filterName == "WebP") ? true : false;
        var added = false;


        if(filterName=="Resize"){
            this._removeFilter("Crop");
        }

        for(i=0;i<this.filters.length ;i++){
            // if we are adding Jpeg, it always needs to be last in the chain
            if(this.filters[i][0] == "Jpeg" ){
                hasJpeg = true;
                continue;
            }
            else if(this.filters[i][0] == "WebP" ){
                hasWebP = true;
                continue;
            }

            // if we are resizing, we need to kill any crops
            if(isResize && this.filters[i][0] == "Crop" ){
                continue;
            }
            // check if it is already added, if so, replace
            if(filterName == this.filters[i][0]){
                newFilters.push(filter);
                added = true;
            }
            else{
                newFilters.push(this.filters[i]);
            }
        }
        if(!added){
            newFilters.push(filter);
        }
        if(hasJpeg && filterName != "Jpeg"){
            newFilters.push(["Jpeg", "/jpeg_q/" + this.compressionValue]);
        }
        if(hasWebP && filterName != "WebP"){
            newFilters.push(["WebP", "/webp_q/"+ this.compressionValue]);
        }
        
        this.filters = newFilters;

        this.writeFilterList();
    },


    removeFilterClick:function(filterName){
        this._removeFilter(filterName);
        
        
        
        this._redrawImage();


    },







     _removeFilter:function(filterName){
        if(this.filters == undefined || this.filters.length ==0){
            return;
        }
        var newArray = new Array();
        for(i=0;i<this.filters.length ;i++){
            var arr = this.filters[i];
            if(filterName != arr[0]){
                newArray.push(arr);
            }
        }

        this.filters = newArray;


        if((filterName =="Resize")){
            this._removeFilter("Crop");
            this.iframe.dijit.byId("showScaleSlider").setValue(100);
            // set the img dimentions so user can see
        }
        else if(filterName=="Flip"){
            this.iframe.dijit.byId("flip").set("value", false, false);
        }
        else if(filterName=="Rotate"){
            this.iframe.dijit.byId("rotate").set("value", 0, false);
        }
        else if(filterName=="Grayscale"){
            this.iframe.dijit.byId("grayscale").set("value", false, false);
        }
        else if(filterName=="Jpeg" || filterName=="WebP"){
            this.iframe.dijit.byId("compression").set("value", "none", false);
        }
        else if(filterName=="Hsb"){
            this.iframe.dojo.byId("hueSpan").innerHTML = 0;
            this.iframe.dojo.byId("satSpan").innerHTML = 0;
            this.iframe.dojo.byId("brightSpan").innerHTML = 0;
            this.iframe.dijit.byId("hueSlider").value = 0;
            this.iframe.dijit.byId("satSlider").value = 0;
            this.iframe.dijit.byId("brightSlider").value = 0;

        }
        else if(filterName == "Crop"){

            var crp = this.iframe.dijit.byId('cropBtn');
            dojo.attr(crp, "disabled", false);
            crp.focus();




        }
        this.writeFilterList();
    },

     writeFilterList: function(){
        var div = this.iframe.dojo.byId("filterListDiv");
        var con = this.iframe.dojo.byId("filtersListContainer");
        dojo.empty(con);

        for(i=0;i<this.filters.length ;i++){
            var x = this.iframe.dojo.create("div", {id:this.filters[i][0] + "Div",innerHTML:this.filters[i][0],
                onclick:function(){
                    var x= this.id.replace("Div", "");
                    window.top._dotImageEditor.removeFilterClick(x);

                }}, con);
            dojo.attr(x, "class", "filterListItem");
        }
    },


    initIframe: function(){
        if(!this.iframe || this.iframe==undefined){
            this.iframe = dojo.byId("imageToolIframe").contentWindow;
            
        }
        this._redrawImage();
    },

    changeViewingUrl: function(){
        var newUrl = this.iframe.dijit.byId("viewingUrl").getValue();
        
        this._redrawImage(newUrl);
    },


    _redrawImage: function (useUrl){
        var x = 0;
        while(window.top._dotImageEditor.painting){

            if(++x > 100000){
                return;
            }
        }


        this.painting = true;
        // this.iframe.dojo.attr(this.iframe.dijit.byId('showScaleSlider'),
        // "disabled", true);
        this.iframe.dojo.attr(this.iframe.dojo.byId('imageViewPort'), "class", "loader");
        var target = this.iframe.dojo.byId('me');
        dojo.fadeOut({node:target}).play();



        // if the slider is drawn, lock it

        this._cropOff();



        // build url args
        var f = "",args="";
        for(i=0;i<this.filters.length ;i++){
            f+=this.filters[i][0] ;
            if(i < this.filters.length -1){
                f+= ",";
            }
            args+= this.filters[i][1];
        }

        var img = new Image();
        // set our onload before loading...
        img.onload = function(){
            var ctx = window.top._dotImageEditor ;
            var target = ctx.iframe.dojo.byId('me');
            target.src=img.src;

            dojo.fadeIn({node:target}).play();

            ctx.iframe.dojo.attr(ctx.iframe.dojo.byId("imageViewPort"), "class", "");
            ctx.painting = false;
            ctx.inited=false;
            ctx.initViewport();
            
            ctx.iframe.dojo.byId("fileSizeDiv").className="";
            var xhr = new XMLHttpRequest();
            xhr.open('HEAD', target.src, true);
            xhr.onreadystatechange = function(){
              if ( xhr.readyState == 4 ) {
                if ( xhr.status == 200 ) {
                    var oldFileSize = parseInt(ctx.iframe.dojo.byId("fileSizeDiv").innerHTML);
                    this.fileSize=parseInt(xhr.getResponseHeader('Content-Length')/1024);
                    ctx.iframe.dojo.byId("fileSizeDiv").innerHTML=  this.fileSize + "k";
                    if(oldFileSize>0 && oldFileSize > this.fileSize){
                        ctx.iframe.dojo.byId("fileSizeDiv").className= "smallerThan";
                    }else if(oldFileSize>0 && oldFileSize < this.fileSize){
                        ctx.iframe.dojo.byId("fileSizeDiv").className="biggerThan";
                    }
                } else {
                  alert('error reloading image:' + xhr.status);
                }
              }
            };
            xhr.send(null);
            
        };

        
        var url = useUrl;
        
        if(!useUrl){
            url= this.baseFilterUrl;
            if(this.filters.length > 0){
                url+= "/filter/" + f + args;
            }
        }
        img.src = url +"?test=" + new Date().getTime()
        
        this.currentUrl = url;
        this.iframe.dijit.byId("viewingUrl").set("value", (url.includes("://") ? url : location.protocol +"//"+  location.host + url), false);
        this.iframe.dojo.byId("showLink").href =  url.includes("://") ? url : location.protocol +"//"+  location.host + url;
        return;
            

        img.src = url +"?test=" + new Date().getTime()
        this.currentUrl = url;
        this.iframe.dijit.byId("viewingUrl").set("value",  url, false);
        this.iframe.dojo.byId("showLink").href =  url;


    },




    _resetSavedFiles:function(){
        dojo.xhrGet({
            url:this.ajaxUrl + "?action=reset&r=" +_rand(),
            preventCache:true,
            load:function(data){


            }
        });
    }


});