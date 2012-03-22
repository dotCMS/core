<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%	
	String dojoPath = Config.getStringProperty("path.to.dojo");
	String id = request.getParameter("identifier");
	String inode = request.getParameter("inode");
	
	String fieldName = (!UtilMethods.isSet(request.getParameter("fieldName"))) ? "image" : request.getParameter("fieldName") ;
	
	String baseImage =  (id != null) ? "/contentAsset/image/" + id + "/image/" :  "/contentAsset/image/" + inode + "/" + fieldName +"/?byInode=true";



	String hostId = null;
	if (request.getParameter("host_id") != null) {
		hostId = request.getParameter("host_id");
	} else {
		hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
	}
		
	
	// everytime this is loaded, wipe the session var for the resulting image
	session.removeAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
	
	
	
%>



<!DOCTYPE HTML PUBLIC "-//W3C//DTD. HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
	
	<title>dotCMS Image Tools </title>

	<style type="text/css">
		@import "/html/common/css.jsp"; 
        @import "<%=dojoPath%>/dojox/layout/resources/ResizeHandle.css";
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
        @import "<%=dojoPath%>/dijit/themes/dmundra/Grid.css";
        @import "/html/portlet/ext/contentlet/image_tools/image-tools.css";
        
        
        .loader{
        	background:url('<%=dojoPath%>/dojox/image/resources/images/loading.gif') no-repeat center center;
        }
        .filterListItem{
			text-align:left;
			cursor: pointer;
			padding:2px;
			border:1px solid silver;	
			background:url('<%=dojoPath%>/dijit/themes/dmundra/images/tabClose.gif') no-repeat right center ;
		}

        
    </style>
	
   	<script type="text/javascript">
       	djConfig={
               parseOnLoad: false,
               useXDomain: false,
               isDebug: false,
               modulePaths: { "dotcms": "/html/js/dotcms" }
       };
		var baseImage="<%=baseImage%>";

		var id="<%=id%>";
		var inode="<%=inode%>";
   	</script>
	<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js.uncompressed.js"></script>

	
	<script>
		dojo.require("dijit.form.HorizontalSlider");
		dojo.require("dijit.form.HorizontalRule");
		dojo.require("dijit.form.HorizontalRuleLabels");
		dojo.require("dijit.form.CheckBox");
		dojo.require("dijit.form.NumberTextBox");
		dojo.require("dijit.Dialog");
		dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
		dojo.require("dojox.layout.ResizeHandle");
		var imageEditor = window.parent._dotImageEditor;
		
		dojo.ready(
			function(){
				 dojo.parser.parse();
				imageEditor.initIframe();
			}
		);

	</script>
	
