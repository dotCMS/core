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
	<link href="//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css" rel="stylesheet">
	
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
	</script>

</head>

<body class="dmundra">


<div id="doc3">

	<div id="hd" style="background: #666;">
		<div class="yui-g">
			<div class="yui-u first"><img alt="dotCMS" src="//dotcms.com/application/themes/dotcms/img/logo.png" style="height:40px;"></div>
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
	


	
	<style>
		.listingTable {
			margin: 0;
			width: 100%;
		}
		.listingTable td, .listingTable th{
			border-style: none none solid none;
		}
		.listingTable td {
			padding: 10px;
		}
		.listingTable tr:hover{
			background-color: #eff3f8;
			cursor: pointer;
		}

		.arrow{
			position:absolute;
			width:23px;
			height:36px;
			right:277px;
			top:3px;
			z-index:9999;
		}
		.hideMe {
			display:none;
		}
		tr .arrow {
			display:none;
		}
		tr.active .arrow {
			display: block;
		}
		tr.active {
			background-color: #eff3f8;
		}
		#actionPanel{
			position:absolute;
			border:1px solid #D0D0D0;
			background:#fff;
			bottom:0px;

		}
		#actionPanelTableHeader{
			padding:0px;
			margin:0px;
		}
		.green{
			color:#8c9ca9;
		}
		.yellow{
			color: #f6d57e;
		}
		.red{
			color: #8c9ca9;
		}
	</style>
	
	<script>
		var actionPanelTable = {
			lastRow:undefined,
			
			
			
			toggle:function(row, jspToShow){
	
				dojo.addClass("actionPanel", "hideMe");
				dojo.destroy("display-arrow");
					
	
	
				// deactivate last clicked row
				if(this.lastRow != undefined){
					dojo.removeClass('row-' + this.lastRow, "active");
					if(this.lastRow == row){
						this.lastRow = null;
						return;
					}
				}
				
				dojo.addClass('row-' + row, "active");
	
				this.lastRow=row;
				
				
				var myCp = dijit.byId("actionPanelContainer");
				var hanger = dojo.byId("actionPanel");
				if(!hanger){
					return;
				}
				if (myCp) {
					myCp.attr("content","");
					myCp.destroyRecursive(true);
				}
				
				myCp = new dojox.layout.ContentPane({
					id : "actionPanelContainer"
					}).placeAt("actionPanel");
				
				
				var r = Math.floor(Math.random() * 1000000000);
	
				if(jspToShow.indexOf("?")<0){
					jspToShow = jspToShow +"?";
				}
	
				
				var selectedRow = dojo.position('row-' + row, true);
				var selectedRowY = (selectedRow.y + (selectedRow.h/2) - 19);
				this.placeActionPanel();
				dojo.removeClass("actionPanel", "hideMe");	
				myCp.attr("href", jspToShow + "&rand=" + r);
				//dojo.parser.parse("actionPanel");
				
				
				

				var actionPanel = dojo.position('actionPanel');

				var actionPanelRight = actionPanel.x-20;

				var style="top:"+ selectedRowY +"px;left:"+actionPanelRight+"px;position:absolute;z-index:9999";
				/*
				console.log("style:" + style);
				console.log("selectedRowY:" + selectedRowY);
				console.log("actionPanelRight:" + actionPanelRight);
				*/
				
				

				var n = dojo.create("div", { 
					innerHTML: "<img src='images/arrow.png' border='4'>",
					style:style,
					id:"display-arrow",
					},dojo.body());
				
				
			},
			
			
			placeActionPanel:function(){
				var tableHeader = dojo.position(dojo.byId("actionPanelTableHeader"), true);
				var actionPanel = dojo.position(dojo.byId("actionPanel"), true);
				var scroll = dojo.body().scrollTop;
				var bottomOfTheHeader =tableHeader.y+tableHeader.h;

				if(bottomOfTheHeader - scroll < 0 ){
					console.log("bottomOfTheHeader:" + bottomOfTheHeader);
					console.log("scroll:" + scroll);
					dojo.style("actionPanel", "top", "0px");
					dojo.style("actionPanel", "position","fixed");
					return;
				}
				else{
					dojo.style("actionPanel", "position","absolute");
				}
				var topOfThePanel = (tableHeader.y+tableHeader.h)<0 ? scroll: tableHeader.y+tableHeader.h;

				
					console.log("zadsada w:" + dojo.position("zadsada").w);
					console.log("tableHeader y:" + tableHeader.y);
					console.log("tableHeader w:" + tableHeader.w);
					console.log("actionPanel y:" + actionPanel.y);
					console.log("actionPanel x:" + actionPanel.x);
					console.log("topOfThePanel:" + topOfThePanel);
					console.log("Scroll:" +scroll);
				
				

				
				dojo.style("actionPanel", "top", topOfThePanel + "px");
				dojo.style("actionPanel", "width", tableHeader.w -1 + "px");
				dojo.style("actionPanel", "left", tableHeader.x  + "px");
			}

		};
		
		
		
		dojo.connect(window, 'onscroll', this, function(event) {
			actionPanelTable.placeActionPanel();

		});
		
		
		dojo.connect(window, 'onresize', this, function(event) {
			actionPanelTable.placeActionPanel();

		});
		

	</script>
	
	<div>
			<table class="listingTable">
				
				<tr>
				    <th width="7%">&nbsp;</th>
				    <th width="7%">&nbsp;</th>
					<th width="35%">Name</th>
					<th width="25%">IP Address</th>
					<th width="20%">Contacted</th>
					<th width="6%" style="text-align:center;">Status</th>
					<th id="actionPanelTableHeader"><div id="zadsada" style="width:290px;">&nbsp;</div></th>
				</tr>
				<%for(int i=0;i<1;i++){ %>
					<tr id="row-<%=i%>" onclick="javascript:actionPanelTable.toggle('<%=i%>','/html/style_guide/host-manager-action-pallete.jsp');">
						<td align="center"><img src="images/icon-server.png"></td>
						<td align="center" style="color:#8c9ca9;"><i class="fa fa-user fa-3x"></i></td>
						<td>My Dotcms Node <%=i+1 %></td>
						<td>192.168.1.<%=5+i %></td>
						<td>1 min ago</td>
						<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
						<td></td>
					</tr>
				<%} %>
			</table>
			
			
			<div id="actionPanel" class="hideMe" style="background:white">

			</div>
			
	</div>

</div> 
</body>
</html>