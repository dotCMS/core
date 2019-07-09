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

    templatePath: dojo.moduleUrl("dotcms", "dijit/form/FileAjaxUploader.jsp"),
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
    accept:'*/*',
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

        
        xhr.onload = (self => {
            return () => {
                if (xhr.status == 200) {
                    
                    let data = JSON.parse(xhr.responseText);
                    console.log("this", self);
                    console.log("data", data.tempFiles[0]);
                    this.fileNameField.value=data.tempFiles[0].id;
                    dojo.style(this.fileUploadStatus, { display: 'none' });
                    
                } else {
                    alert("Error! Upload failed");
                    console.log("xhr error:", xhr);
                }
            };
        })(this);
        
        xhr.onerror = function() {
            alert("Error! Upload failed. Can not connect to server.");
            console.log("xhr error:", xhr);
        };


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
        xhr.open("POST", "/api/v1/temp", true);
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

        dojo.byId(this.name+"_form").reset();
        this.onRemove(this);
    },
    
    _info: function () {
        console.log(this);
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



    _fileUploadFinished: function () {
        if(this.fileNameVisible){
           dojo.style(this.fileNameDisplayField, { display: '' });
        }
        dojo.style(this.fileUploadForm, { display: 'none' });
        dojo.style(this.fileUploadStatus, { display: 'none' });
        dojo.style(this.fileUploadRemoveButton, { display: '' });    
        FileAssetAjax.clearFileUploadStatus(this.name, function (data) {
            var maxSize = document.getElementById("maxSizeFileLimit");
            maxSize.value=data;            
        });

        this.onUploadFinish(this.fileName, this);

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