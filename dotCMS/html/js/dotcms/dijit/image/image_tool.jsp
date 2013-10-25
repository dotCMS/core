<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="java.util.Enumeration"%>
<%	
	String dojoPath = Config.getStringProperty("path.to.dojo");
	String id = request.getParameter("identifier");
	String inode = request.getParameter("inode");
	
	String fieldName = (UtilMethods.isSet(request.getParameter("fieldName"))) ? request.getParameter("fieldName") :"image" ;
	
	String baseImage =  (id != null) ? "/contentAsset/image/" + id + "/image/" :  "/contentAsset/image/" + inode + "/" + fieldName +"/?byInode=true";



	String hostId = null;
	if (request.getParameter("host_id") != null) {
		hostId = request.getParameter("host_id");
	} else {
		hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
	}
		
	
    User user = null;
	try {
		user = com.liferay.portal.util.PortalUtil.getUser(request);
	} catch (Exception e) {
		Logger.warn(this.getClass(), "Unauthorized access to ImageToolAjax from IP + "+ request.getRemoteAddr() +", no user found");
	} 
    if(user ==null || "100".equals(LicenseUtil.getLevel())){
    	response.getWriter().println("Unauthorized");
    	return;
    }
	
    String userAgent = request.getHeader("USER-AGENT");
%>



<!DOCTYPE HTML PUBLIC "-//W3C//DTD. HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
	
	<title>dotCMS Image Tool</title>

	<style type="text/css">
		@import "/html/common/css.jsp";
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
        @import "<%=dojoPath%>/dijit/themes/dmundra/Grid.css";
        @import "/html/js/dotcms/dijit/image/image_tools.css";
    </style>
	
	<!--[if gte IE 7]><link href="/html/css/iehacks.css" rel="stylesheet" type="text/css" /><![endif]-->
	
   	<script type="text/javascript">
       	djConfig={
               parseOnLoad: true,
               useXDomain: false,
               isDebug: false,
               modulePaths: { "dotcms": "/html/js/dotcms" }
       };
		var baseImage="<%=baseImage%>";

		var id="<%=id%>";
		var inode="<%=inode%>";
   	</script>
	<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js"></script>

	
	<script>
		dojo.require("dijit.form.HorizontalSlider");
		dojo.require("dijit.form.HorizontalRule");
		dojo.require("dijit.form.HorizontalRuleLabels");
		dojo.require("dijit.form.CheckBox");
		dojo.require("dijit.form.NumberTextBox");
		dojo.require("dijit.Dialog");
		dojo.require("dijit.MenuItem");
		dojo.require("dijit.Menu");
		dojo.require("dijit.ProgressBar");
		dojo.require("dijit.form.ValidationTextBox");
		dojo.require("dijit.form.ComboButton");
		dojo.require("dijit.Menu");
		dojo.require("dijit.MenuItem");
		
		dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");

		var imageEditor = window.top._dotImageEditor;
		
		dojo.ready(
			function(){
				// dojo.parser.parse();
				imageEditor.initIframe();
			}
		);

		
		function openAddress(){

			newwin = window.open(dijit.byId("viewingUrl").value, "newwin", "width=700,height=500,scrollbars=1,addressbar=1,resizable=1,menubars=1,toolbars=1");
			newwin.focus();
		}
		
		
		
		
	</script>
	
</head>
<body class="dmundra"  >
	


