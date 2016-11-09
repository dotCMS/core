<script type='text/javascript' src='/dwr/interface/MailingListAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.liferay.portal.language.LanguageUtil" %>

<script language="Javascript">

//Mailing list global actions
function submitfm(form) {
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /></portlet:actionURL>';
		submitForm(form);
}

function addSubscriber() {
	var form =document.getElementById("fm2");
	form.<portlet:namespace />cmd.value="update";
	if(confirm(  '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-add-update-a-subscriber")) %>')){
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /></portlet:actionURL>';
		  submitForm(form);
	}	
}


function cancelEdit() {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>';
}


function deleteList(form) {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-mailing-list-this-cannot-be-undone")) %>')){
			form.<portlet:namespace />cmd.value = '<%=com.dotmarketing.util.Constants.DELETE_LIST%>';
			form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/mailinglists/view_mailinglists" /></portlet:renderURL>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /></portlet:actionURL>';
			submitForm(form);
	}
}

function loadLists() {
	loadSubscribers();
	loadUnsubscribers();
	loadBounces();
}

//Global Vars
var subscribersStarts = 0;
var subscribersLimit = <%= perPage %>;
var subscribersOrder = '';
var subscribersTotal = 0;

var unsubscribersStarts = 0;
var unsubscribersLimit = <%= perPage %>;
var unsubscribersOrder = '';
var unsubscribersTotal = 0;

var bouncesStarts = 0;
var bouncesLimit = <%= perPage %>;
var bouncesOrder = '';
var bouncesTotal = 0;

var subscribersPageSelected = false;
var allSubscribersSelected = false;

var unsubscribersPageSelected = false;
var allUnsubscribersSelected = false;

var bouncesPageSelected = false;
var allBouncesSelected = false;

//Subscribers manipulation functions

function deleteSubscribers() {
	var userIds = new Array();
	var selectBox = document.getElementById("subscribersForm").subscribers;
	for (var i = 0; selectBox != null && i < selectBox.length; i++) {
		if(selectBox[i].checked) userIds.push(selectBox[i].value);			
	}
	
	if ((i == 0) && (selectBox != null)){
		if(selectBox.checked) userIds.push(selectBox.value);
	}
	
	if(userIds.length == 0) {
		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-the-desired-unsubscriptions-and-then-hit-delete")) %>');
		return;
	}
	
	var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-the-selected-subscribers")) %>';
	if(allSubscribersSelected)
		message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-all-the-subscribers-of-this-list")) %>';
	if(confirm(message)) {
		if(allSubscribersSelected)
			MailingListAjax.deleteAllSubscribers('<%=ml.getInode()%>', deleteSubscribersCallback);
		else {
			MailingListAjax.deleteSubscribers('<%=ml.getInode()%>', userIds, deleteSubscribersCallback);
		}
	}	
}

function deleteSubscribersCallback () {
	loadSubscribers();
}

function checkAllSubscribers() {

	var form = document.getElementById("subscribersForm");
	var selectBox = form.subscribers;
	if (0 < selectBox.length) {
		for (i=0;selectBox != null && i<selectBox.length;i++) {
			if(!subscribersPageSelected)
				dijit.byId(selectBox[i].id).attr('value', true);
			else
				dijit.byId(selectBox[i].id).attr('value', false);
		}
	} else {
		if(!subscribersPageSelected)
			dijit.byId(selectBox.id).attr('value', true);
		else
			dijit.byId(selectBox.id).attr('value', false);
	}

	var messageDiv = document.getElementById('subscribersMessage');
	if(!subscribersPageSelected) {
		var countSelected = subscribersLimit > subscribersTotal?subscribersTotal:subscribersLimit;
		var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + countSelected + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "subscribers-on-this-page-are-selected")) %>';
		if(subscribersTotal > subscribersLimit)
			html += '<a href="javascript: selectAllSubscribers()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all")) %> ' + subscribersTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Subscribers")) %>.</a>';
		messageDiv.innerHTML = html;
		subscribersPageSelected = true;
	} else {
		clearSubscribersSelection ();
	}	
}

