<%@ page import="java.io.FileNotFoundException" %>
<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.LanguageKey"%>

<%@ include file="/html/portlet/ext/languagesmanager/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp"%>
<%	
	List<Language> languages = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LIST);
	Language language = (Language) request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LANGUAGE);
	
	StringBuilder buff = new StringBuilder();
	buff.append("{identifier:'id', label:'label',imageurl:'imageurl',items:[");

    boolean first=true;
    for (Language lang : languages) {
        if(!first) buff.append(","); else first=false;
        final String ccode= LanguageUtil.getLiteralLocale(lang.getLanguageCode(), lang.getCountryCode());
        final String imgURL="/html/images/languages/"+ccode+".gif";
        final String display=lang.getLanguage() + (UtilMethods.isSet(lang.getCountry()) ? " - " + lang.getCountry().trim() : "");
        buff.append("{");
        buff.append("id:'" + lang.getId() + "',");
        buff.append("label:'<span style=\"background-image:url("+imgURL+");width:16px;height:11px;display:inline-block;vertical-align:middle\"></span> "+display+"',");
        buff.append("imageurl:'"+imgURL+"',");
        buff.append("lang:'"+display+"'");
        buff.append("}");
    }
    buff.append("]}");
%>



<%@page import="com.dotmarketing.util.UtilMethods"%>
<script  type="text/javascript" src="/html/portlet/ext/languagesmanager/languages_ext.js"></script>
<script type='text/javascript' src='/dwr/interface/LanguageAjax.js'></script>