<!--  top button bar -->
<div class="imageToolButtonBar dijitLayoutContainer">
	<table style="width:100%;">
		<tr>
			<td width="100%;" style="white-space: nowrap;">
				<table>
					<tr>
						<td style="white-space: nowrap;">
							<%= LanguageUtil.get(pageContext, "Address") %>: 
						</td>
						<td width="100%;" style="white-space: nowrap;padding-right:25px;">
							<input type="text" id="viewingUrl" dojoType="dijit.form.TextBox" value="<%=baseImage %>" onfocus="imageEditor.selectAllUrl()" style="width:100%;">
						</td>
					</tr>
				</table>
			</td>
			<td style="white-space: nowrap;">
				<button dojoType="dijit.form.Button" id="goUrl" iconClass="arrowIcon" onclick="openAddress()">
					<%= LanguageUtil.get(pageContext, "Show") %>
				</button>

				<button dojoType="dijit.form.Button" onclick="imageEditor.doDownload()" iconClass="downloadIcon">
					<%= LanguageUtil.get(pageContext, "download") %>
				</button>
				&nbsp; &nbsp;
				<button dojoType="dijit.form.Button" id="clipBoard" iconClass="clipIcon" onclick="imageEditor.addToClipboard()">
					<%= LanguageUtil.get(pageContext, "Clip") %>
				</button>
				
				<%-- this span is hidden for binary images --%>
				<span id="saveAsSpan" style="display:none;">
					<button dojoType="dijit.form.ComboButton" iconClass="saveAsIcon" title="save-as">
						<span><%= LanguageUtil.get(pageContext, "save-as") %></span>
						<div dojoType="dijit.Menu" id="createMenu" style="display: none;">
							<div dojoType="dijit.MenuItem"  iconClass="jpgIcon" onClick="imageEditor.showSaveAsDialog('jpg')">
								<%= LanguageUtil.get(pageContext, "jpeg") %>
							</div>
							<div dojoType="dijit.MenuItem"   iconClass="pngIcon" onClick="imageEditor.showSaveAsDialog('png');">
								<%= LanguageUtil.get(pageContext, "png") %>
							</div>
							<div dojoType="dijit.MenuItem"  iconClass="gifIcon" onClick="imageEditor.showSaveAsDialog('gif');">
								<%= LanguageUtil.get(pageContext, "gif") %>
							</div>
						</div>
					</button>
					&nbsp;
				</span>
				<button dojoType="dijit.form.Button" onclick="imageEditor.saveImage()" iconClass="saveIcon">
					<%= LanguageUtil.get(pageContext, "Save") %>
				</button>
				
				<button dojoType="dijit.form.Button" onClick="imageEditor.closeImageWindow()" iconClass="closeIcon">
					<%= LanguageUtil.get(pageContext, "Close") %>
				</button>
			</td>
		</tr>
	</table>
		
</div>
<!--  /top button bar -->




<!--  image viewport -->
<div id="imageViewPort" class="loader">
	<img id="me" src="<%=baseImage%>">
