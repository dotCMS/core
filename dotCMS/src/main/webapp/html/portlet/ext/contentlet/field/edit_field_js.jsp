<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.time.ZoneId"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.time.ZonedDateTime"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="org.apache.velocity.context.Context"%>

<%@page import="com.dotcms.contenttype.model.field.WysiwygField"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/ContentletAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/FileAssetAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>

<!-- AChecker support -->
<script type='text/javascript' src='/dwr/interface/ACheckerDWR.js'></script>

<script src="/html/js/ace-builds-1.2.3/src-noconflict/ace.js" type="text/javascript"></script>
<script type="text/javascript" src="/html/js/tinymce/js/tinymce/tinymce.min.js"></script>

<script type="text/javascript">

	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require("dotcms.dijit.form.FileSelector");
	dojo.require("dotcms.dijit.form.FileAjaxUploader");
	dojo.require("dotcms.dijit.FileBrowserDialog");
	dojo.require("dojo.dnd.Source");

<% User usera= com.liferay.portal.util.PortalUtil.getUser(request); %>
	var textAreaId;
	tinymce.init({
		selector: "textarea#"+textAreaId,
		theme: "modern",
		menubar:false,
	    statusbar: false,
		plugins: [
    		"advlist autolink lists link image charmap print preview hr anchor pagebreak",
    		"searchreplace wordcount visualblocks visualchars code fullscreen",
    		"insertdatetime media nonbreaking save table contextmenu directionality",
    		"emoticons template paste textcolor colorpicker validation textpattern dotimageclipboard"
		],
		languages : '<%= usera.getLanguageId().substring(0,2) %>',
		toolbar1: "insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image",
		toolbar2: "print preview | validation media | forecolor dotimageclipboard backcolor emoticons",
		image_advtab: true,
		file_picker_callback: function(callback, value, meta) {
			cmsFileBrowser(callback, value, meta);
		}
	});

</script>

<script type="text/javascript">

function confirmEditSite(dialogId) {
    dijit.byId(dialogId).show();
}

function enableSiteKeyUpdate(dialogId, siteKeyInputId) {
    dijit.byId(dialogId).hide();
    dojo.removeAttr(siteKeyInputId, 'readonly');
    setTimeout(() => {
        dijit.byId(siteKeyInputId).focus();
    }, 500);
}

var cmsfile=null;
	//Hints
	function showHint(jsevent) {
		var coordinates = jsevent.getXY();
		this.moveTo(coordinates[0] + 10, coordinates[1]);
		this.show();
	}

	function hideHint(jsevent) {
		this.hide();
	}

	//Date/Time fields
	function updateDate(varName) {
		var field = $(varName);
		var dateValue ="";
		var datePart=dijit.byId(varName + "Date");
		var timePart=dijit.byId(varName + 'Time');

        var includesDate = false;
		if (datePart != null) {
			var x = datePart.getValue();
			if (x) {
				var month = (x.getMonth() +1) + "";
				month = (month.length < 2) ? "0" + month : month;
				var day = (x.getDate() ) + "";
				day = (day.length < 2) ? "0" + day : day;
				var year = x.getFullYear();
				dateValue= year + "-" + month + "-" + day;
                includesDate = true
			}
		}

		if (datePart==null || includesDate) {
			// if it is just time or date_time but the value exists
			if (timePart != null) {
				var time = timePart.value;
				if (!isNaN(time) && time != null) {
					var hour = time.getHours();
					if(hour < 10) hour = "0" + hour;
					var min = time.getMinutes();
					if(min < 10) min = "0" + min;
                    var hourAndMin = hour + ":" + min;
                    if (includesDate) {
                        dateValue += " " + hourAndMin;
                    } else {
                        dateValue = hourAndMin;
                    }
					if (datePart==null) {
                        dateValue += ":00";
                    }
				} else if (includesDate) {
					dateValue += " 00:00";
				}
			} else if (includesDate) {
				dateValue += " 00:00";
			}
		}


        <%String offset = new SimpleDateFormat("Z").format(Calendar.getInstance(APILocator.systemTimeZone()).getTime());%>
		const timeZoneOffset = " <%=offset%>";
        if (includesDate) {
            field.value = dateValue + timeZoneOffset;
        } else {
            field.value = dateValue;
        }

		if(typeof updateStartRecurrenceDate === 'function' && (varName == 'startDate' || varName == 'endDate')){
		    updateStartRecurrenceDate(varName);
		}
	}

	  function removeThumbnail(x, inode) {

		    var pDiv = dojo.byId('thumbnailParent'+x);
	    	if(pDiv != null && pDiv != undefined){
	    		dojo.destroy(pDiv);
	    	}

	    	var swDiv = dojo.byId(x+'ThumbnailSliderWrapper');
	       	if(swDiv != null && swDiv != undefined){
	       	    dojo.destroy(swDiv);
	       	}

	    	dojo.query(".thumbnailDiv" + x).forEach(function(node, index, arr){
	    		dojo.destroy(node);
	    	});

	    	var dt = dojo.byId(x+'dt');
	    	if(dt != null && dt != undefined){
	    		dt.innerHTML = '';
	    	}

	    	//http://jira.dotmarketing.net/browse/DOTCMS-5802
	 	    ContentletAjax.removeSiblingBinaryFromSession(x, null);
	    }


	function isInteger(s)
	   {
	      var i;

	      if (isEmpty(s))
	      if (isInteger.arguments.length == 1) return 0;
	      else return (isInteger.arguments[1] == true);

	      for (i = 0; i < s.length; i++)
	      {
	         var c = s.charAt(i);

	         if (!isDigit(c)) return false;
	      }

	      return true;
	   }

	   function isEmpty(s)
	   {
	      return ((s == null) || (s.length == 0))
	   }

	   function isDigit (c)
	   {
	      return ((c >= "0") && (c <= "9"))
	   }


	function updateAmPm(varName) {
		var hour = dijit.byId(varName + 'Hour').value;
		if(hour > 11) {
			$(varName + 'AMPM').update("PM");
		} else {
			$(varName + 'AMPM').update("AM");
		}
	}

	function daysInMonth(month,year)
	{
		return 32 - new Date(year,month,32).getDate();
	}

