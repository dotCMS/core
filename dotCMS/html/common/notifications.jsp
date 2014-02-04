<style>
.notification-flyout{position:absolute;right:30px;top:34px;width:425px;font-size: 85%;border:1px solid #d0d0d0;border-top:0;background:#fff;z-index:9998;-moz-box-shadow:0px 2px 10px rgba(0, 0, 0, 0.2);-webkit-box-shadow:0px 2px 10px rgba(0, 0, 0, 0.2);box-shadow:0px 2px 10px rgba(0, 0, 0, 0.2);}

#closeNotifications{z-index:8000; position:absolute;top:0;left:0;width:100%;height:100%;}

#notificationsGrid .dojoxGridHeader  { display:none; }

.rfloat {float:right}

.notification-header {
padding-left: 10px;
padding-right: 10px;
padding-top: 10px;
}

.timeSince {
color: #969393;
}
</style>

<script>

dojo.require("dojox.data.JsonRestStore")
dojo.require("dojox.grid.DataGrid");
var notificationStore = null;

var notificationlayout = null;

var notificationGrid = null;

var messageFormatter = function(value, index) {
	var message = notificationGrid.store.getValue(notificationGrid.getItem(index), 'message');
	var time_sent = notificationGrid.store.getValue(notificationGrid.getItem(index), 'time_sent');

	return message + "<br><span class='timeSince'>" +time_sent +"</span>";

};

function checkNotifications() {
	var xhrArgs = {
			url : "/api/notification/getNewNotificationsCount/",
			handleAs : "json",
			sync: false,
			load : function(data) {
				refreshNotificationIcon(data.newNotificationsCount);
			},
			error : function(error) {
				console.log(error);
			}
		}

	var deferred = dojo.xhrGet(xhrArgs);
}

function refreshNotificationIcon(count) {

	if(count>0 && dojo.byId("notificationsIcon").className=='hostStoppedIcon') {
		dojo.byId("notificationsIcon").className = 'publishIcon';
	} else if(count==0 && dojo.byId("notificationsIcon").className=='publishIcon') {
		dojo.byId("notificationsIcon").className='hostStoppedIcon';
	}
}

function showNotifications(all, refreshOnly) {
	require(['dojo/_base/lang', 'dojox/grid/DataGrid', 'dojo/dom', 'dojo/domReady!'],
		    function(lang, DataGrid, ItemFileWriteStore, dom){

				var allUsers = all!=null?all:false;
				notificationStore = new dojox.data.JsonRestStore({target:"/api/notification/getNotifications/allusers/"+allUsers, idAttribute:"id"});
				notificationlayout = [[{'name': 'Notifications', 'field': 'message', 'width': '100%', formatter : messageFormatter},]];

				if(refreshOnly) {
					notificationGrid.setStore(notificationStore);
				} else {
					notificationGrid = new dojox.grid.DataGrid({
					    id: 'notificationGrid',
					    store: notificationStore,
					    structure: notificationlayout,
					    rowsPerPage: 25,
					    height: "400px",
					    style: "max-height: 400px; font-size:10px;",
					    rowSelector: '0px'});

					/*append the new grid to the div*/
			        notificationGrid.placeAt("notificationsGrid");
			        dojo.addClass(dojo.byId("notificationsGrid"), "tundra");

			        /*Call startup() to render the grid*/
			        notificationGrid.startup();

			        dojo.byId('notificationsDiv').style.display = 'block';
			        dojo.byId('closeNotifications').style.display = '';

			        refreshNotificationIcon(0);
				}

				if(allUsers) {
					dojo.byId("showAll").style.fontWeight = "bold";
					dojo.byId("onlyMe").style.fontWeight = "normal";

				} else {
					dojo.byId("showAll").style.fontWeight = "normal";
					dojo.byId("onlyMe").style.fontWeight = "bold";
				}

		});
}

function hideNotifications() {
	dojo.byId("closeNotifications").style.display = 'none';

	if(notificationGrid) {
		notificationGrid.destroyRecursive(false);
	}

	dojo.byId("notificationsDiv").style.display = 'none';

}

</script>

<div id="notificationsDiv" class="notification-flyout" style="display:none;">
	<div class="notification-header">
	 	<% if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())) { %>
			<div class="rfloat" id="userFilter">
				<a href="#" id="onlyMe" onclick="showNotifications(false,true)" class="" ><%= LanguageUtil.get(pageContext, "notifications_show_only_me") %></a>
				&nbsp;
				<a href="#" id="showAll" onclick="showNotifications(true,true)" class="" ><%= LanguageUtil.get(pageContext, "notifications_show_all") %></a>
			</div>
		<% } %>
		<div><h3><%= LanguageUtil.get(pageContext, "notifications_title") %></h3></div>
	</div>
	<hr>
	<div id="notificationsGrid" ></div>
</div>
<div id="closeNotifications" onClick="hideNotifications();" style="display:none;"></div>