</div>
<!-- /image viewport -->
		
		
<!--  toolBar -->
<div id="toolBar" >
	<table id="controlTable" align="center">
		<tr>
			<td rowspan="3" id="zoomTd" >
				
				<div style="text-align:center;padding-left:30px;">zoom: <span id="zoomInfo"></span></div>
		   		<div id="showScaleSlider" dojoType="dijit.form.HorizontalSlider" 
						onChange="imageEditor.updateSlider()" 
						onMouseUp="imageEditor.doSliderResize()"
						maximum="200" 
						minimum="1" 
						showButtons="true"
						intermediateChanges="true"> 
							
					<div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=11 style="height:5px;"></div>
					<ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
				        <li>
				            0
				        </li>
				        <li>
				            50%
				        </li>
				        <li>
				            100%
				        </li>
				        <li>
				            150%
				        </li>
				        <li>
				            200%
				        </li>
				    </ol>
				</div> 
				
			</td>
			<td style="border-right:1px solid white;"><%= LanguageUtil.get(pageContext, "Original") %>:</td>
			<td align="center" valign="middle">
				<span  id="baseImageWidth"></span>
				&nbsp; x &nbsp;
				<span id="baseImageHeight"></span>
			</td>
			<td>
				Flip: <input id="flip" dojoType="dijit.form.CheckBox" name="flip" type="checkbox" value="true" onchange="imageEditor.toggleFlip()">
			</td>
			<td style="border-right:1px solid white;">
				Rotate:
					<input id="rotate" type="text" dojoType="dijit.form.NumberTextBox" name="rotate"
						value="0" constraints="{min:-360,max:360,places:0}" required="false" maxlength="4"
						invalidMessage="Angle is between -360 and 360" style="width:55px;">
					<!--<input id="rotate" type="text" class="textInputClass" name="rotate"
						value="0" maxlength="4">  -->
			</td>
			<td>
				<button dojoType="dijit.form.Button"  onclick="imageEditor.doRotate()" iconClass="rotateIcon">
					<%= LanguageUtil.get(pageContext, "Rotate") %>
				</button>
			</td>
			<td rowspan="3" id="filterListTd">
				<div id="filterListBox">
					<div id="filtersUndoDiv">Filters:</div>
					<div id="filterListDiv">
						<div id="filtersListContainer"></div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td style="border-right:white 1px solid"><%= LanguageUtil.get(pageContext, "Resize") %>:</td>
			<td style="white-space:no-wrap;border-right:white 1px solid;text-align: center">
				
					<input type="text" id="displayImageWidth" class="textInputClass" maxlength="4" onblur="imageEditor.setHieghtFromWidth()" onkeydown="return imageEditor.allowNumbers(event)">
					 x 
					<input type="text" id="displayImageHeight" class="textInputClass" maxlength="4" onblur="imageEditor.setWidthFromHeight()"  onkeydown="return imageEditor.allowNumbers(event)"></td>
				
			<td>
				<button dojoType="dijit.form.Button" id="resizeBtn" iconClass="resizeIcon" onclick="imageEditor.resizeBtnClick('resize')">
					<%= LanguageUtil.get(pageContext, "Resize") %>
				</button>
			</td>
			<td style="border-right:1px solid white;">
				<label for="grayscale"><%=LanguageUtil.get(pageContext, "Grayscale") %></label>: <input name="grayscale" id="grayscale" dojoType="dijit.form.CheckBox" name="grayscale" type="checkbox" value="true" onchange="imageEditor.toggleGrayscale()">					
			
			</td>
			<td>
				<%if(UtilMethods.isSet(request.getParameter("fieldName"))) {%>
				<%=LanguageUtil.get(pageContext, "Compress" )%> :
					<input id="jpeg" dojoType="dijit.form.CheckBox" name="jpeg" type="checkbox" value="true" onchange="imageEditor.toggleJpeg()">	
				<%} %>&nbsp;
			</td>
		</tr>
		
		

		<tr>
			<td valign="top" style="border-right:white 1px solid;">
				<%= LanguageUtil.get(pageContext, "Crop") %>:
			</td>
			<td style="white-space:no-wrap;border-right:white 1px solid;text-align: center">
				<input type="text" value="150" id="cropWidth" name="cropWidth" maxlength="4" class="textInputClass" onchange="imageEditor.setCropHeightFromWidth()" onkeydown="return imageEditor.allowNumbers(event)"/> 
					 x 
				<input type="text" value="150" id="cropHeight" name="cropHeight" maxlength="4"  class="textInputClass"  onchange="imageEditor.setCropWidthFromHeight()" onkeydown="return imageEditor.allowNumbers(event)"/>
				<div style="display:none">
				
				<label for="constrain"><%=LanguageUtil.get(pageContext, "Constrain") %></label>: <input name="constrain" id="constrain" dojoType="dijit.form.CheckBox" onclick="imageEditor.doConstrain()" type="checkbox" value="true">					
				</div>
			</td>
			<td valign="top">
				<button id="cropBtn" dojoType="dijit.form.Button"  iconClass="cropIcon" onclick="imageEditor.toggleCrop()">
					<%= LanguageUtil.get(pageContext, "Crop") %>&nbsp;&nbsp;
				</button>
			</td>
			<td colspan="2" onclick="imageEditor.toggleHSB()" style="cursor:pointer;">
				Bright:<span id="brightSpan">0</span>&nbsp;
				Hue:<span id="hueSpan">0</span>&nbsp;
				Sat:<span id="satSpan">0</span>&nbsp;
				
			</td>
		</tr>
	</table>
</div>
<!--  /toolBar -->




	</div>



