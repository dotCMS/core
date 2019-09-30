<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="java.util.Enumeration"%>
<%	
	String dojoPath = Config.getStringProperty("path.to.dojo");

	String id = request.getParameter("id");
	
	String fieldName = (UtilMethods.isSet(request.getParameter("fieldName"))) ? request.getParameter("fieldName") : "fileAsset";

	String baseImage =  "/contentAsset/image/" + id + "/" + fieldName + "/" ;



	String hostId = null;
	if (request.getParameter("host_id") != null) {
		hostId = request.getParameter("host_id");
	} else {
		hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
	}
		
	
    User user = com.liferay.portal.util.PortalUtil.getUser(request);

    if(user ==null || LicenseLevel.COMMUNITY.level == LicenseUtil.getLevel()){
    	response.getWriter().println("Unauthorized");
    	return;
    }
	
    String userAgent = request.getHeader("USER-AGENT");
%>



<!DOCTYPE HTML PUBLIC "-//W3C//DTD. HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
	
	<title>dotCMS Image Tool</title>

    <link rel="stylesheet" type="text/css" href="/html/css/dot_admin.css">
    <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dmundra/dmundra.css">
    <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dmundra/Grid.css">
    <link rel="stylesheet" type="text/css" href="/html/js/dotcms/dijit/image/image_tools.css">

	
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
		dojo.require("dijit.form.ComboBox");
		dojo.require("dijit.form.Select");
		dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
		dojo.extend(dijit.form.Button, {
			scrollOnFocus:false
		});
		dojo.extend(dijit.form.Select, {
			scrollOnFocus:false
		});

		var imageEditor = window.top._dotImageEditor;
		
		dojo.ready(
			function(){
			  
				imageEditor.initIframe();
			}
		);

		

		
		
		
	</script>
	
</head>
<body class="dmundra"  >
<!--  top button bar -->
	<div class="imageToolContainer">
