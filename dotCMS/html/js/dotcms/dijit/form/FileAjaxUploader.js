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
 * 	dojo.require("dotcms.dijit.form.RolesFilteringSelect");
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
 * 								 RolesFilteringSelect.js
 * 								 RolesFilteringSelect.html
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
	uploading: false,
	uploadCompleted:false,
	invalidFileSelectedMessage: 'You have selected a non allowed file',

	postMixInProperties: function (elem) {
		if((this.name == null) || (this.name == ''))
			this.name = this.id;

	},

	postCreate: function () {
		if(this.fileName != '') {
			dojo.style(this.fileNameDisplayField, { display: '' });
			dojo.style(this.fileUploadForm, { display: 'none' });
			dojo.style(this.fileUploadStatus, { display: 'none' });
			dojo.style(this.fileUploadRemoveButton, { display: '' });
			this.fileNameDisplayField.innerHTML = this.fileName;
		}
	},

	_doFileUpload: function () {

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
			//automatically set the file asset name
			if(this.fileInputField && this.fileInputField.value){
				var test = this.fileInputField.value;

				while(test.indexOf("/") > -1){
					test = test.replace("/","|");
				}
				while(test.indexOf("\\") > -1){
					test = test.replace("\\","|");
				}

				test = test.split("|")[test.split("|").length-1];
				var fileNameField = dijit.byId("fileName");

					fileNameField.setValue(test);

				fileNameField = dijit.byId("title");
				if(fileNameField &&  !fileNameField.getValue()){
					fileNameField.setValue(test);
				}
			}
		}
		this.onUploadStart(this.fileName, this);
		if(dojo.isIE || dojo.isChrome || dojo.isSafari){//DOTCMS-5046
			var ieFileName = this.fileInputField.value;
			if(ieFileName.indexOf("\\") >= 0){
				this.fileNameDisplayField.innerHTML = this.fileNameField.value = ieFileName.substring(ieFileName.lastIndexOf("\\")+1);
			}else{
				this.fileNameDisplayField.innerHTML = this.fileNameField.value = ieFileName;
			}
		}else{
			this.fileNameDisplayField.innerHTML = this.fileNameField.value = this.fileInputField.value;
		}
		this.uploading = true;
		this.form.submit();
		dojo.style(this.fileUploadStatus, { display: '' });
		this.progressBar.update({ progress: 0 });
		this._checkStatus();
	},

	_remove: function () {
		this.fileNameDisplayField.innerHTML = this.fileInputField.value = '';
		this.fileNameField.value = '---removed---'
		dojo.style(this.fileNameDisplayField, { display: 'none' });
		dojo.style(this.fileUploadForm, { display: '' });
		dojo.style(this.fileUploadRemoveButton, { display: 'none' });
		this.onRemove(this);
	},

	_checkStatus: function () {
		FileAjax.getFileUploadStatus(this.name,{async:true, callback:dojo.hitch(this,this._statusCheckCallback)});
	},


	_isFileAsset : function(){

		return (
			dijit.byId("fileName")
			&&  dijit.byId("title")
			&& 	dijit.byId("sortOrder")
			&&  dojo.byId("metaData_field")
		);

	},

	_statusCheckCallback: function (fileStats) {
		console.log(fileStats);
		if(fileStats != null){
			if(fileStats.error !=null){
				showDotCMSSystemMessage(fileStats.error);
				this._fileUploadError();
				return;
			}
			if(this.uploading && fileStats)
				this.progressBar.update({ progress: fileStats.percentComplete });

//			if(dojo.isIE || dojo.isChrome){//DOTCMS-5046
				if(this.uploading && fileStats.percentComplete < 100 )
					setTimeout(dojo.hitch(this, this._checkStatus), 1000);

				if(fileStats.percentComplete == 100 ){
					this.uploadCompleted = true;
					setTimeout(dojo.hitch(this, this._fileUploadFinished), 1000);
				}
//			}else{
//				if(this.uploading)
//					setTimeout(dojo.hitch(this, this._checkStatus), 1000);
//			}
		}else{
			if(this.uploading)
				setTimeout(dojo.hitch(this, this._checkStatus), 1000);
		}

	},
	_fileUploadStart: function () {
		if(!this.uploading || !this.uploadCompleted) return;
	},

	_fileUploadError: function () {

		dojo.style(this.fileUploadStatus, { display: 'none' });
		this.fileNameDisplayField.innerHTML = this.fileInputField.value = '';
		this.fileNameField.value = '';
		dojo.style(this.fileNameDisplayField, { display: 'none' });
		dojo.style(this.fileUploadForm, { display: '' });
		dojo.style(this.fileUploadRemoveButton, { display: 'none' });
		FileAjax.clearFileUploadStatus(this.name, function () {});
	},

	_fileUploadFinished: function () {

		dojo.style(this.fileNameDisplayField, { display: '' });
		dojo.style(this.fileUploadForm, { display: 'none' });
		dojo.style(this.fileUploadStatus, { display: 'none' });
		dojo.style(this.fileUploadRemoveButton, { display: '' });
		FileAjax.clearFileUploadStatus(this.name, function () {});
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

})