<%-----------------------   Let the Dialogs commence!        ----------------------%>





	<div id="hsbDialog" dojoType="dijit.Dialog" title="Color" style="display: none" >
		<table  id="hsbTable" align="center">
			<tr>
				<td><%= LanguageUtil.get(pageContext, "Brightness") %>:</td>
				<td>
				
					<div dojoType="dijit.form.HorizontalSlider"  
						maximum="100" 
						minimum="-100" 
						showButtons="false"
						value="0"
						onchange="imageEditor.changeHSB()"
						intermediateChanges="true"
						class="colorSlider"
							id="brightSlider"> 
						<div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=5 style="height:5px;"></div>
						<ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
					        <li>
					           -100
					        </li>
					        <li>
					           -50
					        </li>
					        <li>
					            0
					        </li>
					        <li>
					            50
					        </li>
					        <li>
					            100
					        </li>
					    </ol>
					</div> 
				</td>
			</tr>
			<tr>
				<td><%= LanguageUtil.get(pageContext, "Hue") %>:</td>
				<td>
					<div dojoType="dijit.form.HorizontalSlider"  
						maximum="100" 
						minimum="-100" 
						showButtons="false"
						value="0"
						onchange="imageEditor.changeHSB()"
						intermediateChanges="true"
						class="colorSlider"
						id="hueSlider"> 
					<div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=5 style="height:5px;"></div>
					<ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
					        <li>
					           -100
					        </li>
					        <li>
					           -50
					        </li>
					        <li>
					            0
					        </li>
					        <li>
					            50
					        </li>
					        <li>
					            100
					        </li>
				    </ol>
				</div> 
				</td>
			</tr>
			<tr>
				<td><%= LanguageUtil.get(pageContext, "Saturation") %>:</td>
				<td>
					<div dojoType="dijit.form.HorizontalSlider"  
						maximum="100" 
						minimum="-100" 
						showButtons="false"
						value="0"
						onchange="imageEditor.changeHSB()"
						intermediateChanges="true"
						class="colorSlider"
							id="satSlider"> 
						<div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=5 style="height:5px;"></div>
						<ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
					        <li>
					           -100
					        </li>
					        <li>
					           -50
					        </li>
					        <li>
					            0
					        </li>
					        <li>
					            50
					        </li>
					        <li>
					            100
					        </li>
					    </ol>
					</div> 
				</td>
			</tr>
		</table>
		<div style="text-align:center;padding:15px;">
			<button dojoType="dijit.form.Button"  onclick="imageEditor.applyHSB()">
				<%= LanguageUtil.get(pageContext, "Apply") %>
			</button>
		</div>
	</div>

	<div id="saveAsDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "save-as") %>" style="display: none" onfocus='blur()'>
		<table align="center" id="saveAsDiaTable">
			<tr>
				<td  align="center">
					<%= LanguageUtil.get(pageContext, "File") %>: 
					&nbsp;
					<input id="saveAsName" type="text" dojoType="dijit.form.ValidationTextBox" regExp="^([a-z]|[A-Z]|[0-9]|_|-|\.)+$" name="saveAsName" value="" required="false" maxlength="50">&nbsp;<span id="saveAsFileExt"></span>
				</td>
			</tr>
			<tr style="height:20px;">
				<td  align="center">
					<div style="display:none;" id="saveAsFailMsg">
						<%= LanguageUtil.get(pageContext, "save-as-failed-check-filename") %>
					</div>
					<div style="display:none;" id="saveAsSuccessMsg">
						<%= LanguageUtil.get(pageContext, "save-as-success") %>
					</div>
					<div dojoType="dijit.ProgressBar" style="width:150px;display:none;" id="saveAsProgress" indeterminate="true" 
						id="downloadProgress" maximum="10">
					</div>
				</td>
			</tr>
			<tr style="height:20px;">
				<td  align="center">
				<div id="saveAsFailBtn" style="display:none" >
					<button dojoType="dijit.form.Button" iconClass="closeIcon" onclick="imageEditor.closeSaveAsDia()">
						<%= LanguageUtil.get(pageContext, "Close") %>
					</button>
				</div>
				<div  id="saveAsSuccessBtn" style="display:none" >
					<button dojoType="dijit.form.Button" iconClass="cancelIcon" onclick="imageEditor.closeSaveAsDia()">
						<%= LanguageUtil.get(pageContext, "Close") %>
					</button>
				</div>
					<div id="saveAsButtonCluster">
						<button dojoType="dijit.form.Button" id="saveAsFinalBtn" iconClass="saveIcon" onclick="imageEditor.doSaveAs()">
							<%= LanguageUtil.get(pageContext, "Save") %>
						</button>
						&nbsp;
						<button dojoType="dijit.form.Button" iconClass="closeIcon" onclick="imageEditor.closeSaveAsDia()">
							<%= LanguageUtil.get(pageContext, "Cancel") %>
						</button>
					</div>
				</td>
			</tr>
		</table>
	</div>

	 
	 	<img src="<%=baseImage%>" style="position:absolute;left:-50000px" id="baseImage" />
	 	<iframe id="actionJackson" src="/html/images/shim.gif" frameborder="0" width="0" height="0"></iframe>
	</body>

</html>
