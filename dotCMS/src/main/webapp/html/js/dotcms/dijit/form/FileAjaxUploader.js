dojo.provide("dotcms.dijit.form.FileAjaxUploader");

dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit.form.Button");

/**
 * To include the dijit into your page
 *
 * JS Side
 *
 * <script type="text/javascript">
 *     dojo.require("dotcms.dijit.form.RolesFilteringSelect");
 *
 * ...
 *
 * </script>
 *
 * HTML side
 *
 * <div id="roleSelector" dojoType="dotcms.dijit.form.RolesFilteringSelect"></div>
 *
 *
 * To retrieve values from the dijit
 *
 * To retrieve the selected inode
 * dijit.byId('roleSelector').attr('value') // Will return something like '6fa459ea-ee8a-3ca4-894e-db77e160355e' the id of the role
 *
 * To retrieved the name of the selected role
 * dijit.byId('roleSelector').attr('displayedValue')  // Will return something like 'CMS Administrator' the name of the role
 *
 * To retrieved the selected js item
 * dijit.byId('roleSelector').attr('selectedItem') //Will return a js object like { id: '6fa459ea-ee8a-3ca4-894e-db77e160355e', name: 'CMS Administrator' }
 *
 *
 * Code
 *
 * Dijit
 *
 * dotCMS/html/js/dotcms/dijit/form/
 *                                  RolesFilteringSelect.js
 *                                  RolesFilteringSelect.html
 *
 *
 * Ajax Code used by the dijit
 *
 * com.dotmarketing.portlets.browser.ajax.RoleAjax.getRolesTree
 */
