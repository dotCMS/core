<%@ include file="/html/portlet/ext/browser/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>

<%
request.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CURRENT_DEVICE);
APILocator.getVisitorAPI().removeVisitor(request);

com.dotmarketing.beans.Host myHost =  WebAPILocator.getHostWebAPI().getCurrentHost(request); 
Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
String languageId = String.valueOf(defaultLang.getId());

if(session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED)!= null){
	languageId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
}
List<Language> languages = (List<Language>)request.getAttribute (com.dotmarketing.util.WebKeys.LANGUAGES);

%>

<div id="messagesTable" style="display: none;">
	<span class="exclamation"></span>
	<span id="messageBox"></span>
</div>

<div id="errorsTable" style="display: none;">
	<span class="exclamation-red"></span>
	<span id="errorBox"></span>
</div>




<style type="text/css" media="all">
	@import url(/html/portlet/ext/browser/browser.css);
</style>

<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>
<script src='/dwr/engine.js' type='text/javascript'></script>
<script src='/dwr/util.js' type='text/javascript'></script>
<script src="/dwr/interface/BrowserAjax.js" type="text/javascript"></script>
<script src="/dwr/interface/StructureAjax.js" type="text/javascript"></script>



<%@ include file="/html/portlet/ext/browser/view_browser_js_inc.jsp"%>
<%@ include file="/html/portlet/ext/browser/view_browser_menus_js_inc.jsp"%>

<style type="text/css">
	#borderContainer{ width: 100%; height: 50%;}
	#dijit_layout__Splitter_0{border:1px #eeeeee solid;}
	#assetListHead{border-bottom: 1px solid silver;margin-bottom:10px;}
	#assetListHead span{display:block;padding:5px 8px;font-size:85%;font-weight:bold;}
</style>

<script>

	var browserLoaded = false;
	var multipleLanguages = false;

	function  resizeBrowser(){

        var viewport = dojo.window.getBox();
	    var viewport_height = viewport.h;

	    var e =  dojo.byId("borderContainer");
	    dojo.style(e, "height", viewport_height + "px");

	    var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
	    	bc.resize();
	    }
	    console.log("resizing browser")
	}
	// need the timeout for back buttons

	dojo.addOnLoad(function(){
		resizeBrowser();
		setTimeout(resizeBrowser, 50);
		setTimeout(resizeBrowser, 500);
	});

	dojo.require("dojox.form.uploader.plugins.Flash");
	var counter=0;

	function doSearch() {
		
		var selectedFolder = document.getElementsByClassName("folderSelected")[0];
		var lang = dijit.byId("language_id").get('value');
		
		if(selectedFolder) {
			var folderId = selectedFolder.id;
			folderId = folderId.split("-TreeREF")[0];
			treeFolderSelected(folderId, lang);	
		} else if(counter>0) {
			var hostId = '<%= (myHost != null) ? myHost.getIdentifier() : "" %>';
			treeFolderSelected(hostId, lang);	
		}
		
		selectedLang = lang;
		
		counter++;
		
	}
</script>
	
<!-- <div class="buttonBoxLeft">
	<b><%= LanguageUtil.get(pageContext, "Sites-and-Folders") %></b>
</div> -->

<%if(UtilMethods.isSet(languages) && languages.size()>1) {%>
	<script>
		multipleLanguages = true;
	</script>

<%}%>




<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="white-space: nowrap">
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="leading" style="width: 200px; overflow: auto;" class="portlet-sidebar-wrapper" id="leftContentPane">
	<!-- Browser Tree - Left Hand Side -->
		<div id="assetTreeWrapper">
			<ul id="TreeUL"> </ul>
		</div>
	<!-- End of Browser Tree - Left Hand Side -->

	</div>

     <div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="rightContentPane" id="rightContentPane">

     	<div class="portlet-main">
			<!-- START Toolbar -->
			<div class="portlet-toolbar">
				<div class="portlet-toolbar__actions-primary">
					<div id="combo_zone2" >
						<input id="language_id" />
					</div>
					<%@include file="../contentlet/languages_select_inc.jsp" %>
				</div>
				<div class="portlet-toolbar__info">
					<!-- The trash can -->
						<div  id="trash-DIV">
							<a id="trash-TreeREF" href="javascript: ;">
								<span class="trashIcon" id="Trash-TreeFolderIMG"></span>
								<span id="trashLabel"><%= LanguageUtil.get(pageContext, "Show-Archived") %></span> &nbsp;&nbsp;
							</a>
						</div>
					<!--  End of the trash can -->
				</div>
		    	<div class="portlet-toolbar__actions-secondary">
		    		<div id="addNewDropDownButtonDiv"></div>   	
		    	</div>
		   </div>
		   <!-- END Toolbar -->
		
			<table class="listingTable" id="assetListBodyTD">
				<thead id="assetListHead">
					<tr>
						<th>
							<a href="javascript: changeContentSort('name');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Name") %></a>
						</th>
						<th><a href="javascript: changeContentSort('sortOrder');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Menu") %></a></th>
						<th><%= LanguageUtil.get(pageContext, "Status") %></th>
						<th></th>
						<th><a href="javascript: changeContentSort('modUser');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Mod-User") %></a></th>
						<th><a href="javascript: changeContentSort('modDate');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Mod-Date") %></a></th>
					</tr>
				</thead>
				<tbody id="assetListBody"></tbody>
			</table>

			<div id="loadingContentListing" name="loadingContentListing" align="center" style="">
				<br />
				<br />
				<font class="bg" size="2"> <b><%= LanguageUtil.get(pageContext, "Loading") %></b> <br />
				<img src="/html/images/icons/processing.gif" /></font> <br />
				<br />
			</div>
		</div>
	</div>
</div>



<div id="statusDiv" align="left" style="width: 100%; display: none; height: 100px; overflow: auto;"></div>

<div id="popups" class="context-menus"></div>


<script type="text/javascript">

	<% if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())) { %>
	var cmsAdminUser = true;
	<% } else { %>
	var cmsAdminUser = false;
	<% } %>

	var referer = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/browser/view_browser" /></portlet:renderURL>'

    debugMessagesEnabled = false;

    dojo.addOnLoad(function () {
    	//Adding the events for the trash can
    	Droppables.add('trash-TreeREF', { onDrop: function(draggableElem, droppableElem, e) { droppedOnTrash(draggableElem.id.split('-')[0], 'Trash', e) }});

    	Event.observe('trash-TreeREF', 'mouseup', function (e){ trashRefMouseUp(e) });

    	//Content Area event
    	Event.observe('assetListBodyTD', 'mouseup', function (e){ contentAreaRefMouseUp(e) });
        BrowserAjax.getTree( '<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>', initializeTree);
    });

</script>

<div dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Upload-file(s)")%>" id="addFileDialog" style="width: 700px; height: 474px;"
		onLoad="removeAddlStyleRef();">
</div>