function selectAllSubscribers () {
	var messageDiv = document.getElementById('subscribersMessage');
	var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all")) %> ' + subscribersTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "subscribers-on-this-page-are-selected")) %>' +
		'<a href="javascript: clearSubscribersSelection()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Selection")) %>.</a>';
	messageDiv.innerHTML = html;
	allSubscribersSelected = true;
}

function clearSubscribersSelection () {

	subscribersPageSelected = false;
	allSubscribersSelected = false;
	
	var form = document.getElementById("subscribersForm");
	
	selectBox = form.subscribers;
	if (selectBox != null) {
		if (0 < selectBox.length) {
			for (i=0;selectBox != null && i<selectBox.length;i++) {
				dijit.byId(selectBox[i].id).attr('value', false);
			}
		} else {
			dijit.byId(selectBox.id).attr('value', false);
		}
	}
	if (dijit.byId("subscribersCheckAll") != null)
		dijit.byId("subscribersCheckAll").attr('value', false);

	document.getElementById('subscribersMessage').innerHTML = '';
	
}

function subscribersNextPage() {
	subscribersStarts = subscribersStarts + subscribersLimit;
	loadSubscribers();
}

function subscribersPreviousPage() {
	subscribersStarts = subscribersStarts - subscribersLimit;
	if(subscribersStarts < 0) subscribersStarts = 0; 
	loadSubscribers();
}

function sortSubscribersBy(sortField) {
	subscribersStarts = 0;
	if(sortField == subscribersOrder)
		subscribersOrder = sortField + " desc";
	else
		subscribersOrder = sortField;
	loadSubscribers();
}

function loadSubscribers () {
	clearSubscribersSelection();

	document.getElementById('subscribersCount').innerHTML = 'Loading...';
	MailingListAjax.getSusbscribers('<%=ml.getInode()%>', '<%= user.getUserId() %>', { start: subscribersStarts, limit: subscribersLimit, order: subscribersOrder }, { callback: loadSubscribersCallback, errorHandler: errHandler });
}

function errHandler(error) {
	alert(error);
}