<div class="imageToolButtonBar">
	<table style="width:100%;margin:0px">
		<tr>
			<td width="100%;" style="white-space: nowrap;">
				<table>
					<tr>
						<td style="white-space: nowrap;">
							<%= LanguageUtil.get(pageContext, "Address") %>: 
						</td>
						<td width="100%;" style="white-space: nowrap;padding-right:25px;">
							<input type="text" id="viewingUrl" dojoType="dijit.form.TextBox" value="<%=baseImage %>" style="width:100%;" onkeypress="if (event.which == 13 || event.keyCode == 13) {imageEditor.changeViewingUrl()}" onchange="imageEditor.changeViewingUrl()">
						</td>
					</tr>
				</table>
			</td>
			<td style="white-space: nowrap;">
                <span style="display:inline-block;margin-left:0px;margin-right:10px;">
                    <a href="#" id="showLink" target="showDotImages">show</a>
                </span>


				<button dojoType="dijit.form.Button" onclick="imageEditor.doDownload()" >
					<%= LanguageUtil.get(pageContext, "download") %>
				</button>
				&nbsp; &nbsp;
				<button dojoType="dijit.form.Button" id="clipBoard"  <% if(id == null || id.startsWith("temp_")) { %>disabled<% } %> onclick="imageEditor.addToClipboard()">
					<%= LanguageUtil.get(pageContext, "Clip") %>
				</button>
				
				<%-- this span is hidden for binary images --%>
				<span id="saveAsSpan" style="display:none;">
					<button dojoType="dijit.form.ComboButton"  title="save-as">
						<span><%= LanguageUtil.get(pageContext, "save-as") %></span>
						<div dojoType="dijit.Menu" id="createMenu" style="display: none;">
							<div dojoType="dijit.MenuItem"   onClick="imageEditor.showSaveAsDialog('jpg')">
								<%= LanguageUtil.get(pageContext, "jpeg") %>
							</div>
							<div dojoType="dijit.MenuItem"   onClick="imageEditor.showSaveAsDialog('png');">
								<%= LanguageUtil.get(pageContext, "png") %>
							</div>
							<div dojoType="dijit.MenuItem"  onClick="imageEditor.showSaveAsDialog('gif');">
								<%= LanguageUtil.get(pageContext, "gif") %>
							</div>
						</div>
					</button>
					&nbsp;
				</span>
                &nbsp;
				<button dojoType="dijit.form.Button" onclick="imageEditor.saveImage()" >
					<%= LanguageUtil.get(pageContext, "Save") %>
				</button>
				&nbsp;
				<button dojoType="dijit.form.Button" onClick="imageEditor.closeImageWindow()" >
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
	<table id="controlTable">

         <tr>
            <th colspan=2><%=LanguageUtil.get(pageContext, "image.editor.heading.file.settings")%></th>
         </tr>
         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Compress")%> :</td>
            <td class="rightCol"><select dojoType="dijit.form.Select" name="compression" id="compression"
               style="width: 70px; margin-left: 2.6px;" onchange="imageEditor.toggleCompression()" >
                  <option value="none" selected>none</option>
                  <option value="auto">auto</option>
                  <option value="jpeg">jpeg</option>
                  <option value="webp">webp</option>
            </select></td>
         </tr>
         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "image.editor.label.filesize")%> :</td>
            <td class="rightCol">
                <div id="fileSizeDiv" ></div>
            </td>
        </tr>
         <tr>
            <td class="leftCol compressTd"><%=LanguageUtil.get(pageContext, "image.editor.label.quality")%> :</td>
            <td class="rightCol compressTd"><span id="compressionValueSpan"></span></td>
         </tr>
         <tr>
            <td colspan="2" class="compressTd">
               <div id="compressionValue" dojoType="dijit.form.HorizontalSlider" 
                    onMouseUp="imageEditor.toggleCompression();" 
                    onKeyUp="imageEditor.toggleCompression();" 
                    onChange="imageEditor.updateCompressionValue();"
                    maximum="100" minimum="1" 
                    discreteValues="100" 
                    showButtons="true" 
                    intermediateChanges="true"
					scrollOnFocus=false>

                  <div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=11 style="height: 5px;"></div>
                  <ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height: 1em; font-size: 75%; color: gray;">
                     <li>1%</li>
                     <li>50%</li>
                     <li>100%</li>
                  </ol>
               </div>

            </td>
         </tr>


        
        <tr><td colspan=2><hr style="height:1px; border:none;  background-color:silver; "></td></tr>
    
        <tr><th colspan=2 ><%=LanguageUtil.get(pageContext, "image.editor.heading.image-size" )%></th></tr>

         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Original")%> :</td>
            <td class="rightCol">
               <div class="spacerDiv">
                  <span id="baseImageWidth"></span>
               </div>
               <div class="spacerDiv" style="width: 20px">x</div>
               <div class="spacerDiv">
                  <span id="baseImageHeight"></span>
               </div>
            </td>
         </tr>

         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Resize")%> :</td>

            <td class="rightCol">
               <div class="spacerDiv">
                  <input type="text" id="displayImageWidth" class="textInputClass" maxlength="4" onblur="imageEditor.setHieghtFromWidth()"
                     onkeydown="return imageEditor.allowNumbers(event)">
               </div>
               <div class="spacerDiv" style="width: 20px">x</div>
               <div class="spacerDiv">
                  <input type="text" id="displayImageHeight" class="textInputClass" maxlength="4" onblur="imageEditor.setWidthFromHeight()"
                     onkeydown="return imageEditor.allowNumbers(event)">
               </div>
               <div class="spacerDiv">
                  <button dojoType="dijit.form.Button" id="resizeBtn" onclick="imageEditor.resizeBtnClick('resize')">
                     <%=LanguageUtil.get(pageContext, "Resize")%>
                  </button>
               </div>
            </td>
         </tr>
         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Crop")%> :</td>
            <td class="rightCol">
               <div class="spacerDiv">
                  <input type="text" value="150" id="cropWidth" name="cropWidth" maxlength="4" class="textInputClass"
                     onchange="imageEditor.setCropHeightFromWidth()" onkeydown="return imageEditor.allowNumbers(event)" />
               </div>
               <div class="spacerDiv" style="width: 20px;">x</div>
               <div class="spacerDiv">
                  <input type="text" value="150" id="cropHeight" name="cropHeight" maxlength="4" class="textInputClass"
                     onchange="imageEditor.setCropWidthFromHeight()" onkeydown="return imageEditor.allowNumbers(event)" />
               </div>
               <div class="spacerDiv">
                  <button id="cropBtn" dojoType="dijit.form.Button" onclick="imageEditor.toggleCrop()">
                     <%=LanguageUtil.get(pageContext, "Crop")%>
                  </button>
               </div>
            </td>
         </tr>
         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "image.editor.label.scale")%> :</td>
            <td class="rightCol">
                <span id="zoomInfo"></span>
            </td>
        </tr>
         <tr>
            <td colspan="2" id="zoomTd" >
                

                <div id="showScaleSlider" dojoType="dijit.form.HorizontalSlider" 
                        onChange="imageEditor.updateSlider()" 
                        onMouseUp="imageEditor.doSliderResize()"
                        maximum="200" 
                        minimum="1" 
                        showButtons="true"
                        intermediateChanges="true"
						scrollOnFocus=false>

                    <div dojoType="dijit.form.HorizontalRule" container="bottomDecoration" count=11 style="height:5px;"></div>
                    <ol dojoType="dijit.form.HorizontalRuleLabels" container="bottomDecoration" style="height:1em;font-size:75%;color:gray;">
                        <li>0</li>
                        <li>100%</li>
                        <li>200%</li>
                    </ol>
                </div> 
                
            </td>
        </tr>



         <tr>
            <td colspan=2><hr style="height: 1px; border: none; background-color: silver;"></td>
         </tr>

         <tr>
            <th colspan=2><%=LanguageUtil.get(pageContext, "image.editor.heading.transform")%></th>
         </tr>

         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Rotate")%> :</td>
            <td class="rightCol"><input id="rotate" class="textInputClass" type="text" dojoType="dijit.form.NumberTextBox" name="rotate"
               value="0" constraints="{min:-360,max:360,places:0}" required="false" maxlength="3" onchange="imageEditor.doRotate()"
               invalidMessage="Angle is between -360 and 360" style="width: 55px;" scrollOnFocus="false"></td>
         </tr>

         <tr>
            <td class="leftCol"><%=LanguageUtil.get(pageContext, "Flip")%> :</td>
            <td class="rightCol"><input id="flip" dojoType="dijit.form.CheckBox" name="flip" type="checkbox" value="true"
               onchange="imageEditor.toggleFlip()"></td>
         </tr>



         <tr>
            <td class="leftCol"><label for="grayscale"><%=LanguageUtil.get(pageContext, "Grayscale")%></label> :</td>
            <td class="rightCol"><input name="grayscale" id="grayscale" dojoType="dijit.form.CheckBox" name="grayscale" type="checkbox"
               value="true" onchange="imageEditor.toggleGrayscale()"></td>
         </tr>



         <tr>
            <td class="leftCol" style="height: 25px; vertical-align: middle;"><%=LanguageUtil.get(pageContext, "Color")%> :</td>
            <td class="rightCol">
               <div id="hsbButton" onclick="imageEditor.toggleHSB()" style="cursor: pointer; border: 1px solid #eeeeee; padding: 10px;">
                  Bright:<span id="brightSpan">0</span>&nbsp; Hue:<span id="hueSpan">0</span>&nbsp; Sat:<span id="satSpan">0</span>
               </div>
            </td>
         </tr>




         <tr><td colspan=2><hr style="height:1px; border:none;  background-color:silver; "></td></tr>




         <tr>
            <td colspan="2">
               <div id="filterListBox">
                  <div id="filtersUndoDiv">Filters:</div>
                  <div id="filterListDiv">
                     <div id="filtersListContainer"></div>
                  </div>
               </div>
            </td>
         </tr>



      </table>