<script type="text/javascript">

	var currentIndex = 0;
	var alternate = 'alternate_1';
	var currentLanguage = '<%= language.getLanguageCode() %>';
	var currentCountry = '<%= language.getCountryCode() %>';
	var dirty = false;
	var separator = "<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>";
	var keysToAdd = new Array();
	var keysToUpdate = new Array();	
	var keysToDelete = new Array();
	var currentPage = 1;
	
	function changeLanguage(selected) {
		if(selected != <%= language.getId() %>) {
			var form = $('fm');
			if(dirty && !confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.languagemanager.abandon.applied.changes")) %>'))
				return;
			form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /></portlet:actionURL>';
			form.cmd.value = 'edit';
			form.id.value = selected;
			form.submit();
		}
	}

	function addNewProperty() {
		if(currentIndex == 0){
			if(Ext.get('noprops') != null)
				Ext.get('noprops').remove();
			}
			
		alternate = (alternate == 'alternate_1'?'alternate_2':'alternate_1');		
		
		var buffer = '<tr class="' + alternate + '" id="row-' + currentIndex + '">';

		buffer += '<td width="6%" align="center"><input type="hidden" id="' + currentLanguage + '-' + currentIndex + '-new" name="' + currentLanguage + '-' + currentIndex + '-new" value="true"/>' +
			'<input dojoType="dijit.form.CheckBox" id="' + currentLanguage + '-' + currentIndex + '-remove" ' +
			'type="checkbox" name="' + currentLanguage + '-' + currentIndex + '-remove" value=""/></td>';

		buffer += '<td width="14%"><input id="' + currentLanguage + '-' + currentIndex + '-key" onchange="keyChanged(' + currentIndex + ');refreshAddKeys(' + currentIndex + ');" ' +
			'type="text" dojoType="dijit.form.TextBox" style="width: 96%" name="' + currentLanguage + '-' + currentIndex + '-key" value=""/></td>';

		buffer += '<td width="40%"><input id="' + currentLanguage + '-general-' + currentIndex + '-value"  onchange="generalValueChanged(' + currentIndex + ');refreshAddKeys(' + currentIndex + ');"  ' +
			'type="text" dojoType="dijit.form.TextBox" style="width: 98%" name="' + currentLanguage + '-general-' + currentIndex + '-value" value=""/></td>';
			
		buffer += '<td width="40%"><input id="' + currentLanguage + '-' + currentCountry + '-' + currentIndex + '-value"  onchange="languageValueChanged(' + currentIndex + ');refreshAddKeys(' + currentIndex + ');"  ' +
			'type="text" dojoType="dijit.form.TextBox" style="width: 98%" name="' + currentLanguage + '-' + currentCountry + '-' + currentIndex + '-value" value=""/></td>';

		buffer += '</tr>';

		Ext.get('propertiesTable').insertHtml('beforeEnd', buffer);

		Ext.get(currentLanguage + '-' + currentIndex + '-key').focus();

		dojo.parser.parse([dojo.byId('propertiesTable')]);

		currentIndex++;
	}

	function keyChanged (id) {
		dirty = true;
	}

	function generalValueChanged (id) {
		dirty = true;
	}

	function languageValueChanged (id) {
		dirty = true;
	}

	function discardChanges (form) {
		var form = $('fm');
		if(dirty && !confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.languagemanager.abandon.applied.changes")) %>'))
			return;
		form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /></portlet:actionURL>';
		form.cmd.value = 'edit';
		form.id.value = '<%= language.getId() %>';
		form.submit();
	}

	function filterResults() {
		viewLanguageKeys();
	}

	function clearFilter() {
		Ext.get('filter').dom.value = "";
		viewLanguageKeys();
	}

	function doSubmit() {

		for(i = 0; i < currentIndex; i++) {
			var key = Ext.get(currentLanguage + '-' + i + '-key').dom.value;
			var isNew = Ext.get(currentLanguage + '-' + i + '-new') != null?Ext.get(currentLanguage + '-' + i + '-new').dom.value == 'true':false;
			if(isNew) {
				for(k = 0; k < currentIndex; k++) {
					var key2 = Ext.get(currentLanguage + '-' + k + '-key').dom.value;
					if(key == key2 && k != i) {
						alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.languagemanager.key.already.registered")) %>');
						Ext.get(currentLanguage + '-' + i + '-key').dom.focus();
						return;
					}
				}
			}

			var regex = new RegExp("^[A-Za-z0-9-_\.]+$");
			if(isNew && !regex.test(key)) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.languagemanager.key.invalid")) %>');
				Ext.get(currentLanguage + '-' + i + '-key').dom.focus();
				return;
			}
		}
		
		for(var i=0; i<keysToAdd.length; i++){
			var langKey = dojo.attr(currentLanguage + '-' + keysToAdd[i] + '-key','value');
			var generalValue = dojo.attr(currentLanguage + '-general-' + keysToAdd[i] + '-value','value');
			var specificValue = dojo.attr(currentLanguage + '-' + currentCountry + '-' + keysToAdd[i] + '-value','value');						
			keysToAdd[i] = langKey+separator+generalValue+separator+specificValue;
		}

		for(var j=0; j<keysToUpdate.length; j++){
			var langKey = dojo.attr(currentLanguage + '-' + keysToUpdate[j] + '-key','value');
			var generalValue = dojo.attr(currentLanguage + '-general-' + keysToUpdate[j] + '-value','value');
			var specificValue = dojo.attr(currentLanguage + '-' + currentCountry + '-' + keysToUpdate[j] + '-value','value');						
			keysToUpdate[j] = langKey+separator+generalValue+separator+specificValue;
		}

		for(var k = 0; k < currentIndex; k++){
			var removeKeyCheck = document.getElementById(currentLanguage + '-' + k + '-remove');
			if(removeKeyCheck.checked == true){
				keysToDelete[keysToDelete.length] = document.getElementById(currentLanguage + '-' + k + '-key').value;
			}
		}

		dijit.byId('savingKeysDialog').show();
		LanguageAjax.saveKeys(currentLanguage,currentCountry,keysToAdd,keysToUpdate,keysToDelete,saveKeysCallback);
		keysToUpdate.length = 0;
        keysToAdd.length = 0;
	}

	function saveKeysCallback(data){
		dijit.byId('savingKeysDialog').hide();
		viewLanguageKeys(currentPage);
	}

	function cancelEdit(form) {
		
		if(dirty && !confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.languagemanager.abandon.applied.changes")) %>'))
			return;
		
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>';
	}


	function displayKeyAltText(obj, text) {
		if(text.length >= 20) {
			var obj = Ext.get(obj);
			Ext.get('altText').moveTo(obj.getLeft() + 20, obj.getTop() + 20);
			Ext.get('altText').update(text);
			Ext.get('altText').show();
			setTimeout('hideKeyAltText()', 2000);
		}
	}

	function hideKeyAltText() {
		if(Ext.get('altText') != null)
			Ext.get('altText').hide();
	}
	
	//Layout Initialization
	function  resizeBrowser(){
		var viewport = dijit.getViewport();
		var viewport_height = viewport.h;

		var  e =  dojo.byId("borderContainer");
		dojo.style(e, "height", viewport_height -290+ "px");
	}

	var cellFuncs = [
		function(data) {
			var returnStr = "";			
			returnStr += "<input type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" "
				+ " id=\"<%= language.getLanguageCode() %>-" + data['idx'] + "-remove\" "
				+ " name=\"<%= language.getLanguageCode() %>-" + data['idx'] + "-remove\" "
				+ " value=\"\"/>";				
			return returnStr; 
		},
		
		function(data) {
			var returnStr = "";
			returnStr += "<input type=\"hidden\" "
				+ " id=\"<%= language.getLanguageCode() %>-" + data['idx'] + "-key\"  "
				+ " name=\"<%= language.getLanguageCode() %>-" + data['idx'] + "-key\" "
				+ " value=\"" + data['key'] + "\"/>"
				+ " <input type=\"text\" dojoType=\"dijit.form.TextBox\" style=\"width:200px;\" "
				+ " onmouseover=\"displayKeyAltText(this, '" + data['key'] + "')\" "
				+ " readonly=\"readonly\" "
				+ " value=\"" + data['key'] + "\"/>";
			return returnStr; 
		},
								
		function(data) {
			var returnString = "";
			returnString += "<input type=\"text\" dojoType=\"dijit.form.TextBox\" style=\"width:300px;\" "
				+ " id=\"<%= language.getLanguageCode() %>-general-" + data['idx'] + "-value\" "
				+ " onchange=\"generalValueChanged(" + data['idx'] + ");refreshKeysToUpdate(" + data['idx'] + ");\""
				+ " name=\"<%= language.getLanguageCode() %>-general-" + data['idx'] + "-value\" "
				+ " value=\"" + data['generalValue'] + "\"/>";
			return returnString;
		},
		
		function(data) {
			count++;			 
			var returnStr1 = "";
			returnStr1 += "<input type=\"text\" dojoType=\"dijit.form.TextBox\" style=\"width:300px;\" "
				+ " id=\"<%= language.getLanguageCode() %>-<%= language.getCountryCode() %>-" + data['idx'] + "-value\" "
				+ " onchange=\"languageValueChanged(" + data['idx'] + ");refreshKeysToUpdate(" + data['idx'] + ");\""
				+ " name=\"<%= language.getLanguageCode() %>-<%= language.getCountryCode() %>-" + data['idx'] + "-value\" "
				+ " value=\"" + data['specificValue'] + "\"/>";
						
			alternate = data['alternate'];
			currentIndex = data['idx'];
			currentIndex++;			
			return returnStr1;
		}
];

	var loadProgressToggle;
	function viewLanguageKeys(page){
		
		if (page == null)
			currentPage = 1;
		else 
			currentPage = page;
		
		var filter = Ext.get('filter').dom.value;
		
		
		loadProgressToggle.show();
		LanguageAjax.getPaginatedLanguageKeys('<%=language.getLanguageCode()%>',
				                              '<%=language.getCountryCode()%>',
				                              currentPage,filter,viewLanguageKeysCallback);
		
	}

	
	function viewLanguageKeysCallback(data){
		if(data.length > 0){

			var widgets = dijit.findWidgets(dojo.byId('propertiesTable'));
			if (widgets.length) {
				dojo.forEach(widgets, function(w) {
					w.destroyRecursive();
				});
			}

			var counters = data[0];
			var hasNext = counters["hasNext"];
			var hasPrevious = counters["hasPrevious"];
			var total = counters["total"];
			var begin = counters["begin"];
			var end = counters["end"];
	        var totalPages = counters["totalPages"];		

			showMatchingResults (total,begin,end,totalPages);

			if (hasNext) {
	 			document.getElementById("nextDiv").style.display = "";
	 		} else {
	 			document.getElementById("nextDiv").style.display = "none";
	 		}
			
			if (hasPrevious) {
	 			document.getElementById("previousDiv").style.display = "";
	 		} else {
	 			document.getElementById("previousDiv").style.display = "none";
	 		}
				
			for (var i = 1; i < data.length; i++) {
				data[i - 1] = data[i];
			}

			data.length = data.length - 1;
								
			dwr.util.removeAllRows('propertiesTable');
			if(data.length > 0){
				dwr.util.addRows( "propertiesTable",data, cellFuncs, { 
					rowCreator:function(options) {
				    var row = document.createElement("tr");
				    row.id = "row-"+data[options.rowIndex]['idx'];
				    return row;
				  },
				  cellCreator:function(options) {
					var td = document.createElement("td");
					if(options.cellNum == 0){
						td.align = "center";
					}
					return td;
				  },
				  escapeHtml:false });
				}else{
					currentIndex = 0;
				}
			keysToAdd.length = 0;
			keysToUpdate.length = 0; 
			keysToDelete.length = 0;
		}
		loadProgressToggle.hide();

		dojo.parser.parse([dojo.byId('propertiesTable')]);
	}

	function showMatchingResults (num,begin,end,totalPages) {
	    
	    eval("totalContents=" + num + ";");
	    
		//Bottom Matching Results
		var div = document.getElementById("matchingResultsBottomDiv")
			var strbuff = "<table border='0' width=\"100%\"><tr><td align='center'><b><%= LanguageUtil.get(pageContext, "showing") %> " + begin + " - " + end + " <%= LanguageUtil.get(pageContext, "of1") %> " + num;
		if(num > 0)
		{
			strbuff += " | <%= LanguageUtil.get(pageContext, "pages") %> ";
			for(i = 4;i >= 1;i--)
			{
				var auxPage = currentPage - i;
				if(auxPage >= 1)
				{
					strbuff += "<a href='javascript:viewLanguageKeys (" + auxPage + ");'> " + auxPage + " </a>";
				}				
			}
			strbuff += " " + currentPage + " ";
			for(i = 1;i <= 4;i++)
			{
				var auxPage = currentPage + i;
				if(auxPage <= totalPages)
				{
					strbuff += "<a href='javascript:viewLanguageKeys(" + auxPage + ");'> " + auxPage + " </a>";
				}				
			}
		}
		strbuff += "</b></td></tr></table>";
			div.innerHTML = strbuff;
	}

	function nextPage () {
		viewLanguageKeys(currentPage + 1);
	}

	function previousPage () {
		viewLanguageKeys(currentPage - 1);
	}
	
	function refreshAddKeys(idx){
		var exists = false;
		for(var index = 0; index < keysToAdd.length; index++){
			if(keysToAdd[index] == idx)
				exists = true;			
		}
		if(!exists)
			keysToAdd[keysToAdd.length] = idx;
	}

	function refreshKeysToUpdate(idx){
		var exists = false;
		for(var index = 0;index < keysToUpdate.length; index++){
			if(keysToUpdate[index] == idx)
				exists = true;
		}
		if(!exists)
			keysToUpdate[keysToUpdate.length] = idx;
	}

	function refreshKeysToDelete(idx){
		var exists = false;
		for(var index = 0;index < keysToUpdate.length; index++){
			if(keysToUpdate[index] == idx)
				exists = true;
		}
		if(!exists)
			keysToUpdate[keysToUpdate.length] = idx;
	}	

// need the timeout for back buttons

	dojo.addOnLoad(resizeBrowser);
	dojo.connect(window, "onresize", this, "resizeBrowser");
	dojo.addOnLoad (function(){
		loadProgressToggle=new dojo.fx.Toggler({node:'loadProgress'});
		loadProgressToggle.hide();
		viewLanguageKeys(1);
	});
	
</script>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Editing-Language-Variables")) %>' />
<html:form action="/ext/languages_manager/edit_language_keys" styleId="fm">
	<input type="hidden" name="id" value="<%= language.getId() %>">
	<input type="hidden" name="cmd" value="save">
	<input type="hidden" name="referer" value="<%= request.getParameter("referer") %>">
    
<div class="portlet-main">
	<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			
			<div id="languagesCombo" style="width:200px; ">
	            <input type="text" id='languagesComboSelect'>
	        </div>
        
			<script type="text/javascript">
				function updateSelectBoxImage(myselect) {
	                var imagestyle = "url('" + myselect.item.imageurl + "')";
	                var selField = dojo.query('#languagesCombo div.dijitInputField')[0];
	                dojo.style(selField, "backgroundImage", imagestyle);
	                dojo.style(selField, "backgroundRepeat", "no-repeat");
	                dojo.style(selField, "padding", "0px 0px 0px 20px");
	                dojo.style(selField, "backgroundColor", "transparent");
	                dojo.style(selField, "backgroundPosition", "0px 8px");
	            }
	
	            dojo.addOnLoad(
	              function() {
	                var storeData=<%=buff.toString()%>;
	                var langStore = new dojo.data.ItemFileReadStore({data: storeData});
	                var myselect = new dijit.form.FilteringSelect({
	                         id: "languagesComboSelect",
	                         name: "lang",
	                         value: '',
	                         required: true,
	                         store: langStore,
	                         searchAttr: "lang",
	                         labelAttr: "label",
	                         labelType: "html",
	                         onChange: function() { changeLanguage(dijit.byId("languagesComboSelect").item.id) },
	                         labelFunc: function(item, store) { return store.getValue(item, "label"); }
	                    },
	                    dojo.byId("languagesComboSelect"));
	
	                myselect.setValue('<%=language.getId()%>');
	                updateSelectBoxImage(myselect);
	                });
			</script>
		</div>
		<div class="portlet-toolbar__info">
			<div class="inline-form">
	    		<!-- <label><%= LanguageUtil.get(pageContext, "Filter") %>:</label> -->
				<input type="text" dojoType="dijit.form.TextBox" name="filter" id="filter" value="" onkeyup="filterResults()" />
			
				<button dojoType="dijit.form.Button" onClick="clearFilter();" class="dijitButtonFlat">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear")) %>
				</button>
			</div>
	        <script language="Javascript">
				/**
					focus on search box
				**/
				require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
					dojo.require('dojox.timing');
					t = new dojox.timing.Timer(500);
					t.onTick = function(){
					  focusUtil.focus(dom.byId("filter"));
					  t.stop();
					}
					t.start();
				});
			</script> 
			<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="loadProgress" id="loadProgress"></div>
		</div>
    	<div class="portlet-toolbar__actions-secondary">
			<button dojoType="dijit.form.Button" onClick="cancelEdit('fm');return false;" iconClass="cancelIcon" class="dijitButtonFlat">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel-Changes")) %>
			</button>
		    <button dojoType="dijit.form.Button" onClick="addNewProperty();return false;" iconClass="plusIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Property")) %>
		    </button>
			<button dojoType="dijit.form.Button" onClick="discardChanges('fm');return false;" iconClass="cancelIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset-Changes")) %>
			</button>
		    <button dojoType="dijit.form.Button"  onClick="doSubmit('fm');return false;" iconClass="saveIcon">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Changes")) %>
		    </button>
    	</div>
   </div>
   <!-- END Toolbar -->