function loadSubscribersCallback(results) {
	
	var table = document.getElementById('subscribersTable');	
	var totalRows = table.rows.length; //DOTCMS-4204
	for(var i = totalRows - 1; i >= 0; i--){	
		table.deleteRow(i);
	}
	
	var total = results.total;
	var data = results.data;
	
	for(var i = 0; i < data.length; i++) {
		var record = data[i];
		var rowClass = 'alternate_1';
		if ((i % 2) == 1)
			rowClass = 'alternate_2';
		var html = getRecordHTMLRow(record, rowClass);
		new Insertion.Bottom(table, html);
	}
	
	if(data.length == 0) {
		var html = '<td nowrap valign="top" colspan=13 align="center">' +
			'	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "There-are-no-subscribers-for-this-list")) %>' +
			'</td>';
		new Insertion.Bottom(table, html);
	}

	
	var subscribersPagination = document.getElementById('subscribersPagination');
	subscribersPagination.innerHTML = '';
	if(data.length > 0) {
		html = '<div class="yui-u first" style="text-align:left;">';
		if(subscribersStarts > 0) {
			dijit.registry.remove("subscribersPreviousButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="previousIcon" id="subscribersPreviousButton" name="subscribersPreviousButton" href="#subscribers" onclick="subscribersPreviousPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Previous-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>';
	
		var startIdx = (subscribersStarts + 1) <= total?(subscribersStarts + 1):total;
		var endIdx = (subscribersStarts + subscribersLimit) <= total?(subscribersStarts + subscribersLimit):total;
		
		html += '<div class="yui-u" style="text-align:center;">' +	startIdx + ' - ' + endIdx + ' <%=LanguageUtil.get(pageContext, "of")%> ' + total + '</div>';
		
		html += '<div class="yui-u" style="text-align:right;">';
		if(subscribersStarts + subscribersLimit < total) {
			dijit.registry.remove("subscribersNextButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="nextIcon" id="subscribersNextButton" name="subscribersNextButton" href="#subscribers" onclick="subscribersNextPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Next-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>'
		new Insertion.Bottom(subscribersPagination, html);
	}
	
	document.getElementById('subscribersCount').innerHTML = total;
	subscribersTotal = total;

	if(total == 0) 
		document.getElementById('subscribersControls').innerHTML = "";

	dojo.parser.parse(table);
	dojo.parser.parse(subscribersPagination);

}

//Unsubscribers

function deleteUnsubscribers() {
	var userIds = new Array();
	var selectBox = document.getElementById("unsubscribersForm").subscribers;
	for (var i = 0; selectBox != null && i < selectBox.length; i++) {
		if(selectBox[i].checked) userIds.push(selectBox[i].value);			
	}
	
	if ((i == 0) && (selectBox != null)){
		if(selectBox.checked) userIds.push(selectBox.value);
	}
	
	if(userIds.length == 0) {
		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-the-desired-unsubscriptions-and-then-hit-delete")) %>');
		return;
	}
	
	var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-the-selected-unsubcriptions")) %>';
	if(allUnsubscribersSelected)
		message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-all-the-unsubcriptions-from-this-list")) %>';
	if(confirm(message)) {
		if(allUnsubscribersSelected)
			MailingListAjax.deleteAllUnsubscribers('<%=ml.getInode()%>', deleteUnsubscribersCallback);
		else {
			MailingListAjax.deleteUnsubscribers('<%=ml.getInode()%>', userIds, deleteUnsubscribersCallback);
		}
	}	
}

function deleteUnsubscribersCallback () {
	loadUnsubscribers();
}

function checkAllUnsubscribers() {

	var form = document.getElementById("unsubscribersForm");
	var selectBox = form.subscribers;
	if (0 < selectBox.length) {
		for (i=0;selectBox != null && i<selectBox.length;i++) {
			if(!unsubscribersPageSelected)
				dijit.byId(selectBox[i].id).attr('value', true);
			else
				dijit.byId(selectBox[i].id).attr('value', false);
		}
	} else {
		if(!subscribersPageSelected)
			dijit.byId(selectBox.id).attr('value', true);
		else
			dijit.byId(selectBox.id).attr('value', false);
	}

	var messageDiv = document.getElementById('unsubscribersMessage');
	if(!unsubscribersPageSelected) {
		var countSelected = unsubscribersLimit > unsubscribersTotal?unsubscribersTotal:unsubscribersLimit;
		var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + countSelected + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unsubscriptions-on-this-page-are-selected")) %>';
		if(unsubscribersTotal > unsubscribersLimit)
			html += '<a href="javascript: selectAllUnsubscribers()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all")) %> ' + unsubscribersTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unsubscriptions")) %></a>';
		messageDiv.innerHTML = html;
		unsubscribersPageSelected = true;
	} else {
		clearUnsubscribersSelection ();
	}	
}

function selectAllUnsubscribers () {
	var messageDiv = document.getElementById('unsubscribersMessage');
	var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + unsubscribersTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unsubscriptions-on-this-page-are-selected")) %>' +
		'<a href="javascript: clearUnsubscribersSelection()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Selection")) %>.</a>';
	messageDiv.innerHTML = html;
	allUnsubscribersSelected = true;
}

function clearUnsubscribersSelection () {

	unsubscribersPageSelected = false;
	allUnsubscribersSelected = false;
	
	var form = document.getElementById("unsubscribersForm");
	
	selectBox = form.subscribers;
	if (selectBox != null) {
		if (0 < selectBox.length) {
			for (i=0;selectBox != null && i<selectBox.length;i++) {
				dijit.byId(selectBox[i].id).attr('value', false);
			}
		} else {
			dijit.byId(selectBox.id).attr('value', false);
		}
	}
	if (dijit.byId("unsubscribersCheckAll") != null)
		dijit.byId("unsubscribersCheckAll").attr('value', false);

	document.getElementById('unsubscribersMessage').innerHTML = '';
	
}

function unsubscribersNextPage() {
	unsubscribersStarts = unsubscribersStarts + unsubscribersLimit;
	loadUnsubscribers();
}

function unsubscribersPreviousPage() {
	unsubscribersStarts = unsubscribersStarts - unsubscribersLimit;
	if(unsubscribersStarts < 0) unsubscribersStarts = 0; 
	loadUnsubscribers();
}

function sortUnsubscribersBy(sortField) {
	unsubscribersStarts = 0;
	if(sortField == unsubscribersOrder)
		unsubscribersOrder = sortField + " desc";
	else
		unsubscribersOrder = sortField;
	loadUnsubscribers();
}

function loadUnsubscribers () {
	clearUnsubscribersSelection();

	document.getElementById('unsubscribersCount').innerHTML = 'Loading...';
	MailingListAjax.getUnsusbscribers('<%=ml.getInode()%>', '<%= user.getUserId() %>', { start: unsubscribersStarts, limit: unsubscribersLimit, order: unsubscribersOrder }, loadUnsubscribersCallback);
}

function loadUnsubscribersCallback(results) {
	var table = document.getElementById('unsubscribersTable');
	var totalRows = table.rows.length;
	for(var i = totalRows - 1; i >= 0; i--){	
		table.deleteRow(i);
	}
	
	var total = results.total;
	var data = results.data;
	
	for(var i = 0; i < data.length; i++) {
		var record = data[i];
		var rowClass = 'alternate_1';
		if ((i % 2) == 1)
			rowClass = 'alternate_2';
		var html = getRecordHTMLRow(record, rowClass);
		new Insertion.Bottom(table, html);
	}
	
	if(data.length == 0) {
		var html = '<td nowrap valign="top" colspan=13 align="center">' +
			'	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "There-are-no-unsubscriptions-for-this-list")) %>' +
			'</td>';
		new Insertion.Bottom(table, html);
	}

	var unsubscribersPagination = document.getElementById('unsubscribersPagination');
	unsubscribersPagination.innerHTML = '';
	if(data.length > 0) {
		html = '<div class="yui-u first" style="text-align:left;">';
		if(unsubscribersStarts > 0) {
			dijit.registry.remove("unsubscribersPreviousButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="previousIcon" id="unsubscribersPreviousButton" name="unsubscribersPreviousButton" href="#unsubscribers" onclick="unsubscribersPreviousPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Previous-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>';
	
		var startIdx = (unsubscribersStarts + 1) <= total?(unsubscribersStarts + 1):total;
		var endIdx = (unsubscribersStarts + unsubscribersLimit) <= total?(unsubscribersStarts + unsubscribersLimit):total;
		
		html += '<div class="yui-u" style="text-align:center;">' +	startIdx + ' - ' + endIdx + ' <%=LanguageUtil.get(pageContext, "of")%> ' + total + '</div>';
		
		html += '<div class="yui-u" style="text-align:right;">';
		if(unsubscribersStarts + unsubscribersLimit < total) {
			dijit.registry.remove("unsubscribersNextButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="nextIcon" id="unsubscribersNextButton" name="unsubscribersNextButton" href="#unsubscribers" onclick="unsubscribersNextPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Next-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>'
		new Insertion.Bottom(unsubscribersPagination, html);
	}
	
	document.getElementById('unsubscribersCount').innerHTML = total;
	unsubscribersTotal = total;
	
	if(total == 0) 
		document.getElementById('unsubscribersControls').innerHTML = "";
	
	dojo.parser.parse(table);
	dojo.parser.parse(unsubscribersPagination);
}

// Bounces

function deleteBounces() {
	var userIds = new Array();
	var selectBox = document.getElementById("bouncesForm").subscribers;
	for (var i = 0; selectBox != null && i < selectBox.length; i++) {
		if(selectBox[i].checked) userIds.push(selectBox[i].value);			
	}
	if ((i == 0) && (selectBox != null)){
		if(selectBox.checked) userIds.push(selectBox.value);
	}
	
	if(userIds.length == 0) {
		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-the-desired-bounces-and-then-hit-delete")) %>');
		return;
	}
	
	var message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-the-selected-bounces")) %>';
	if(allBouncesSelected)
		message = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-all-the-bounces-from-this-list")) %>';
	if(confirm(message)) {
		if(allBouncesSelected)
			MailingListAjax.deleteAllBounces('<%=ml.getInode()%>', deleteBouncesCallback);
		else {
			MailingListAjax.deleteBounces('<%=ml.getInode()%>', userIds, deleteBouncesCallback);
		}
	}	
}

function deleteBouncesCallback () {
	loadBounces();
}

function checkAllBounces() {

	var form = document.getElementById("bouncesForm");
	var selectBox = form.subscribers;
	if (0 < selectBox.length) {
		for (i=0;selectBox != null && i<selectBox.length;i++) {
			if(!bouncesPageSelected)
				dijit.byId(selectBox[i].id).attr('value', true);
			else
				dijit.byId(selectBox[i].id).attr('value', false);
		}
	} else {
		if(!subscribersPageSelected)
			dijit.byId(selectBox.id).attr('value', true);
		else
			dijit.byId(selectBox.id).attr('value', false);
	}

	var messageDiv = document.getElementById('bouncesMessage');
	if(!bouncesPageSelected) {
		var countSelected = bouncesLimit > bouncesTotal?bouncesTotal:bouncesLimit;
		var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + countSelected + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bounces-on-this-page-are-selected")) %>';
		if(bouncesTotal > bouncesLimit)
			html += '<a href="javascript: selectAllBounces()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + bouncesTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bounces")) %>.</a>';
		messageDiv.innerHTML = html;
		bouncesPageSelected = true;
	} else {
		clearBouncesSelection ();
	}	
}

function selectAllBounces () {
	var messageDiv = document.getElementById('bouncesMessage');
	var html = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + bouncesTotal + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bounces-on-this-page-are-selected")) %>' +
		'<a href="javascript: clearBouncesSelection()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Selection")) %>.</a>';
	messageDiv.innerHTML = html;
	allBouncesSelected = true;
}

function clearBouncesSelection () {

	bouncesPageSelected = false;
	allBouncesSelected = false;
	
	var form = document.getElementById("bouncesForm");
	
	selectBox = form.subscribers;
	if (selectBox != null) {
		if (0 < selectBox.length) {
			for (i=0;selectBox != null && i<selectBox.length;i++) {
				dijit.byId(selectBox[i].id).attr('value', false);
			}
		} else {
			dijit.byId(selectBox.id).attr('value', false);
		}
	}
	if (dijit.byId("bouncesCheckAll") != null)
		dijit.byId("bouncesCheckAll").attr('value', false);

	document.getElementById('bouncesMessage').innerHTML = '';
	
}

function bouncesNextPage() {
	bouncesStarts = bouncesStarts + bouncesLimit;
	loadBounces();
}

function bouncesPreviousPage() {
	bouncesStarts = bouncesStarts - bouncesLimit;
	if(bouncesStarts < 0) bouncesStarts = 0; 
	loadBounces();
}

function sortBouncesBy(sortField) {
	bouncesStarts = 0;
	if(sortField == bouncesOrder)
		bouncesOrder = sortField + " desc";
	else
		bouncesOrder = sortField;
	loadBounces();
}

function loadBounces () {
	clearBouncesSelection();

	document.getElementById('bouncesCount').innerHTML = 'Loading...';
	MailingListAjax.getBounces('<%=ml.getInode()%>', '<%= user.getUserId() %>', { start: bouncesStarts, limit: bouncesLimit, order: bouncesOrder }, loadBouncesCallback);
}

function loadBouncesCallback(results) {
	var table = document.getElementById('bouncesTable');
	var totalRows = table.rows.length;
	for(var i = totalRows - 1; i >= 0; i--){	
		table.deleteRow(i);
	}
	
	var total = results.total;
	var data = results.data;
	
	for(var i = 0; i < data.length; i++) {
		var record = data[i];
		var rowClass = 'alternate_1';
		if ((i % 2) == 1)
			rowClass = 'alternate_2';
		var html = getRecordHTMLRow(record, rowClass);
		new Insertion.Bottom(table, html);
	}
	
	if(data.length == 0) {
		var html = '<td nowrap valign="top" colspan=13 align="center">' +
			'	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "There-are-no-bounces-or-errors-for-this-list")) %>' +
			'</td>';
		new Insertion.Bottom(table, html);
	}

	var bouncesPagination = document.getElementById('bouncesPagination');
	bouncesPagination.innerHTML = '';
	if(data.length > 0) {
		html = '<div class="yui-u first" style="text-align:left;">';
		if(bouncesStarts > 0) {
			dijit.registry.remove("bouncesPreviousButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="previousIcon" id="bouncesPreviousButton" name="bouncesPreviousButton" href="#bounces" onclick="bouncesPreviousPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Previous-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>';
	
		var startIdx = (bouncesStarts + 1) <= total?(bouncesStarts + 1):total;
		var endIdx = (bouncesStarts + bouncesLimit) <= total?(bouncesStarts + bouncesLimit):total;
		
		html += '<div class="yui-u" style="text-align:center;">' +	startIdx + ' - ' + endIdx + ' <%=LanguageUtil.get(pageContext, "of")%> ' + total + '</div>';

		html += '<div class="yui-u" style="text-align:right;">';
		if(bouncesStarts + bouncesLimit < total) {
			dijit.registry.remove("bouncesNextButton");
			html += '	<button dojoType="dijit.form.Button" iconClass="nextIcon" id="bouncesNextButton" name="bouncesNextButton" href="#bounces" onclick="bouncesNextPage();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Next-Page")) %></button>';
		} else {
			html += '&nbsp;';
		}
		html += '</div>'
		new Insertion.Bottom(bouncesPagination, html);
	}
		
	document.getElementById('bouncesCount').innerHTML = total;
	bouncesTotal = total;

	if(total == 0) 
		document.getElementById('bouncesControls').innerHTML = "";

	dojo.parser.parse(table);
	dojo.parser.parse(bouncesPagination);
}

//Utility funcs
function getRecordHTMLRow (record, rowClass) {

	var html= '';
	html += '<tr class="' + rowClass + '">';
	html += '	<td valign="top" align="center" width="100">';
<% 
	if (mlEditable) { 
%>
	html += '	<input type="checkbox" dojoType="dijit.form.CheckBox" name="subscribers" id="subscribers_' + record.inode + '" value="' + record.inode + '">';
	dijit.registry.remove("subscribers_" + record.inode);
<% 
	} 
%>
	html += '	</td>';
	if(record.hasPermissionToWrite) {

		<%
		com.liferay.portlet.PortletURLImpl portletURLImpl = new com.liferay.portlet.PortletURLImpl(request,"EXT_USER_ADMIN", layoutId, false);			
		String linkHREF = portletURLImpl.toString() + "&dm_rlout=1";
	    %>	
		html += '<td valign="top" align="center">';
		html += '	<a href=" <%= linkHREF %> &user_id='+ record.userId+ '">';
		html += '	<span class="editIcon"></span></a>';
		html += '</td>';
		html += '<td valign="top">';
		html += '	<a href=" <%= linkHREF %> &user_id='+ record.userId+ '">';
		html += '	' + record.firstName ;
		html += '	</a>';
		html += '</td>';
	} else {	
		html += '<td valign="top" align="center">';
		html += '</td>';
		html += '<td valign="top">';
		html += '	' + record.firstName;
		html += '</td>';
	}
	html += '	<td valign="top">' + record.lastName + '&nbsp;</td>';
	html += '	<td valign="top">' + record.emailAddress + '&nbsp;</td>';
	html += '	<td valign="top">' + ((record.lastResult == 500)?'<font color="red">' + record.lastMessage + '</font>':'ok') + '</td>';
	html += '</tr>';
	
	return html;
}

</script>
