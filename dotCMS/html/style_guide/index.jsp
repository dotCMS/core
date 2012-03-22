<%@page import="com.dotmarketing.util.Config"%>
<%String dojoPath = Config.getStringProperty("path.to.dojo");%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>  
<head> 
<link rel="shortcut icon" href="//www.dotcms.com/global/favicon.ico" type="image/x-icon">

	<title>dotCMS Style Guide</title>
	<style media="all" type="text/css">

		@import "/html/common/css.jsp"; 
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
	</style>
	
<style>
	#doc,#doc1,#doc2,#doc3,#doc4{background-color:#fff;padding:10px;border:1px solid #d1d1d1;}
	#hd{margin-top:5px;}
	#bd{margin-bottom:20px;-moz-box-shadow:0px 0px 0px #fff;-webkit-box-shadow:0px 0px 0px #fff;border:0px;padding:0;}
	body{font:13px/1.22 Verdana, Geneva,  Arial, sans-serif;*font-size:small;*font:x-small;color:#555;line-height:138.5%;background-color:#f3f3f3;}/* 13pt */
	h1{font-size:174%;font-family:"Baskerville Old Face", "Times New Roman";margin:10px 0 10px 10px;font-weight:normal;}/* 22pt */
	h2{font-size:108%;color:#990000;padding:10px 15px 0 0;font-weight:normal;}/* 18pt */
	p,fieldset,table{font-size:85%;margin:0 0 .2em 0;}
	a{color: #2C548D;}
	a:hover{}
	hr {border:0; color: #9E9E9E;background-color: #9E9E9E;height: 1px;width: 100%;text-align: left;}
	pre {width: 100%;overflow: auto;font-size: 12px;padding: 0;margin: 0;background: #f0f0f0;border: 1px solid #ccc;line-height: 20px;overflow-Y: hidden;}
	pre code {margin: 0 0 0 20px;padding: 0 0 16px 0;display: block;}
</style>
	
<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js" djConfig="parseOnLoad:true, isDebug:false"></script>
<script type="text/javascript" src="<%=dojoPath%>/dojo/dot-dojo.js"></script>
<script type="text/javascript">
   dojo.require("dojo.parser");
   dojo.require("dijit.form.Button");
   dojo.require('dijit.layout.TabContainer');
   dojo.require('dijit.layout.ContentPane');
   dojo.require('dijit.form.FilteringSelect');
   dojo.require('dojo.data.ItemFileReadStore');
   dojo.require("dojo.fx");
   
	dojo.require("dijit.layout.SplitContainer");
	dojo.require("dijit.ColorPalette");
	dojo.require("dijit.form.Slider");
	dojo.require("dijit.Dialog");
	dojo.require("dijit.ProgressBar");
	dojo.require("dijit.form.ComboBox");
	dojo.require("dijit.Dialog");
	dojo.require("dijit.form.Button");
	dojo.require("dijit.form.CheckBox");
	dojo.require("dijit.form.DateTextBox");
	dojo.require("dijit.form.FilteringSelect");
	dojo.require("dijit.form.TextBox");
	dojo.require("dijit.form.ValidationTextBox");
	dojo.require("dijit.form.Textarea");
	dojo.require("dijit.Menu");
	dojo.require("dijit.MenuItem");
	dojo.require("dijit.MenuSeparator");
	dojo.require("dijit.ProgressBar");
	dojo.require("dijit.PopupMenuItem");
	dojo.require('dijit.layout.TabContainer');
	dojo.require('dijit.layout.ContentPane');
	dojo.require("dijit.layout.BorderContainer");
	dojo.require("dijit.TitlePane");
	dojo.require("dijit.Tooltip");
	dojo.require("dojo.parser");
	dojo.require("dojo.fx");
</script>

</head>

<body class="dmundra">

<div id="doc3">

<div id="hd">
	<img src="http://www.dotcms.org/global/images/template/logo2.gif" />
	<div class="yui-g">
		<div class="yui-u first"><h1>dotCMS UI Style Guide</h1></div>
		<div class="yui-u" style="text-align:right;padding: 10px 10px 0 0;">
			<div style="font-size:85%;">
				<b><a href="/html/style_guide/code_style_guide.jsp">Code Style Guide</a></b> |
				<a href="http://demos.dojotoolkit.org/demos/" target="_blank">Dojo Demos</a> | 
				<a href="http://archive.dojotoolkit.org/nightly/dojotoolkit/dijit/tests/form/test_Button.html" target="_blank">Dijit Button Test</a> | 
				<a href="http://docs.dojocampus.org/" target="_blank">Dojo Campus</a>
			</div>
		</div>
	</div>
</div>
	
<div id="bd">

<!-- START TABS -->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">


<!-- START ICON TABS -->
<div id="TabOne" dojoType="dijit.layout.ContentPane" title="Icons" >
	
	<h2>Action Icons</h2>
	<p>Please look at the icon set below.  If you need an icon for an action that is not specified below, please ask Jason Smith - jason - @ - dotcms.org.</p>
	<div class="yui-g">
			<div class="yui-u first">
				<table class="listingTable">
					<tr>
						<th scope="col" width="30px" align="center">Icon</th>
						<th scope="col">Description</th>
						<th scope="col" width="125px">File Name</th>
						<th scope="col" width="125px">Class Name</th>
					</tr>
					<tr>
						<td align="center"><span class="archiveIcon"></span></td>
						<td>Archive</td>
						<td>minus-shield.png</td>
						<td>archiveIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="unarchiveIcon"></span></td>
						<td>Unarchive</td>
						<td>plus-shield.png</td>
						<td>unarchiveIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="browseIcon"></span></td>
						<td>Browse</td>
						<td>images-stack.png</td>
						<td>browseIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="calMonthIcon"></span></td>
						<td>Calendar Month</td>
						<td>calendar-month.png</td>
						<td>calMonthIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="calWeekIcon"></span></td>
						<td>Calendar Week</td>
						<td>calendar-week.png</td>
						<td>calWeekIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="calListIcon"></span></td>
						<td>Calendar List</td>
						<td>calendar-list.png</td>
						<td>calListIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="cancelIcon"></span></td>
						<td>Cancel</td>
						<td>arrow-curve-180-double.png</td>
						<td>cancelIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="closeIcon"></span></td>
						<td>Close</td>
						<td>across-circle.png</td>
						<td>closeIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="copyIcon"></span></td>
						<td>Copy</td>
						<td>document-copy.png</td>
						<td>copyIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="cutIcon"></span></td>
						<td>Cut</td>
						<td>scissors-blue.png</td>
						<td>cutIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="deleteIcon"></span></td>
						<td>Delete</td>
						<td>cross.png</td>
						<td>deleteIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="editIcon"></span></td>
						<td>Edit</td>
						<td>pencil.png</td>
						<td>editIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="folderIcon"></span></td>
						<td>Folder</td>
						<td>folder-horizontal.png</td>
						<td>folderIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="formNewIcon"></span></td>
						<td>Form New</td>
						<td>ui-scroll-pane-form-plus.png</td>
						<td>formNewIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="helpIcon"></span></td>
						<td>Help / Hint</td>
						<td>lifebuoy.png</td>
						<td>helpIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="bugIcon"></span></td>
						<td>Report Bug</td>
						<td>leaf-wormhole.png</td>
						<td>bugIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="hostIcon"></span></td>
						<td>Host</td>
						<td>globe.png</td>
						<td>hostIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="hostDefaultIcon"></span></td>
						<td>Host Default</td>
						<td>globe-check.png</td>
						<td>hostDefaultIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="hostStoppedIcon"></span></td>
						<td>Host Stopped</td>
						<td>globe-grey.png</td>
						<td>hostStoppedIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="hostArchivedIcon"></span></td>
						<td>Host Archived</td>
						<td>globe-stop.png</td>
						<td>hostArchivedIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="imageNewIcon"></span></td>
						<td>Image Upload</td>
						<td>image-plus.png</td>
						<td>imageNewIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="linkIcon"></span></td>
						<td>Link/Virtual Link</td>
						<td>chain.png</td>
						<td>linkIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="pageIcon"></span></td>
						<td>Page</td>
						<td>blog-blue.png</td>
						<td>pageIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="newPageIcon"></span></td>
						<td>New Page</td>
						<td>blog-blue-plus.png</td>
						<td>newPageIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="movePageIcon"></span></td>
						<td>Move Page</td>
						<td>arrow-continue-180-top.png</td>
						<td>movePageIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="publishIcon"></span></td>
						<td>Publish</td>
						<td>globe.png</td>
						<td>publishIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="republishIcon"></span></td>
						<td>Republish</td>
						<td>globe-republish.png</td>
						<td>republishIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="unpublishIcon"></span></td>
						<td>Unpublsih</td>
						<td>globe-unpublish.png</td>
						<td>unpublishIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="lockIcon"></span></td>
						<td>Locked</td>
						<td>lock.png</td>
						<td>lockIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="unlockIcon"></span></td>
						<td>Unlocked</td>
						<td>lock-unlock.png</td>
						<td>unlockIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="keyIcon"></span></td>
						<td>Key</td>
						<td>key.png</td>
						<td>keyIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="uploadIcon"></span></td>
						<td>Upload</td>
						<td>drive-upload.png</td>
						<td>uploadIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="downloadIcon"></span></td>
						<td>Download</td>
						<td>drive-download.png</td>
						<td>downloadIcon</td>
					</tr>
				</table>
			</div>
			<div class="yui-u">
				<table class="listingTable">
					<tr>
						<th scope="col" width="30px" align="center">Icon</th>
						<th scope="col">Description</th>
						<th scope="col" width="125px">File Name</th>
						<th scope="col" width="125px">Class Name</th>
					</tr>
					<tr>
						<td align="center"><span class="mailListIcon"></span></td>
						<td>Mailing List</td>
						<td>mail-open-table.png</td>
						<td>mailListIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="nextIcon"></span></td>
						<td>Next</td>
						<td>navigation.png</td>
						<td>nextIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="previousIcon"></span></td>
						<td>Previous</td>
						<td>navigation-180.png</td>
						<td>previousIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="previewIcon"></span></td>
						<td>Preview</td>
						<td>eye.png</td>
						<td>previewIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="hideIcon"></span></td>
						<td>Hide</td>
						<td>eye-cross.png</td>
						<td>hideIcon</td>
					</tr>					
					<tr>
						<td align="center"><span class="plusIcon"></span></td>
						<td>Plus</td>
						<td>plus.png</td>
						<td>plusIcon</td>
					</tr>
					<tr>
						<td align="center"><span class=""></span></td>
						<td>Page Properties</td>
						<td>document-block.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><span class="minusIcon"></span></td>
						<td>Remove</td>
						<td>minus-small.png</td>
						<td>minusIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="reorderIcon"></span></td>
						<td>Reorder</td>
						<td>arrow-switch.png</td>
						<td>reorderIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="resetIcon"></span></td>
						<td>Reset or Clear</td>
						<td>action_refresh_blue.gif</td>
						<td>resetIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="saveIcon"></span></td>
						<td>Save</td>
						<td>disk.png</td>
						<td>saveIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="saveAssignIcon"></span></td>
						<td>Save and Assign</td>
						<td>ticket-save.png</td>
						<td>saveAssignIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="editScriptIcon"></span></td>
						<td>Edit Script/VTL</td>
						<td>script-pencil.png</td>
						<td>editScriptIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="searchIcon"></span></td>
						<td>Search</td>
						<td>magnifier-left.png</td>
						<td>searchIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="cartIcon"></span></td>
						<td>Shopping Cart</td>
						<td>shopping-basket.png</td>
						<td>cartIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="statisticsIcon"></span></td>
						<td>Statistics</td>
						<td>chart.png</td>
						<td>statisticsIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="liveIcon"></span></td>
						<td>Status Published</td>
						<td>status.png</td>
						<td>liveIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="workingIcon"></span></td>
						<td>Status Not Published</td>
						<td>status-away.png</td>
						<td>workingIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="archivedIcon"></span></td>
						<td>Status Archived</td>
						<td>status-busy.png</td>
						<td>archivedIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="spellIcon"></span></td>
						<td>Spell Check</td>
						<td>spell-check.png</td>
						<td>spellIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="templateIcon"></span></td>
						<td>Template</td>
						<td>layout-header-3-mix.png</td>
						<td>templateIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="workflowIcon"></span></td>
						<td>Work Flow Task</td>
						<td>ticket.png</td>
						<td>workflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="newWorkflowIcon"></span></td>
						<td>Work Flow Task <b>New</b></td>
						<td>ticket-plus.png</td>
						<td>newWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="cancelWorkflowIcon"></span></td>
						<td>Work Flow Task <b>Cancel</b></td>
						<td>ticket-minus.png</td>
						<td>cancelWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="resolveWorkflowIcon"></span></td>
						<td>Work Flow Task <b>Resolve</b></td>
						<td>ticket-check.png</td>
						<td>resolveWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="assignWorkflowIcon"></span></td>
						<td>Work Flow Task <b>Assign</b></td>
						<td>ticket-arrow.png</td>
						<td>assignWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="deleteWorkflowIcon"></span></td>
						<td>Work Flow Task <b>Delete</b></td>
						<td>ticket-exclamation.png</td>
						<td>deleteWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="reopenWorkflowIcon"></span></td>
						<td>Work Flow Task <b>Reopen</b></td>
						<td>ticket-plus-circle.png</td>
						<td>reopenWorkflowIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="infoIcon"></span></td>
						<td>More Information</td>
						<td>question-balloon.png</td>
						<td>infoIcon</td>
					</tr>
					<tr>
						<td align="center"><span class=""></span></td>
						<td>Website Browser</td>
						<td>application-browser.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><span class="gearIcon"></span></td>
						<td>Widget</td>
						<td>gear.png</td>
						<td>gearIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="gearPlusIcon"></span></td>
						<td>Widget Add</td>
						<td>gear-plus.png</td>
						<td>gearPlusIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="gearMinusIcon"></span></td>
						<td>Widget Remove</td>
						<td>gear-minus.png</td>
						<td>gearMinusIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="gearPencilIcon"></span></td>
						<td>Widget Edit</td>
						<td>gear-pencil.png</td>
						<td>gearPencilIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="resolveIcon"></span></td>
						<td>Resolve</td>
						<td>tick.png</td>
						<td>resolveIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="queryIcon"></span></td>
						<td>Query</td>
						<td>sort-quantity-descending.png</td>
						<td>queryIcon</td>
					</tr>					
				</table>
			</div>
		</div>
		
		<hr />
		
		<h2>Form Icon</h2>
		<div class="yui-g">
			<div class="yui-u first">
				<table class="listingTable">
					<tr>
						<th scope="col" width="30px" align="center">Icon</th>
						<th scope="col">Description</th>
						<th scope="col" width="125px">File Name</th>
						<th scope="col" width="125px">Class Name</th>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-check-box.png"></td>
						<td>Checkbox</td>
						<td>ui-check-box.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><span class="calDayIcon"></span></td>
						<td>Date</td>
						<td>calendar-day.png</td>
						<td>calDayIcon</td>
					</tr>
					<tr>
						<td align="center"><span class="calClockIcon"></span></td>
						<td>Date & Time</td>
						<td>calendar-clock.png</td>
						<td>calClockIcon</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-radio-button.png"></td>
						<td>Radio</td>
						<td>ui-radio-button.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-combo-box.png"></td>
						<td>Select</td>
						<td>ui-combo-box.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-list-box.png"></td>
						<td>Multi Select</td>
						<td>ui-list-box.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-text-field.png"></td>
						<td>Text</td>
						<td>ui-text-field.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-scroll-pane.png"></td>
						<td>Text Area</td>
						<td>ui-scroll-pane.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-scroll-pane-blog.png"></td>
						<td>WYSIWYG</td>
						<td>ui-scroll-pane-blog.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/document-text.png"></td>
						<td>File</td>
						<td>document-text.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/image.png"></td>
						<td>Image</td>
						<td>image.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/tag.png"></td>
						<td>Tag</td>
						<td>tag.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/node-select-all.png"></td>
						<td>Catagory</td>
						<td>node-select-all.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/document-number.png"></td>
						<td>Binary</td>
						<td>document-number.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/property.png"></td>
						<td>Custom Field</td>
						<td>property.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/folder-open-globe.png"></td>
						<td>Host Folder</td>
						<td>folder-open-globe.png</td>
						<td>NA</td>
					</tr>
				</table>
			</div>
			<div class="yui-u">
				<table class="listingTable">
					<tr>
						<th scope="col" width="30px" align="center">Icon</th>
						<th scope="col">Description</th>
						<th scope="col" width="125px">File Name</th>
						<th scope="col" width="125px">Class Name</th>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-splitter-horizontal.png"></td>
						<td>Line Divider</td>
						<td>ui-splitter-horizontal.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-splitter.png"></td>
						<td>Column Divider</td>
						<td>ui-splitter.png</td>
						<td>NA</td>
					</tr>
					<tr>
						<td align="center"><img src="/html/images/icons/ui-tab.png"></td>
						<td>Tab Divider</td>
						<td>ui-tab.png</td>
						<td>NA</td>
					</tr>
				</table>
			</div>
		</div>
</div>
<!-- END ICON TABS -->

<!-- START TABLES TABS -->
<div id="TabThree" dojoType="dijit.layout.ContentPane" title="Tables">
	<h2>Listing Table Style</h2>
	<p>All of our tables need to look like the following, with search in the header on the left and "Add XXX" on the right.  Please notice the iconClass tags used to set the proper icon value</p>
	<hr />
	<div style="margin:0 50px;">
		<div class="yui-gc portlet-toolbar">
			<div class="yui-u first">
				<input type="text" name="filter" id="filter" class="large" value="" />						
				<button dojoType="dijit.form.Button"  iconClass="searchIcon">Search</button>
				<button dojoType="dijit.form.Button" iconClass="resetIcon">Reset</button>
			</div>
			<div class="yui-u" style="text-align:right;">
				<button dojoType="dijit.form.Button"  iconClass="plusIcon">New Thing</button>
			</div>
		</div>
			
		<table class="listingTable">
			<tr>
			    <th width="45" align="center">Action</th>
				<th>Name</th>
				<th width="75">Key</th>
				<th width="125">Last Editor</th>
				<th width="100" align="center">Last Edit Date</th>
			</tr>
			<tr class="alternate_1" id="row-0">
				<td align="center">
					<input type="hidden" name="hvar_key-0" id="hvar_key-0" value="myKey" >
					<input type="hidden" name="hvar_n-0" id="hvar_n-0" value="Our first Key" >
					<input type="hidden" name="hvar_value-0" id="hvar_value-0" value="testing My Key" >
					<input type="hidden" name="hvar_deleted-0" id="hvar_deleted-0" value="false" >
					<a href="#"><span class="editIcon"></span></a>
					<a><span class="deleteIcon"></span></a>
				</td>
				<td>Mouse Over Icons to See Tool Tip.</td>
				<td>tipExample</td>
				<td>Test Test</td>
				<td align=center>04-09-2009</td>
			</tr>
			<tr class="alternate_2" id="row-1">
				<td align="center">
					<input type="hidden" name="hvar_key-1" id="hvar_key-1" value="secondKey" >
					<input type="hidden" name="hvar_n-1" id="hvar_n-1" value="Our Second Key" >
					<input type="hidden" name="hvar_value-1" id="hvar_value-1" value="Here is our second Key" >
					<input type="hidden" name="hvar_deleted-1" id="hvar_deleted-1" value="false" >
					<a href="#"><span class="editIcon"></span></a>
					<a><span class="deleteIcon"></span></a>
				</td>
				<td>Our Second Key</td>
				<td>secondKey</td>
				<td>Test Test</td>
				<td align=center>04-09-2009</td>
			</tr>
			<tr class="alternate_1" id="row-2">
				<td align="center">
					<input type="hidden" name="hvar_key-2" id="hvar_key-2" value="Hospitals" >
					<input type="hidden" name="hvar_n-2" id="hvar_n-2" value="Hosts" >
					<input type="hidden" name="hvar_value-2" id="hvar_value-2" value="213123," >
					<input type="hidden" name="hvar_deleted-2" id="hvar_deleted-2" value="false" >
					<a href="#"><span class="editIcon"></span></a>
					<a><span class="deleteIcon"></span></a>
				</td>
				<td>Hosts</td>
				<td>Hospitals</td>
				<td>Test Test</td>
				<td align=center>04-09-2009</td>
			</tr>
			<tr>
				<td colspan="5">
					<div class="noResultsMessage">No Results Message</div>
				</td>
			</tr>
		</table>
		
		<div class="yui-gb buttonRow">
			<div class="yui-u first" style="text-align:left;">
				<button dojoType="dijit.form.Button" iconClass="previousIcon">Previous</button>
			</div>
			<div class="yui-u" style="text-align:center;">Viewing Results 1 of 10</div>
			<div class="yui-u" style="text-align:right;">
				<button dojoType="dijit.form.Button" iconClass="nextIcon">Next</button>
			</div>
		</div>
		
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" onClick="alert('test');" iconClass="saveIcon">Save Thing</button>
			<button dojoType="dijit.form.Button"onClick="alert('test');" iconClass="cancelIcon">Cancel</button>
		</div>
	</div>
	
	<!-- START Right Click Menu Code for row 1 -->
		<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr1" contextMenuForWindow="false" style="display: none;" targetNodeIds="row-0">
			<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="alert('TODO')">Edit</div>
			<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="alert('TODO')">Publish</div>
			<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="alert('TODO')">Archive</div>
			<div dojoType="dijit.MenuItem" iconClass="copyIcon" onClick="alert('TODO')">Copy</div>
			<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="alert('TODO')">Unpublish</div>
			<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="alert('TODO')">Unlock</div>
			<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="alert('TODO')">Delete</div>
		</div>
	<!-- END Right Click Menu Code for row 1 -->
	
	<hr />
	<h2>Example Code</h2>
	<p><pre><code>
	&lt;div class="yui-g portlet-toolbar">
		&lt;div class="yui-u first">
			&lt;input type="text" name="filter" id="filter" class="large" value="" />						
			&lt;button dojoType="dijit.form.Button"  iconClass="searchIcon">Search&lt;/button>
			&lt;button dojoType="dijit.form.Button" iconClass="resetIcon">Reset&lt;/button>
		&lt;/div>
		&lt;div class="yui-u" style="text-align:right;">
			&lt;button dojoType="dijit.form.Button"  iconClass="plusIcon">Add new&lt;/button>
		&lt;/div>
	&lt;/div>
	&lt;table class="listingTable">
		&lt;tr>
		    &lt;th>Column Title 1&lt;/th>
			&lt;th>Column Title 2&lt;/th>
		&lt;/tr>
		&lt;tr class="alternate_1" id="row-0">
			&lt;td>Row 1 content&lt;/td>
			&lt;td>Row 1 content&lt;/td>
		&lt;/tr>
		&lt;tr class="alternate_2" id="row-1">
			&lt;td>Row 2 content&lt;/td>
			&lt;td>Row 2 content&lt;/td>
		&lt;/tr>
		&lt;tr class="alternate_1" id="row-0">
			&lt;td colspan="5">
				&lt;div class="noResultsMessage">No Results Message&lt;/div>
			&lt;/td>
		&lt;/tr>
	&lt;/table>
	&lt;div class="yui-gb buttonRow">
		&lt;div class="yui-u first" style="text-align:left;">
			&lt;button dojoType="dijit.form.Button" iconClass="previousIcon">Previous&lt;/button>
		&lt;/div>
		&lt;div class="yui-u" style="text-align:center;">Viewing Results 1 of 10&lt;/div>
		&lt;div class="yui-u" style="text-align:right;">
			&lt;button dojoType="dijit.form.Button" iconClass="nextIcon">Next&lt;/button>
		&lt;/div>
	&lt;/div>
	&lt;div class="buttonRow">
		&lt;button dojoType="dijit.form.Button" onClick="alert('test');" iconClass="saveIcon">save&lt;/button>
		&lt;button dojoType="dijit.form.Button"onClick="alert('test');" iconClass="cancelIcon">cancel&lt;/button>
	&lt;/div>
	
	&lt;!-- START Right Click Menu Code for row 1 -->
		&lt;div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr1" contextMenuForWindow="false" style="display: none;" targetNodeIds="row-0">
			&lt;div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="alert('TODO')">Edit&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="alert('TODO')">Publish&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="alert('TODO')">Archive&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="copyIcon" onClick="alert('TODO')">Copy&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="alert('TODO')">Unpublish&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="alert('TODO')">Unlock&lt;/div>
			&lt;div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="alert('TODO')">Delete&lt;/div>
		&lt;/div>
	&lt;!-- END Right Click Menu Code for row 1 -->

	</code></pre></p>
</div>
<!-- END TABLES TABS -->

<!-- START FORMS TABS -->
<div id="TabFour" dojoType="dijit.layout.ContentPane" title="Forms">
	<h2>Forms</h2>
	<p>All of our forms need to look like the following. Please notice the iconClass tags used to set the proper icon value</p>
	<hr />
	<div style="margin:0 50px;">
	
		<form>
			<dl>
				<dt><span class="required"></span> First Name:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px;" /></dd>
				
				<dt>Last Name:</dt>
				<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px;" /></dd>
				<dd class="inputCaption">Add a second &lt;dd&gt; with class "inputCaption" for captions</dd>
				
				<dt>Home State:</dt>
				<dd>
					<select name="state1" dojoType="dijit.form.ComboBox" autocomplete="false" value="Florida" onChange="setVal1">
		                <option selected="selected">California</option>
		                <option >Illinois</option>
		                <option >New York</option>
						<option >South Carolina</option>
		                <option >Texas</option>
	        		</select>
				</dd>
				
				<dt>Description:</dt>
				<dd>
					<textarea dojoType="dijit.form.Textarea" name="description" style="width:300px;min-height:150px;"></textarea>
					<div class="callOutBox2 hintBox" style="top:200px;">This is a hint box. The DIV is place inside the DD with the class callOutBox2 and hintBox applied. SEE code example below.</div>
				</dd>
				<dd class="inputCaption">
					<button dojoType="dijit.form.Button" iconClass="spellIcon">
				    	Spell Check
	            	</button>
				</dd>
				
				<dt>Interest:</dt>
				<dd>
					<input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb1" value="news">
					<label for="g1rb1">News</label>
					<input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb2" value="talk"  checked="checked"/>
					<label for="g1rb2">Sports</label>
					<input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb3" value="weather" disabled="disabled"/>
					<label for="g1rb3">Weather</label>
				</dd>

				<dt>Skill Level:</dt>
				<dd>
					<input type="checkbox" dojoType="dijit.form.CheckBox" name="cb1" id="cb1" value="foo" onClick="console.log('clicked cb1')">
					<label for="cb1">Amateur</label> 
					
					<input type="checkbox" name="cb2" id="cb2" dojoType="dijit.form.CheckBox" checked="checked"/>
					<label for="cb2">Professionally</label>
				</dd>
				
				<dt>Favorite color:</dt>
				<dd>
					<div dojoType="dijit.form.DropDownButton" iconClass="noteIcon">
  						<span>Color Picker</span>
  						<div dojoType="dijit.ColorPalette" id="colorPalette" style="display: none" palette="3x4" onChange="console.log(this.value);"></div>
					</div>
				</dd>
				
				<dt>What do you think of the dotCMS:</dt>
				<dd>
					<div dojoType="dijit.form.HorizontalSlider" name="horizontal1" 
						onChange="dojo.byId('slider1input').value=dojo.number.format(arguments[0]/100,{places:1,pattern:'#%'});"
						value="75"
						maximum="100"
						minimum="0"
						pageIncrement="100"
						showButtons="false"
						intermediateChanges="false"
						style="width:340px; height: 20px;"
						id="slider1">
					 		
					  		<div dojoType="dijit.form.HorizontalRule" container="topDecoration" count=6 style="height:5px;"></div>
					  		<div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=5 style="height:5px;"></div>
					  		<ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
							    <li>Poor</li>
							    <li>Good</li>
							    <li>Great</li>
							</ol>
					</div>
				</dd>
			</dl>
			
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" onClick="alert('test');" iconClass="saveIcon">
					save
				</button>
				<button dojoType="dijit.form.Button"onClick="alert('test');" iconClass="cancelIcon">
					cancel
				</button>
			</div>
		</form>
	</div>
	
	<hr/>
	<h2>Example Code</h2>
<p><pre><code>
	&lt;form&gt;
	&lt;dl&gt;
		&lt;dt&gt;&lt;span class=&quot;required&quot;&gt;&lt;/span&gt; First Name:&lt;/dt&gt;
		&lt;dd&gt;&lt;input type="text" dojoType="dijit.form.TextBox" style="width:350px;" /&gt;&lt;/dd&gt;
		
		&lt;dt&gt;Last Name:&lt;/dt&gt;
		&lt;dd&gt;&lt;input type="text" dojoType="dijit.form.TextBox" style="width:350px;" /&gt;&lt;/dd&gt;
		&lt;dd class="inputCaption"&gt;Add a second &lt;dd&gt; with class "inputCaption" for captions&lt;/dd&gt;

		&lt;dt>Home State:&lt;/dt>
		&lt;dd>
			&lt;select name="state1" dojoType="dijit.form.ComboBox" autocomplete="false" value="Florida" onChange="setVal1">
				&lt;option selected="selected">Florida&lt;/option>
				&lt;option>Illinois&lt;/option>
				&lt;option>New York&lt;/option>
				&lt;option>South Carolina&lt;/option>
				&lt;option>Texas&lt;/option>
			&lt;/select>
		&lt;/dd>

		&lt;dt&gt;Description:&lt;/dt&gt;
		&lt;dd&gt;
			&lt;textarea dojoType="dijit.form.Textarea" name="description" style="width:350px;min-height:150px;"&gt;&lt;/textarea&gt;
			&lt;div class="callOutBox2 hintBox" style="top:200px;"&gt;
				This is a hint box. The DIV is place inside the DD with the class callOutBox2 and hintBox applied. SEE code example below.
			&lt;/div&gt;
		&lt;/dd&gt;
		&lt;dd class="inputCaption"&gt;
			&lt;button dojoType="dijit.form.Button" iconClass="spellIcon"&gt;
		    	Spell Check
           	&lt;/button&gt;
		&lt;/dd&gt;
		
		&lt;dt&gt;Interest:&lt;/dt&gt;
		&lt;dd&gt;
			&lt;input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb1" value="news"&gt;
			&lt;label for="g1rb1"&gt;News&lt;/label&gt;
			&lt;input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb2" value="talk"  checked="checked"/&gt;
			&lt;label for="g1rb2"&gt;Sports&lt;/label&gt;
			&lt;input dojoType="dijit.form.RadioButton" type="radio" name="g1" id="g1rb3" value="weather" disabled="disabled"/&gt;
			&lt;label for="g1rb3"&gt;Weather&lt;/label&gt;
		&lt;/dd&gt;

		&lt;dt&gt;Skill Level:&lt;/dt&gt;
		&lt;dd&gt;
			&lt;input type="checkbox" dojoType="dijit.form.CheckBox" name="cb1" id="cb1" value="foo" onClick="console.log('clicked cb1')"&gt;
			&lt;label for="cb1"&gt;Amateur&lt;/label&gt; 
			
			&lt;input type="checkbox" name="cb2" id="cb2" dojoType="dijit.form.CheckBox" checked="checked"/&gt;
			&lt;label for="cb2"&gt;Professionally&lt;/label&gt;
		&lt;/dd&gt;
		
		&lt;dt&gt;Favorite color:&lt;/dt&gt;
		&lt;dd&gt;
			&lt;div dojoType="dijit.form.DropDownButton" iconClass="noteIcon"&gt;
				&lt;span&gt;Color Picker&lt;/span&gt;
				&lt;div dojoType="dijit.ColorPalette" id="colorPalette" style="display: none" palette="3x4" onChange="console.log(this.value);"&gt;&lt;/div&gt;
			&lt;/div&gt;
		&lt;/dd&gt;
		
		&lt;dt&gt;What do you think of the dotCMS:&lt;/dt&gt;
		&lt;dd&gt;
			&lt;div dojoType="dijit.form.HorizontalSlider" name="horizontal1" 
				onChange="dojo.byId('slider1input').value=dojo.number.format(arguments[0]/100,{places:1,pattern:'#%'});"
				value="75"
				maximum="100"
				minimum="0"
				pageIncrement="100"
				showButtons="false"
				intermediateChanges="false"
				style="width:340px; height: 20px;"
				id="slider1"&gt;
			 		
		  		&lt;div dojoType="dijit.form.HorizontalRule" container="topDecoration" count=6 style="height:5px;"&gt;&lt;/div&gt;
		  		&lt;div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=5 style="height:5px;"&gt;&lt;/div&gt;
		  		&lt;ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;"&gt;
				    &lt;li&gt;Poor&lt;/li&gt;
				    &lt;li&gt;Good&lt;/li&gt;
				    &lt;li&gt;Great&lt;/li&gt;
				&lt;/ol&gt;
			&lt;/div&gt;
		&lt;/dd&gt;
	&lt;/dl&gt;
	
	&lt;div class="buttonRow"&gt;
		&lt;button dojoType="dijit.form.Button" onClick="alert('test');" iconClass="saveIcon"&gt;
			save
		&lt;/button&gt;
		&lt;button dojoType="dijit.form.Button"onClick="alert('test');" iconClass="cancelIcon"&gt;
			cancel
		&lt;/button&gt;
	&lt;/div&gt;
	&lt;/form>
</code></pre></p>

</div>
<!-- END FORMS TABS -->

<!-- START SPLIT SCREEN TABS -->



<div id="TabFive" dojoType="dijit.layout.ContentPane" title="Split Screen">

<h2>Split Screen</h2>
<p>Demonstration of how to split a screen for a side navigation, folder tree, or search interface. <br/><b style="color:red;">ALETR:</b> Height of Dojo BorderContainer and internal wrappers must be set with JavaScript based of window size. See file /dotCMS/html/portlet/ext/useradmin/view_user.jsp for a working example.</p>
<p>&nbsp;</p>


<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:420px;" id="borderContainer" class="shadowBox headerBox">				
	
	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" id="searchBox" class="lineRight" style="width: 350px;">
		<div style="margin:5px 0 0 10px;">
			Filter:
			<input dojoType="dijit.form.TextBox" onkeyup="filterUsers()" trim="true" style="width:175px;" name="usersFilter" id="usersFilter">
			<button dojoType="dijit.form.Button" onclick="clearUserFilter()" type="button" iconClass="resetIcon">Clear</button>
		</div>
		<div class="sideMenuWrapper">
			<div style="padding:10px;">
				<h2>Side Menu Here.</h2>
				<p>Height of wrapper DIV needs to be set with JavaScript based on screen height. See file /dotCMS/html/portlet/ext/useradmin/view_user.jsp for a working example.</p>
			</div>
		</div>
	</div>
	<!-- END Left Column -->
	
	<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		
		<!-- START Tabs -->
		<div dojoType="dijit.layout.TabContainer" id="userTabsContainer">
		
			<!-- START Tab 1 -->
			<div dojoType="dijit.layout.ContentPane" id="exampleTabOne" title="Tab One">
				<div id="contentWrapper" style="overflow:auto;border:1px solid #ccc;height:305px;">
					<h2>Content Here.</h2>
					<p>Height of wrapper DIV needs to be set with JavaScript based on screen height. See file /dotCMS/html/portlet/ext/useradmin/view_user.jsp for a working example.</p>
					<p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p>
				</div>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" onclick="" type="button" iconClass="cancelIcon">Cancel</button>
					<button dojoType="dijit.form.Button" onclick="" type="button" iconClass="saveIcon">Save</button>
	    		</div>
			</div>
			<!-- END Tab 1 -->
			
			<!-- START Tab 2 -->
			<div dojoType="dijit.layout.ContentPane" id="exampleTabTwo" title="Tab Two">
				Tab 2 Content
			</div>
			<!-- END Tab 2 -->
			
			<!-- START Tab 3 -->
			<div dojoType="dijit.layout.ContentPane" id="exampleTabThree" title="Tab Three">
				Tab 3 Content
			</div>
			<!-- END Tab 3 -->
			
		</div>
		<!-- END Tabs -->
		
	</div>
	<!-- END Right Column -->
	
</div>
<p>&nbsp;</p>
	<p><pre><code>
&lt;script language="Javascript">
//Layout Initialization
	function  resizeBrowser(){
		var viewport = dijit.getViewport();
		var viewport_height = viewport.h;

		var  e =  dojo.byId("borderContainer");
		dojo.style(e, "height", viewport_height -200+ "px");

		var  e =  dojo.byId("sideMenuWrapper");
		dojo.style(e, "height", viewport_height -257+ "px");

		var  e =  dojo.byId("contentWrapper");
		dojo.style(e, "height", viewport_height -257+ "px");
	}
// need the timeout for back buttons

	dojo.addOnLoad(resizeBrowser);
	dojo.connect(window, "onresize", this, "resizeBrowser");
&lt;/script>
		
&lt;div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:400px;" id="borderContainer" class="shadowBox headerBox">				
	
	&lt;!-- START Left Column -->
	&lt;div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" class="lineRight">
		&lt;div class="buttonBoxLeft">
			Filter:
			&lt;input dojoType="dijit.form.TextBox" onkeyup="filterUsers()" trim="true" name="usersFilter" id="usersFilter">
			&lt;button dojoType="dijit.form.Button" onclick="clearUserFilter()" type="button" iconClass="resetIcon">Clear</button>
		&lt;/div>
		&lt;div class="sideMenuWrapper" style="overflow:auto;">
				&lt;h2>Side Menu Here.&lt;/h2>
		&lt;/div>
	&lt;/div>
	&lt;!-- END Left Column -->
	
	&lt;!-- START Right Column -->
	&lt;div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		
		&lt;!-- START Tabs -->
		&lt;div dojoType="dijit.layout.TabContainer" id="userTabsContainer">
		
			&lt;!-- START Tab 1 -->
			&lt;div dojoType="dijit.layout.ContentPane" id="exampleTabOne" title="Tab One">
				&lt;div id="contentWrapper" style="overflow:auto;border:1px solid #ccc;">
					&lt;h2>Content Here.&lt;/h2>
				&lt;/div>
				&lt;div class="buttonRow">
					&lt;button dojoType="dijit.form.Button" onclick="" type="button" iconClass="cancelIcon">Cancel&lt;/button>
					&lt;button dojoType="dijit.form.Button" onclick="" type="button" iconClass="saveIcon">Save&lt;/button>
	    		&lt;/div>
			&lt;/div>
			&lt;!-- END Tab 1 -->
			
			&lt;!-- START Tab 2 -->
			&lt;div dojoType="dijit.layout.ContentPane" id="exampleTabTwo" title="Tab Two">
				Tab 2 Content
			&lt;/div>
			&lt;!-- END Tab 2 -->
			
			&lt;!-- START Tab 3 -->
			&lt;div dojoType="dijit.layout.ContentPane" id="exampleTabThree" title="Tab Three">
				Tab 3 Content
			&lt;/div>
			&lt;!-- END Tab 3 -->
			
		&lt;/div>
		&lt;!-- END Tabs -->
		
	&lt;/div>
	&lt;!-- END Right Column -->
	
	&lt;/div>
	</code></pre></p>

</div>
<!-- END SPLIT SCREEN TABS -->

<!-- START Dojo Help TABS -->
<div id="TabSix" dojoType="dijit.layout.ContentPane" title="Dojo">

<h2>Dojo Quick Reference</h2>
	<p>
		Below are few example of commen Dojo functions used in the dotCMS. For other Dojo resources and examples checkout these other sites: 
		<a href="http://demos.dojotoolkit.org/demos/" target="_blank">Dojo Demos</a> | 
		<a href="http://archive.dojotoolkit.org/nightly/dojotoolkit/dijit/tests/form/test_Button.html" target="_blank">Dijit Button Test</a> | 
		<a href="http://docs.dojocampus.org/" target="_blank">Dojo Campus</a>
	</p>
<hr />

<h2>Tabs</h2>
	<p>Cutting and pasting from the code below will recreate the tabbed interface found above.</p>
				
	<p><pre><code>
	&lt;div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer"&gt;
		&lt;div id="TabOne" dojoType="dijit.layout.ContentPane" title="Tab One"&gt;
			Tab Content
		&lt;/div&gt;
		&lt;div id="TabTwo" dojoType="dijit.layout.ContentPane" title="Tab Two"&gt;
			Tab Content
		&lt;/div&gt;
		&lt;div id="TabThree" dojoType="dijit.layout.ContentPane" title="Tab Three"&gt;
			Tab Content
		&lt;/div&gt;
	&lt;/div&gt;
	</code></pre></p>
<hr />
	
<h2>Tool Tip</h2>
	<button id="tip1" dojoType="dijit.form.Button" iconClass="infoIcon">Tool Tip</button>
	<span dojoType="dijit.Tooltip" connectId="tip1" id="one_tooltip">This is a tool tip</span>
	
	<p><pre><code>
	&lt;a href="#" id="tip1">Tool Tip&lt;/a>
	&lt;span dojoType="dijit.Tooltip" connectId="tip1" id="one_tooltip">This is a Dojo Tool Tip!&lt;/span>
	</code></pre></p>
<hr/>

<h2>Dialog Box</h2>
	<button dojoType="dijit.form.Button" onClick="dijit.byId('sample').show();" iconClass="plusIcon">Open Dialog Box</button>
	<div id="sample" dojoType="dijit.Dialog" style="display: none" onfocus='blur()'>
		<h2>This is a Dialog Box</h2>
		<p>You can place your content here.</p>
	</div>
	
	<p><pre><code>
	&lt;button dojoType="dijit.form.Button" onClick="dijit.byId('sample').show();" iconClass="plusIcon">Open Dialog Box&lt;/button>
	
	&lt;div id="sample" dojoType="dijit.Dialog" style="display: none" onfocus='blur()'>
		&lt;h2>This is a Dialog Box&lt;/h2>
		&lt;p>You can place your content here.&lt;/p>
	&lt;/div>
	</code></pre></p>
<hr/>

<h2>Dropdown Dialog Box</h2>
	<div dojoType="dijit.form.DropDownButton">
		<span>Login Form</span>
		<div dojoType="dijit.TooltipDialog" id="dialog1" title="Login Form" execute="checkPw();">
			<form>
				<dl>
					<dt>Username</dt>
					<dd><input dojoType="dijit.form.TextBox" type="text"></dd>
					
					<dt>Password:</dt>
					<dd><input dojoType="dijit.form.TextBox" type="password"></dd>
				</dl>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" type="submit">Login</button></td>
				</div>
			</form>
		</div>
	</div>

	<p><pre><code>
	&lt;div dojoType="dijit.form.DropDownButton">
		&lt;span>Login Form&lt;/span>
		&lt;div dojoType="dijit.TooltipDialog" id="dialog1" title="Login Form" execute="checkPw();">
			&lt;form>
				&lt;dl>
					&lt;dt>Username</dt>
					&lt;dd>&lt;input dojoType="dijit.form.TextBox" type="text">&lt;/dd>
					
					&lt;dt>Password:</dt>
					&lt;dd>&lt;input dojoType="dijit.form.TextBox" type="password">&lt;/dd>
				&lt;/dl>
				&lt;div class="buttonRow">
					&lt;button dojoType="dijit.form.Button" type="submit">Login&lt;/button>&lt;/td>
				&lt;/div>
			&lt;/form>
		&lt;/div>
	&lt;/div>
	</code></pre></p>
	
	<hr/>

	<h2>Progress Bar</h2>
	
	<div dojoType="dijit.ProgressBar" style="width:100px;" jsId="progressBar" id="progressBar"></div>
	
	<script type="text/javascript">
		dojo.addOnLoad(function () {
			progressBar.update({ progress: 50 });
		});
	</script>
	
	<p><pre><code>	
		&lt;div dojoType=&quot;dijit.ProgressBar&quot; style=&quot;width:100px;&quot; jsId=&quot;progressBar&quot; id=&quot;progressBar&quot;&gt;&lt;/div&gt;
			
			&lt;script type=&quot;text/javascript&quot;&gt;
				progressBar.update({ progress: 50 });
			&lt;/script&gt;					
	</code></pre></p>
</div>
<!-- END Dojo Help TABS -->

</div>
<!-- END TABS -->


</div>
<!-- END Body -->


<div id="ft">
	<div style="float:right;margin-top:10px;">
		<script language="JavaScript" type="text/javaScript">document.write((new Date()).getFullYear());</script> &copy; dotCMS Inc. All rights reserved.
	</div>
</div>



</body>
</html>
