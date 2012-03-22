<%@ include file="/html/portlet/ext/browser/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>



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
	.leftContentPane{ width:25%; }
</style>

<script>

	var browserLoaded = false;
	
	function  resizeBrowser(){
		if(browserLoaded) return;
		browserLoaded=true;
	    var viewport = dijit.getViewport();
	    var viewport_height = viewport.h;
	    var e =  dojo.byId("borderContainer");
	    dojo.style(e, "height", viewport_height -150 + "px");
	    var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
	    	bc.resize();
	    }
	}
	// need the timeout for back buttons
	setTimeout(resizeBrowser, 50);
	dojo.addOnLoad(resizeBrowser);
	
</script>

 	<div id="addNewDropDownButtonDiv" class="buttonBoxRight">
  	</div>
  	
 	<div class="buttonBoxLeft">
		<b><%= LanguageUtil.get(pageContext, "Sites-and-Folders") %></b>
  	</div>


<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="white-space: nowrap">
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="leading" style="margin-top:37px; overflow: auto;" class="leftContentPane" id="leftContentPane">

	<!-- Browser Tree - Left Hand Side -->
		<div id="assetTreeWrapper" style="padding:8px 0 0 0;">
			<ul id="TreeUL"> </ul>
			<span class="shimIcon"></span>
		</div>
	<!-- End of Browser Tree - Left Hand Side -->

	</div>

     <div dojoType="dijit.layout.ContentPane" splitter="true" style="margin-top:35px;" region="center" class="rightContentPane" id="rightContentPane">
		<table class="browserTable" id="assetListBodyTD">
			<thead id="assetListHead">
				<tr>
					<th>
						<a href="javascript: changeContentSort('name');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Name") %></a>
					</th>
					<th><a href="javascript: changeContentSort('sortOrder');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Menu") %></a></th>
					<th><%= LanguageUtil.get(pageContext, "Status") %></th>
					<th><%= LanguageUtil.get(pageContext, "Description") %></th>
					<th><a href="javascript: changeContentSort('modUser');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Mod-User") %></a></th>
					<th><a href="javascript: changeContentSort('modDate');" style="text-decoration: underline"><%= LanguageUtil.get(pageContext, "Mod-Date") %></a></th>
				</tr>
			</thead>
			
			<tbody id="assetListBody"></tbody>
			
			
		<!-- The trash can -->
			<tr>
				<td colspan="6" style="height:100px;vertical-align: bottom;">
					<div style="text-align: right;vertical-align: bottom" id="trash-DIV">
						<a id="trash-TreeREF" style="text-decoration: none; color: black;" href="javascript: ;">
							<span class="trashIcon" id="Trash-TreeFolderIMG"></span>
							<span id="trashLabel"><%= LanguageUtil.get(pageContext, "Show-Archived") %></span> &nbsp;&nbsp;
						</a>
					</div>
				</td>
			</tr>
		<!--  End of the trash can -->
		
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



<div id="statusDiv" align="left" style="width: 100%; display: none; height: 100px; overflow: auto;"></div>

<div id="popups"></div>


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

	<div dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Upload-file(s)")%>" id="addFileDialog" style="width: 700px; height: 430px;"
			onLoad="removeAddlStyleRef();">
			
	</div>