dojo.declare("dotcms.dijit.form.FileAjaxUploader", [dijit._Widget, dijit._Templated], {

    templatePath: dojo.moduleUrl("dotcms", "dijit/form/FileAjaxUploader.html"),
    widgetsInTemplate: true,

    name: '',
    fileName: '',
    fileNameExpression: '',
    dowloadRestricted: false,
    fileNameVisible: true,
    uploading: false,
    uploadCompleted:false,
    identifier:'0',
    inode:'0',
    fieldName:'fileAsset',
    lang:'0',
    invalidFileSelectedMessage: 'You have selected a non allowed file',
    replaceAssetNameWarning: 'Do you want to replace the existing asset-name "{oldAssetName}" with "{newAssetName}" ?',
    inodeShorty:'',
    idShorty:'',
    licenseLevel:100,
    accept:'*/*',
    maxFileLength:"-1",
    postMixInProperties: function (elem) {
        if((this.name == null) || (this.name == ''))
            this.name = this.id;

    },

    postCreate: function () {
        if(this.fileName != '') {
            if(this.fileNameVisible){
               dojo.style(this.fileNameDisplayField, { display: '' });
            } else {
               dojo.style(this.fileNameDisplayField, { display: 'none' });
            }
            dojo.style(this.fileUploadForm, { display: 'none' });
            dojo.style(this.fileUploadStatus, { display: 'none' });
            dojo.style(this.fileUploadRemoveButton, { display: '' });
            if(this.dowloadRestricted){
                dojo.style(this.fileUploadInfoButton, { display: 'none' });
            } else {
                dojo.style(this.fileUploadInfoButton, { display: '' });
            }
            this.fileNameDisplayField.innerHTML = this.fileName;
        }
    },

    _fileInfoTemplate: function () {
        return this.fileNameVisible ? '<div>\
        <table class="listingTable">\
            <tr class="alternate_1">\
                <td nowrap><b>File Name</b></td>\
                <td>{fileName}</td>\
            </tr>\
            <tr class="alternate_2">\
                <td><b>File Link</b></td>\
                <td><a target="_blank" href="{path}">{path}</a></td>\
            </tr>\
        </table>\
        </div>' :
            '<div>\
                    <table class="listingTable">\
                        <tr class="alternate_2">\
                            <td nowrap><b>File Link</b></td>\
                            <td><a target="_blank" href="{path}">{path}</a></td>\
                        </tr>\
                    </table>\
            </div>'
            ;
    },


    _showFileAssetUpdateConfirmation: function (oldAssetName, newAssetName) {
        let dialogTemplate =
            '<div>\
              <table>\
                <tr>\
                    <td colspan="2" align="center">{message}</td>\
                    </tr>\
                    <tr>\
                    <td>&nbsp;</td>\
                    </tr>\
                    <tr>\
                    <td colspan="2" align="center">\
                    <button dojoType="dijit.form.Button" onClick="doReplaceFileAssetName(\'{newAssetName}\');" type="button">Yes</button>\
                     &nbsp; &nbsp;\
                    <button dojoType="dijit.form.Button" onClick="doNotReplaceFileAssetName();" type="button">No</button>\
                    </td>\
                </tr>\
              </table>\
            </div>';

        let warningInfo = {};
        warningInfo['newAssetName'] = newAssetName;
        warningInfo['oldAssetName'] = oldAssetName;
        let message = dojo.replace(this.replaceAssetNameWarning, warningInfo);

        let fileInfo = {};
        fileInfo['newAssetName'] = newAssetName;
        fileInfo['oldAssetName'] = oldAssetName;
        fileInfo['message'] = message;

        let html = dojo.replace(dialogTemplate, fileInfo);
        this.fileInfoDialog.title = 'Replace Asset Name';
        let domObj = dojo._toDom( html );
        this.fileInfoDialog.setContent(domObj);
        this.fileInfoDialog.show();
    },


    _doFileUpload: function () {

        var file = this.fileInputField.files;
        if(file.length!=1){
            console.log("upload 1 file only - you have " + file.length)
            return;
        }
        file=file[0];

        var formData = new FormData();
        formData.append("files", file);

        var xhr = new XMLHttpRequest();

        xhr.upload.onprogress = (self => {
            return (e) => {
                var percentComplete = (e.loaded / e.total) * 100;
                self.progressBar.update({ progress: percentComplete });
            };
        })(this);

        xhr.onerror = function() {
            alert("Error! Upload failed. Can not connect to server.");
            console.log("xhr error:", xhr);
        };
        
        xhr.onload = (self => {
            return () => {
                dojo.style(this.fileUploadStatus, { display: 'none' });
                if (xhr.status == 200) {
                    
                    let data = JSON.parse(xhr.responseText);
                    console.log("this", self);
                    console.log("data", data.tempFiles[0]);
                    this.fileNameField.value=data.tempFiles[0].id;
                    dojo.style(this.fileUploadStatus, { display: 'none' });
                    
                    
                    self._postUploadCallback(data.tempFiles[0]);

                    //Only set fileName if file uploaded sucessfully
                    if(this.fileNameExpression != '') {
                        var expressions;
                        if(!dojo.isArray(this.fileNameExpression)) {
                            expressions = [ this.fileNameExpression ];
                        } else {
                            expressions = this.fileNameExpression;
                        }
                        for(var i = 0; i < expressions.length; i ++) {
                            var expression = expressions[i];
                            if(!this.fileInputField.value.match(expression)) {
                                alert(this.invalidFileSelectedMessage);
                                this.fileInputField.value = '';
                                return;
                            }
                        }
                    }

                    if(this._isFileAsset()){
                        // automatically set the file asset name
                        if(this.fileInputField && this.fileInputField.value){
                            let newAssetName = file.name;

                            let fileNameField = dijit.byId("fileName");

                            let oldAssetName = fileNameField.getValue();

                            if(oldAssetName == '' || oldAssetName == newAssetName ){
                                fileNameField.setValue(newAssetName);
                                let titleField = dijit.byId("title");
                                if(titleField &&  !titleField.getValue()){
                                    titleField.setValue(newAssetName);
                                }
                            } else {
                                this._showFileAssetUpdateConfirmation(oldAssetName, newAssetName);
                            }
                        }
                    }


                } else {
                    let data = JSON.parse(xhr.responseText);
                    if(data.message){
                        alert(data.message);
                    }else{
                        alert("Error! Upload failed");
                    }
                    //clean up binary field since upload failed
                    this.fileInputField.value = '';
                }
            };
        })(this);

        xhr.open("POST", "/api/v1/temp?maxFileLength=" + this.maxFileLength, true);
        xhr.send(formData);
        this.progressBar.update({ progress: 0 });
        dojo.style(this.fileUploadStatus, { display: '' });
    },

    _remove: function () {
        this.fileNameDisplayField.innerHTML = this.fileInputField.value = '';
        this.fileNameField.value = '---removed---';

        dojo.style(this.fileNameDisplayField, { display: 'none' });
        dojo.style(this.fileUploadForm, { display: '' });
        dojo.style(this.fileUploadRemoveButton, { display: 'none' });
        dojo.style(this.fileUploadInfoButton, { display: 'none' });
        if(document.getElementById('fileTextEditorDiv')){
            dojo.style("fileTextEditorDiv", { display: 'none' });
        }

		this.onRemove(this);

    },

    _info: function () {

        var fileInfo = {};
        fileInfo['fileName'] = this.fileName;
        fileInfo['path'] = location.protocol +"//"+ location.host + '/dA/';

        fileInfo['path'] += (this.identifier != '0') ? this.idShorty : this.inodeShorty;
        fileInfo['path'] += (this.id != 'fileAsset') ? '/' +  this.id : "";
        fileInfo['path'] += "/" + this.fileName +'?language_id='+this.lang;


        var html = dojo.replace(this._fileInfoTemplate(), fileInfo);

        this.fileInfoDialog.title = this.fileName;
        var domObj = dojo._toDom(html);
        this.fileInfoDialog.setContent(domObj);
        this.fileInfoDialog.show();
    },


    _isFileAsset : function(){

        return (
            dijit.byId("fileName")
            &&  dijit.byId("title")
            &&  dijit.byId("sortOrder")
            &&  dojo.byId("metaData_field")
        );

    },


    _postUploadCallback : function(fileData){



        var fieldRelatedData = {"fieldContentlet" : this.name,
                "fieldVarName" : this.id,
                "fileName" : fileData.fileName,
                "fileId"   : fileData.id};


        if(this.fileNameVisible){
            dojo.style(this.fileNameDisplayField, { display: '' });
         }
         dojo.style(this.fileUploadForm, { display: 'none' });
         dojo.style(this.fileUploadStatus, { display: 'none' });
         dojo.style(this.fileUploadRemoveButton, { display: '' });


        var elements = document.getElementsByName(fieldRelatedData['fieldContentlet']);

        for(var i=0; i<elements.length; i++) {
            if(elements[i].tagName.toLowerCase() =="input") {
                elements[i].value = fileData.id;
            }
        }

        var thumbnailParentDiv = document.createElement("div");
        thumbnailParentDiv.setAttribute("id",'thumbnailParent'+this.id);
        var fieldDiv = dojo.byId(this.id+'_field');
        if(fieldDiv.childNodes.length > 0){
            fieldDiv.insertBefore(thumbnailParentDiv,fieldDiv.childNodes[0])
        }else{
            fieldDiv.appendChild(thumbnailParentDiv);
        }

        this.onUploadFinish(this.fileName, this);



        if(!fileData.image){
            return;
        }



        if ( this.licenseLevel < 200 ){
            var newFileDialogTitle = "";

            var newFileDialogContent = '<div style="text-align:center;margin:auto;overflow:auto;width:700px;height:400px;">'
                + '<img src="/contentAsset/image/'+ fileData.id +'/' + this.id + '/"/>'
                + '</div>'
                + '<div class="callOutBox">'
                + '<%=LanguageUtil.get(pageContext,"dotCMS-Enterprise-comes-with-an-advanced-Image-Editor-tool") %>'
                + '</div>';

            if(dijit.byId(fileData.id +'_Dialog') == undefined){
                var newFileDialog = new dijit.Dialog({
                    id: fileData.id+'_Dialog',
                    title: newFileDialogTitle,
                    content: newFileDialogContent,
                    style: "overflow:auto;width:760px;height:540px;"
                });
            }

            var thumbNailImg = document.createElement("img");
            var thumbnailImage;

            if(!fieldRelatedData.fileName.toLowerCase().endsWith('.svg')) {
                thumbnailImage = "/contentAsset/image/"+fileData.id+ "/" + this.id +"/?filter=Thumbnail&thumbnail_w=300&thumbnail_h=300";
            }else{
                thumbnailImage = "/contentAsset/image/" + fileData.id + "/" + this.id;
            }

            thumbNailImg.setAttribute("src", thumbnailImage);
            thumbNailImg.setAttribute("onmouseover","dojo.attr(this, 'className', 'thumbnailDivHover');");
            thumbNailImg.setAttribute("onmouseout","dojo.attr(this, 'className', 'thumbnailDiv');");
            thumbNailImg.setAttribute("onclick","dijit.byId('"+fileData.id+'_Dialog'+"').show()");
            thumbnailParentDiv.appendChild(thumbNailImg);

        } else {


            var newImageEditor = new dotcms.dijit.image.ImageEditor({
                editImageText : "",
                tempId: fileData.id,
                fieldName : this.id,
                binaryFieldId : this.name,
                fieldContentletId : this.name,
                editImageText : "Edit Image",
                //class : "thumbnailDiv"+fieldRelatedData['fieldVarName'],
                //parentNode: thumbnailParentDiv
            });
            newImageEditor.placeAt(thumbnailParentDiv);

        }

    },

    onUploadStart: function (fileName, dijitReference) {
    },

    onUploadFinish: function (fileName, dijitReference) {
    },

    onRemove: function (dijitReference) {
    },

    uninitialize : function (event) {
    }







});