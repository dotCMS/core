<%@page import="com.liferay.util.Xss"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus" %>

<html xmlns="http://www.w3.org/1999/xhtml">

<%
User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
if(user == null){
	response.setStatus(HttpStatus.SC_FORBIDDEN);
	return;
}
String query = request.getParameter("query");
if (!UtilMethods.isSet(query)) {
	query = null != session.getAttribute(com.dotmarketing.util.WebKeys.EXECUTED_LUCENE_QUERY)
		? (String) session.getAttribute(com.dotmarketing.util.WebKeys.EXECUTED_LUCENE_QUERY)
		: "";
}
String sortBy = request.getParameter("sort");
String offset = request.getParameter("offset");
String limit = request.getParameter("limit");
String userToPullID = request.getParameter("userId");
boolean userIsAdmin = false;

if(!UtilMethods.isSet(sortBy)){
	sortBy = "";
}
if(!UtilMethods.isSet(offset)){
	offset = "0";
}
if(!UtilMethods.isSet(limit)){
	limit = "20";
}

if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
	userIsAdmin = true;
}

if(!UtilMethods.isSet(userToPullID)){
	userToPullID = null;
}

if(query == null){
	query = "";
}
query = Xss.strip(query);
%>

<script>
	dojo.require("dijit.form.NumberTextBox");


	function search(event) {
		luceneLoadingMode();
		var form = document.forms.luceneQueryForm;
		var formData = new FormData(form);
		fetch('/api/content/_search?rememberQuery=true', {
			method: 'post',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(Object.fromEntries(formData))
		})
		.then(response => response.json())
		.then(data => {
			if (data.entity) {
				var contentlets = data.entity.jsonObjectView.contentlets;
				setSummary(data.entity.contentTook, data.entity.queryTook, data.entity.resultsSize);
				if (contentlets && contentlets.length){
					setContentletTable(contentlets);
				} else {
					showNoResults();
				}
			} else {
				setErrorMessage(data.message);
			}
			luceneResultMode();
		}).catch(error => {
			setErrorMessage(error);
			luceneResultMode();
		});

	}

	function setContentletTable(contentlets) {
		const luceneResultTable = document.getElementById('luceneResultTable');
		let htmlRows = '';
		contentlets.forEach((item, index) => {
			htmlRows += setContentletItem(item, index+1);
		});
		luceneResultTable.innerHTML = htmlRows;
	}

	function setContentletItem(contentlet, index) {
		return `<tr><th><strong>${index}.</th><th><strong><%= LanguageUtil.get(pageContext, "Title") %></strong></th>
				<th><a onclick="return openContent(event, '${contentlet.inode}')" href="/dotAdmin/#/c/content/${contentlet.inode}">
				${contentlet.title}</a></th></tr><tr><td></td>
				<td><strong><%= LanguageUtil.get(pageContext, "ContentType") %>:</strong></td>
				<td width="90%">${contentlet.contentType}</td>
				</tr><tr><td></td>
				<td><strong><%= LanguageUtil.get(pageContext, "Inode") %>:</strong></td>
				<td width="90%">${contentlet.inode}</td>
				</tr><tr><td></td>
				<td><strong><%= LanguageUtil.get(pageContext, "Identifier") %>:</strong></td>
				<td>${contentlet.identifier}</td>
				</tr>`
	}

	function setSummary(contentTook, queryTook, resultsSize) {
		const luceneResultSize = document.getElementById('luceneResultSize');
		const luceneQueryTook = document.getElementById('luceneQueryTook');
		const luceneContentPopulation = document.getElementById('luceneContentPopulation');

		luceneResultSize.innerText = resultsSize;
		luceneQueryTook.innerText = queryTook;
		luceneContentPopulation.innerText = contentTook;
	}

	function setErrorMessage(error) {
		const errorMessageContainer = document.getElementById('errorMessageContainer');
		const errorElement = document.getElementById('errorMessage');
		errorElement.innerText = error;
		errorMessageContainer.style.display= 'block';
		showNoResults();
		setSummary(0, 0, 0);

	}

	function luceneLoadingMode() {
		clearErrorMessage();
		const loadingContainer = document.getElementById('luceneResultLoading');
		const resultContainer = document.getElementById('luceneResultContainer');

		resultContainer.style.display= 'none';
		loadingContainer.style.display= 'block';
	}


	function luceneResultMode() {
		const loadingContainer = document.getElementById('luceneResultLoading');
		const resultContainer = document.getElementById('luceneResultContainer');

		loadingContainer.style.display= 'none';
		resultContainer.style.display= 'block';
	}


	function clearErrorMessage() {
		const errorMessageContainer = document.getElementById('errorMessageContainer');
		errorMessageContainer.style.display= 'none';
	}

	function showNoResults() {
		const luceneResultTable = document.getElementById('luceneResultTable');
		luceneResultTable.innerHTML = `<tr><td><div style="text-align:center; padding: 40px;"><%= LanguageUtil.get(pageContext, "No-Results") %></div></td></tr>`;
	}

	function openContent(event, inode)	{
		if (! hastModifiers(event)){
			window.parent.location = `/dotAdmin/#/c/content/${inode}`;
			return false;
		}
		return true;
	}


	function hastModifiers(event) {
		return event.getModifierState("Shift") || event.getModifierState("Alt") ||
				event.getModifierState("Control") || event.metaKey;
	}

