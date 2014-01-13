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

	<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js" djConfig="parseOnLoad:true, isDebug:false"></script>
	<script type="text/javascript" src="<%=dojoPath%>/dojo/dot-dojo.js"></script>
	<script type="text/javascript">
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
		dojo.require("dojox.layout.ContentPane");
		dojo.require("dojo.window")
		dojo.require("dojo/request")
		dojo.require("dojo/request/xhr")
	</script>

	<script>

		/**
			ActionPanel JS Object
		**/
		dojo.declare("com.dotcms.ui.ActionPanel", null, {
			lastRow:undefined,
			jspToShow:undefined,
			ff : 0,
			constructor: function(/* Object */args){

				this.alwaysShow = args.alwaysShow;
				this.jspToShow = args.jspToShow;
				
				if(this.alwaysShow){
					dojo.ready(function(){
						if( dojo.byId("row-0") != undefined){
							actionPanelTable.toggle(0);
						}
					})
				}
				
				if(dojo.isMozilla) {
					this.ff=-1;
				}
				
				/**
					Handle scolling and resize
				**/
				dojo.connect(window, 'onscroll', this, function(event) {
					this.placeActionPanel();
	
				});
				dojo.connect(window, 'onresize', this, function(event) {
					this.initPanel();
				});
				
				
				
			},
			
			
			/** Setting this true will show the first row automatically **/
			alwaysShow:false,

			/**
				hide/show action Panel
			**/
			toggle:function(row){
				if(this.jspToShow == undefined){
					return;
				}
	
				dojo.addClass("actionPanel", "hideMe");
				dojo.destroy("display-arrow");
					
				// deactivate last clicked row,
				if(this.lastRow != undefined){
					dojo.removeClass('row-' + this.lastRow, "active");
					if( ! this.alwaysShow){
						if(this.lastRow == row){
							this.lastRow = null;
							return;
						}
					}
				}
				
				dojo.addClass('row-' + row, "active");
				this.lastRow=row;
				
				// Build the action Panel
				var panelDiv = dojo.byId("actionPanelContainer");
				var hanger = dojo.byId("actionPanelContent");
				if(!hanger){
					return;
				}
				if (panelDiv) {
					dojo.destroy("panelDiv")
				}
				

				panelDiv = dojo.create("div", {
					id : "actionPanelContainer"
					},"actionPanelContent");

				
		        // Execute a HTTP GET request
		        dojo.xhr.get({
		        	preventCache:true,
		            url: this.jspToShow,
		            load: function(result) {
		            	dojo.byId("actionPanelContainer").innerHTML = result;
		            }
		        });
				

				

				actionPanelTable.initPanel();
				


				
				dojo.removeClass("actionPanel", "hideMe");	

				//dojo.parser.parse("actionPanel");
				dojo.style("actionPanel", "width", dojo.position("actionPanelTableHeader",true).w -1 +this.ff + "px");

			},

			/**
				Move actionpanel/arrow around - does not hide/show
			**/
			placeActionPanel:function(){
				
				if(this.lastRow == undefined){
					return;
				}

				var selectedRow = dojo.position('row-' + this.lastRow, true);
				var tableHeader = dojo.position("actionPanelTableHeader",true);
				var actionPanel = dojo.position("actionPanel", true);
				var scroll = window.pageYOffset ? window.pageYOffset : document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop;
				var bottomOfTheHeader = tableHeader.h + this.ff + tableHeader.y -scroll;

				/*
				console.log("--------------------------");
				console.log("tableHeader x:" + tableHeader.x);
				console.log("tableHeader y:" + tableHeader.y);
				console.log("tableHeader h:" + tableHeader.h);
				console.log("tableHeader w:" + tableHeader.w);
				console.log("selectedRow:" + this.lastRow);
				console.log("bottomOfTheHeader x:" + bottomOfTheHeader);
				console.log("selectedRow y:" + selectedRow.y);
				console.log("selectedRow h:" + selectedRow.h);
				console.log("actionPanel y:" + actionPanel.y);
				console.log("actionPanel x:" + actionPanel.x);
				console.log("Scroll:" +scroll);
				*/

				
				if(bottomOfTheHeader < 0 ){
					dojo.style("actionPanel", "position","fixed");
					dojo.style("actionPanel", "top", "0px");
					dojo.style("actionPanel", "left", tableHeader.x + "px");
					var arrowY = 	selectedRow.y - scroll +13  ;
					dojo.style("arrow", "top", arrowY + "px");

					return;
				}
				else{
					dojo.style("actionPanel", "top", bottomOfTheHeader + "px");
					dojo.style("actionPanel", "left", tableHeader.x + "px");
					var arrowY = 	selectedRow.y -  (Math.abs( bottomOfTheHeader) )  - scroll + 13;
					dojo.style("arrow", "top", arrowY + "px");
				}
			},
			
			
			
			initPanel : function(){
				var tableHeader = dojo.position("actionPanelTableHeader",true);
				var scroll = window.pageYOffset ? window.pageYOffset : document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop;
				var bottomOfTheHeader = tableHeader.h + tableHeader.y -scroll;
				
				dojo.style("actionPanel", "width", tableHeader.w -1 + "px");
				
				require(["dojo/window"], function(win){
					   var vs = win.getBox();
						dojo.style("actionPanelContent", "height", vs.h -bottomOfTheHeader  + "px");
					});
				
				actionPanelTable.placeActionPanel();
				

			}
		});
		
		
		
		/**
			Set the jsp to load up when the action panel is activated
		**/
		var actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/style_guide/action-panel-ui-example-panel-content.jsp", alwaysShow:true});


	</script>






</head>

<body class="dmundra">


<div id="doc3">

	
	<div class="actionPannelPage">
		
		<table class="listingTable actionTable">
			<tr>
				<!--  Add these up to 100% -->
			    <th width="7%">&nbsp;</th>
			    <th width="7%">&nbsp;</th>
				<th width="35%">Name</th>
				<th width="25%">IP Address</th>
				<th width="20%">Contacted</th>
				<th width="6%" style="text-align:center;">Status</th>
				<!-- /Add these up to 100% -->
				
				
				<%-- the width of the inner div is the only thing you have to set to change the width --%>
				<th id="actionPanelTableHeader"><div style="width:400px;"></div></th>
				
				
				
			</tr>
			<%for(int i=0;i<100;i++){ %>
				<tr id="row-<%=i%>" onclick="javascript:actionPanelTable.toggle(<%=i%>);">
					<td align="center"><img src="/html/images/skin/icon-server.png"></td>
					<td align="center" style="color:#8c9ca9;"><%if(i==2){ %><i class="fa fa-user fa-3x"><%} %></i></td>
					<td>My Dotcms Node <%=i+1 %></td>
					<td>192.168.1.<%=5+i %></td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td id="td-<%=i%>"></td>
				</tr>
			<%} %>
		</table>
		
		
		<div id="actionPanel" class="hideMe">
			<div id="arrow"><img src='/html/images/skin/arrow.png'></div>
			<div id="actionPanelContent" style="overflow:auto;">
			
			</div>
		</div>
			
	</div>

</div> 
</body>
</html>