<div id="borderContainer" style="overflow: auto;">
	<table width="100%" class="listingTable">
	   <thead>
			<tr>
				<th width="6%" class="beta" align="center"><%= LanguageUtil.get(pageContext, "Remove") %></th>
				<th width="14%" class="beta" align="center"><%= LanguageUtil.get(pageContext, "Language-Key") %></th>
				<th width="40%" class="beta" align="center"><%= language.getLanguage() %></th>
				<th width="40%" class="beta" align="center"><%= language.getCountry() %>&nbsp;<%= LanguageUtil.get(pageContext, "Specific-country-values") %></th>
			</tr>
		</thead>
		<tbody id="propertiesTable">
			<tr>
				<td colspan="4">
					<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Properties-Found") %></div>
				</td>
			</tr>

		</tbody>
	</table>
	
	<!-- Start Pagination -->
	<div class="yui-gb buttonRow">
		<div class="yui-u first" style="text-align:left;">
			<div id="previousDiv" style="display: none;">
				<button dojoType="dijit.form.Button" onClick="previousPage();return false;" iconClass="previousIcon" id="previousDivButton">
					<%= LanguageUtil.get(pageContext, "Previous")%>
				</button>
			</div>&nbsp;
		</div>
		<div class="yui-u">
			<div id="matchingResultsBottomDiv"></div>
		</div>
		<div class="yui-u" style="text-align:right;">
			<div id="nextDiv" style="display: none;">
				<button dojoType="dijit.form.Button" onClick="nextPage();return false;" iconClass="nextIcon" id="nextDivButton">
					<%= LanguageUtil.get(pageContext, "Next")%>
				</button>
			</div>&nbsp;
		</div>
	</div>
<!-- END Pagination -->

</div>

<div id="altText" style="display: none; position: absolute; border: 1px dashed gray; background: white;"></div>

</html:form>
</div>

<!-- To show lightbox effect "Saving Keys.."  -->
<div id="savingKeysDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Saving") %> . . ." style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>

<script type="text/javascript">
	dojo.addOnLoad(function () {
		dojo.style(dijit.byId('savingKeysDialog').closeButtonNode, 'visibility', 'hidden');
		
	});
	
</script>
</liferay:box>