</div>
<!--  /toolBar -->







<%--   Let the Dialogs commence!      --%>





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
	s					scrollOnFocus=false
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
						scrollOnFocus=false
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
						scrollOnFocus=false
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
					<button dojoType="dijit.form.Button"  onclick="imageEditor.closeSaveAsDia()">
						<%= LanguageUtil.get(pageContext, "Close") %>
					</button>
				</div>
				<div  id="saveAsSuccessBtn" style="display:none" >
					<button dojoType="dijit.form.Button"  onclick="imageEditor.closeSaveAsDia()">
						<%= LanguageUtil.get(pageContext, "Close") %>
					</button>
				</div>
					<div id="saveAsButtonCluster">
						<button dojoType="dijit.form.Button" id="saveAsFinalBtn"  onclick="imageEditor.doSaveAs()">
							<%= LanguageUtil.get(pageContext, "Save") %>
						</button>
						&nbsp;
						<button dojoType="dijit.form.Button"  onclick="imageEditor.closeSaveAsDia()">
							<%= LanguageUtil.get(pageContext, "Cancel") %>
						</button>
					</div>
				</td>
			</tr>
		</table>
	</div>
    <div style="position:absolute;top:-50000px">
        <input type="file" name="hiddenFileUploader" id="hiddenFileUploader">
    
    
    
    </div>
	 
	 	<img src="<%=baseImage%>" style="position:absolute;top:-50000px;left:-50000px;" id="baseImage" />
	 	<iframe id="actionJackson" src="/html/images/shim.gif" frameborder="0" width="0" height="0"></iframe>
	</div>
	</body>

</html>
