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
    identifier:'0',
    inited: false,
    painting : false,
    thumbnailWidth:250,
    thumbnailHeight:250,
    thumbDivId:'',
    fieldName:'fileAsset',
    saveAsIncrement:1,
    editImageText:"Edit Image",
    binaryFieldId:'',
    fieldContentletId:'',
    saveAsFileName:'',
/*
 * _loadCss:function () { var e = dojo.create("link"); e.href = this.cssUrl;
 * e.type = "text/css"; e.rel = "stylesheet"; e.media = "screen";
 * document.getElementsByTagName("head")[0].appendChild(e); },
 */
    _initBaseFilterUrl: function(){
        if(this.inode != '0'){
            this.baseFilterUrl+= "/" + this.inode;
        }
        else if(this.identifier != '0'){
            this.baseFilterUrl+= "/" + this.identifier;
        }
        if(this.fieldName != undefined){
            this.baseFilterUrl+= "/" + this.fieldName;
        }
        if(this.inode != '0'){
            this.baseFilterUrl+= "/byInode/1";
        }

        this.currentUrl = this.baseFilterUrl;
        console.log("baseFilterUrl: " + this.baseFilterUrl);
    },


    postCreate: function(){
        window.top._dotImageEditor = this;
        // this._loadCss();
        this.filters = new Array();

        var where= (this.parentNode != undefined) ? dojo.byId(this.parentNode) : this.domNode;
        // create divs

        this.thumbnailDiv = dojo.create('div' ,{
            style:"width:" + (this.thumbnailWidth+4)+ "px;height:" + (this.thumbnailHeight+4) +"px"},where);

            dojo.attr(this.thumbnailDiv, "class", "thumbnailDiv");


        this.editFileWindow = dojo.create('div' ,{
            innerHTML:this.editImageText},this.thumbnailDiv);

            dojo.attr(this.editFileWindow, "class", "editImageText");


        this.thumbnailImage = dojo.create("img" , {
            style:"width:" + this.thumbnailWidth + "px;height:" + this.thumbnailHeight +"px;"},this.thumbnailDiv);





        this._initBaseFilterUrl();
        this.setThumbnail();

        dojo.connect(this.thumbnailDiv , "onmouseover", this, "setDivHover");
        dojo.connect(this.thumbnailDiv , "onmouseout", this, "setDivOut");
        dojo.connect(this.thumbnailDiv , "onclick", this, "createImageWindow");

        // this.createImageWindow();
        // console.log(this);
    },



    /***************************************************************************
     *
     * Thumbnail preview
     *
     **************************************************************************/



    setDivHover:function(){
        dojo.attr(this.thumbnailDiv, "class", "thumbnailDivHover");
        dojo.style(this.editFileWindow, "opacity", "1");
        dojo.style(this.editFileWindow, "color", "black");

    },
    setDivOut:function(){
        dojo.attr(this.thumbnailDiv, "class", "thumbnailDiv");
        dojo.style(this.editFileWindow, "opacity", ".4");
    },


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

            thumbUrl = beforeFilter + "filter/" + filter + ",Thumbnail/" + afterFilter +"/thumbnail_w/" + this.thumbnailWidth+ "/thumbnail_h/" + this.thumbnailHeight;
        }
        else{
            if(thumbUrl.indexOf("?") <0){
                //thumbUrl+="?";
            }
            thumbUrl+= "/filter/Thumbnail/thumbnail_w/" + this.thumbnailWidth+ "/thumbnail_h/"+ this.thumbnailHeight;
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

        }

        //this.thumbnailImage.src =  "#"; // thumbUrl;
        this.thumbnailImage.src =  thumbUrl ;

    },



    createImageWindow: function(){
        this.tabindex = 0;
        this.thumbnailDiv.tabindex=0;





        // clean up any old image editors laying around
        this._cleanUpImageEditor();
        window.top._dotImageEditor = this;
        var url = this.imageToolJsp + "?inode=" + this.inode + "&fieldName="+ this.fieldName ;
        console.log("url=" + url);
        this.imageEditor = new dijit.Dialog({
              title: "Image Editor",
              content: "<div id='iFrameWrapper'><iframe scrolling='no' src='#' height='0' id='imageToolIframe' width='0' frameborder='0' style='width:0px;height:0px;overflow:hidden;'></iframe></div>",
              style:"position:absolute;top:10%;bottom:10%;left:10%;right:10% ;padding:0;margin:0;",
              id:"imgDialog",
              widgetId:"imgDialog",
              draggable:false,
              refocus:false

          });

        dojo.style(this.imageEditor.closeButtonNode, "visibility", "hidden");
        this.imageEditor.show();
        var frame=dojo.byId("imageToolIframe");
        console.log("frame:" + frame);

        var parent = frame.parentNode;
        


        console.log("parent:" + parent);

        frame.src = url;



    },

    /**
     * This function runs on pageload from the image_tool.jsp page. It loads the
     * underlying images to get w/h info from them.
     *
     */
    initViewport: function(){



        // make the iframe all big
        var frame=dojo.byId("imageToolIframe");
        console.log("ctx : " + this);
        console.log("frame : " + frame);

        dojo.attr(frame, {
            style:"width:100%;height:100%; "
            }
        );

        //if we are a File Asset
        if(this.binaryFieldId == ''){
            this.iframe.dojo.style(this.iframe.dojo.byId("saveAsSpan"), "display", "inline");
            this.iframe.dijit.byId("jpeg").disabled = true;
            this.iframe.dijit.byId("jpeg").focus();
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
        console.log("imageViewPort.class:" + dojo.attr(this.iframe.dojo.byId("imageViewPort"), "class"));
        console.log("imageViewPort.display:" + dojo.style(this.iframe.dojo.byId("imageViewPort"), "display"));
    },


    baseAndShowLoaded : function(){
        var showImage = this.iframe.dojo.byId("me");
        var baseImage = this.iframe.dojo.byId("baseImage");

        if(showImage.complete && baseImage.complete && !this.imagesLoaded){
            this.imagesLoaded=true;
            console.log("showImage:" + showImage);
            console.log("baseImage:" + baseImage);

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
            console.log("showScaleSlider:" + x.getValue());
        }

    },






    closeImageWindow : function(e){
        if(dojo.isIE){
            if(this.imageEditor){
                try{
                    this.imageEditor.destroy();
                }catch(e){
                    console.log(e);
                }
            }
        }else{
            this._cleanUpImageEditor();
        }
    },


    /**
     * cleans up old references, resets imageEditor
     */
    _cleanUpImageEditor : function (e){

        window.top._dotImageEditor = null;
        // if we have an Image Editor
        if(this.imageEditor){
            // this.setThumbnail();
            try{
                this.imageEditor.destroyRecursive();
            }
            catch(e){
                console.log(e);
                try{
                    this.imageEditor.destroy();
                }
                catch(ex){
                    console.log(e);
                }
            }
            this.iframe =null;
            this.inited=false;
            this.painting =false;
            this.filters=new Array();
            this.saveAsIncrement=1;
            this.imagesLoaded=false;
            this.resizeFilter=false;
        }
    },







    /***************************************************************************
     *
     * Button Actions
     *
     **************************************************************************/



    addToClipboard : function(){
        var fileUrl = this.cleanUrl(this.currentUrl);
        var url = (this.ajaxUrl.indexOf("?")>-1) ? this.ajaxUrl + "&"  : this.ajaxUrl + "?";

        url+="action=setClipboard&";
        url+="fileUrl=" + fileUrl+"&";
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
            this.saveBinaryImage();
        }
        else{
            this.saveFileImage();
        }
    },



    /**
     * This saves an image
     * that lives on a contentlet
     *
     */

    saveBinaryImage: function(){
        // http://jira.dotmarketing.net/browse/DOTCMS-6191
        var field=this.binaryFieldId;
        if(this.fieldContentletId.length>0) {
            field=this.fieldContentletId;
        }

        var url =   this.cleanUrl(this.currentUrl) ;
        url = (url.indexOf("?")>-1) ? url + "&"  : url + "?";
        url += (field.length > 0) ? "&binaryFieldId=" +field : "";
        url += "&_imageToolSaveFile=true";
        console.log("this.ajaxUrl:" + this.ajaxUrl);
        dojo.xhrGet({
            url:url,
            fileName:this.saveAsFileName,
            inode:this.inode,
            preventCache:true,
            sync:true,
            binaryFieldId:field,
            ajaxUrl:this.ajaxUrl,
            load:function(){
                var url = this.ajaxUrl   + "?action=save&binaryFieldId=" + field + "&inode=" + this.inode + "&fileName=" + this.fileName;
                console.log("saving:" + url);
                dojo.xhrPost({
                    sync:true,
                    url:url,
                    handle:function(data){
                        window.parent.document.getElementById(field).value=fileName;
                    },
                    preventCache:true,
                    binaryFieldId:field,
                    ajaxUrl:this.ajaxUrl,
                    inode:this.inode
                });
            },
            error:function(data){
                alert(data);

            }

        });


        this.setThumbnail();

        // close without wiping out the saved value
        this.closeImageWindow();
    },









    /**
     * Save as passes a var:_imageToolSaveFile to the binary servlet wit// http://jira.dotmarketing.net/browse/DOTCMS-6191
        var field=this.binaryFieldId;
        if(this.fieldContentletId.length>0) {
            field=this.fieldContentletId;
        }h all the other
     * params.  This saves the file handle in the users session.  We look for this
     * file and save it when the user checks the file or content in
     */
    saveFileImage: function(){
        this.painting = false;

        //if this is not a png
        if(this.saveAsFileName.toLowerCase().indexOf(".jpg") >-1){
            this.iframe.dijit.byId("jpeg").checked=true;
            this._removeFilter("Gif");
            this._addFilter("Jpeg", "/jpeg_q/85");
            this._redrawImage();
        }
        else if(this.saveAsFileName.toLowerCase().indexOf(".gif") >-1){
            this.iframe.dijit.byId("jpeg").checked=false;
            this._removeFilter("Jpeg");
            this._addFilter("Gif", "/gif/2");
            this._redrawImage();
        }
        else if(this.saveAsFileName.toLowerCase().indexOf(".png") >-1){
            this.iframe.dijit.byId("jpeg").checked=false;
            this._removeFilter("Jpeg");
            //this._addFilter("Gif", "gif=2");
            this._redrawImage();
        }
        var x = this.cleanUrl(this.currentUrl);
        
        var url = x + "?_imageToolSaveFile=true";
        console.log(url);
        //console.log(url);
        //console.log(url);
        dojo.xhrGet({
            url:url,
             preventCache:true,
             sync:true,
             load:function(data){
                 var ctx = window.top._dotImageEditor ;
                 window.top.dojo.byId("_imageToolSaveFile").value = data;
             }
        });
        this.setThumbnail();

        // close without wiping out the saved value
        setTimeout("window.top._dotImageEditor.closeEditorWhenRedrawFinish()", 500);
    },

    closeEditorWhenRedrawFinish: function() {
        if (this.painting) {
            setTimeout("window.top._dotImageEditor.closeEditorWhenRedrawFinish()", 500);
        } else {
            this.closeImageWindow();
        }
    },

    doDownload: function(){
        var x =this.cleanUrl(this.currentUrl);
        var aj =this.iframe.dojo.byId("actionJackson");
        var url = (this.ajaxUrl.indexOf("?")>-1) ? this.ajaxUrl + "&"  : this.ajaxUrl + "?";
        url = url + "r=" +_rand()+ "&action=download&fileUrl=" + x;
        console.log(url);
        aj.src=url;

    },




    showSaveAsDialog: function(ext){
        if(ext.indexOf("jpg") >-1){
            this._removeFilter("Gif");
            this._addFilter("Jpeg", "/jpeg_q/85");
            this._redrawImage();
        }
        else if(ext.indexOf("gif") >-1){
            this._removeFilter("Jpeg");
            this._addFilter("Gif", "/gif/2");
            this._redrawImage();
        }
        else if(ext.indexOf("png") >-1){
            this.iframe.dijit.byId("jpeg").checked=false;
            this._removeFilter("Jpeg");
            this._removeFilter("Gif");
            if(this.filters.length ==0){
                this._addFilter("Png", "");
            }
            this._redrawImage();
        }
        else{
            alert("invalid extension")
            return;
        }


        var x = this.cleanUrl(this.currentUrl);

        var url = this.currentUrl + "?_imageToolSaveFile=true&r=" +_rand();


        dojo.xhrGet({
            url:url,
            preventCache:true,
            load:function(data){

            }
        });


        saveAsDia = this.iframe.dijit.byId("saveAsDialog");

        if(this.saveAsFileName != "0"){
            var fakename = this.saveAsFileName;

            // strip extension
            if(fakename.indexOf(".")>-1){
                fakename=   fakename.substring(0, fakename.lastIndexOf("."));
            }

            // increment version
            var re = new RegExp("_[0-9]+");
            if(fakename.match(re)){
                var x = fakename.substring(fakename.lastIndexOf("_")+1, fakename.length);
                this.saveAsIncrement =  (this.saveAsIncrement > parseInt(x) + 1) ? this.saveAsIncrement :parseInt(x) + 1;
                fakename = fakename.substring(0,fakename.lastIndexOf("_"));

            }


            fakename = fakename + "_" + this.saveAsIncrement ;
            this.iframe.dojo.attr(this.iframe.dojo.byId("saveAsFileExt"), "innerHTML", "." + ext);

            this.iframe.dojo.attr(this.iframe.dojo.byId("saveAsName"), "value", fakename );

        }


        dojo.style(this.iframe.dojo.byId("saveAsButtonCluster"), "display", "block");
        dojo.style(this.iframe.dojo.byId("saveAsProgress"), "display", "none");
        dojo.style(this.iframe.dojo.byId("saveAsFailBtn"), "display", "none");
        dojo.style(this.iframe.dojo.byId("saveAsSuccessBtn"), "display", "none");
        dojo.style(this.iframe.dojo.byId("saveAsFailMsg"), "display", "none");
        dojo.style(this.iframe.dojo.byId("saveAsSuccessMsg"), "display", "none");
        saveAsDia.show();
    },


    doSaveAs: function(){


        if(!this.iframe.dijit.byId("saveAsName").isValid()){

            alert("filename invalid:\nA-z, 0-0 & _");
            this.iframe.dojo.byId("saveAsFinalBtn").disabled = false;
            return;
        }
        var fileName=this.iframe.dojo.byId("saveAsName").value + this.iframe.dojo.byId("saveAsFileExt").innerHTML ;
        this.iframe.dojo.byId("saveAsFinalBtn").disabled = true;


        var x =this.cleanUrl(this.currentUrl);
        var aj =this.iframe.dojo.byId("actionJackson");
        var url = (this.ajaxUrl.indexOf("?")>-1) ? this.ajaxUrl + "&"  : this.ajaxUrl + "?";
        url+=  "&fileName=" + fileName
        url+=  "&action=saveAs";
        url+=  "&fileUrl=" + x;
        url+=  "&inode=" + this.inode;




        dojo.style(this.iframe.dojo.byId("saveAsButtonCluster"), "display", "none");
        dojo.style(this.iframe.dojo.byId("saveAsProgress"), "display", "block");
        dojo.xhrGet({
            url:url,
            handleAs: "text",
            preventCache:true,
            handle: function(data, ioargs){
                var ctx = window.top._dotImageEditor ;
                dojo.style(ctx.iframe.dojo.byId("saveAsProgress"), "display", "none");

                ctx.saveAsIncrement++ ;
                if(data.toLowerCase().indexOf("failure") > -1){
                    dojo.style(ctx.iframe.dojo.byId("saveAsFailMsg"), "display", "block");
                    dojo.style(ctx.iframe.dojo.byId("saveAsFailBtn"), "display", "block");
                    return;
                }
                dojo.style(ctx.iframe.dojo.byId("saveAsSuccessBtn"), "display", "block");
                dojo.style(ctx.iframe.dojo.byId("saveAsSuccessMsg"), "display", "block");
                return;


            }


        });



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
        //if(confirm("Crop this?")){
                //this.iframe.dijit.byId("constrain").checked=false;
                this.doCrop();
        //  }else{
            //  this.iframe.dijit.byId("constrain").checked=false;
            //  this._cropOff();
            //}

        }
        else{
            this._cropOn();
        }
    },



    doConstrain : function(){
        var checked = dojo.attr(this.iframe.dojo.byId("constrain"), "checked");
        if(!checked)return;
        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;
        dojo.attr(this.iframe.dojo.byId("cropWidth"),"value", Math.floor(width/2));
        dojo.attr(this.iframe.dojo.byId("cropHeight"),"value", Math.floor(height/2));


    },
    setCropHeightFromWidth : function(){
        var checked = dojo.attr(this.iframe.dojo.byId("constrain"), "checked");
        if(!checked)return;
        var cw = dojo.attr(this.iframe.dojo.byId("cropWidth"),"value");
        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;

        dojo.attr(this.iframe.dojo.byId("cropHeight"),"value", Math.floor(height * (cw/width) ));
    },

    setCropWidthFromHeight: function(){
        var checked = dojo.attr(this.iframe.dojo.byId("constrain"), "checked");
        if(!checked)return;
        var ch = dojo.attr(this.iframe.dojo.byId("cropHeight"),"value");
        var coord = dojo.coords(this.iframe.dojo.byId("me"));
        var width = coord.w;
        var height = coord.h;

        dojo.attr(this.iframe.dojo.byId("cropWidth"),"value", Math.floor(width * (ch/height) ));
    },





    // turns cropping on
    _cropOn: function(){
        dojo.attr(this.iframe.dojo.byId("constrain"), "disabled", true);
        this.iframe.dojo.byId("constrain").blur();
        dojo.style(this.iframe.dojo.byId("cropBtn"), "border", "red 1px solid");
        if(this.crop && this.crop != undefined){
            return;
        }


        //dojo.attr(this.iframe.dijit.byId('constrain'), "disabled", true);


        this.crop = new dotcms.dijit.image.Crop({
            hoverable:true
        }, this.iframe.dojo.byId("me"));
        this.crop.showCropBox();

    },
    _cropOff: function(){
        dojo.attr(this.iframe.dojo.byId("constrain"), "disabled", false);
        this.iframe.dojo.byId("constrain").blur();
        if(!this.crop || this.crop ==undefined){
            return;
        }
        dojo.style(this.iframe.dojo.byId("cropBtn"), "border", "red 0px dotted");
        //dojo.attr(this.iframe.dijit.byId('constrain'), "disabled", false);
        // this.iframe.dojo.byId("dojoxGlobalResizeHelper").destroy();
        var img = this.iframe.dojo.byId("me");

        this.crop.destroy();
        this.crop=null;
        this.iframe.dojo.byId("imageViewPort").appendChild(img);
    },

    doCrop: function(){
        this.iframe.dojo.byId("cropBtn").blur();
        dojo.attr(this.iframe.dijit.byId('cropBtn'), "disabled", true);
        //dojo.attr(this.iframe.dijit.byId('constrain'), "disabled", true);
        var showImage = this.iframe.dojo.byId("me");
        var baseImage = this.iframe.dojo.byId("baseImage");

        pc = dojo.coords(this.crop.picker);
        vp = dojo.coords(this.iframe.dojo.byId("imageViewPort"));
        sTop = this.iframe.dojo.byId("imageViewPort").scrollTop;
        sLeft = this.iframe.dojo.byId("imageViewPort").scrollLeft;
        var x = pc.l - vp.l + sLeft;
        var y = pc.t - vp.t + sTop;

        var sw = parseInt(this.iframe.dojo.byId("displayImageWidth").value);
        var sh = parseInt(this.iframe.dojo.byId("displayImageHeight").value);


        var w = (x + pc.w > sw) ? sw -x-1 : pc.w;

        var h = (y + pc.h > sh) ? sh - y-1: pc.h;
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
        //console.log(val);
        //console.log(showImageCoords.h);
        //console.log(showImageCoords.w);
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

    toggleJpeg: function(){
        var x = this.iframe.dijit.byId("jpeg");

        if(x.checked){
            this._addFilter("Jpeg", "/jpeg_q/85");
        }
        else{
            this._removeFilter("Jpeg");
        }
        this._redrawImage();
    },
    /*
    toggleConstrain: function(){
        var x = this.iframe.dijit.byId("constrain");

        if(x.checked){
            if(this.crop){

            }
        }
        else{
            this._removeFilter("Jpeg");
        }
        this._redrawImage();
    },
    */
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
        var added = false;


        if(filterName=="Crop"){

            //dojo.attr(this.iframe.dijit.byId('constrain'), "checked", false);
            //dojo.attr(this.iframe.dijit.byId('constrain'), "disabled", true);

        }
        else if(filterName=="Resize"){
            this._removeFilter("Crop");
        }

        for(i=0;i<this.filters.length ;i++){
            // if we are adding Jpeg, it always needs to be last in the chain
            if(this.filters[i][0] == "Jpeg" ){
                hasJpeg = true;
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
            newFilters.push(["Jpeg", "/jpeg_q/75"]);
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
        else if(filterName=="Jpeg"){
            this.iframe.dijit.byId("jpeg").setValue(false);
        }
        else if(filterName=="Flip"){
            this.iframe.dijit.byId("flip").setValue(false);
        }
        else if(filterName=="Rotate"){
            this.iframe.dijit.byId("rotate").setValue(0);
        }
        else if(filterName=="Grayscale"){
            this.iframe.dijit.byId("grayscale").setValue(false);
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
            //dojo.attr(this.iframe.dijit.byId('constrain'), "disabled", false);
            //dojo.attr(this.iframe.dijit.byId('constrain'), "checked", false);
            var crp = this.iframe.dijit.byId('cropBtn');
            dojo.attr(crp, "disabled", false);
            crp.focus();
            dojo.attr(this.iframe.dojo.byId("constrain"), "disabled", false);
            //this.iframe.dojo.byId("constrain").focus();



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
            this._redrawImage();
        }
    },



    _redrawImage: function (){

        var x = 0;
        while(this.painting){
            x++;
            if(x > 100000000){
                alert("redrawing image, please wait");
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


            //ctx.iframe.dojo.style(ctx.iframe.dojo.byId("me"), "display", "block");
            ctx.iframe.dojo.attr(ctx.iframe.dojo.byId("imageViewPort"), "class", "");
            if(f.indexOf("Crop") <0){
                // ctx.iframe.dojo.attr(ctx.iframe.dijit.byId('showScaleSlider'),
                // "disabled", false);
            }
            ctx.painting = false;
            ctx.initViewport();
        };


        var url = this.baseFilterUrl;
        if(this.filters.length > 0){
            //url+=(this.baseFilterUrl.indexOf("?")<0) ? "?":"&";
        	//alert(f);
            url+= "/filter/" + f + args;
        }

        img.src = url ;
        this.currentUrl = url;
        this.iframe.dojo.byId("viewingUrl").value = location.protocol +"//"+ location.host + url;


    },




    _resetSavedFiles:function(){
        dojo.xhrGet({
            url:this.ajaxUrl + "?action=reset&r=" +_rand(),
            preventCache:true,
            load:function(data){


            }
        });
    }


})


