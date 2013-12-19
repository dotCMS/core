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
	</script>

</head>

<body class="dmundra">


<div id="doc3">

	<div id="hd" style="background: #666;">
		<div class="yui-g">
			<div class="yui-u first"><img alt="dotCMS" src="//dotcms.com/application/themes/dotcms/img/logo.png" style="height:30px;"></div>
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
		.arrowWrapper{
			position:relative;
			height:41px;
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
		#pannel{
			position:absolute;
			top: 35px;
			bottom: 0px;
			right:0px;
			width:288px;
			min-height:450px;
			border:1px solid #D0D0D0;
			background:#fff;
			padding-bottom:30px;
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
		function toggle1(row){
			dojo.toggleClass("panel", "hideMe");
			dojo.toggleClass('row-' + row, "active");
			if (row > 7){
				y = (row - 7) * 60;
			}else{
				y = 0;
			}
			dojo.style("insideWrapper", "height", y + "px");
		}
	</script>
	
	<div style="position:relative;">
			<table class="listingTable">
				<tr>
				    <th width="7%">&nbsp;</th>
				    <th width="7%">&nbsp;</th>
					<th width="35%">Name</th>
					<th width="25%">IP Address</th>
					<th width="20%">Contacted</th>
					<th width="6%" style="text-align:center;">Status</th>
					<th><div style="width:290px;">&nbsp;</div></th>
				</tr>
				<tr id="row-0" onclick="javascript:toggle1('0');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center" style="color:#8c9ca9;"><i class="fa fa-user fa-3x"></i></td>
					<td>My Dotcms Node 1</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-1" onclick="javascript:toggle1('1');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 2</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x red"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-2" onclick="javascript:toggle1('2');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 3</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x yellow"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-3" onclick="javascript:toggle1('3');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-4" onclick="javascript:toggle1('4');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-5" onclick="javascript:toggle1('5');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-6" onclick="javascript:toggle1('6');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-7" onclick="javascript:toggle1('7');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-8" onclick="javascript:toggle1('8');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-9" onclick="javascript:toggle1('9');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
				<tr id="row-10" onclick="javascript:toggle1('10');">
					<td align="center"><img src="images/icon-server.png"></td>
					<td align="center">&nbsp;</td>
					<td>My Dotcms Node 4</td>
					<td>255.255.255:7801</td>
					<td>1 min ago</td>
					<td align="center"><i class="fa fa-circle fa-2x green"></i></td>
					<td><div class="arrowWrapper"><div class="arrow"><img src="images/arrow.png"></div></div></td>
				</tr>
			</table>
			
			
			<div id="panel" class="hideMe">
				<div id="insideWrapper"></div>
				<div style="margin:15px;padding:0 0 10px 0;border-bottom:1px solid #D0D0D0">
					<b>Action Pallete</b>
				</div>
				
				<div style="margin:15px;padding:0 0 20px 0;border-bottom:1px solid #D0D0D0; font-size: 88%;">
					<table>
						<tr>
							<td style="width:15px;padding: 0 5px;"><i style="color: #f6d57e;" class="fa fa-circle"></i></td>
							<td style="width:100%;padding: 0 5px;"><b>Cache</b></td>
							<td style="width:15px;padding: 0 5px;">&nbsp;</td>
						</tr>
						<tr>
							<td style="padding: 0 5px;">&nbsp;</td>
							<td style="padding: 0 5px;">Nodes(s)</td>
							<td  style="padding: 0 5px;" align="right">3</td>
						</tr>
					</table>
				</div>
				
				<div style="margin:15px;padding:0 0 20px 0;border-bottom:1px solid #D0D0D0; font-size: 88%;">
					<table>
						<tr>
							<td style="width:15px;padding: 0 5px;"><i style="color: #6baf73;" class="fa fa-circle"></i></td>
							<td style="width:100%;padding: 0 5px;"><b>Index</b></td>
							<td style="width:15px;padding: 0 5px;">&nbsp;</td>
						</tr>
						<tr>
							<td style="padding: 0 5px;">&nbsp;</td>
							<td style="padding: 0 5px;">Nodes(s)</td>
							<td  style="padding: 0 5px;" align="right">4</td>
						</tr>
					</table>
				</div>
				
				<div style="margin:15px;padding:0 0 20px 0;border-bottom:1px solid #D0D0D0; font-size: 88%;">
					<table>
						<tr>
							<td style="width:15px;padding: 0 5px;"><i style="color: #6baf73;" class="fa fa-circle"></i></td>
							<td style="width:100%;padding: 0 5px;"><b>Assets</b></td>
							<td style="width:15px;padding: 0 5px;">&nbsp;</td>
						</tr>
						<tr>
							<td style="padding: 0 5px;">&nbsp;</td>
							<td style="padding: 0 5px;">Read/Write</td>
							<td  style="padding: 0 5px;" align="right">YES</td>
						</tr>
						<tr>
							<td style="padding: 0 5px;">&nbsp;</td>
							<td style="padding: 0 5px;">Started</td>
							<td  style="padding: 0 5px;" align="right">01/12/2013</td>
						</tr>
						<tr>
							<td style="padding: 0 5px;">&nbsp;</td>
							<td style="padding: 0 5px;"colspan="2">
								Address:  /nfshare/dotcms/assets
							</td>
						</tr>
					</table>
				</div>
				
				<div style="margin:15px;padding:0 0 20px 0;border-bottom:1px solid #D0D0D0; font-size: 88%;text-align:center;">
					<button dojoType="dijit.form.Button"><div style="width:200px;padding:8px 0;">Edit Node</div></button>
				</div>
				
				<div style="margin:15px;padding:0 0 20px 0;font-size: 88%;text-align:center;">
					<a href="#">Delete Node</a> &nbsp;  &nbsp; | &nbsp;  &nbsp;  <a href="#">Refresh Status</a>
				</div>
			</div>
			
	</div>

</div> 
</body>
</html>