</script>

<!-- START Split Screen -->
    <div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer">
	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" id="filterWrapper" splitter="false" region="leading" style="width: 400px;" class="portlet-sidebar-wrapper" >
	    <div class="portlet-sidebar">

			<div id="advancedSearch">
				<form name="query" id="luceneQueryForm" onsubmit="search(event);return false">
					<dl class="vertical">

						<dt><label for="query"><%= LanguageUtil.get(pageContext, "Lucene-Query") %> </label></dt>
						<dd><textarea dojoType="dijit.form.Textarea" name="query" style="width:365px;min-height: 150px;" id="query" type="text"><%=UtilMethods.htmlifyString(query)%></textarea></dd>

						<dt><label for="offset"><%= LanguageUtil.get(pageContext, "Offset") %> </label></dt>
						<dd><input name="offset" id="offset" dojoType="dijit.form.NumberTextBox" type="text" value="<%=offset%>" size="10" /></dd>

						<dt><label for="limit"><%= LanguageUtil.get(pageContext, "Limit") %> </label></dt>
						<dd><input name="limit" id="limit" dojoType="dijit.form.NumberTextBox" type="text" value="<%=limit%>" size="10" /></dd>

						<dt><label  for="sort"><%= LanguageUtil.get(pageContext, "Sort") %> </label></dt>
						<dd><input name="sort" id="sort" dojoType="dijit.form.TextBox" type="text" value="<%=sortBy%>" size="10" /></dd>

						<dt><label for="userId"><%= LanguageUtil.get(pageContext, "UserID") %> </label></dt>
						<dd><input name="userId" id="userId" dojoType="dijit.form.TextBox" type="text" value="<%=UtilMethods.webifyString(userToPullID)%>" size="40" <% if(!userIsAdmin){ %> disabled="disabled" <% } %>/></dd>
					</dl>
						<div class="buttonRow">
							<button type="submit" id="submitButton" dojoType="dijit.form.Button" value="Submit"><%= LanguageUtil.get(pageContext, "Submit") %></button>
					</div>

			        <script language="Javascript">
						/**
							focus on search box
						**/
						require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
							dojo.require('dojox.timing');
							t = new dojox.timing.Timer(500);
							t.onTick = function(){
							  focusUtil.focus(dom.byId("query"));
							  t.stop();
							}
							t.start();
						});
					</script>
				</form>
			</div>
		</div>
	</div>

	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="portlet-content-search" id="contentWrapper" style="overflow-y:auto; overflow-x:auto;">
		<div class="portlet-main"  id="luceneResultLoading" style="margin: 35px 20px; display:none"><%= LanguageUtil.get(pageContext, "Loading") %>... </div>
		<div class="portlet-main"  id="luceneResultContainer"  style="margin: 35px 20px;">

			<table id="errorMessageContainer" style="display:none">
				<tr>
					<td style='color:red;'><strong><%= LanguageUtil.get(pageContext, "Query-Error") %> :</strong></td>
					<td><span id="errorMessage"></span></td>
				</tr>
			</table>
			<table class="listingTable">
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "The-total-results-are") %> :</strong> <span id="luceneResultSize">0</span></td>
					<td></td>
				</tr>
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "Query-took") %> :</strong> <<span id="luceneQueryTook">0</span> ms</td>
					<td><em><%= LanguageUtil.get(pageContext, "This-includes-permissions-but-returns-only-the-index-objects") %></em></td>
				</tr>
				<tr>
					<td><strong><%= LanguageUtil.get(pageContext, "Content-Population-took") %> :</strong> <span id="luceneContentPopulation">0</span> ms</td>
					<td><em><%= LanguageUtil.get(pageContext, "This-includes-permissions-and-returns-full-content-objects") %></em></td>
				</tr>
			</table>
			<div style="margin-top: 30px;">
				<table class="listingTable" id="luceneResultTable">
					<tr><td><div style="text-align:center; padding: 40px;"><%= LanguageUtil.get(pageContext, "No-Results") %></div></td></tr>
				</table>
			</div>

		</div>
	</div>
</div>