</head>
<body class="dmundra"  style="background:white url()" >
	

	<div id="main" >
		<div class="imageToolButtonBar">
		
			<%= LanguageUtil.get(pageContext, "Address") %>: 
			<input type="text" id="viewingUrl" readonly="true" value="<%=baseImage %>" onfocus="imageEditor.selectAllUrl()">
		</div>
		




		<div id="viewerMain">
			<div id="imageViewPort" class="loader">
				<img id="me" src="<%=baseImage%>">
			</div>
		</div>

		<!--  toolBar -->
		<div id="toolBar" >
			<table id="controlTable" align="center">
				<tr>
					<td rowspan="3" id="zoomTd">
					
					
						<div style="text-align:left;padding-left:30px;">zoom: <span id="zoomInfo"></span></div>
				   		<div id="showScaleSlider" dojoType="dijit.form.HorizontalSlider" 
								onChange="imageEditor.sliderUpdate()" 
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
					<td><%= LanguageUtil.get(pageContext, "Original") %>:</td>
					<td>
						<div class="baseImageDim" id="baseImageWidth"></div>
						<div style="float:left">x</div>
						<div class="baseImageDim"  id="baseImageHeight"></div>
					</td>
					<td>
						Flip: <input id="flip" dojoType="dijit.form.CheckBox" name="flip" type="checkbox" value="true" onchange="imageEditor.toggleFlip()">
					</td>
					<td>
						Rotate:
							<input id="rotate" type="text" dojoType="dijit.form.NumberTextBox" name="rotate"
								value="0" constraints="{min:0,max:360,places:0}" required="false" maxlength="3"
								invalidMessage="Angle is between 0 and 359" style="width:50px;">
					</td>
					<td>
						<button dojoType="dijit.form.Button"  onclick="imageEditor.doRotate()">
							<%= LanguageUtil.get(pageContext, "Rotate") %>
						</button>
					</td>
					<td rowspan="3" id="filterListTd">
						<div id="filtersUndoDiv">Filters:</div>
						<div id="filterListDiv">
							<div id="filtersListContainer"></div>
						</div>
					</td>
				</tr>
				<tr>
					<td><%= LanguageUtil.get(pageContext, "Resize") %>:</td>
					<td style="white-space:no-wrap;">
						<div style="width:110px;">
							<div id="displayImageWidth"  style="backgroud:#eeeeee" class="displayImageDim" contenteditable="true" onblur="imageEditor.setHieghtFromWidth()" onkeydown="return imageEditor.allowNumbers(event)"></div>
							<div style="float:left;">x</div>
							<div id="displayImageHeight"  class="displayImageDim" contenteditable="true" onblur="imageEditor.setWidthFromHeight()"  onkeydown="return imageEditor.allowNumbers(event)"></div></td>
						</div>
					<td>
						<button" dojoType="dijit.form.Button"  onclick="imageEditor.doTypedResize()">
							<%= LanguageUtil.get(pageContext, "Resize") %>
						</button>
					</td>
					<td style="border-right:1px solid white;">
						Grayscale: <input id="grayscale" dojoType="dijit.form.CheckBox" name="grayscale" type="checkbox" value="true" onchange="imageEditor.toggleGrayscale()">					
					</td>
					<td>
						Compress :
							<input id="jpeg" dojoType="dijit.form.CheckBox" name="jpeg" type="checkbox" value="true" onchange="imageEditor.toggleJpeg()">	
					
					</td>
				</tr>
				
				
	
				<tr>
					<td><%= LanguageUtil.get(pageContext, "Crop") %>:</td>
					<td>
						<input id="constrain" dojoType="dijit.form.CheckBox" name="constrain" type="checkbox" value="true"> 
						<label for="constrain"><%= LanguageUtil.get(pageContext, "constrain") %></label> 
					</td>
					<td>
						<button id="cropBtn" dojoType="dijit.form.Button"  onclick="imageEditor.toggleCrop()">
							<%= LanguageUtil.get(pageContext, "Crop") %>
						</button>
					</td>
					<td colspan="2" onclick="imageEditor.toggleHSB()" style="cursor:pointer;">
						Hue:<span id="hueSpan">0</span>&nbsp;
						Sat:<span id="satSpan">0</span>&nbsp;
						Bright:<span id="brightSpan">0</span>
					</td>
				</tr>
			</table>
		</div>
		<!--  /toolBar -->

		<div class="imageToolButtonBar" style="text-align:right;">
			<button dojoType="dijit.form.Button" onclick="imageEditor.doDownload()" iconClass="downloadIcon">
				<%= LanguageUtil.get(pageContext, "download") %>
			</button>
			&nbsp;
			<button dojoType="dijit.form.Button" onclick="imageEditor.saveImageAs()" iconClass="saveAsIcon">
				<%= LanguageUtil.get(pageContext, "save-as") %>
			</button>
			&nbsp;
			<button dojoType="dijit.form.Button" onclick="imageEditor.saveImage()" iconClass="saveIcon">
				<%= LanguageUtil.get(pageContext, "save") %>
			</button>
			&nbsp;
		
			<button dojoType="dijit.form.Button" onClick="imageEditor.closeImageWindow()" iconClass="closeIcon">
				<%= LanguageUtil.get(pageContext, "Close") %>
			</button>
		</div>
	<div>


	<div id="hsbDialog" dojoType="dijit.Dialog" title="Color" style="display: none" onfocus='blur()'>
		<table  id="hsbTable" align="center">
			<tr>
				<td><%= LanguageUtil.get(pageContext, "brightness") %>:</td>
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
	<%-- 
	<div id="saveAsDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Save-As") %>" style="display: none" onfocus='blur()'>
		<table>
			<tr>
				<td>Filename: </td>
				<td>
					<input id="saveAsName" type="text" dojoType="dijit.form.TextBox" name="saveAsName" value="" required="false" maxlength="50"><span id="saveAsFileExt"></span>
			</td>
		</tr>
		<tr>
			<td>Folder:</td>
			<td>
				<div id="saveAsFolder" name="saveAsFolder" onlySelectFolders="true" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" <%= UtilMethods.isSet(hostId)?"hostId=\"" + hostId + "\"":"" %>></div>
			
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<button dojoType="dijit.form.Button"  onclick="imageEditor.doSaveAs()">
					<%= LanguageUtil.get(pageContext, "Save-As") %>
				</button>

			</td>
		</tr>
	</div>
 --%>
	 
	 	<img src="<%=baseImage%>" style="position:absolute;left:-50000px" id="baseImage" />
	 	<iframe id="actionJackson" frameborder="0" width="1000" height="1000"></iframe>
	</body>
</html>