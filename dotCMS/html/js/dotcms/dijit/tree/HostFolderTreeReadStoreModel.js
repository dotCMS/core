dojo.provide("dotcms.dijit.tree.HostFolderTreeReadStoreModel");

dojo.require("dijit.tree.TreeStoreModel");

dojo.declare("dotcms.dijit.tree.HostFolderTreeReadStoreModel", null, {
	hostId: '',
	requiredPermissions:'',
	includeAll:false,
	themesOnly:false,

	constructor: function (options) {
		if (options != null) {
			if ((options.hostId != null) && (options.hostId != '') && (options.hostId != undefined) && (options.hostId != null)) {
				this.hostId = options.hostId;
			}
			if ((options.requiredPermissions != null) && (options.requiredPermissions != '') && (options.requiredPermissions != undefined) && (options.requiredPermissions != null)) {
				this.requiredPermissions = options.requiredPermissions;
			}
			if ((options.includeAll != null) && (options.includeAll != '') && (options.includeAll != undefined) && (options.includeAll != null)) {
				this.includeAll = options.includeAll;
			}
			if ((options.themesOnly != null) && (options.themesOnly != '') && (options.themesOnly != undefined) && (options.themesOnly != null)) {
				this.themesOnly = options.themesOnly;
			}
		}
	},

	getRoot: function(onItem, onError) {
		onItem({ identifier:"root", label:"Root", type: 'root', isRoot: true });
	},

	mayHaveChildren: function(/*dojo.data.Item*/ item) {
		return true;
	},

	getChildren: function(/*dojo.data.Item*/ parentItem, /*function(items)*/ onComplete, /*function*/ onError) {

		var parentItemId = this.getIdentity(parentItem);
		if(parentItem.isRoot) {
			if (this.hostId == ''){
				if(this.themesOnly) {
					BrowserAjax.getHostsWithThemes(dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				} else if(this.requiredPermissions == '' && this.includeAll){
				    BrowserAjax.getHostsIncludeAll(dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}else if(this.requiredPermissions == ''){
					BrowserAjax.getHosts(dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}else{
					BrowserAjax.getHostsByPermissions(this.requiredPermissions, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}
			}else{
				if(this.requiredPermissions == ''){
				    BrowserAjax.getHostSubfolders(this.hostId, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}else{
					BrowserAjax.getHostSubfoldersByPermissions(this.hostId, this.requiredPermissions, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}
			}
		} else if(parentItem.type == 'host') {
			if(this.requiredPermissions == ''){
				if(this.themesOnly){
				    BrowserAjax.getHostThemes(parentItemId, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				} else {
			    BrowserAjax.getHostSubfolders(parentItemId, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
				}
			}else{
				BrowserAjax.getHostSubfoldersByPermissions(parentItemId, this.requiredPermissions, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
			}
		} else if(parentItem.type == 'folder') {
			if(this.requiredPermissions == ''){
			    BrowserAjax.getFolderSubfolders(parentItemId, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
			}else{
				BrowserAjax.getFolderSubfoldersByPermissions(parentItemId, this.requiredPermissions, dojo.hitch(this, this._getChildrenCallback, parentItem, onComplete, onError));
			}
		}
	},

	_getChildrenCallback: function(parentItem, onComplete, onError, data) {
		onComplete(data);
	},

	isItem: function(/* anything */ something){
		return something.type == 'root' || something.type == 'folder' || something.type == 'host';	// Boolean
	},

	fetchItemByIdentity: function(/* object */ keywordArgs){
		BrowserAjax.findHostFolder(keywordArgs.identity, dojo.hitch(this, this._fetchItemByIdentityCallback, keywordArgs));
	},

	_fetchItemByIdentityCallback: function(/* object */ keywordArgs, object){
		keywordArgs.onItem(object);
	},

	getIdentity: function(/* item */ item){
		if(item.isRoot) {
			return "root";
		} else if(item.type == 'host') {
			return item.identifier;
		} else if(item.type == 'folder') {
			return item.inode;
		}
	},

	getLabel: function(/*dojo.data.Item*/ item){
		if(item.isRoot) {
			return "Root";
		} else if(item.type == 'host') {
			return item.hostName;
		} else if(item.type == 'folder') {
			return item.name;
		}
	},

	newItem: function(/* dojo.dnd.Item */ args, /*Item*/ parent, /*int?*/ insertIndex){
		throw "Unsupported operation this a read only store";
	},

	pasteItem: function(/*Item*/ childItem, /*Item*/ oldParentItem, /*Item*/ newParentItem, /*Boolean*/ bCopy, /*int?*/ insertIndex){
		throw "Unsupported operation this a read only store";
	}

});

