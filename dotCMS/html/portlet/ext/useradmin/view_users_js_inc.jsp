
<%@page import="com.dotmarketing.util.UtilMethods"%>

<script type="text/javascript" src="/dwr/interface/UserAjax.js"></script>
<script type="text/javascript" src="/dwr/interface/RoleAjax.js"></script>
<script type="text/javascript" src="/dwr/interface/TagAjax.js"></script>
<script type="text/javascript">

	dojo.require("dijit.Dialog");
	dojo.require("dijit.form.Form");
	dojo.require("dijit.form.TextBox");
	dojo.require("dijit.form.ValidationTextBox");
	dojo.require("dijit.layout.TabContainer");
	dojo.require("dijit.layout.ContentPane");
	dojo.require("dijit.form.Button");
	dojo.require("dijit.form.CheckBox");
	dojo.require("dijit.Tree");
	dojo.require("dojox.grid.DataGrid");
	dojo.require("dojo.data.ItemFileReadStore");
	dojo.require("dijit.dijit");
	dojo.require("dojox.data.JsonRestStore");

    dojo.require("dotcms.dojo.push.PushHandler");
    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

	//I18n messages
	var abondonUserChangesConfirm = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "abondon-user-changes-confirm")) %>';
	var passwordsDontMatchError = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "passwords-dont-match-error")) %>';
	var deleteYourOwnUserError = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-your-own-user-error")) %>';
	var deactivateYourOwnUserError = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "deactivate-your-own-user-error")) %>';
	var deleteUserConfirm = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-user-confirm")) %>';
	var userDeleted = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-deleted")) %>';
	var userDeleteFailed = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-delete-failed")) %>';
	var userRoles = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-roles")) %>';
	var addressSaved = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "address-saved")) %>';
	var removeAddressConfirmation = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "remove-address-confirmation")) %>';
	var addressDeleted = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "address-deleted")) %>';
	var userInfoSaved = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-info-saved")) %>';
	var phone = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "phone")) %>';
	var fax = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "fax")) %>';
	var cell = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cell")) %>';
	var userRolesSaved = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-roles-saved")) %>';
	var sameEmailAlreadyRegisteredErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-email-already-registered")) %>';
	var sameUserIdAlreadyRegisteredErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-id-already-registered")) %>';
	var nameColumn = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "name")) %>';
	var emailColumn = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Email")) %>';
	var addressColumn = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Address")) %>';
	var userSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Info-Saved")) %>';
	var userInfoSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Info-Saved")) %>';
	var userSaveFailedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Info-Save-Failed")) %>';
	var userCategoriesSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Categories-Saved")) %>';
	var userLocaleSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Locale-Saved")) %>';
	var userClicktrackingSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Clicktracking-Saved")) %>';
	var invalidAddresPhoneMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.user.address.invalid.phone")) %>';
	var invalidAddresFaxMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.user.address.invalid.fax")) %>';
	var invalidAddresCellMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.user.address.invalid.cell")) %>';
	var invalidEmail = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "please-enter-a-valid-email-address")) %>';
	var currentUserId = '<%= user.getUserId() %>';
	var layoutId = '<%=((Layout) request.getAttribute(WebKeys.LAYOUT)).getId()%>';

	<% if(authByEmail) { %>
	var authByEmail = true;
	<% } else { %>
	var authByEmail = false;
	<% }  %>

	var additionalVariablesCount = <%=additionalVariablesCount%>;


	//General initialization


	//Layout Initialization
	function  resizeRoleBrowser(){
	    var viewport = dijit.getViewport();
	    var viewport_height = viewport.h;

		var  e =  dojo.byId("borderContainer");
		if(e){
		   dojo.style(e, "height", viewport_height -150+"px");
		}
		var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
	    	bc.resize();
	    }


		var  d =  dijit.byId("userTabsContainer");
		if(d){
			try{
		       d.resize();
			}catch(x){
				//http://jira.dotmarketing.net/browse/DOTCMS-5151
			}
		}

		var  e =  dojo.byId("userTabsContainer");
        if(e){
       	  dojo.style(e, "height", viewport_height -155+"px");
        }


	}
	// need the timeout for back buttons

	//dojo.addOnLoad(resizeRoleBrowser);
	dojo.addOnLoad(function () {
		dojo.byId('userProfileTabs').style.display = 'none';
		dwr.util.useLoadingMessage("<%=LanguageUtil.get(pageContext, "Loading")%>....");
	});

	dojo.connect(window, "onresize", this, "resizeRoleBrowser");

	//Users list functions

    //Users grid Initialization
    var usersData = {
        identifier: 'id',
        label: 'id',
        items: [ { name: "Loading ...", email: "", id: "0" } ]
    };
    var usersStore = new dojo.data.ItemFileReadStore({data: usersData});

    var usersGridLayout = [[
        { name: nameColumn, field: 'name', width:'50%' },
        { name: emailColumn, field: 'email', width:'50%' },
    ]];

    //Initialization kicking the loading of users
    dojo.ready(function() {

        dojo.declare('dotcms.dojox.grid.DataGrid', dojox.grid.DataGrid, {
            onRowContextMenu: function(e) {

                if(enterprise) {

                    var selected = e.grid.getItem(e.rowIndex);
                    window.selectedUser = selected.id;
                    try {
                        usersGrid_rowMenu.bindDomNode(e.grid.domNode);
                    } catch (ex) {
                        console.error(ex);
                    }
                }
            }
        });

        window.usersDataGrid = new dotcms.dojox.grid.DataGrid({
            id: 'usersGrid',
            store: usersStore,
            structure: usersGridLayout,
            style: 'cursor: pointer; cursor: hand',
            autoHeight: true
        }, dojo.byId('usersGrid'));
        usersDataGrid.startup();

        //Connecting the action of clicking a user row
        dojo.connect( usersDataGrid, "onRowClick", function (evt) {
                var id = evt.grid.getItem(evt.rowIndex).id[0];
                editUser(id);
        });

        //Loading the grid for first time
        UserAjax.getUsersList(null, null, { start: 0, limit: 50 }, dojo.hitch(this, getUsersListCallback));
    });

	//Gethering the data from server and setting the grid to display it
	function getUsersListCallback (list) {

		dojo.byId('loadingUsers').style.display = 'none';
		dojo.byId('usersGrid').style.display = '';

		var usersData = {
				identifier: 'id',
				label: 'id',
				items: [  ]
			};
		list.data.each(function (record, idx) {
			usersData.items.push({ name: record.name, id: record.id, email: record.emailaddress });
		});
 		var usersStore = new dojo.data.ItemFileReadStore({data: usersData });
        usersDataGrid.setStore(usersStore);
	}

    var remotePublishUser = function () {
        if (window.selectedUser) {
            pushHandler.showDialog( "user_" + window.selectedUser);
        }
    };

    var remotePublishUsers = function () {
        pushHandler.showDialog( "users_", true );
    };

    var addToBundleUser = function () {
        if (window.selectedUser) {
            pushHandler.showAddToBundleDialog("user_" + window.selectedUser, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
        }
    };

    var addToBundleUsers = function () {
        pushHandler.showAddToBundleDialog("users_", '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>', true);
    };

	var filterUsersHandler;

	//Event handler then the user types to filter users
	function filterUsers() {

		//Canceling any other delayed request of filtering in case
		// the user typed more
		if(filterUsersHandler != null) {
			clearTimeout(filterUsersHandler);
		}

		//Executed in a delayed fashion to allow the user type more keystrokes
		//before loading the server
		filterUsersHandler = setTimeout('filterUsersDelayed()', 700);
	}

	//Executed after the user has typed some characters to filter users
	function filterUsersDelayed(){
		dojo.byId('loadingUsers').style.display = '';
		dojo.byId('usersGrid').style.display = 'none';
		var value = dijit.byId('usersFilter').attr('value');
		UserAjax.getUsersList(null, null, { start: 0, limit: 50, query: value }, dojo.hitch(this, getUsersListCallback));
	}

	//Event handler for clearing the users filter
	function clearUserFilter () {
		dojo.byId('loadingUsers').style.display = '';
		dojo.byId('usersGrid').style.display = 'none';
		dijit.byId('usersFilter').attr('value', '');
		UserAjax.getUsersList(null, null, { start: 0, limit: 50, query: '' }, dojo.hitch(this, getUsersListCallback));
	}

	//CRUD operations over users

	//Executed when a grid row is clicked
	dojo.addOnLoad(function () {
		<% if(request.getParameter("userId") != null) { %>
		editUser('<%=request.getParameter("userId")%>');
		<% } %>
	});

	var currentUser;
	function editUser(userId) {
		if(userChanged && currentUser && userId != currentUser.id &&
			!confirm(abandonUserChangesConfirm))
			return;
		dojo.byId('userProfileTabs').style.display = 'none';
		dojo.byId('loadingUserProfile').style.display = '';
		UserAjax.getUserById(userId, editUserCallback);
	}

	//Gathering the user info from the server and setting up the right hand side
	//of user info
	function editUserCallback(user) {

		//Global user variable
		currentUser = user;

		//SEtting user info form
		if(!authByEmail) {
			dijit.byId('userId').attr('value', user.id);
			dijit.byId('userId').setDisabled(true);
		} else {
			dojo.byId('userIdValue').innerHTML = user.id;
			dojo.byId('userId').value = user.id;
		}
		dojo.byId('userIdLabel').style.display = '';
		dojo.byId('userIdValue').style.display = '';
		dijit.byId('firstName').attr('value', user.firstName);
		dijit.byId('lastName').attr('value', user.lastName);
		dijit.byId('emailAddress').attr('value', user.emailaddress);
		dijit.byId('password').attr('value', '********');
		dijit.byId('passwordCheck').attr('value', '********');

		dojo.query(".fullUserName").forEach(function (elem) { elem.innerHTML = user.name; });

		userChanged = false;
		newUser = false;
		dojo.byId('userProfileTabs').style.display = '';
		dojo.byId('loadingUserProfile').style.display = 'none';

		initStructures();
		loadUserRolesTree(currentUser.id);
		buildRolesTree();
		dijit.byId('userTabsContainer').selectChild(dijit.byId('userRolesTab'));
	}

	//Setting up tab actions
	var currentSelectedTab = 'userRolesTab';
	dojo.addOnLoad(function () {
		dojo.subscribe("userTabsContainer-selectChild", (function(child){
			currentSelectedTab = child.attr("id");
			renderCurrentTab();
		}).bind(this));
	});

	function renderCurrentTab () {
        var userId = null;
		if(currentUser!=null){
           userId = currentUser.id;
		}
	 	switch (currentSelectedTab) {
			case 'userDetailsTab':
				break;
			case 'userRolesTab':
				initStructures();
				loadUserRolesTree(userId);
				break;
			case 'userPermissionsTab':
				RoleAjax.getUserRole(userId, userRoleCallback);
				break;
			case 'userAdditionalInfoTab':
				loadUserAdditionalInfo(currentUser);
				loadUserAddresses(userId);
				break;
			case 'marketingInfoTab':
				loadMarketingInfo(userId);
				break;
		}
		resizeRoleBrowser();
	}

	function userRoleCallback(userRole) {
		if(userRole.id)
		   loadPermissionsForRole(userRole.id);
	}

	//Executed when adding a new user
	var newUser = false;
	function addUser() {
        currentUser = null;

		//Clearing the form to enter a new user
		if(!authByEmail) {
			dojo.byId('userIdLabel').style.display = '';
			dojo.byId('userIdValue').style.display = '';
			dijit.byId('userId').setDisabled(false)
			dijit.byId('userId').attr('value', "");
		} else {
			dojo.byId('userIdLabel').style.display = 'none';
			dojo.byId('userIdValue').style.display = 'none';
			dojo.byId('userId').value  = "";
		}

		dijit.byId('firstName').attr('value', "");
		dijit.byId('lastName').attr('value', "");
		dijit.byId('emailAddress').attr('value', "");
		dijit.byId('password').attr('value', "");
		dijit.byId('passwordCheck').attr('value', "");
		dojo.byId('fullUserName').innerHTML = "";
		userChanged = false;
		newUser = true;
		dojo.byId('userProfileTabs').style.display = '';
		dojo.byId('loadingUserProfile').style.display = 'none';
		if(dojo.isIE){
		  //http://jira.dotmarketing.net/browse/DOTCMS-5679
		  dijit.byId('userTabsContainer').selectChild(dijit.byId('userRolesTab'));
		}
		dijit.byId('userTabsContainer').selectChild(dijit.byId('userDetailsTab'));
		resizeRoleBrowser();
	}

	//Handler from when the user info has changed
	var userChanged = false;
	function userInfoChanged() {
		userChanged = true;
	}

	//Handler from when the user password has changed
	var passwordChanged = false;
	function userPasswordChanged () {
		userChanged = true;
		passwordChanged = true;
	}
	
	//Handler from when the user info has changed
	var emailChanged = false;
	function userEmailChanged() {
		userChanged = true;
		emailChanged = true;
	}

	//Handler to save the user details
	function saveUserDetails() {

		//If the form is not valid focus on the first not valid field and
		//hightlight the other not valid ones
		if(!dijit.byId('userInfoForm').validate()) {
			return;
		}

		//If user has not changed do nothing
		if(!userChanged) {
			showDotCMSSystemMessage(userSavedMsg);
			return;
		}

		var userEmail;
		if(emailChanged){
			var pattern=/^([a-zA-Z0-9_.-])+@([a-zA-Z0-9_.-])+\.([a-zA-Z])+([a-zA-Z])+/;
			userEmail = dijit.byId('emailAddress').attr('value');
			if(!pattern.test(userEmail)){
				alert(invalidEmail);
				return;
			}
		}

		var passswordValue;
		var reenterPasswordValue;
		if(passwordChanged) {
			passswordValue = dijit.byId('password').attr('value');
			reenterPasswordValue = dijit.byId('passwordCheck').attr('value');
			if(passswordValue != reenterPasswordValue) {
				alert(passwordsDontMatchError);
				return;
			}
		}

		//Executing the update user logic
		var callbackOptions = {
			callback: saveUserCallback,
			exceptionHandler: saveUserException
		}
		if(!newUser) {
			UserAjax.updateUser(currentUser.id, currentUser.id, dijit.byId('firstName').attr('value'), dijit.byId('lastName').attr('value'),
					userEmail, passswordValue, callbackOptions);
		} else {
			if (!authByEmail) {
				UserAjax.addUser(dijit.byId('userId').attr('value'), dijit.byId('firstName').attr('value'), dijit.byId('lastName').attr('value'), dijit.byId('emailAddress').attr('value'), passswordValue, callbackOptions);
			} else {
				UserAjax.addUser(null, dijit.byId('firstName').attr('value'), dijit.byId('lastName').attr('value'), dijit.byId('emailAddress').attr('value'), passswordValue, callbackOptions);
			}
		}
	}

	//Callback from the server to confirm the user saved
	function saveUserCallback (userId) {
		if(userId) {
			newUser = false;
			userChanged = false;
			passwordChanged = false;
			showDotCMSSystemMessage(userSavedMsg);
			editUser(userId);
			filterUsersDelayed();
		} else {
			showDotCMSErrorMessage(userSaveFailedMsg);
		}
	}

	function saveUserException(message, exception){
		if(exception.javaClassName == 'com.dotmarketing.business.DuplicateUserException') {
			if(authByEmail) {
				showDotCMSErrorMessage(sameEmailAlreadyRegisteredErrorMsg);
			}
			else {
				showDotCMSErrorMessage(sameUserIdAlreadyRegisteredErrorMsg);
			}
		} else {
			alert("Server error: " + exception);
		}

	}

	//Event handler then deleting a user
	function deleteUser() {
		if(currentUserId  == currentUser.id) {
			alert(deleteYourOwnUserError);
			return;
		}
		if(confirm(deleteUserConfirm)) {
			UserAjax.deleteUser(currentUser.id, deleteUserCallback);
		}
	}

	//Callaback from the server to confirm a user deletion
	function deleteUserCallback (isDeleted) {
		if(isDeleted) {
			userChanged = false;
			passwordChanged = false;
			showDotCMSSystemMessage(userDeleted);
			filterUsersDelayed();
			dojo.byId('userProfileTabs').style.display = 'none';
		} else {
			showDotCMSErrorMessage(userDeleteFailed);
		}
	}


	/* --------------------------------------------------------- */


	//User roles


	var userRoles;
	var rolesTree;
	var flatTree = [];
	var rolesCheckedCounter;
	var rolesChecked;
	var rolesAdded;

	function initStructures () {

		rolesCheckedCounter = 0;
		rolesChecked = new Array();
		rolesAdded = new Array();
	}

	//Function that kicks the loading of user roles
	function loadUserRolesTree (userid) {
		dojo.style(dojo.byId('noRolesFound'), { display: 'none' });
		UserAjax.getUserRoles(userid, builtUserRolesMutiSelect);
	}

	function builtUserRolesMutiSelect(roles) {
		var userRolesSelect = dijit.byId('userRolesSelect');

	    if(userRolesSelect && userRolesSelect instanceof dijit.form.MultiSelect) {
	    	userRolesSelect.destroyRecursive(false);
	    }

		dojo.destroy("userRolesSelect");
	    dojo.create('select',{id:'userRolesSelect'},'userRolesSelectWrapper');

		require(["dojo/ready", "dijit/form/MultiSelect", "dijit/form/Button", "dojo/dom", "dojo/_base/window", "dojo/on"], function(ready, MultiSelect, Button, dom, win, on){

		        var sel = dojo.byId("userRolesSelect");
		        var n = 0;
		        for(var i = 0; i<roles.length; i++) {
		            var c = win.doc.createElement('option');
		            var nodeName = getDBFQNLabel(roles[i].DBFQN);
		            c.innerHTML = nodeName;
		            c.value = roles[i].id;
		            c.title = nodeName;
		            sel.appendChild(c);

		            var alreadyAdded = false;
		            for(var j=0; j<rolesAdded.length; j++) {
		            	if(rolesAdded[j]==roles[i].id) {
		            		alreadyAdded = true;
		            		break;
		            	}
		            }

		            if(!alreadyAdded)
		            	rolesAdded.push(roles[i].id);
		        }
		        var myMultiSelect = new MultiSelect({ name: 'userRolesSelect', id: 'userRolesSelect', style:"width:100%; height:100%" }, sel);

		        on(myMultiSelect, "click", function(e){
		        	handleUserRoleClick(e);
		        });


		});
	}

	function getDBFQNLabel(DBFQN) {
        var nodeName = '';
        var dbfqnItems = DBFQN.split(" --> ");

        for(var j=0; j<dbfqnItems.length; j++) {
        	var roleId = dbfqnItems[j];
        	var role = findRole(roleId);

        	if(j<dbfqnItems.length-1) {
        		nodeName+= role.name + ' --> ';
        	} else {
        		nodeName+= role.name;
        	}
        }

        return nodeName;
	}

	var treeRoleOptionTemplate = '<input id="role_node_${nodeId}_chk" name="role_node_${nodeId}_chk" dojoType="dijit.form.CheckBox" ${nodeChecked} ${nodeDisabled}\
		value="on" onChange="roleChecked(\'${nodeId}\')">\
		<label id="role_node_label_${nodeId}" for="role_node_${nodeId}_chk">\
    		${nodeName}\
		</label>';

	function buildRolesTree(tree) {
		dojo.style(dojo.byId('loadingRolesWrapper'), { display: '' });
		dojo.style(dojo.byId('userRolesTreeWrapper'), { display: 'none' });
		var autoExpand = false;

		if(tree==null) {
			store = new dojox.data.JsonRestStore({ target: "/api/role/loadchildren/id", labelAttribute:"name"});
		} else {
			store = new dojo.data.ItemFileReadStore({ data: tree });
			autoExpand = true;
		}

	    treeModel = new dijit.tree.TreeStoreModel({
	        store: store,
	        query: { top:true },
	        rootId: "root",
	        rootLabel: "Root",
	        deferItemLoadingUntilExpand: true,
	        childrenAttrs: ["children"]
	    });

	    var treeContainer = dijit.byId('userRolesTree');

	    if(treeContainer && treeContainer instanceof dijit.Tree) {
			treeContainer.destroyRecursive(false);
	    }

	    dojo.destroy("userRolesTree");
	    dojo.create('div',{id:'userRolesTree'},'userRolesTreeWrapper');

	    initializeRolesTreeWidget(treeModel, autoExpand);
	}

	function initializeRolesTreeWidget(treeModel, autoExpand) {

		//Overriding the dojo tree nodes to allow having html within nodes
	    dojo.declare("dotcms.dojo.RolesTreeNode", dijit._TreeNode, {
			attributeMap: dojo.delegate(dijit._Widget.prototype.attributeMap, {
				label: {node: "labelNode", type: "innerHTML"},
				tooltip: {node: "rowNode", type: "attribute", attribute: "title"}
			}),

	    	postCreate: function(){
				this.inherited(arguments);
				if(dijit.byId('role_node_' + norm(this.item.id) + '_chk'))
					dijit.registry.remove('role_node_' + norm(this.item.id) + '_chk');
				dojo.parser.parse(this.labelNode);
			}
	    });
		//Overriding the dojo tree to handle some of our own actions
		dojo.declare("dotcms.dojo.RolesTree", dijit.Tree, {

			_isItemChecked: function(item) {


				return false;

			},

			//Returns the node text based on the treeRoleOptionTemplate html template
			getLabel: function(item) {
				var alreadyAdded = false;

				for(var i=0; i<rolesAdded.length; i++) {
					if(norm(item.id)==rolesAdded[i]) {
						alreadyAdded = true;
						break;
					}
				}

				if(!alreadyAdded) {
					var checked = this._isItemChecked(item)?"checked=\"checked\"":"";
					var role = findRole(item.id);
					var editusers = role==null?false:(dojo.isArray(role.editUsers)?eval(role.editUsers[0]):eval(role.editUsers));
					var html = dojo.string.substitute(treeRoleOptionTemplate, {
							nodeId: item.id, nodeName: item.name, nodeChecked:checked, nodeDisabled: (editusers?"":"disabled=\"disabled\"") })
					return html;
				} else {
					return dojo.string.substitute('${nodeName}', { nodeId: item.id, nodeName: item.name });
				}



			},

			//Hiding the icon that dojo tries to attach to every node
			getIconClass: function (item, opened) {
				return null;
			},

			//Hiding the icon that dojo tries to attach to every node
			getIconStyle: function (item, opened) {
				return { width: 0, height: 0 };
			},

			_createTreeNode: function (args) {
				args.item.id[0] = args.item.id[0].replace(/_/g, "-");
				var alreadyAdded = false;

				for(var i=0; i<rolesAdded.length; i++) {
					if(norm(args.item.id)==rolesAdded[i]) {
						alreadyAdded = true;
						break;
					}
				}

				if(!alreadyAdded) {
					args.id = "treeNode-" + norm(args.item.id);
					return new dotcms.dojo.RolesTreeNode(args);
				} else {
					args.id = "treeNode-" + norm(args.item.id);
					return new dijit._TreeNode(args);
				}
			},

			//Some housekeeping after tree creation
			postCreate: function () {
// 				//Calling the parent
				this.inherited(arguments);
// 				//hiding the role loading image
				dojo.style(dojo.byId('loadingRolesWrapper'), { display: 'none' });
// 				//Showing the tree
				dojo.style(dojo.byId('userRolesTreeWrapper'), { display: '' });


			}

		});

		//Unregistering any old loaded tree and nodes before try to render a new tree
		if (dijit.byId('userRolesTree')) {
			flatTree.each(function (role) {
				if(dijit.byId("role_node_" + role.id + "_chk")) {
					dijit.registry.remove("role_node_" + role.id + "_chk");
				}
				if(dijit.byId('treeNode-' + role.id))
					dijit.registry.remove('treeNode-' + role.id);
			});
			dijit.registry.remove('userRolesTree');
			dijit.registry.remove('treeNode-root');
		}

		//Rendering the tree
	   	var tree = new dotcms.dojo.RolesTree({
	        model: treeModel,
	        showRoot: false,
	        autoExpand: autoExpand,
	        persist: false
	    }, "userRolesTree");

	}

	//Action handler when the user type something to filter the roles tree
	var filterRolesHandle;
	function filterUserRoles(){
		if(filterRolesHandle)
			clearTimeout(filterRolesHandle);

		filterRolesHandler = setTimeout("filterUserRolesDeferred()", 600);

	}

	//The logic is executed deferred within 600 ms to handle multiple user keystrokes
	function filterUserRolesDeferred () {
		var tree = dijit.byId('userRolesTree')
		dijit.byId('onlyUserRolesFilter').attr('value', false)
		var filter = dojo.byId('userRolesFilter').value;
		dojo.style(dojo.byId('noRolesFound'), { display: 'none' });
		if(filter == '') {
			for (var i = 0; i < flatTree.length; i++) {
				var treeRole = flatTree[i];
				var node = dojo.byId('treeNode-' + treeRole.id);
				if(node)
					dojo.style(node, { display: ''});
			}
			return;
		}
		var roles = searchRoles(filter, flatTree);
		var branches = getRolesFlatUpBranches(roles);
		var matchesCount = 0;
		for(var i = 0; i < flatTree.length; i++) {
			var treeRole = flatTree[i];
			if(!findRole(treeRole.id, branches)) {
				var node = dojo.byId('treeNode-' + treeRole.id);
				if(node)
					dojo.style(node, { display: 'none'});
			} else {
				matchesCount++;
				var node = dojo.byId('treeNode-' + treeRole.id);
				if(node) {
					dojo.style(node, { display: ''});
					var dijitNode = dijit.byId(node.id);
					tree._expandNode(dijitNode)
				}
			}
		}
		if(matchesCount == 0)
			dojo.style(dojo.byId('noRolesFound'), { display: '' });
	}

	function expandWholeTree (tree, node) {
		var children = new Array();
		if(!node) {
			children = dijit.byId('treeNode-root').getChildren();
		} else {
			children = node.getChildren();
		}
		dojo.forEach(children, function(treeNode, index){
			tree._expandNode(treeNode);
			expandWholeTree(tree, treeNode);
		});
	}

	//Filters from the roles tree only user assigned roles
	function filterOnlyUserRoles() {

		var tree = dijit.byId('userRolesTree');
		dojo.byId('userRolesFilter').value = '';
		var checked = dijit.byId('onlyUserRolesFilter').attr('value') != false;
		dojo.style(dojo.byId('noRolesFound'), { display: 'none' });

		if (checked) {
			expandWholeTree(tree);
			flatTree.each(function (role) {
				var treeNode = dijit.byId('treeNode-' + role.id);
				if(treeNode)
					dojo.style(treeNode.domNode, { display: 'none' });
			});
			var matchesCount = 0;
			for(var i = 0; i < userRoles.length; i++) {
				var userRole = userRoles[i];
				var treeNode = dijit.byId('treeNode-' + userRole.id);
				var tree = dijit.byId('userRolesTree');

				var branchesUp = getRoleFlatUpBranch(userRole.id);
				branchesUp.each(function(upRole){
					var tree = dijit.byId('userRolesTree');
					var treeNode = dijit.byId('treeNode-' + upRole.id);
					if (!treeNode) return;
					tree._expandNode(treeNode);
					dojo.style(treeNode.domNode, { display: '' });
					matchesCount++;
				});

				if(!treeNode)
					continue
				tree._expandNode(treeNode);
				dojo.style(treeNode.domNode, { display: '' });
				matchesCount++;

			}
			if(matchesCount == 0)
				dojo.style(dojo.byId('noRolesFound'), { display: '' });

		} else {
			clearUserRolesFilter ();
		}

	}

	//Clears any filter applied to the user roles tree
	function clearUserRolesFilter () {
		dijit.byId('onlyUserRolesFilter').attr('value', false);
		dojo.byId('userRolesFilter').value = '';
		filterUserRolesDeferred ();
	}

	function addUserRoles() {

		require(["dojo/_base/window", ], function(win){
			var select = dojo.byId("userRolesSelect");

			for(var i=0; i<rolesChecked.length; i++) {
				var roleCheck = rolesChecked[i];
				var id = roleCheck.id.replace("role_node_", "").replace("_chk", "");
				var alreadyAdded = false;

				// check if the role is already added in the multiselect
				for(var j=0; j<select.options.length; j++) {
					var option = select.options[j];

					if(option.value == id) {
						alreadyAdded = true;
						break;
					}
				}

				if(!alreadyAdded) {
					var role = findRole(id);
					var dbfqnLabel = getDBFQNLabel(role.DBFQN);
		            var c = win.doc.createElement('option');
		            c.innerHTML = dbfqnLabel;
		            c.value = id;
		            c.title = dbfqnLabel;
		            select.appendChild(c);


		            // remove the just added role from the tree
		            rolesAdded.push(id);
				}
			}
			rolesChecked.length = 0;
			dijit.byId("addUserRoleBtn").setAttribute('disabled',true);
			buildRolesTree();


		});

	}

	function removeUserRoles() {
		var select = dojo.byId("userRolesSelect");

		for(var j=0; j<select.options.length; j++) {
			var option = select.options[j];
		    if(option.selected) {
		    	select.options[j] = null;
		    	rolesAdded.splice(rolesAdded.indexOf(option.value), 1);
		    	j--;
		    }

		}

		dijit.byId("removeUserRoleBtn").setAttribute('disabled',true);
		buildRolesTree();
	}

	function handleUserRoleClick(e) {
		var select = dijit.byId("userRolesSelect");
		var selectedRoles = select.getSelected();

		dijit.byId("removeUserRoleBtn").setAttribute('disabled',selectedRoles.length==0);
	}

	//Action executed when a tree checkbox is hit
	function roleChecked(id) {
		var checkbox = dijit.byId('role_node_' + id + '_chk');
		var checked = checkbox.attr('value') != false;
		if (checked) {
			rolesCheckedCounter++;
			rolesChecked.push(checkbox);
			//If role is check everything underneath should be checked
			var branchDown = getRoleFlatDownBranch(id);
			branchDown.each(function (role) {
				var checkbox = dijit.byId('role_node_' + role.id + '_chk');
				if(checkbox != undefined && !checkbox.attr('disabled'))
					checkbox.attr('value', 'on');
			});
		}
		else {
			rolesCheckedCounter--;
			rolesChecked.splice(rolesChecked.indexOf(checkbox), 1);

			//If role is un-checked everything above should be unchecked
			var branchesUp = getRoleFlatUpBranch(id);
			branchesUp.each(function (role) {
				var checkbox = dijit.byId('role_node_' + role.id + '_chk');
				if(checkbox != undefined &&  !checkbox.attr('disabled'))
					checkbox.attr('value', false);
			});
		}

		dijit.byId("addUserRoleBtn").setAttribute('disabled',rolesCheckedCounter==0);

	}

	//Resets the roles selection to how it was when loaded
	function resetRoles () {
		flatTree.each(function (role) {
			var checkbox = dijit.byId('role_node_' + role.id + '_chk');
			if(!findRole(role.id, userRoles))
				if(!checkbox.attr('disabled'))
					checkbox.attr('value', false);
			else {
				if(!checkbox.attr('disabled'))
					checkbox.attr('value', 'on');
				var branchDown = getRoleFlatDownBranch(role.id);
				branchDown.each(function (role) {
					var checkbox = dijit.byId('role_node_' + role.id + '_chk');
					if(!checkbox.attr('disabled'))
						checkbox.attr('value', 'on');
				});
			}
		});
		dojo.byId('userRolesFilter').value = '';
		dijit.byId('onlyUserRolesFilter').attr('value', 'on');
		filterOnlyUserRoles();
	}

	//Saves the current selection of roles
	function saveRoles () {
		if(currentUser != null)
			UserAjax.updateUserRoles(currentUser.id, rolesAdded, saveRolesCallback);
	}

	//Callback from the server after successful save
	function saveRolesCallback () {
		showDotCMSSystemMessage(userRolesSaved);
	}

	//Utility functions
	function flattenTree (tree) {
		tree.each(function (node) {
			flatTree.push(node);
			if (node.children)
				flattenTree(node.children);
		});
	}

	//Used to filter roles
	function searchRoles(query, roles){
		var matches = [];
		for(var i = 0; i < roles.length; i++) {
			var roleName = dojo.isArray(roles[i].name)?roles[i].name[0]:roles[i].name;
			var regexQuery = new RegExp(query, "i");
			if(roleName.match(regexQuery))
				matches.push(roles[i]);
		}
		return matches;
	}

	//Retrieves a plain list of roles up in the same branch of the given roles
	function getRolesFlatUpBranches(roles) {
		branches = []

		for(var i = 0; i < roles.length; i++) {
			var role = roles[i];
			branches.push(role);
			var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
			var roleId = dojo.isArray(role.id)?role.id[0]:role.id;

			while(parentId && parentId != roleId) {
				role = findRole(role.parent, flatTree);
				branches.push(role);
				var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
				var roleId = dojo.isArray(role.id)?role.id[0]:role.id;
			}

		}
		return branches;
	}

	//Retrieves a plain list of roles up in the same branch of the given role
	function getRoleFlatUpBranch(roleId) {
		branches = []

		var role = findRole(roleId, flatTree);
		if(role == null)
			return [];
		branches.push(role);
		var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
		var roleId = dojo.isArray(role.id)?role.id[0]:role.id;

		while(parentId && parentId != roleId) {
			role = findRole(role.parent, flatTree);
			branches.push(role);
			var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
			var roleId = dojo.isArray(role.id)?role.id[0]:role.id;
		}

		return branches;
	}

	//Retrieves a plain list of roles underneath in the same branch of the given role
	function getRoleFlatDownBranch(id) {
		branches = [];
		ids = [];
		var role = findRole(id, flatTree);

		branches.push(role);
		var children = role.children;

		for(var i = 0; children && i < children.length; i++) {
			var id = dojo.isArray(children[i].id)?children[i].id[0]:children[i].id;
			ids.push(id);
		}

		while(ids.length > 0) {
			var roleId = ids.pop();
			role = findRole(roleId, flatTree);
			branches.push(role);
			var children = role.children;
			if(children != null) {
				for(var i = 0; i < children.length; i++) {
					var id = dojo.isArray(children[i].id)?children[i].id[0]:children[i].id;
					ids.push(id);
				}
			}
		}

		return branches;
	}

	//Additional information tab functions

	var addressesData = {
		identifier: 'addressId',
		label: 'address',
		items: [  ]
	};

	var userAddressesStore = new dojo.data.ItemFileReadStore({data: addressesData});

	var userAddressesGridLayout = [[{
            field: 'description',
            name: nameColumn,
            width: '20%'
        },{
            field: 'address',
            name: addressColumn,
            width: '70%',
			get: addressCell,
			formatter: addressCellFormatter
        },{
            field: 'addressId',
            name: ' ',
            width: '5%',
			get: addressActionsCell,
			formatter: addressActionsCellFormatter
        }
	]];

	//Initializing addresses

	function loadUserAddresses(userId) {
		if(!userId && currentUser!=null) userId = currentUser.id;
		UserAjax.loadUserAddresses(userId, loadUserAddressesCallback);
	}

	function loadUserAddressesCallback(addresses){
		var addressesGrid = dijit.byId('userAddressesGrid');
		addressesData.items = [];
		addresses.each(function (newAddress) {
			newAddress.address = newAddress.street1 + "<br/>" + newAddress.street2 + "<br/>" + newAddress.city + ", " +
				newAddress.state + " " + newAddress.zip + "<br/>" + newAddress.country;
			addressesData.items.push(newAddress);
		});
		if(addressesData.items.length == 0) {
			var empty = { addressId: '0', description: 'None', address: '', street1: '', street2: '', city: '', state: '', zip: '', country: '', phone: '', fax: '', cell: '' };
			addressesData.items.push(empty);
		}
 		var addressesStore = new dojo.data.ItemFileReadStore({data: addressesData });
		addressesGrid.setStore(addressesStore);
		addressesGrid.render();
	}

	function addressCellFormatter (item) {
		if(!item || !item.street1)
			return item;
		var addressHTML = item.street1;
		if(item.street2 && item.street2 != '')
			 addressHTML += ' ' + item.street2;
		if(item.city && item.city != '')
			 addressHTML += ', ' + item.city;
		if(item.state && item.state != '')
			 addressHTML += ', ' + item.state;
		if(item.zip && item.zip != '')
			 addressHTML += ' ' + item.zip;
		if(item.country && item.country != '')
			 addressHTML += ' ' + item.country;
		if(item.phone && item.phone != '')
			 addressHTML += '<br/><b>' + phone + ':</b> ' + item.phone;
		if(item.fax && item.fax != '')
			 addressHTML += '<br/><b>' + fax + ':</b> ' + item.fax;
		if(item.cell && item.cell != '')
			 addressHTML += '<br/><b>' + cell + ':</b> ' + item.cell;

		return addressHTML;
	}

	function addressCell(rowid, item) {
		if(!item || !item.street1)
			return item;
		return item;
	}


	function addressActionsCellFormatter (item) {
		if(!item || !item.addressId || item.addressId == '0')
			return '';
		var addressHTML =
			'<span class="editIcon" onclick="editAddress(\'' + item.addressId + '\')"></span>\
			 <span class="deleteIcon" onclick="deleteAddress(\'' + item.addressId + '\')"></span>';


		return addressHTML;
	}

	function addressActionsCell(rowid, item) {
		if(!item)
			return item;
		return item;
	}

	function addAddress () {
		dijit.byId('addressForm').reset();
		dojo.byId('addressId').value = '';
		dijit.byId('addressDialog').show();
	}

	function editAddress (addressId) {
		dijit.byId('addressForm').reset();
		dojo.byId('addressId').value = addressId;

		var selectedAddress;
		for (var i = 0; i < addressesData.items.length; i++){
			if(addressesData.items[i].addressId == addressId) {
				selectedAddress = addressesData.items[i];
				break;
			}
		};

		dijit.byId('addressDescription').attr('value', selectedAddress.description);
		dijit.byId('addressStreet1').attr('value', selectedAddress.street1);
		dijit.byId('addressStreet2').attr('value', selectedAddress.street2);
		dijit.byId('addressCity').attr('value', selectedAddress.city);
		dijit.byId('addressState').attr('value', selectedAddress.state);
		dijit.byId('addressZip').attr('value', selectedAddress.zip);
		dijit.byId('addressCountry').attr('value', selectedAddress.country);
		dijit.byId('addressPhone').attr('value', selectedAddress.phone);
		dijit.byId('addressFax').attr('value', selectedAddress.fax);
		dijit.byId('addressCell').attr('value', selectedAddress.cell);

		dijit.byId('addressDialog').show();
	}

	function saveAddress(){
		if(currentUser == null)
			return;
		if(!dijit.byId('addressForm').validate())
			return;		
		var id = dojo.byId('addressId').value;
		var desc = dijit.byId('addressDescription').attr('value');
		var street1 = dijit.byId('addressStreet1').attr('value');
		var street2 = dijit.byId('addressStreet2').attr('value');
		var city = dijit.byId('addressCity').attr('value');
		var state = dijit.byId('addressState').attr('value');
		var zip = dijit.byId('addressZip').attr('value');
		var country = dijit.byId('addressCountry').attr('value');
		var phone = dijit.byId('addressPhone').attr('value');
		var fax = dijit.byId('addressFax').attr('value');
		var cell = dijit.byId('addressCell').attr('value');

		dwr.engine.setErrorHandler(errorHandler);

		if(id== '')
			UserAjax.addNewUserAddress(currentUser.id, desc, street1, street2, city, state, zip, country, phone, fax, cell, saveAddressCallback);
		else
			UserAjax.saveUserAddress(currentUser.id, id, desc, street1, street2, city, state, zip, country, phone, fax, cell, saveAddressCallback);
	}

	function errorHandler(message, exception) {
		if (message == 'com.liferay.portal.AddressPhoneException')
			alert(invalidAddresPhoneMsg);
		else if (message == 'com.liferay.portal.AddressFaxException')
			alert(invalidAddresFaxMsg);
		else if (message == 'com.liferay.portal.AddressCellException')
			alert(invalidAddresCellMsg);
	}

	function saveAddressCallback (newAddress) {
		showDotCMSSystemMessage(addressSaved);
		loadUserAddresses();
		dijit.byId('addressDialog').hide();
	}

	function cancelSaveAddress () {
		dijit.byId('addressDialog').hide();
	}

	function deleteAddress(addressId){
		if(confirm(removeAddressConfirmation))
			UserAjax.deleteAddress(currentUser.id, addressId, deleteAddressCallback);

	}

	function deleteAddressCallback(addressId) {
		showDotCMSSystemMessage(addressDeleted);
		loadUserAddresses();
		dijit.byId('addressDialog').hide();
	}

	//User additional info

	function loadUserAdditionalInfo(user) {

		 if(user!=null){
				dijit.byId('userActive').attr('value', user.active?'on':false);
				dijit.byId('prefix').attr('value', user.prefix);
				dijit.byId('suffix').attr('value', user.suffix);
				dijit.byId('title').attr('value', user.title);
				dijit.byId('company').attr('value', user.company);
				dijit.byId('website').attr('value', user.website);

				for (var i = 1; i <= additionalVariablesCount; i++) {
					var value = user['var' + i];
					if(value) {
						dijit.byId('var' + i).attr('value', value);
					}
				}
			 }else{
					dijit.byId('userActive').attr('value', true);
					dijit.byId('prefix').attr('value', '');
					dijit.byId('suffix').attr('value', '');
					dijit.byId('title').attr('value', '');
					dijit.byId('company').attr('value', '');
					dijit.byId('website').attr('value', '');

				}
	}

	function saveUserAdditionalInfo(){
		if(currentUser == null)
			return;
		if (!dijit.byId('userAdditionalInfoForm').validate())
			return;		
		var active = dijit.byId('userActive').attr('value') != false;
		var prefix = dijit.byId('prefix').attr('value');
		var suffix = dijit.byId('suffix').attr('value');
		var title = dijit.byId('title').attr('value');
		var company = dijit.byId('company').attr('value');
		var website = dijit.byId('website').attr('value');
		var additionalVars = [];
		for(var i = 1; i <= additionalVariablesCount; i++) {
			var varValue = dijit.byId('var' + i).attr('value');
			additionalVars.push(varValue);
		}

		if(!active && currentUser.id === currentUserId){
			alert(deactivateYourOwnUserError);
			return;
		}

		UserAjax.saveUserAddittionalInfo(currentUser.id, active, prefix, suffix, title, company, website, additionalVars, saveUserAdditionalInfoCallback);
	}

	function saveUserAdditionalInfoCallback(){
		showDotCMSSystemMessage(userInfoSavedMsg);
	}

	function norm(value) {
		return dojo.isArray(value)?value[0]:value;
	}

	//Marketing TAB

	function loadMarketingInfo(userId) {
		if(currentUser!=null){
		   initTags();
		   loadUserCategories();
		 }
		initUserLocale();
		initUserClicktracking();
	}

	//Click tracking
	var clicktrackingInitialized = false;
	function initUserClicktracking() {
		clicktrackingInitialized = false;
		if(currentUser!=null){
		  if(currentUser.noclicktracking) {
			dijit.byId('userClickTrackingCheck').attr('value', 'on')
		  } else {
			dijit.byId('userClickTrackingCheck').attr('value', false)
		  }
		}else{
			dijit.byId('userClickTrackingCheck').attr('value', 'on');
		}
		clicktrackingInitialized= true;
	}

	function userClicktrackingChanged() {
		if(!clicktrackingInitialized || currentUser==null)
			return;
		var cb = dijit.byId('userClickTrackingCheck');
		UserAjax.disableUserClicktracking(currentUser.userId, cb.attr('value') != false, disableClickTrackingCallback);
	}

	function disableClickTrackingCallback() {
		showDotCMSSystemMessage(userClicktrackingSavedMsg);
	}

	function viewFullClickHistory() {
		if(currentUser == null)
			return;
		if(dijit.byId('userClickHistoryPane'))
			dijit.registry.remove('userClickHistoryPane');

		dojo.style('userClickHistoryPane', { display: '' });
		dojo.style('userClickHistoryDetailPane', { display: 'none' });

        var pane = new dijit.layout.ContentPane({
         	href: "/html/portlet/ext/useradmin/view_users_click_history.jsp?userId=" + currentUser.userId,
			preload: true,
			refreshOnShow:true,
			style: "height: auto; max-height: 500px; width: 650px"
        }, "userClickHistoryPane");
		dijit.byId('userClickHistoryDialog').show();

		pane.startup();
	}

	function closeUserClickHistoryDetails() {
		dojo.style('userClickHistoryPane', { display: '' });
		dojo.style('userClickHistoryDetailPane', { display: 'none' });
	}

	function viewClickstreamDetails(clickstreamId, userId) {
		if(currentUser == null)
			return;
		if(dijit.byId('userClickHistoryDetailPane'))
			dijit.registry.remove('userClickHistoryDetailPane');

        var pane = new dijit.layout.ContentPane({
         	href: "/html/portlet/ext/useradmin/view_users_click_history_detail.jsp?clickstreamId=" + clickstreamId +
				"&userId=" + currentUser.userId + "&layoutId=" + layoutId,
			preload: true,
			refreshOnShow:true,
			style: "height: auto; max-height: 500px; width: 650px"
        }, "userClickHistoryDetailPane");

		pane.startup();

		dojo.style('userClickHistoryPane', { display: 'none' });
		dojo.style('userClickHistoryDetailPane', { display: '' });

	}

	//User Tags

	function initTags() {
		if(currentUser != null)
			TagAjax.getTagsByUser(currentUser.userId, showResult);
	}
	function removeTagInode(tagName) {
		if(currentUser != null)
			TagAjax.deleteTag(tagName, currentUser.userId, showResult);
	}
	function editTag(tagName) {
		var tagTable = document.getElementById('tags_detail');
		document.getElementById('cmd').value = 'edit';
		document.getElementById('tagName').value = tagName;
		tagTable.style.display = "";
	}
	function assignTag() {
		var tagName = document.getElementById('tagName').value;
		tagName = RTrim(tagName);
		tagName = LTrim(tagName);
		var cmd = document.getElementById('cmd').value;
		document.getElementById('tagName').value = '';
		document.getElementById('cmd').value = '';
		if(currentUser != null)
			TagAjax.addTag(tagName, currentUser.userId, currentUser.inode, showResult);
	}
	function showResult(result) {
		DWRUtil.removeAllRows("tags_table");
		var table = document.getElementById("tags_table");
		console.log(result);
		var tags =  result.tags;
		console.log(tags);
		if (tags.length > 0) {
			var row = table.insertRow(table.rows.length);
			row.setAttribute("bgColor","#EEEEEE");
			var cell = row.insertCell (row.cells.length);
			cell.setAttribute("colspan", "2");
			cell.innerHTML = '<b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Tag-Name")) %></b>';
			//cell = row.insertCell (row.cells.length);

			for (var i = 0; i < tags.length; i++) {
				row = table.insertRow(table.rows.length);
				if (i % 2 == 1)
					row.setAttribute("bgColor","#EEEEEE");

				var tagName = tags[i]["tagName"];
				tagName = RTrim(tagName);
				tagName = LTrim(tagName);
				var tagReplaced = tagName.replace("'", "\\\'");

				cell = row.insertCell (row.cells.length);
				cell.setAttribute("width", "30px");
				cell.innerHTML = "<a class=\"beta\" href=\"javascript: removeTagInode ('"+tagReplaced+"')\"><span class=\"deleteIcon\"></span>";

				cell = row.insertCell (row.cells.length);
				cell.innerHTML = tagName;
			}
		}
		else {
				var row = table.insertRow(table.rows.length);
				var cell = row.insertCell (row.cells.length);
				cell.innerHTML = '<center><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Tags-Assigned")) %></b></center>';
		}
	}

	//User categories
	function loadUserCategories() {
		if(currentUser != null)
			UserAjax.getUserCategories(currentUser.userId, loadUserCategoriesCallback);
	}

	function loadUserCategoriesCallback(categories) {
		dojo.query('#userCategorySelectsWrapper select').forEach(function (selectBox) {
			for(var i = 0; i < selectBox.length; i++) {
				if(containsCategory(categories, selectBox[i].value))
					selectBox[i].selected = true;
				else
					selectBox[i].selected = false;
			}
		}, this)
	}

	function updateUserCategories() {
		var selectedCategories = [];
		dojo.query('#userCategorySelectsWrapper select').forEach(function (selectBox) {
			for(var i = 0; i < selectBox.length; i++) {
				if(selectBox[i].selected) {
					selectedCategories.push(selectBox[i].value)
				}
			}
		}, this);
		if(currentUser != null)
			UserAjax.updateUserCategories(currentUser.userId, selectedCategories, updateUserCategoriesCallback);
	}

	function updateUserCategoriesCallback(){
		showDotCMSSystemMessage(userCategoriesSavedMsg);
	}

	function addUserProxyEntity(inode)
	{
   		var url = '<liferay:actionURL portletName="EXT_6"><liferay:param name="struts_action" value="/ext/entities/edit_entity" /></liferay:actionURL>';
   		url += '&inode=' + inode;
		window.location.href = url;
	}

	function containsCategory(categories, id) {
		for(var i = 0; i < categories.length; i++) {
			if(categories[i].inode == id) return true;
		}
		return false;
	}

	//User locale
	function initUserLocale() {

		var timeZoneSelect = dojo.query('#userTimezoneWrapper select')[0];
		if(timeZoneSelect)
			timeZoneSelect = new dijit.form.FilteringSelect({ id: 'userTimeZone' }, timeZoneSelect);
		else
			timeZoneSelect = dijit.byId('userTimeZone');
		if(currentUser!=null){
            var timeZone = currentUser.timeZoneId;
            var language = currentUser.languageId;
            if(timeZoneSelect) {
                timeZoneSelect.attr('value', timeZone);
                dijit.byId('userLanguage').attr('value', language);
            }
		}
	}


	function updateUserLocale() {
		var timeZoneId = dijit.byId('userTimeZone').attr('value');
		var languageId = dijit.byId('userLanguage').attr('value');
		if(currentUser != null)
			UserAjax.updateUserLocale(currentUser.userId, timeZoneId, languageId, updateUserLocaleCallback);
	}


	function updateUserLocaleCallback(){
		showDotCMSSystemMessage(userLocaleSavedMsg);
	}

	function findRole(roleid) {

		var roleNode;

		var xhrArgs = {
			url : "/api/role/loadbyid/id/" + roleid,
			handleAs : "json",
			sync: true,
			load : function(data) {
				roleNode = data;
			},
			error : function(error) {
				targetNode.innerHTML = "An unexpected error occurred: " + error;
			}
		};

		var deferred = dojo.xhrGet(xhrArgs);
		return roleNode;
	}


</script>