</script>

<script type="text/javascript">
<jsp:include page="/html/portlet/ext/contentlet/field/tiny_mce_config.jsp"/>
	//### END INIT TINYMCE ###
	//### TINYMCE ###



	var enabledWYSIWYG = new Array();
	var enabledCodeAreas = new Array();
	var aceEditors = new Array();


	function enableDisableWysiwygCodeOrPlain(id, isFullscreen = "false") {
		var toggleValue=dijit.byId(id+'_toggler').attr('value');
		if (toggleValue=="WYSIWYG"){
			toWYSIWYG(id, isFullscreen);
			updateDisabledWysiwyg(id,"WYSIWYG");
			}
		else if(toggleValue=="CODE"){
			toCodeArea(id, isFullscreen);
			updateDisabledWysiwyg(id,"CODE");
			dojo.query(".wysiwyg-container").style({display:'none'});
			}
		else if(toggleValue=="PLAIN"){
			toPlainView(id);
			updateDisabledWysiwyg(id,"PLAIN");
			dojo.query(".wysiwyg-container").style({display:'block'});
			}
	}

	function updateDisabledWysiwyg(id,mode){

		//Updating the list of disabled wysiwyg list
		var elementWysiwyg = document.getElementById("disabledWysiwyg");
		var wysiwygValue = elementWysiwyg.value;
		var result = "";
		var existingInDisabledWysiwyg = false;
		if(mode == "WYSIWYG"){

			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if((wysiwygFieldVar == id) || (wysiwygFieldVar == id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>")){
						wysiwygFieldVar = "";
					}
					result += wysiwygFieldVar + ",";
				}
			}
		}else if(mode == "CODE"){

			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if(wysiwygFieldVar == id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>"){
						wysiwygFieldVar = id;
						existingInDisabledWysiwyg = true;
					}
					result += wysiwygFieldVar + ",";
				}
				if(!existingInDisabledWysiwyg)
					result += id;
			}else{
				result += id;
			}
		}else{// to PLAIN

			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++){
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if(wysiwygFieldVar == id){
						wysiwygFieldVar = id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
						existingInDisabledWysiwyg = true;
					}
					result += wysiwygFieldVar + ",";
				}
				if(!existingInDisabledWysiwyg)
					result += id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
			}else{
				result += id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
			}
		}
		elementWysiwyg.value = result;
	}

	function toPlainView(id) {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
			dojo.query('#'+id).style({display:''});
		}
		else if(enabledCodeAreas[id]){
			aceRemover(id);
		}
		document.getElementById(id).value = document.getElementById(id).value.trim();
	}

	function toCodeArea(id, isFullscreen = "false") {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
			dojo.query('#'+id).style({display:''});
		}
		if(!enabledCodeAreas[id]) {
			aceAreaById(id, isFullscreen);
		}
	}

	function toWYSIWYG(id, isFullscreen = "false") {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.switch.wysiwyg")) %>'))
        {
			if(enabledCodeAreas[id]){
				aceRemover(id);
			}
			enableWYSIWYG(id, false, isFullscreen);
		}
	}

	function emmitFieldDataChange(val) {
			var customEvent = document.createEvent("CustomEvent");
			customEvent.initCustomEvent("ng-event", false, false,  {
					name: "edit-contentlet-data-updated",
					payload: val
			});
			document.dispatchEvent(customEvent)
	}

	function insertAssetInEditor(dotAssets) {
		dotAssets.forEach(async (asset) => {
            const languageQuery = asset.languageId ? `&language=${asset.languageId}` : "";
			let results = await fetch(
				`/api/v1/content/resourcelinks?identifier=${asset.identifier}${languageQuery}`
			);
			results = await results.json();
            if (!results || !results.entity) {
                showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "an-unexpected-system-error-occurred")) %>');
                return;
            }

			const mimeWhiteList = [
				"image/jpeg",
				"image/gif",
				"image/webp",
				"image/tiff",
				"image/png",
			];
			const { mimeType, idPath, configuredImageURL } = results.entity[asset.titleImage];
			const image = `
					<img
					src="${configuredImageURL}"
					alt="${asset.title}"
					data-field-name="${asset.titleImage}"
					data-inode="${asset.inode}"
					data-identifier="${asset.identifier}"
					data-saveas="${asset.title}"
					/>`;

			const link = `<a href="${idPath}">${asset.title}</a>`;
			const assetToInsert = mimeWhiteList.includes(mimeType) ? image : link;
        	tinymce.execCommand("mceInsertContent", false, assetToInsert);
			setAssetDimensionsInEditor();

		});
	}

	function setAssetDimensionsInEditor () {
		const images = tinymce.activeEditor.contentDocument.querySelectorAll('img');
		images.forEach(image => {
			image.addEventListener('load', () => {
				if(!image.getAttribute('width')){
					image.setAttribute('width', image.naturalWidth);
					image.setAttribute('height', image.naturalHeight);
				}
			})
		});
	}

  var dropzoneEvents = false

  function bindDropZoneUploadComplete(activeEditor, textAreaId) {
        const dropZone = document.getElementById(`dot-asset-drop-zone-${textAreaId}`);
        if (dropZone && dropZone.dataset["disabled"] !== 'false') {
            dropZone.addEventListener('uploadComplete', async (asset) => {
                dropZone.style.pointerEvents = "none";
                asset.detail && insertAssetInEditor(asset.detail)
            })
            dropzoneEvents = true
        }
  }

	function enableWYSIWYG(textAreaId, confirmChange, isFullscreen = "false") {
		if (!isWYSIWYGEnabled(textAreaId)) {
			//Confirming the change
			if (confirmChange == true &&
				!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.switch.wysiwyg.view")) %>')) {
				return;
			}
		    <%if(request.getAttribute("contentlet")!=null){%>
			    <%final Context ctx = com.dotcms.rendering.velocity.util.VelocityUtil.getInstance().getWebContext(request, response);%>
			    <%ctx.put("content", request.getAttribute("contentlet"));%>
			    <%ctx.put("contentlet", request.getAttribute("contentlet"));%>
				<%for(com.dotcms.contenttype.model.field.Field field : ((Contentlet)request.getAttribute("contentlet")).getContentType().fields()){%>
				     <%if(field instanceof WysiwygField && field.fieldVariablesMap().containsKey("tinymceprops")){%>
					     var <%=field.variable()%>tinyPropOverride={};
					     try{
					    	   <%ctx.put("field", field);%>
					    	   <%
					    	     String propsOverride = com.dotcms.rendering.velocity.util.VelocityUtil.getInstance().parseVelocity(field.fieldVariablesMap().get("tinymceprops").value(),ctx);
					    	     if(StringUtils.isEmpty(propsOverride) || StringUtils.isBlank(propsOverride) ){
					    	        propsOverride = "{}";
					    	        //This is here to prevent a javascript syntax err
					    	     }
					    	   %>
					    	   <%=field.variable()%>tinyPropOverride =  <%=propsOverride%>;
					     }
					     catch(e){
					    	 showDotCMSErrorMessage("unable to initialize WYSIWYG " + e.message);
					     }

					 <%}else{%>
					         var <%=field.variable()%>tinyPropOverride =  tinyMCEProps;
				     <%}%>
				<%}%>
			<%}%>

			let tinyConf = eval(textAreaId + "tinyPropOverride");
			if(tinyConf.plugins != undefined && Array.isArray(tinyConf.plugins)){
			    for(i=0;i<tinyConf.plugins.length;i++){
			        tinyConf.plugins[i]=tinyConf.plugins[i].replace("compat3x","");
			    }
			}else if(tinyConf.plugins != undefined ){
			    tinyConf.plugins=tinyConf.plugins.replace("compat3x","");
			}
			//Enabling the wysiwyg
			try {
			  // Init instance callback to fix the pointer-events issue.
			  tinyConf = {
			    ...tinyConf,
                height: isFullscreen === "true" ? "100%" : 332,
			    init_instance_callback: (editor) => {
			      let dropZone = document.getElementById(
			        `dot-asset-drop-zone-${textAreaId}`
			      );

									if(isFullscreen === "true"){

										editor.editorContainer.style.height = "100%";
									}

                  if (dropZone) {
                    editor.on("dragover", function (e) {
                        const { kind } = Array.from(e.dataTransfer.items)[0];
                        if (kind === 'file') {
                            dropZone.style.pointerEvents = "all";
                        }
                    });

                    editor.on("ExecCommand", function (e) {
                        if (e.command === 'mceFullScreen'){
                            if (dropZone.style.position === '') {
                                dropZone.style.position = 'fixed';
                                dropZone.style.zIndex = '999';
                            } else {
                                dropZone.style.position = '';
                                dropZone.style.zIndex = '';
                            }
                        }

                    });

                    editor.dom.bind(document, "dragleave", function (e) {
                        dropZone.style.pointerEvents = "none";
                        return false;
                    });
                  }

                }
			  };
			  var wellTinyMCE = new tinymce.Editor(
			    textAreaId,
			    tinyConf,
			    tinymce.EditorManager
			  );
			  if (!dropzoneEvents) {
			    bindDropZoneUploadComplete(tinymce, textAreaId);
			  }
			  wellTinyMCE.render();
			  wellTinyMCE.on("change", emmitFieldDataChange);
			} catch (e) {
			  showDotCMSErrorMessage("Enable to initialize WYSIWYG " + e.message);
			}
		dojo.query(".wysiwyg-container").style({display:'block'});
			enabledWYSIWYG[textAreaId] = true;
		}
	}

	function disableWYSIWYG(textAreaId) {
		if(isWYSIWYGEnabled(textAreaId)) {
			//Disabling the control
			tinymce.EditorManager.get(textAreaId).remove();
			enabledWYSIWYG[textAreaId] = false;
		}
	}

	function isWYSIWYGEnabled(id) {
		return (enabledWYSIWYG[id]);
	}

	//WYSIWYG special functions
	function cmsURLConverter (url, node, on_save) {
		var idx = url.indexOf('#');
		var servername = "http://<%= request.getServerName() %>";
		var start = url.substring(0, servername.length);
		var returl = "";
		if (idx >= 0 && start == servername) {
			returl = url.substring(idx, url.length);
		} else {
			returl = url;
		}
		return returl;
	}

	var wysiwyg_field_name;
	var wysiwyg_url;
	var wysiwyg_type;
	var wysiwyg_win;
	var tinyMCEFilePickerCallback;

	function cmsFileBrowser(callback, value, meta) {
		tinyMCEFilePickerCallback=callback;

		if (meta.filetype=="image") {
            dojo.query('.mce-window .mce-close')[0].click();
            cmsFileBrowserImage.show();
        } else {
            cmsFileBrowserFile.show()
            dojo.style(dojo.query('.mce-window')[0], { zIndex: '100' })
		    dojo.style(dojo.byId('mce-modal-block'), { zIndex: '90' })
        }

	}

	//Glossary terms search

	var glossaryTermId;
	var lastGlossarySearch;

	function lookupGlossaryTerm(textAreaId, language) {

        glossaryTermId = textAreaId;


		var gt = dojo.byId("glossary_term_" + textAreaId);
		var toSearch = dojo.byId("glossary_term_" + textAreaId).value;
	    var menu = dojo.byId("glossary_term_popup_" + textAreaId);
	    //var gtCoords = dojo.coords(gt, true);

	    if (toSearch != "" && lastGlossarySearch != toSearch) {
	    	lastGlossarySearch = toSearch;
	    	dojo.byId("glossary_term_table_" + textAreaId).innerHTML = '';
	        ContentletAjax.doSearchGlossaryTerm(lastGlossarySearch, language, lookupGlossaryTermReply);
	    }
	}

	function lookupGlossaryTermReply(data) {

	    if (!data) {
	    	return;
	    }

		var glossaryTermsTable = dojo.byId("glossary_term_table_" + glossaryTermId);

		var strHTML = "";

		if (data.length > 0) {
			strHTML = '<table class="listingTable" style="width: 300px;">' +
				'<tr><th><%= LanguageUtil.get(pageContext, "Term") %></th>' +
				'<th><%= LanguageUtil.get(pageContext, "Definition") %></th>' +
				'<th><a href="javascript: clearGlossaryTerms();">X</a></th></tr>';
			var className = 'alternate_1';
		     for(var loop = 0; loop < data.length; loop++) {
		    	if(className == 'alternate_1')
		    		className = 'alternate_2';
		    	else
		    		className = 'alternate_1';
	            option = data[loop][0];
	            value = data[loop][1];
	            strHTML += '<tr class="' + className + '">' +
	        	        '<td><a href="javascript: addGlossaryTerm(\'' + option + '\');">' + option + '</a></td>' +
	        	        '<td colspan="2">' + value + '</td></tr>';
	          }
	        strHTML += "</tbody></table>";
	    } else {
		    strHTML = "<table border='0' cellpadding='4' cellspacing='0' class='beta glosaryTermsTable'><tbody><tr class=\"beta\"><td class=\"beta\"><%= LanguageUtil.get(pageContext, "There-are-no-dictionary-terms-that-matched-your-search") %></td></tr></tbody></table>";
	    }

	    dojo.byId("glossary_term_table_" + glossaryTermId).innerHTML = strHTML;

	    dojo.style("glossary_term_popup_" + glossaryTermId, { display: "" });

	}

	function clearGlossaryTermsDelayed() {
		setTimeout('clearGlossaryTerms()', 500);
	}

	function clearGlossaryTerms() {
		if(document.getElementById("glossary_term_popup_" + glossaryTermId)){
			document.getElementById("glossary_term_popup_" + glossaryTermId).style.display = "none";
		}
		if(document.getElementById("glossary_term_" + glossaryTermId)){
			document.getElementById("glossary_term_" + glossaryTermId).value = "";
		}
		dwr.util.removeAllRows("glossary_term_table_" + glossaryTermId);
	}

	function addGlossaryTerm(option) {
		var insertValue = "$text.get('" + option + "')";
		if(enabledWYSIWYG[glossaryTermId]) {
			tinymce.EditorManager.get(glossaryTermId).selection.setContent(insertValue);
		} else if (aceEditors[glossaryTermId]) {
			aceEditors[glossaryTermId].insert(insertValue);
		} else if(enabledCodeAreas[glossaryTermId]){
			textEditor[glossaryTermId].insert(insertValue);
		} else {
			var txtArea = document.getElementById(glossaryTermId);
			txtArea.value = txtArea.value.substring(0, txtArea.selectionStart) + insertValue + txtArea.value.substring(txtArea.selectionEnd, txtArea.value.length);
            txtArea.focus();
            txtArea.selectionStart = txtArea.selectionStart + insertValue.length;
            txtArea.selectionEnd = txtArea.selectionStart + insertValue.length;
		}
		clearGlossaryTerms();
	}

	//Links kind of fields
	function popupEditLink(inode, varName) {
	    editlinkwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /><portlet:param name="popup" value="1" /></portlet:actionURL>&inode=' + inode + '&child=true&page_width=650', "editlinkwin", 'width=700,height=400,scrollbars=yes,resizable=yes');
	}

	//Other functions
	function popUpMacroHelp(){
		openwin = window.open('http://dotcms.com/docs/latest/CreatingMacros',"newin","menubar=1,width=1100,height=750,scrollbars=1,toolbar=1,status=0,resizable=1");
	}

	function updateHostFolderValues(field){
	  if(!isInodeSet(dijit.byId('HostSelector').attr('value'))){
		 dojo.byId(field).value = "";
		 dojo.byId('hostId').value = "";
		 dojo.byId('folderInode').value = "";
	  }else{
		 var data = dijit.byId('HostSelector').attr('selectedItem');
		 if(data["type"]== "host"){
			dojo.byId(field).value =  dijit.byId('HostSelector').attr('value');
			dojo.byId(field).dataset.hostType =  data["type"];
			dojo.byId('hostId').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folderInode').value = "";

		 }else if(data["type"]== "folder"){
			dojo.byId(field).value =  dijit.byId('HostSelector').attr('value');
			dojo.byId(field).dataset.hostType =  data["type"];
			dojo.byId('folderInode').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('hostId').dataset.hostType = data["type"];
			dojo.byId('hostId').value = "";
		}
	  }
	}

	function setDotAssetHost() {
		const contentHost = dojo.byId("contentHost");
		const dropZoneComponents = Array.from(
			document.querySelectorAll(".wysiwyg__dot-asset-drop-zone")
		);
		dropZoneComponents.forEach((dropZone) => {
			if (
				contentHost.dataset.hostType === "host" && contentHost.value !== "SYSTEM_HOST" ||
				contentHost.dataset.hostType === "folder"
			) {
				dropZone["folder"] = contentHost.value;
			} else {
				dropZone["folder"] = ""
			}
		});
	}

	function aceAreaById(textarea, isFullscreen = "false"){
		document.getElementById(textarea).style.display = "none";
		var id = document.getElementById(textarea).value.trim();
		aceEditors[textarea] = document.getElementById(textarea+'aceEditor');
		var aceClass = aceEditors[textarea].className;
		aceEditors[textarea].className = aceClass.replace('classAce', 'aceText');
		aceEditors[textarea].style.display = 'block';
		aceEditors[textarea].style.height = '100%';
		aceEditors[textarea] = ace.edit(textarea+'aceEditor');
	    aceEditors[textarea].setTheme("ace/theme/textmate");
	    aceEditors[textarea].getSession().setMode("ace/mode/velocity");
	    aceEditors[textarea].getSession().setUseWrapMode(true);
	    aceEditors[textarea].setValue(id);

		if(isFullscreen === "false") {
	    aceEditors[textarea].setOptions({
		    minLines: 25,
		    maxLines:40
	    });
		}


    	aceEditors[textarea].clearSelection();
		enabledCodeAreas[textarea]=true;
		aceEditors[textarea].on("change", function(){
			document.getElementById(textarea).value = aceEditors[textarea].getValue();
		})
	}

	function aceRemover(textarea){
		var editorText = aceEditors[textarea].getValue();
		var aceEditor = document.getElementById(textarea+'aceEditor');
		var aceClass = aceEditor.className;
		aceEditor.className = aceClass.replace('aceText', 'classAce');
		aceEditor.style.display = 'none';
		dojo.query('#'+textarea).style({display:''});
		dojo.query('#'+textarea)[0].value = editorText;
		enabledCodeAreas[textarea]=false;
		aceEditors[textarea] = null;
	}

	function addFileImageCallback(file) {
		var pattern = "<%=Config.getStringProperty("WYSIWYG_IMAGE_URL_PATTERN", "/dA/{path}{name}?language_id={languageId}")%>";
        var assetURI = replaceUrlPattern(pattern, file);
        insertAssetInEditor([file])
	}


	function replaceUrlPattern(pattern, file){

	     return pattern
          .replace(/{name}/g        ,file.fileName)
          .replace(/{fileName}/g        ,file.fileName)
          .replace(/{path}/g        ,file.path)
          .replace(/{extension}/g   ,file.extension)
          .replace(/{languageId}/g   ,file.languageId)
          .replace(/{hostname}/g    ,file.hostName)
          .replace(/{hostName}/g    ,file.hostName)
          .replace(/{inode}/g       ,file.inode)
          .replace(/{hostId}/g      ,file.host)
          .replace(/{identifier}/g  ,file.identifier)
          .replace(/{id}/g          ,file.identifier)
          .replace(/{shortyInode}/g ,file.inode.replace("-", "").substring(0, 10))
          .replace(/{shortyId}/g    ,file.identifier.replace("-", "").substring(0, 10))
          ;

	}

	function addFileCallback(file) {
		var ident, assetURI;
		var ext = file.extension;
		var ident = file.identifier;
		var fileExt = getFileExtension(file.name).toString();
		<% String extension = com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"); %>
		if (fileExt == "<%= extension %>" || ext == "page") {
			assetURI = file.pageURI;
		} else if (file.baseType === "FILEASSET") {
			assetURI = `${file.path}`;
		} else {
			assetURI = `/dA/${file.path.split(".")[1]}/${file.name}`;
		}

		tinyMCEFilePickerCallback(assetURI, {});
	}

	function replaceAll(find, replace, str) {
  		return str.replace(new RegExp(find, 'g'), replace);
	}

	function addKVPair(fieldId, fieldValueId){
		var node = dojo.byId(fieldId+'_kvtable');
		var key = dijit.byId(fieldValueId+'_key').value;
		if(key===''){
			 alert('<%= LanguageUtil.get(pageContext, "empty-key") %>');
		}else{
		key = key.trim();
		//escape double quotes
		key = replaceAll('"', '&#x22;', key);
		var value = dijit.byId(fieldValueId+'_value').value;
		//escape double quotes
		value = replaceAll('"', '&#x22;', value);
		var table = document.getElementById(fieldId+'_kvtable');
		var row = document.getElementById(fieldId+'_'+key);
		var trs = table.getElementsByTagName('tr');
		var duplicateKey = false;
		for(var i=0;i<trs.length;i++){
			if(trs[i].id.toUpperCase() == (fieldId+'_'+key).toUpperCase()){
				duplicateKey = true;
				break;
			}
		}
		if(duplicateKey){
			 alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "key-already-exists")) %>');
		}else{
			var newRow = table.insertRow(table.rows.length);
			newRow.id = fieldId+'_'+key;
			if((table.rows.length%2)==0){
		        newRow.className = "dojoDndItem alternate_2";
			}else{
				newRow.className = "dojoDndItem alternate_1";
			}
			var cell0 = newRow.insertCell(0);
			var cell1 = newRow.insertCell(1);
			var cell2 = newRow.insertCell(2);
			var anchor = document.createElement("a");
			cell0.width = "6%";
			anchor.href= 'javascript:javascript:deleteKVPair('+'"'+ fieldId +'"'+','+'"'+ fieldValueId +'"'+', '+'"'+ key +'"'+');';
			anchor.innerHTML = '<span class="deleteIcon"></span>';
			cell0.appendChild(anchor);
			cell1.innerHTML = key;
			cell1.width="34%";
			cell2.innerHTML = value;
			cell2.width="60%";
			var input = document.createElement("input");
			input.type="hidden";
			input.id=newRow.id+"_k";
			if(key!=null && key!='') {
				key = key.replace("\"","\\\"");
			}
			input.value=key;
			table.appendChild(input);
		    input = document.createElement("input");
			input.type="hidden";
			input.id=newRow.id+"_v";
			if(value!=null && value!='') {
				value = value.replace("\"","\\\"");
			}
			input.value=value;
			table.appendChild(input);
			var dndTable = new dojo.dnd.Source(node);
			dojo.connect(dndTable, "insertNodes", function(){
				 setKVValue(fieldId, fieldValueId);
		    	 recolorTable(fieldId);
			 });
			var row1 = document.getElementById(fieldId+'_'+key);
			document.getElementById(row1.id+'_v').value = value;
			setKVValue(fieldId, fieldValueId);
			dijit.byId(fieldValueId+'_key').reset();
			dijit.byId(fieldValueId+'_value').reset();

		}
	  }
	}


	function deleteKVPair(fieldId, fieldValueId, key){

		var table = document.getElementById(fieldId+'_kvtable');
		var row = document.getElementById(fieldId+'_'+key);
			if(row){
				try {
					 var rowCount = table.rows.length;
					 for(var i=0; i<rowCount; i++) {
						if(row.id==table.rows[i].id) {

							table.deleteRow(i);

							rowCount--;
							i--;
						}
					 }
					 recolorTable(fieldId);
				 }catch(e) {}
			}
			setKVValue(fieldId, fieldValueId);
	}


	function recolorTable(fieldId){
		var table = document.getElementById(fieldId+'_kvtable');
		if(table){
			var rowCount = table.rows.length;
			for(var i=0; i<rowCount; i++) {
				 var node = dojo.byId(table.rows[i].id);
				 dojo.removeClass(node, 'alternate_1');
	             dojo.removeClass(node, 'alternate_2');
				 if( (i % 2) != 0 ){
		             dojo.addClass(node, 'alternate_2');
		         }else{
		             dojo.addClass(node, 'alternate_1');
		         }
			 }
		}
	}

	function setKVValue(fieldId, fieldValueId){
		var rowCount;
		var jsonStr="";
		var table = document.getElementById(fieldId+'_kvtable');
		 if(table){
			 jsonStr = "{";
			 rowCount= table.rows.length;
			 for(var i=0; i<rowCount; i++) {
					var rowId = table.rows[i].id;
					var key = document.getElementById(rowId+'_k').value;
					var value = document.getElementById(rowId+'_v').value;
					key = replaceAll('"', '&#x22;', key);
					value = replaceAll('"', '&#x22;', value);
					jsonStr+= '"' + key + '"' + ":" + '"' + value + '"' + (i!=rowCount-1?",":"");
				}
			 jsonStr+="}";
		 }
		 var keyfieldId = document.getElementsByClassName(fieldValueId);
		 for (var i = 0; i < keyfieldId.length; ++i) {
		     keyfieldId[i].value= jsonStr;
		 }
	}


	function editText(inode) {
		editTextManager.editText(inode);
	}

	var textEditor = new Array();
	var aceTextId = new Array();
	function aceText(textarea,keyValue,isWidget, isFullscreen = "false") {
		var elementWysiwyg = document.getElementById("disabledWysiwyg");
		var wysiwygValue = elementWysiwyg.value;
		var result = "";
		var toggleEditor = textarea+"<%=com.dotmarketing.util.Constants.TOGGLE_EDITOR_SEPARATOR%>";
		var wysiwygValueArray = wysiwygValue.split(",");
		if(document.getElementById('aceTextArea_'+textarea).style.position != 'relative'){
			document.getElementById('aceTextArea_'+textarea).style.position='relative';
			document.getElementById('aceTextArea_'+textarea).style.height='100%'; // This gets overrided when setting the min and max lines
			textEditor[textarea] = ace.edit('aceTextArea_'+textarea);
			//textEditor[textarea].setTheme("ace/theme/textmate");
			textEditor[textarea].getSession().setMode("ace/mode/"+keyValue);
			textEditor[textarea].getSession().setUseWrapMode(true);

				if(isFullscreen !== "true"){
					textEditor[textarea].setOptions({
										minLines: 15,
										maxLines:35
								});
				}

			 aceTextId[textarea] = textarea;
		}
    	dijit.byId("toggleEditor_"+textarea).disabled=true;
		var acetId = document.getElementById('aceTextArea_'+textarea);
		var aceClass = acetId.className;
		if (dijit.byId("toggleEditor_"+textarea).checked) {
			document.getElementById(textarea).style.display = "none";
			if(isWidget == 'true')
				acetId.className = aceClass.replace('classAce', 'aceText');
			else
				acetId.className = aceClass.replace('classAce', 'aceText');

			textEditor[textarea].setValue(document.getElementById(textarea).value);
			textEditor[textarea].clearSelection();
			enabledCodeAreas[textarea]=true;
			if(wysiwygValue != ""){
				for(i = 0;i < wysiwygValueArray.length;i++){
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if((wysiwygFieldVar == textarea) || (wysiwygFieldVar == toggleEditor))
						continue;
					else
						result += wysiwygFieldVar + ",";
				}
			}
			result += toggleEditor + ",";
		} else {
			var editorText = textEditor[textarea].getValue();
			if(isWidget == 'true')
				acetId.className = aceClass.replace('aceText', 'classAce');
			else
				acetId.className = aceClass.replace('aceText', 'classAce');

			document.getElementById(textarea).style.display = "inline";
			document.getElementById(textarea).value = editorText;
			textEditor[textarea].setValue("");
			enabledCodeAreas[textarea]=false;
			if(wysiwygValue != ""){
				for(i = 0;i < wysiwygValueArray.length;i++){
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if((wysiwygFieldVar == textarea) || (wysiwygFieldVar == toggleEditor))
						continue;
					else
						result += wysiwygFieldVar + ",";
				}
			}
		}
		dijit.byId("toggleEditor_"+textarea).disabled=false;
		elementWysiwyg.value = result;
	}
</script>
<jsp:include page="/html/portlet/ext/files/edit_text_inc.jsp"/>
