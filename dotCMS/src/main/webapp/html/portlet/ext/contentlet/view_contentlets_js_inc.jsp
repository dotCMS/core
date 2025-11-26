<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel" %>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.List"%>
<%@ page import="io.vavr.control.Try" %>

<script language="JavaScript"><!--

<%boolean canReindex= APILocator.getRoleAPI().doesUserHaveRole(user,APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))|| com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());%>

<%
final ContentType calendarEventSt = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser()).find("calendarEvent")).getOrNull();
final String calendarEventInode = null!=calendarEventSt ? calendarEventSt.inode() : StringPool.BLANK;
%>

        dojo.require("dojox.dtl.filter.strings");
        dojo.require("dijit.form.FilteringSelect");
        dojo.require("dijit.form.MultiSelect");
        dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
        dojo.require("dojo.aspect");

        const DOTCMS_DATAVIEW_MODE = 'dotcms.dataview.mode';
        const DOTCMS_DEFAULT_CONTENT_SORT_BY = "score,modDate desc";

        var state = {
          data: [],
          view: localStorage.getItem(DOTCMS_DATAVIEW_MODE) || 'list',
          headers: []
        }

        var radiobuttonsIds = new Array();
        var checkboxesIds = new Array();
        var counter_radio = 0;
        var counter_checkbox = 0;
        var userId = '<%= user.getUserId() %>';
        var crumbtrailSelectedHostId = '<%= crumbtrailSelectedHostId %>';
        var hasHostFolderField = false;
        var conHostFolderValue = '';
        var loadingSearchFields = true;
        var categoriesLastSearched = new Array();

        var queryRaw;
        var structureInode;
        var currentStructureFields;
        var currentPage = 1;
        var currentSortBy = DOTCMS_DEFAULT_CONTENT_SORT_BY;
        var setDotFieldTypeStr = "";
        var DOT_FIELD_TYPE = "dotFieldType";
        var cbContentInodeList = new Array();
        var totalContents = 0;
        var perPage = <%= com.dotmarketing.util.Config.getIntProperty("PER_PAGE") %>;
        var showVideoThumbnail = <%= Config.getBooleanProperty("SHOW_VIDEO_THUMBNAIL", true) %>
        var headerLength = 0;
        var headers;
        var userRolesIds = new Array ();
        var selectedStructureVarName = '';
        var bindTagFieldEvent;

        var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;
        var formNum=100;
		var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty()%>;

        <%
                List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser (user.getUserId());
                for (Role role : roles) {
        %>
        userRolesIds[userRolesIds.length] = '<%= role.getId() %>';
        <%
                }
        %>

        var languages = new Array();
        var language;

        <%for (Language language: languages) {%>
                language = new Array(<%= language.getId() %>, "<%= language.getLanguageCode() %>", "<%= language.getCountryCode() %>", "<%= language.getLanguage() %>", "<%= language.getCountry() %>", "<%= LanguageUtil.getLiteralLocale(language.getLanguageCode(), language.getCountryCode()) %>");
                languages[languages.length] = language;
        <%      } %>

        <%for(String category: categories) { %>
                categoriesLastSearched[categoriesLastSearched.length] = '<%= category %>';
        <%      } %>

        var unCheckedInodes = "";
        function updateUnCheckedList(inode,checkId){
	        if(document.getElementById("fullCommand").value == "true"){
	            if(!document.getElementById(checkId).checked){
	                    unCheckedInodes = document.getElementById('allUncheckedContentsInodes').value;

	                    if(unCheckedInodes == "")
	                            unCheckedInodes = inode;
	                    else
	                            unCheckedInodes = unCheckedInodes + ","+ inode;

	            }else{
	                    unCheckedInodes = unCheckedInodes.replace(inode,"-");
              }
	            document.getElementById('allUncheckedContentsInodes').value = unCheckedInodes;
	        }
        }


		function initAdvancedSearch(){
			var x = dojo.cookie("ShAdDi");
			if(x !=null && x != undefined && x != 0){
				setTimeout(resizeAdvancedSearch, 500);
			}
		}


		function resizeAdvancedSearch(){
			var start = dojo.getStyle(dojo.byId('advancedSearchOptions'),'height');
			// how tall should we be
			var end=dojo.position(dojo.byId("measureTheHeightOfSearchTools")).y - dojo.position(dojo.byId("advancedSearchOptions")).y;

			// resize

			dojo.setStyle(dojo.byId('advancedSearchOptions'),'height', '0px');

			dojo.animateProperty({
		        node: dojo.byId("advancedSearchOptions"),
		        properties: {
		            height: {start: start, end: end, unit: "px"},
		        },
		        duration: 500,
                        onEnd: function() {
                                        dojo.byId("advancedSearchOptions").style.overflow = "visible";
                                }
		    }).play();
			dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "hide") %>";
			dojo.cookie("ShAdDi", end, { });
		}



		function toggleAdvancedSearchDiv(){
			// how tall are we
			var showing = dojo.getStyle(dojo.byId('advancedSearchOptions'),'height');

			// resize
			if("0px" == showing || 0 == showing) {
				dojo.cookie("ShAdDi", "0", { });
				dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "hide") %>";
				resizeAdvancedSearch();
                                dojo.byId("advancedSearchOptions").style.overflow = "hidden";
			// hide
			} else {
                                dojo.byId("advancedSearchOptions").style.overflow = "hidden";

			        dojo.animateProperty({
			        node: dojo.byId("advancedSearchOptions"),
			        properties: {
			            height: {start: showing, end: 0, unit: "px"},
			        },
			        duration: 500,
			    }).play();
				dojo.cookie("ShAdDi", 0, { });
				dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "advanced") %>";
			}
		}








		/**
			focus on search box
		**/
		require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
			dojo.require('dojox.timing');
			t = new dojox.timing.Timer(900);
			t.onTick = function(){
			  focusUtil.focus(dom.byId("allFieldTB"));
			  t.stop();
			}
			t.start();
		});

        function getViewCardEl() {
            return document.querySelector('dot-card-view');
        }

        function getListEl() {
            return document.getElementById('results_table')
        }


        function getSelectButton() {
            return document.querySelector('dot-data-view-button')
        }

		function setDotSelectButton(){
			var dotSelectButton = getSelectButton();
			dotSelectButton.addEventListener('selected', function (event) {
                changeView(event.detail);
			}, false);
		}

        function printData(data, headers) {
            const list = getListEl();

            if (state.view === 'list') {
                fillResultsTable(headers, data);
                const card = getViewCardEl();
                card ? card.style.display = 'none' : false;
                list.style.display = ''
            } else {
                fillCardView(data);
                list.style.display = 'none'
                getViewCardEl().style.display = ''
            }
        }

        function fillResults(data) {
            var counters = data[0];
            var hasNext = counters["hasNext"];
            var hasPrevious = counters["hasPrevious"];
            var total = counters["total"];
            var begin = counters["begin"];
            var end = counters["end"];
            var totalPages = counters["totalPages"];
            const cardEl = getViewCardEl();

            headers = data[1];

            for (var i = 3; i < data.length; i++) {
                data[i - 3] = data[i];
            }
            data.length = data.length - 3;

            if (cardEl) {
                cardEl.items = [];
            }
            dwr.util.removeAllRows("results_table");

            var funcs = new Array ();
            if (data.length <= 0) {
                    if (1 < totalPages) {
                            doSearch(totalPages, counters["sortByUF"]);
                    } else {
                            funcs[0] = noResults;
                            dwr.util.addRows("results_table", [ headers ] , funcs, { escapeHtml: false });
                            document.getElementById("nextDiv").style.display = "none";
                            document.getElementById("previousDiv").style.display = "none";
                            state = {
                                ...state,
                                data: []
                            };
                            showMatchingResults(0,0,0,0);
                            fillQuery (counters);
                            dijit.byId("searchButton").attr("disabled", false);
                    }

                    return;
            }

            state = {
              ...state,
              data: data,
              headers: headers
            }

            const selectButton = getSelectButton();
            if (selectButton) {
                selectButton.style.display = '';
            }
            printData(data, headers);
            showMatchingResults(total, begin, end, totalPages);
            fillQuery (counters);


            var popupsiframe = document.getElementById("popups");
            for (var j = 0; j < data.length; j++) {
				var contentlet = data[j];
				var inode = contentlet["inode"];
				var live = contentlet["live"] == "true"?"1":"0";
				var working = contentlet["working"] == "true"?"1":"0";
				var deleted = contentlet["deleted"] == "true"?"1":"0";
				var locked = contentlet["locked"] == "true"?"1":"0";
				var permissions = contentlet["permissions"];
				var read = userHasReadPermission (contentlet, userId)?"1":"0";
				var write = userHasWritePermission (contentlet, userId)?"1":"0";
				var publish = userHasPublishPermission (contentlet, userId)?"1":"0";
            }

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

            dijit.byId("searchButton").attr("disabled", false);
    		dijit.byId("clearButton").setAttribute("disabled", false);
            togglePublish();

            //SelectAll functionality
            if(document.getElementById("fullCommand").value == "true"){
                    dijit.byId('checkAll').attr('checked',true);
                    selectAllContents();
            }


        }



        function titleCell (data,text, x) {

                text = shortenString(text, 100);

                var inode = data["inode"];
                var checkId = "checkbox" + x;
                var live = data["live"] == "true"?true:false;
                var working = data["working"] == "true"?true:false;
                var deleted = data["deleted"] == "true"?true:false;
                var locked = data["locked"] == "true"?true:false;
                var liveSt = live?"1":"0";
                var workingSt = working?"1":"0";
                var permissions = data["permissions"];
                var write = userHasWritePermission (data, userId)?"1":"0";
                var publish = userHasPublishPermission (data, userId)?"1":"0";
                var structure_id = data["structureInode"];
                var typeVariable = data["typeVariable"];
                var editRef ='';

                if(structure_id == '<%=calendarEventInode %>'){


                editRef = `editEvent('${inode}','<%=user.getUserId()%>','<%= referer %>','${liveSt}','${workingSt}','${write}','${typeVariable}')`;
            }else{
                editRef = `editContentlet('${inode}','<%=user.getUserId()%>','<%= referer %>',${liveSt},${workingSt},${write},'${typeVariable}')`;
            }

            var ref = "<div class='content-search__result-item'><tr>";
                if(publish == "1") {

	                if(dijit.byId(checkId)){
	                	dijit.byId(checkId).destroy();
	                }


					ref+=  "<td style='width:25px;' valign='top'>";
					ref+=  "<input dojoType=\"dijit.form.CheckBox\" type=\"checkbox\" name=\"publishInode\" id=\"";
					ref+=  checkId + "\" value=\"" + inode + "\" onClick=\"togglePublish();updateUnCheckedList(";
					ref+=  "'" + inode + "'" + "," + "'" +  checkId + "'" +  ");\" ";

					if((document.getElementById("fullCommand").value == "true")
							&& (unCheckedInodes.indexOf(inode) == -1)){
						ref+=  "checked = \"checked\" ";
					}
					ref+=  ">";
					ref+=  "</td>";
                }else{
	                ref+=  "<td style='width:25px;' valign='top'>";
	                ref+=  "<span class='newTaskIcon'></span>";
	                ref+=  "</td>";
                }

                ref+=  "<td valign='top'>"
                ref+=   "<a draggable='false' href=\"javascript: " + editRef + "\">";
                ref+=   text;
                ref+=   "</a>";
                ref+=   "</td>";
                ref+=   "</tr></div>";
                return ref;
        }

        function statusDataCell (data, i) {
                var inode = data["inode"];

                var live = data["live"] == "true"?true:false;
                var working = data["working"] == "true"?true:false;
                var deleted = data["deleted"] == "true"?true:false;
                var locked = data["locked"] == "true"?true:false;
                var hasLive = data["hasLive"] == "true"?true:false;
                var liveSt = live?"1":"0";
                var workingSt = working?"1":"0";
                var permissions = data["permissions"];
                var write = userHasWritePermission (data, userId)?"1":"0";
                var structure_id = data["structureInode"];
                var typeVariable = data["typeVariable"];

                var editRef = '';

            if(structure_id == '<%=calendarEventInode %>'){
              editRef = " editEvent('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + typeVariable + "') ";
            }else{
              editRef = " editContentlet('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + typeVariable + "') ";
            }

            var ref = "<a onMouseOver=\"style.cursor='pointer'\" href=\"javascript: " + editRef + "\">";
                //ref = ref + '<span class="editIcon"></span>';
                ref = ref + "</a>";
                ref = ref + data["statusIcons"] ;

                eval("cbContentInodeList[i] = '" + inode + "';++i;");

                return ref;
        }

        function fillCategoryOptions (selectId, data) {
                var select = document.getElementById(selectId);
                if (select != null) {
                        for (var i = 0; i < data.length; i++) {
                                var option = new Option ();
                                option.text = data[i]['categoryName'];
                                option.value = data[i]['inode'];
                                for (var j = 0; j < categoriesLastSearched.length; j++) {
                                        if(categoriesLastSearched[j] == data[i]['inode'])
                                                option.selected = true;
                                }
                                option.style.marginLeft = (data[i]['categoryLevel']*10)+"px";
                        <%
                            if(categories!=null){
                                        for (String cat : categories) {
                        %>
                                        if (option.value == '<%=cat%>')
                                                option.selected = true;
                        <%
                                        }
                                }
                        %>
                                select.options[i]=option;
                        }
                }
        }

        function renderSearchField (field) {

                var structureVelraw=dojo.byId("structureVelocityVarNames").value;
                var structInoderaw=dojo.byId("structureInodesList").value;
                var structureVel=structureVelraw.split(";");
                var structInode=structInoderaw.split(";");
                var fieldStructureInode = field["fieldStructureInode"];
                var fieldContentlet = field["fieldVelocityVarName"];
                var fieldContentlet2 = field["fieldContentlet"];
                var value = "";
        		var selectedStruct="";
                for(var m=0; m <= structInode.length ; m++ ){
             		if(fieldStructureInode==structInode[m]){
                 		selectedStruct=structureVel[m];
                 	}
                }
                selectedStructureVarName = selectedStruct;

        <%
                String conHostValue = fieldsSearch.get("conHost");
                String conFolderValue = fieldsSearch.get("conFolder");
                String conHostFolderValue;
                if (conHostValue != null && !conHostValue.equalsIgnoreCase("allHosts")) {
                        conHostFolderValue = conHostValue;
                        conFolderValue = "";
                } else if (conFolderValue != null) {
                        conHostValue = "";
                        conHostFolderValue = conFolderValue;
                } else {
                        conHostFolderValue = "";
                }

        Set<String> keys = fieldsSearch.keySet();
        String value;
        for (String key : keys) {
                if (UtilMethods.isSet(fieldsSearch.get(key)))
                value = fieldsSearch.get(key);
                else
                        value = "";%>
                                if (selectedStructureVarName+"."+fieldContentlet == '<%=key%>')
                                        value = '<%= UtilMethods.escapeSingleQuotes(value.trim()) %>';
                    <% }%>

                var type = field["fieldFieldType"];
            if(type=='checkbox'){
                   //checkboxes fields
                    var option = field["fieldValues"].split("\r\n");
                    var lastChecked = value.split(",");


                    var result="";

                    for(var i = 0; i < option.length; i++){
                       var actual_option = option[i].split("|");
                       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){

                                if(dijit.byId(selectedStruct+"."+ fieldContentlet + "Field"+ counter_checkbox)){
                                                dijit.byId(selectedStruct+"."+ fieldContentlet + "Field"+ counter_checkbox).destroy();
                                        }

                                var myD= selectedStruct+"."+ fieldContentlet + "Field"+ counter_checkbox ;



                                result += "<div class=\"checkbox\"><input onchange='doSearch()' type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" value=\""
                                                        + actual_option[1] + "\" id=\"" + selectedStruct + "." + fieldContentlet + "Field"+ counter_checkbox
                                                        + "\" name=\"" + selectedStruct + "." + fieldContentlet + "\"";
                                for(var j = 0;j < lastChecked.length; j++){
                                                if(lastChecked[j] == actual_option[1]){
                                                result = result + "checked = \"checked\"";
                                        }
                                }
                                result = result + "><label for='"+myD+"'>" + actual_option[0] + "</label></div>";
                            checkboxesIds[counter_checkbox] = selectedStruct+"."+fieldContentlet + "Field" + counter_checkbox;

                            setDotFieldTypeStr = setDotFieldTypeStr
                                                                        + "dojo.attr("
                                                                        + "'" + selectedStruct + "." + fieldContentlet + "Field" + counter_checkbox + "'"
                                                                        + ",'" + DOT_FIELD_TYPE + "'"
                                                                        + ",'" + type + "');";

                            counter_checkbox++;
                        }
                    }
                    return result;

          }else if(type=='radio'){
                    //radio buttons fields
                    var option = field["fieldValues"].split("\r\n");
                    var result="";

                    for(var i = 0; i < option.length; i++){

                       dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field"+ counter_radio);

                       var myD= selectedStruct+"."+ fieldContentlet + "Field"+ counter_radio;


                       var actual_option = option[i].split("|");
                       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
                                result = result + "<div class=\"radio\"><input onchange='doSearch()' type=\"radio\" dojoType=\"dijit.form.RadioButton\" value=\""
                                                        + actual_option[1] + "\" id=\"" + selectedStruct+"."+ fieldContentlet + "Field"+ counter_radio
                                                        + "\" name=\"" + selectedStruct+ "." + fieldContentlet + "\"";
                                        if(value == actual_option[1]){
                                        result = result + "checked = \"checked\"";
                                }
                                result = result + "><label for='" + myD+ "'>" + actual_option[0] + "</label></div>";
                                radiobuttonsIds[counter_radio] = selectedStruct+"."+fieldContentlet + "Field"+ counter_radio;

                                 setDotFieldTypeStr = setDotFieldTypeStr
                                                                        + "dojo.attr("
                                                                        + "'" + selectedStruct + "." + fieldContentlet + "Field" + counter_radio + "'"
                                                                        + ",'" + DOT_FIELD_TYPE + "'"
                                                                        + ",'" + type + "');";

                                 counter_radio++;
                        }
                    }
                    return result;

          }else if(type=='select'){
                    dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field");
                    dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field_popup");
                    var option = field["fieldValues"].split("\r\n");
                    var result="";
                    if (type=='multi_select')
                                result = result+"<select onchange='doSearch()' dojoType='dijit.form.MultiSelect'  multiple=\"multiple\" size=\"4\" id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" name=\"" + selectedStruct+"."+ fieldContentlet + "\">\n";
                        else
                                result = result+"<select onchange='doSearch()' dojoType='dijit.form.FilteringSelect' id=\"" + selectedStruct+"."+ fieldContentlet + "Field\"  name=\"" + selectedStruct+"."+ fieldContentlet + "\">\n<option value=\"\"></option>";

                    for(var i = 0; i < option.length; i++){
                       var actual_option = option[i].split("|");
                       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
                                        auxValue = actual_option[1];
                            if(fieldContentlet2.indexOf("bool") != -1)
                            {
                                        if(actual_option[1] == "true" || actual_option[1] == "t" || actual_option[1] == "1")
                                    {
                                        auxValue = 't';
                                    }else if(actual_option[1] == "false" || actual_option[1] == "f" || actual_option[1] == "0")
                                    {
                                                auxValue = 'f';
                                    }
                                }
                                result = result + "<option value=\""
                                                                + auxValue + "\""
                                if(value == auxValue){
                                        result = result + " selected ";
                                }
                                result = result + " >" + actual_option[0]+"</option>\n";
                        }
                    }

                     setDotFieldTypeStr = setDotFieldTypeStr
                                                                        + "dojo.attr("
                                                                        + "'" + selectedStruct + "." + fieldContentlet + "Field" + "'"
                                                                        + ",'" + DOT_FIELD_TYPE + "'"
                                                                        + ",'" + type + "');";

                    result = result +"</select>\n";
                    return result;

          }else if(type=='multi_select'){
                    var lastSelected = value.split(",");
                    dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field");
                    dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field_popup");
                    var option = field["fieldValues"].split("\r\n");
                    var result="";
                    if (type=='multi_select')
                                result = result+"<select onchange='doSearch()'  dojoType='dijit.form.MultiSelect'  multiple=\"multiple\" size=\"4\" id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" name=\"" + selectedStruct+"."+ fieldContentlet + "\">\n";
                        else
                                result = result+"<select onchange='doSearch()' dojoType='dijit.form.FilteringSelect' id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" style=\"width:160px;\" name=\"" + selectedStruct+"."+ fieldContentlet + "\">\n<option value=\"\">None</option>";

                    for(var i = 0; i < option.length; i++){
                       var actual_option = option[i].split("|");
                       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
                                        auxValue = actual_option[1];
                            if(fieldContentlet2.indexOf("bool") != -1)
                            {
                                        if(actual_option[1] == "true" || actual_option[1] == "t" || actual_option[1] == "1")
                                    {
                                        auxValue = 't';
                                    }else if(actual_option[1] == "false" || actual_option[1] == "f" || actual_option[1] == "0")
                                    {
                                                auxValue = 'f';
                                    }
                                }
                                result = result + "<option value=\""
                                                                + auxValue + "\"";
                                for(var j = 0;j < lastSelected.length; j++){
                                        if(lastSelected[j] == auxValue){
                                                result = result + " selected ";
                                        }
                                }
                                result = result + " >" + actual_option[0]+"</option>\n";
                        }
                    }

                     setDotFieldTypeStr = setDotFieldTypeStr
                                                                        + "dojo.attr("
                                                                        + "'" + selectedStruct + "." + fieldContentlet + "Field" + "'"
                                                                        + ",'" + DOT_FIELD_TYPE + "'"
                                                                        + ",'" + type + "');";

                    result = result +"</select>\n";
                    return result;

          }else if(type=='tag'){
                        var fieldId = selectedStruct + fieldContentlet + "Field";
                        var searchFieldId = selectedStruct + "." + fieldContentlet + "Field";

                        dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field");
                        dijit.registry.remove(selectedStruct + fieldContentlet + "Field");

                        var result = [
                            "<div class=\"tagsWrapper\" id=\"" + fieldId + "Wrapper" + "\">",
                            "<input type=\"hidden\" value=\"" + value + "\" id=\"" + searchFieldId + "\" onchange=\"setTimeout(doSearch, 500); resizeAdvancedSearch();\" />",
                            "<input type=\"hidden\" style=\"border: solid 1px red\" id=\"" + fieldId + "Content" + "\" value=\"" + value + "\"  />",
                            "<input type=\"text\" dojoType=\"dijit.form.TextBox\" id=\"" + fieldId + "\" name=\"" + selectedStruct+"."+ fieldContentlet + "Field\" />",
                            "<span class='hint-text'><%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></span>",
                            "<div class=\"tagsOptions\" id=\"" + fieldId.replace(".", "") + "SuggestedTagsDiv" + "\" style=\"display:none;\"></div>",
                            "</div>"
                        ].join("");

                        bindTagFieldEvent = function() {
                          var tagField = dojo.byId(fieldId);
                          dojo.connect(tagField, "onkeyup", function(e) {

                            <%
                                //Search for the selected host
                                String selectedHost = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
                                if(UtilMethods.isSet(selectedHost) && !selectedHost.equals("allHosts")) {
                            %>
                                    suggestTagsForSearch(e, searchFieldId,'<%=selectedHost%>');
                            <%
                                } else {
                            %>
                                    suggestTagsForSearch(e, searchFieldId);
                            <%
                                }
                            %>
                          });
                          dojo.connect(tagField, "onblur", closeSuggetionBox);
                          if (value.length) {
                            fillExistingTags(fieldId, value, searchFieldId);
                          }
                        }

                        setDotFieldTypeStr = setDotFieldTypeStr
                                            + "bindTagFieldEvent();\n"
                                            + "dojo.attr("
                                            + "'" + selectedStruct + "." + fieldContentlet + "Field" + "'"
                                            + ",'" + DOT_FIELD_TYPE + "'"
                                            + ",'" + type + "');";

                    return result;
          }//http://jira.dotmarketing.net/browse/DOTCMS-3232
          else if(type=='host or folder'){
                  // Below code is used to fix the "widget already registered error".
                  const folderHostSelectorField = dijit.byId('FolderHostSelector');
                  const folderHostSelectorCurrentValue = folderHostSelectorField ? folderHostSelectorField.value : null;
                  const oldTree = dijit.byId('FolderHostSelector-tree');

                  if(dojo.byId('FolderHostSelector-hostFoldersTreeWrapper')){
                          dojo.byId('FolderHostSelector-hostFoldersTreeWrapper').remove();
                  }
                  if(dijit.byId('FolderHostSelector')){
                          dijit.byId('FolderHostSelector').destroy();
                  }
                  if(dijit.byId('FolderHostSelector-tree')){
                          dijit.byId('FolderHostSelector-tree').destroy();
                 }


                  var field = selectedStruct+"."+fieldContentlet + "Field";
                  var hostId = "";
                  <% if(UtilMethods.isSet(conHostValue)){%>
                        hostId = '<%= conHostValue %>';
                  <%}else if(UtilMethods.isSet(crumbtrailSelectedHostId)){ %>
                        hostId = '<%= conHostValue %>';
                  <%} %>
                  var fieldValue = folderHostSelectorCurrentValue || hostId;
                  <% if(UtilMethods.isSet(conFolderValue)){%>
                        fieldValue = '<%= conFolderValue %>';
                  <%}%>

                  var result = "<div onchange=\"doSearch(null, '<%=orderBy%>')\" id=\"FolderHostSelector\" style='width270px' dojoType=\"dotcms.dijit.form.HostFolderFilteringSelect\" includeAll=\"true\" onClick=\"resetHostValue();\" onChange=\"getHostValue();\" "
                                                +" hostId=\"" + hostId + "\" value = \"" + fieldValue + "\"" + "></div>";

                 hasHostFolderField = true;

                 // Set the previous selected value of the tree or the conHostValue after the tree is loaded.
                 setTimeout(()=> {
                        const newTree = dijit.byId('FolderHostSelector-tree');
                        newTree.set('path', oldTree?.path ||  "<%= conHostValue %>");
                        newTree.set('selectedItem', oldTree?.selectedItem ||   "<%= conHostValue %>");
                 },1000);

                return result;
          }else if(type=='category' || type=='hidden'){

             return "";

          }else if(type.indexOf("date") > -1){
                        dijit.registry.remove(selectedStruct+"."+ fieldContentlet + "Field");
                        if(dijit.byId(selectedStruct+"."+ fieldContentlet + "Field")){
                                dijit.byId(selectedStruct+"."+ fieldContentlet + "Field").destroy();
                        }
                        dojo.require("dijit.form.DateTextBox");
                var result = "<input onchange='doSearch()' type=\"text\" displayedValue=\""+value+"\" constraints={datePattern:'MM/dd/yyyy'} dojoType=\"dijit.form.DateTextBox\" validate='return false;' invalidMessage=\"\"  id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" name=\"" + selectedStruct+"."+ fieldContentlet + "\" >";
                return result;
          }else if(type=="relationship"){
	          var relationSearchField= selectedStruct+"."+ fieldContentlet;
	          var relationType = field["fieldRelationType"];

	          var boxTmpl= `
            	   <div id='${relationSearchField}Div'></div>
            	   <input type="hidden" id='${relationSearchField}Field' />
            	   <span class='hint-text'><%= LanguageUtil.get(pageContext, "Type-id-or-title-related-content") %></span>
            	   <script>
            	      dijit.registry.remove("${relationSearchField}Id");
            	      dijit.registry.remove("${relationSearchField}Id_popup");
		              var relationshipSearch = new dijit.form.FilteringSelect({
		                  id: "${relationSearchField}Id",
		                  name: "${relationSearchField}Name",
		                  pageSize:30,
		                  labelAttr: "label",
		                  store:null,
		                  searchAttr: "searchMe",
		                  queryExpr: '*${0}*',
		                  isValid : function(){
		                	  return true;
		                  },
		                  autoComplete: false,
	                      onKeyUp:function(event){
	                    	  if (event.keyCode != 13 &&  event.keyCode!= 38 && event.keyCode!=40) {
	                    		  reloadRelationshipBox(this, "${relationType}");
	                    	  }
                         },
                         onChange : function(value){
                        	 document.getElementById("${relationSearchField}Field").value=this.getValue().split(' ')[0];
                        	 doSearch(null, "<%=orderBy%>");
                         }


		              }, dojo.byId("${relationSearchField}Div"));

		              dojo.aspect.around(relationshipSearch, '_announceOption', function(origFunction) {
                          return function(node) {
                              this.searchAttr = 'label';
                              var r = origFunction.call(this, node);
                              this.searchAttr = 'searchMe';
                              return r;
                          }
                      });

		              reloadRelationshipBox(relationshipSearch,"${relationType}");

                      dojo.connect(dijit.byId("searchButton"), "onClick", null, function() {
                            if (relationshipSearch.get('value')==""){
                                relationshipSearch.set("displayedValue","");
                                reloadRelationshipBox(relationshipSearch,"${relationType}");
                            }
                      });

                      dojo.connect(dijit.byId("clearButton"), "onClick", null, function() {
                            dijit.byId("${relationSearchField}Id").set("displayedValue","");
                            reloadRelationshipBox(relationshipSearch,"${relationType}");
                      });
                </script>
              `;



              return boxTmpl;
          }else{
                dijit.registry.remove(selectedStruct+"."+ fieldContentlet + "Field");
                if(dijit.byId(selectedStruct+"."+ fieldContentlet + "Field")){
                        dijit.byId(selectedStruct+"."+ fieldContentlet + "Field").destroy();
                }
        return "<input type=\"text\" dojoType=\"dijit.form.TextBox\"  id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" name=\"" + selectedStruct+"."+ fieldContentlet + "\"  onkeyup='doSearch()'  value=\"" + value + "\">";

      }

        }


		function updateSelectedStructAux(){
			structureInode = dijit.byId('selectedStructAux').value;
			addNewContentlet(structureInode);


		}

        function dispatchCreateContentletEvent(url, contentType) {
            var customEvent = document.createEvent("CustomEvent");

            customEvent.initCustomEvent("ng-event", false, false,  {
                name: "create-contentlet",
                data: {
                    url,
                    contentType
                }
            });

            document.dispatchEvent(customEvent);
            dijit.byId("selectStructureDiv").hide();
        }


        function addNewContentlet(structureInode, contentType){

                if(!contentType){

                        // This is the same way they get the current var name on downloadToExcel method

                        let structureVelraw = dojo.byId("structureVelocityVarNames").value;

                        let structInoderaw = dojo.byId("structureInodesList").value;

                        let structureVelArray = structureVelraw.split(";");

                        let structureInodeArray = structInoderaw.split(";");

                        let contentTypeInode = dijit.byId('structure_inode').value;

                        contentType = structureVelArray.find((varName, i) => structureInodeArray[i] === contentTypeInode)
                }

		if(structureInode == undefined || structureInode==""){
                                // This gets the catchall and opens the dialog to select a contentType, and also retrieves the content type when is a custom portlet
        		structureInode = dijit.byId('structure_inode').value;
        	}
			if(structureInode == undefined || structureInode=="" || structureInode == "catchall"){
                                dijit.byId("selectStructureDiv").show();
				return;
			}
          else if(structureInode == '<%=calendarEventInode %>'){
                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/calendar/edit_event' />";
                href += "<portlet:param name='cmd' value='new' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "<portlet:param name='inode' value='' />";
                href += "</portlet:actionURL>";
                href += "&selectedStructure=" + structureInode ;
                href += "&lang=" + getSelectedLanguageId();
                dispatchCreateContentletEvent(href, contentType);
          }else{
                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
                href += "<portlet:param name='cmd' value='new' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "<portlet:param name='inode' value='' />";
                href += "</portlet:actionURL>";
                href += "&selectedStructure=" + structureInode ;
                href += "&lang=" + getSelectedLanguageId();
                dispatchCreateContentletEvent(href, contentType);
          }
        }

        function donwloadToExcel() {
                var structureInode = dijit.byId('structure_inode').value;

                if (structureInode == "") {
                        dijit.byId('structure_inode').focus();
                        return false;
                }
                cbContentInodeList = new Array();
                var fieldsValues = new Array();
                if (currentStructureFields == undefined) {
                        currentStructureFields = Array();
                }

                var structureVelraw = dojo.byId("structureVelocityVarNames").value;
                var structInoderaw = dojo.byId("structureInodesList").value;
                var structureVel = structureVelraw.split(";");
                var structInode = structInoderaw.split(";");
                var selectedStruct = "";
                for (var m2 = 0; m2 <= structInode.length; m2++) {
                        if (structureInode == structInode[m2]) {
                                selectedStruct = structureVel[m2];
                        }
                }

                if (hasHostFolderField) {
                        getHostValue();
                        var hostValue = document.getElementById("hostField").value;
                        var folderValue = document.getElementById("folderField").value;
                        if (isInodeSet(hostValue)) {
                                fieldsValues[fieldsValues.length] = "conHost";
                                fieldsValues[fieldsValues.length] = hostValue;
                        }
                        if (isInodeSet(folderValue)) {
                                fieldsValues[fieldsValues.length] = "conFolder";
                                fieldsValues[fieldsValues.length] = folderValue;
                        }
                }

                var allField = dijit.byId("allFieldTB").getValue();
                if (allField != undefined && allField.length > 0) {

                        fieldsValues[fieldsValues.length] = "catchall";
                        fieldsValues[fieldsValues.length] = allField + "*";
                }

                for (var j = 0; j < currentStructureFields.length; j++) {
                        var field = currentStructureFields[j];
                        var fieldId = selectedStruct + "." + field["fieldVelocityVarName"] + "Field";
                        var formField = document.getElementById(fieldId);
                        var fieldValue = "";

                        if (formField != null) {
                                if (dojo.attr(formField.id, DOT_FIELD_TYPE) == 'select') {

                                        var tempDijitObj = dijit.byId(formField.id);
                                        fieldsValues[fieldsValues.length] = selectedStruct + "." + field["fieldVelocityVarName"];
                                        fieldsValues[fieldsValues.length] = tempDijitObj.value;

                                } else if (formField.type == 'select-one' || formField.type == 'select-multiple') {

                                        var values = "";
                                        for (var i = 0; i < formField.options.length; i++) {
                                                if (formField.options[i].selected) {
                                                        fieldsValues[fieldsValues.length] = selectedStruct + "." + field["fieldVelocityVarName"];
                                                        fieldsValues[fieldsValues.length] = formField.options[i].value;

                                                }
                                        }

                                } else {
                                        fieldsValues[fieldsValues.length] = selectedStruct + "." + field["fieldVelocityVarName"];
                                        fieldsValues[fieldsValues.length] = formField.value;

                                }

                        }

                }

                for (var i = 0; i < radiobuttonsIds.length; i++) {
                        var formField = document.getElementById(radiobuttonsIds[i]);
                        if (formField != null && formField.type == 'radio') {
                                var values = "";
                                if (formField.checked) {
                                        values = formField.value;
                                        fieldsValues[fieldsValues.length] = formField.name;
                                        fieldsValues[fieldsValues.length] = values;
                                }
                        }
                }

                for (var i = 0; i < checkboxesIds.length; i++) {
                        var formField = document.getElementById(checkboxesIds[i]);
                        if (formField != null && formField.type == 'checkbox') {
                                var values = "";
                                if (formField.checked) {
                                        values = formField.value;
                                        fieldsValues[fieldsValues.length] = formField.name;
                                        fieldsValues[fieldsValues.length] = values;
                                }
                        }
                }

                if (getSelectedLanguageId() != 0) {
                        fieldsValues[fieldsValues.length] = "languageId";
                        fieldsValues[fieldsValues.length] = getSelectedLanguageId();
                }

                // if we have an identifier
                if (isInodeSet(document.getElementById("Identifier").value)) {
                        var contentId = "";
                        fieldsValues[fieldsValues.length] = "identifier";
                        contentId = document.getElementById("Identifier").value;
                        fieldsValues[fieldsValues.length] = contentId;
                }

                var allField = dijit.byId("allFieldTB").getValue();
                if (allField != undefined && allField.length > 0) {
                        fieldsValues[fieldsValues.length] = "catchall";
                        fieldsValues[fieldsValues.length] = allField + "*";
                }

                var categoriesValues = new Array();
                var form = document.getElementById("search_form");
                var categories = document.getElementsByName("categories");
                if (categories != null) {
                        if (categories.options != null) {
                                var opts = categories.options;
                                for (var j = 0; j < opts.length; j++) {
                                        var option = opts[j];
                                        if (option.selected) {
                                                categoriesValues[categoriesValues.length] = option.value;
                                        }
                                }
                        } else {
                                for (var i = 0; i < categories.length; i++) {
                                        var catSelect = categories[i];
                                        var opts = catSelect.options;
                                        for (var j = 0; j < opts.length; j++) {
                                                var option = opts[j];
                                                if (option.selected) {
                                                        categoriesValues[categoriesValues.length] = option.value;
                                                }
                                        }
                                }
                        }
                }

                var filterSystemHost = false;
                if (document.getElementById("filterSystemHostCB").checked && document.getElementById("filterSystemHostTable").style.display != "none") {
                        filterSystemHost = true;
                }

                var filterLocked = false;

                if (dijit.byId("showingSelect").getValue() == "locked") {
                        filterLocked = true;
                }

                var filterUnpublish = false;
                if (dijit.byId("showingSelect").getValue() == "unpublished") {
                        filterUnpublish = true;
                }

                var showDeleted = false;
                if (dijit.byId("showingSelect").getValue() == "archived") {
                        showDeleted = true;
                }

                dijit.byId("searchButton").attr("disabled", true);
                //dijit.byId("clearButton").attr("disabled", false);

                fieldsValues = fieldsValues.map(value => {
                        if (value.includes('[') || value.includes(']')) {
                                return encodeURIComponent(value);
                        } else {
                                return value;
                        }
                });

                document.getElementById('fieldsValues').value = fieldsValues;
                document.getElementById('categoriesValues').value = categoriesValues;
                document.getElementById('showDeleted').value = showDeleted;
                document.getElementById('currentSortBy').value = currentSortBy;
                document.getElementById('filterSystemHost').value = filterSystemHost;
                document.getElementById('filterLocked').value = filterLocked;
                document.getElementById('filterUnpublish').value = filterUnpublish;

                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
                href += "<portlet:param name='cmd' value='export' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "</portlet:actionURL>";
                href += "&expStructureInode="+structureInode+"&expFieldsValues="+fieldsValues+"&expCategoriesValues="+categoriesValues+"&showDeleted="+showDeleted+"&expCurrentSortBy="+currentSortBy+"&filterSystemHost="+filterSystemHost+"&filterLocked="+filterLocked+"&filterUnpublish="+filterUnpublish;

                /*if we have a date*/
                var dateFrom = null;
                var dateTo = null;
                if ((document.getElementById("lastModDateFrom").value != "")) {
                        dateFrom = document.getElementById("lastModDateFrom").value;
                        var dateFromsplit = dateFrom.split("/");
                        if (dateFromsplit[0] < 10) dateFromsplit[0] = "0" + dateFromsplit[0]; if (dateFromsplit[1] < 10) dateFromsplit[1] = "0" + dateFromsplit[1];
                        dateFrom = dateFromsplit[2] + dateFromsplit[0] + dateFromsplit[1] + "000000";
                        href += "&modDateFrom=" + dateFrom;
                }

                if ((document.getElementById("lastModDateTo").value != "")) {
                        dateTo = document.getElementById("lastModDateTo").value;
                        var dateTosplit = dateTo.split("/");
                        if (dateTosplit[0] < 10) dateTosplit[0] = "0" + dateTosplit[0]; if (dateTosplit[1] < 10) dateTosplit[1] = "0" + dateTosplit[1];
                        dateTo = dateTosplit[2] + dateTosplit[0] + dateTosplit[1] + "235959";
                        href += "&modDateTo=" + dateTo;
                }

                if (href.length > 6584) {
                        showDotCMSErrorMessage("Error: The URL has exceeded the size limit due to the large number of content filters. Please reduce them.");
                } else {
                        window.location.href = href;
                }
        }


        function copySearchForm(){
        	var newForm = document.createElement("form");
            newForm.style = "display: none";
        	newForm.method="POST";
        	newForm.target="AjaxActionJackson";
         	var oldFormElements = document.getElementById("search_form").elements;
			for (i=0; i < oldFormElements.length; i++){
			    newForm.appendChild(oldFormElements[i].cloneNode(true));
		  	}

		  	//var newForm = document.getElementById("search_form").cloneNode(true);
        	newForm.name="form" + formNum;
        	newForm.id="form" + formNum;
            document.body.appendChild(newForm);

		  	return newForm;
        }

        function pushPublishSelectedContentlets() {

            var selectedInodes = getSelectedInodes ();
			pushHandler.showDialog(selectedInodes);
        }

        function addToBundleSelectedContentlets() {

            var selectedInodes = getSelectedInodes ();
			pushHandler.showAddToBundleDialog(selectedInodes, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
        }

        function reindexSelectedContentlets(){
                var form = copySearchForm()
            form.cmd.value = 'full_reindex_list';
                form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_reindex_list" /></portlet:actionURL>';


          /*if we have a date*/
                        var dateFrom= null;
                        var dateTo= null;
                        if((document.getElementById("lastModDateFrom").value!="")){
                                dateFrom = document.getElementById("lastModDateFrom").value;
                                var dateFromsplit = dateFrom.split("/");
                                if(dateFromsplit[0]< 10) dateFromsplit[0]= "0"+dateFromsplit[0]; if(dateFromsplit[1]< 10) dateFromsplit[1]= "0"+dateFromsplit[1];
                                dateFrom= dateFromsplit[2]+dateFromsplit[0]+dateFromsplit[1]+"000000";
                                form.action+= "&modDateFrom="+dateFrom;
                        }

                        if((document.getElementById("lastModDateTo").value!="")){
                                dateTo = document.getElementById("lastModDateTo").value;
                                var dateTosplit = dateTo.split("/");
                                if(dateTosplit[0]< 10) dateTosplit[0]= "0"+dateTosplit[0]; if(dateTosplit[1]< 10) dateTosplit[1]= "0"+dateTosplit[1];
                                dateTo= dateTosplit[2]+dateTosplit[0]+dateTosplit[1]+"235959";
                                form.action+= "&modDateTo="+dateTo;
                        }
            form.action+= "&structure_id=<%=structure.getInode()%>";
            form.submit();
            form.parentNode.removeChild(form);
        }


		function fakeAjaxCallback(){
			clearAllContentsSelection();
			refreshFakeJax();
		}

        function structureChanged (sync) {
		    if(sync != true)
		            async = true;
		    else
		            async = false;

		    var form = document.getElementById("search_form");
		    var structureInode = dijit.byId('structure_inode').value;
		    document.getElementById("structureInode").value = structureInode;
		    hasHostFolderField = false;
		    loadingSearchFields = true;
		    setDotFieldTypeStr = "";

		    StructureAjax.getSearchableStructureFields (structureInode,
		            { callback:fillFields, async: async });
		    StructureAjax.getStructureCategories (structureInode,
		            { callback:fillCategories, async: async });

		    dwr.util.removeAllRows("results_table");
		    hideMatchingResults ();
		    document.getElementById("nextDiv").style.display = "none";
		    document.getElementById("previousDiv").style.display = "none";
		    counter_radio = 0;
			counter_checkbox = 0;
		    var div = document.getElementById("matchingResultsBottomDiv")
		    div.innerHTML = "";



		    initAdvancedSearch();
        }

        function fieldName (field) {
             var type = field["fieldFieldType"];
             if(type=='category' || type=='hidden'){
                  return "";
             }else{
                if ((3 < field["fieldContentlet"].length) && (field["fieldContentlet"].substring(0, 4) == "date")) {
                        var id = field["fieldStructureInode"] + '_' + field["fieldContentlet"];

	             dijit.registry.remove("tipMsg_" + id);
	                     if (dijit.byId("tipMsg_" + id)) {
	                             dijit.byId("tipMsg_" + id).destroy();
	                     }
	                     if(field["fieldFieldType"] == 'date')
	                             var hintLabel = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "viewcontentlets.message.date.hint")) %>';
	                     if(field["fieldFieldType"] == 'date_time')
	                             var hintLabel = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "viewcontentlets.message.datetime.hint")) %>';
	                     else if(field["fieldFieldType"] == 'time')
	                             var hintLabel = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "viewcontentlets.message.time.hint")) %>';
	                     return field["fieldName"] + " <a href=\"#\" id=\"hint_" + id + "\">?</a>:<div dojoType=\"dijit.Tooltip\" connectId=\"hint_" + id + "\" label=\"" + hintLabel + "\"></div>";
	             } else {
	                     var isOneOption = field["fieldValues"].split("\r\n").length === 1;
	                     if (type === 'checkbox' && isOneOption || type === 'radio' && isOneOption) {
	                         return field["fieldName"];
	                     } else {
	                         return field["fieldName"] + ":";
	                     }
	             }
             }
        }

        function getSelectedInodes() {
            const inodes = state.view === 'list' ? getSelectedInodesFromList() : getSelectedInodesFromCardView();
            return inodes;
        }

        function getSelectedInodesFromCardView() {
          let viewCard = getViewCardEl();

          if (viewCard) {
            const value = viewCard.getAttribute('value');
            if (value) {
                return viewCard.getAttribute('value').split(',');
            }
          }

          return [];
        }

        function getSelectedInodesFromList() {
            var selectedInodes;
            if ( document.getElementById("fullCommand").value == "true" ) {

                /*
                 If we choose to select all the elements, not just the ones in the current page, we can't just
                 send the selected elements as we are using pagination, we only have track of the current page, for
                 that reason lets send the lucene query that returned the current values LESS the uncheked values.
                */
                var excludeInodes = "";
                if (unCheckedInodes != undefined && unCheckedInodes != null && unCheckedInodes.length > 0) {

                    var inodesToExcludeColl = unCheckedInodes.split(",");
                    for (var i=0; i < inodesToExcludeColl.length; i++) {
                        if (inodesToExcludeColl[i] != "" && inodesToExcludeColl[i] != " " && inodesToExcludeColl[i] != "-" ) {
                            excludeInodes += " inode:" + inodesToExcludeColl[i];
                        }
                    }
                    //excludeInodes = unCheckedInodes.replace(/,/g, " inode:");
                    excludeInodes = " -(" + excludeInodes + ")";
                }

                selectedInodes = "query_" + queryRaw + excludeInodes;

            } else {
                selectedInodes = dojo.query("input[name='publishInode']")
                                    .filter(function(x){return x.checked;})
                                    .map(function(x){return x.value;});
            }

            return selectedInodes;
        }

        /**
         * Renders the UI components -- i.e., text fields, radio buttons, dropdowns, etc. -- that represent each
         * user-searchable field for a given Content Type.
         *
         * @param data The object array with the information of every field that must be rendered.
         */
        function fillFields (data) {
            currentStructureFields = data;
            let htmlstr = "";
            let siteFolderFieldHtml = "";
            for (const element of data) {
                    const { fieldFieldType } = element;
                    if (fieldFieldType === 'category' || fieldFieldType === 'hidden' || fieldFieldType == '<%= com.dotmarketing.portlets.structure.model.Field.FieldType.HOST_OR_FOLDER.toString() %>') {
                            continue;
                        }
                        htmlstr += `<dl class='vertical'>
                                <dt>
                                        <label>${fieldName(element)}</label>
                                        </dt>
                                        <dd style='min-height:0px'>${renderSearchField(element)}</dd>
                                        </dl>
                        <div class='clear'></div>`;
            }
            siteFolderFieldHtml = getSiteFolderFieldDefaultHTML();

            $('search_fields_table').update(htmlstr);
            $('site_folder_field').update(siteFolderFieldHtml);
            <% if (APILocator.getPermissionAPI().doesUserHavePermission(APILocator.getHostAPI().findSystemHost(), PermissionAPI.PERMISSION_READ, user, true)) { %>
                    dojo.byId("filterSystemHostTable").style.display = "";
            <% } %>

            dojo.parser.parse(dojo.byId("search_fields_table"));
            dojo.parser.parse(dojo.byId("site_folder_field"));
            eval(setDotFieldTypeStr);
            loadingSearchFields = false;
        }


        var categories = new Array();

        function fillCategories (data) {

                var searchCategoryList = dojo.byId("search_categories_list");
                searchCategoryList.innerHTML ="";


                var form = document.getElementById("search_form");
                form.categories = null;
                if(form.categories != null){
                	var tempChildNodesLength = form.categories.childNodes.length;
                	for(var i = 0; i < tempChildNodesLength; i++){
                		form.categories.removeChild(form.categories.childNodes[0]);
                	}
                }
                dojo.require("dijit.form.MultiSelect");
                if (data != null) {
                        categories = data;
                        for(i = 0;i< categories.length;i++){
                                dojo.create("dt", { innerHTML: categories[i]["categoryName"] + ":" }, searchCategoryList);
                                var selectId = categories[i]["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select";
                                dijit.registry.remove(selectId);
                                if(dijit.byId(selectId)){
                                        dijit.byId(selectId).destroy();
                                }
                                var selectObj = "<select dojoType='dijit.form.MultiSelect' multiple='true' name=\"categories\" id=\"" + selectId + "\"></select>";

                                dojo.create("dd", { innerHTML: selectObj }, searchCategoryList);

                        }


                }


                fillSelects();
                dojo.parser.parse(dojo.byId("search_categories_list"));
        }

        function fillSelects () {

                for (var i = 0; i < categories.length; i++) {
                        var cat = categories[i];
                        var selectId = cat["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select";
                        var mycallbackfnc = function(data) { fillCategorySelect(selectId, data); }

                        CategoryAjax.getSubCategories(cat["inode"], '', { callback: mycallbackfnc, async: false });
                }
        }

        function fillCategorySelect (selectId, data) {
                fillCategoryOptions (selectId, data);
                var selectObj = document.getElementById (selectId);
                if (data.length > 1) {
                        var len = data.length;
                        if (len > 9) len = 9;
                        selectObj.size = len;
                }
        }

        function getSelectedLanguageId () {
            var obj=dijit.byId('language_id');
            if(!obj)
                obj=dojo.byId('language_id');
            return obj.value;
        }

        function getSelectedWorkflowId(){
            var obj = dijit.byId("scheme_id");
            if(!obj)
               obj = dojo.byId("scheme_id");
            return obj.value;
        }

        function getSelectedStepId(){
           var obj = dijit.byId("step_id");
           if(!obj)
              obj = dojo.byId("step_id");
           return obj.value;
        }
        function getSiteFolderFieldDefaultHTML() {

                const defaultSiteFolderField = {
                    "fieldName": "<%= LanguageUtil.get(pageContext, "Host-Folder") %>",
                    "fieldFieldType": "<%= com.dotmarketing.portlets.structure.model.Field.FieldType.HOST_OR_FOLDER.toString() %>",
                    "fieldVelocityVarName": "siteOrFolder",
                    "fieldValues": "",
                    "fieldContentlet": "system_field",
                    "fieldStructureInode": structureInode
                };

                return `<dl class='vertical'>
                        <dt>
                                <label>${fieldName(defaultSiteFolderField)}</label>
                        </dt>
                        <dd style='min-height:0px'> ${renderSearchField(defaultSiteFolderField)}</dd>
                        </dl>
                        <div class='clear'>
                </div>`;
        }


        const debounce = (callback, time = 250, interval) =>
        (...args) => {
          clearTimeout(interval, interval = setTimeout(() => callback(...args), time));

        }
        const debouncedSearch = debounce(doSearch1, 250);

        var currentPage;
        function doSearch (page, sortBy, viewDisplayMode) {
          if (page) {
              currentPage = page;
          } else {
              page = currentPage;
          }

          if (viewDisplayMode) {
              changeView(viewDisplayMode, true);
          }

          // Wait for the "HostFolderFilteringSelect" widget to end the values updating process before proceeding with the search, if necessary.
          if (
              dijit.byId('FolderHostSelector') &&
              dijit
                  .byId('FolderHostSelector')
                  .attr('updatingSelectedValue')
          ) {
              setTimeout(
                  'doSearch (' + page + ", '" + sortBy + "');",
                  250
              );
          } else {
              if (dijit.byId('structure_inode')) {
                  debouncedSearch(page, sortBy);
              } else {
                  setTimeout(
                      'doSearch (' +
                          page +
                          ", '" +
                          sortBy +
                          "');",
                      250
                  );
              }
          }
        }





        function doSearch1 (page, sortBy) {

                if (page == undefined || page == null ) {
                    //Unless we are using pagination we don't need to keep the All selection across searches
                    if(dijit.byId('checkAll')!= undefined)
                    	clearAllContentsSelection();
                }

	            var structureInode;


	            if(dijit.byId('structure_inode')) {
	              structureInode  = dijit.byId('structure_inode').getValue();
	            }



                cbContentInodeList = new Array();
                var fieldsValues = new Array ();
                if(currentStructureFields == undefined){
                        currentStructureFields = Array();
                }



                var structureVelraw=dojo.byId("structureVelocityVarNames").value;
                var structInoderaw=dojo.byId("structureInodesList").value;
                var structureVel=structureVelraw.split(";");
                var structInode=structInoderaw.split(";");
                var selectedStruct="";
                for(var m2=0; m2 <= structInode.length ; m2++ ){
		             if(structureInode==structInode[m2]){
		                 selectedStruct=structureVel[m2];
	                 }
                 }

                if (hasHostFolderField) {
                        getHostValue();
                }
                var hostValue = document.getElementById("hostField").value;
                var folderValue = document.getElementById("folderField").value;
                if (isInodeSet(hostValue)) {
                        fieldsValues[fieldsValues.length] = "conHost";
                        fieldsValues[fieldsValues.length] = hostValue;
                }
                if (isInodeSet(folderValue)) {

                        fieldsValues[fieldsValues.length] = "conFolder";
                        fieldsValues[fieldsValues.length] = folderValue;
                }

                let schemeId = dijit.byId("scheme_id").getValue();
                if ("catchall"!= schemeId) {

                    fieldsValues[fieldsValues.length] = "wfscheme";
                    fieldsValues[fieldsValues.length] = schemeId;
                }

                let stepId = dijit.byId("step_id").getValue();
                if ("catchall"!= stepId) {

                    fieldsValues[fieldsValues.length] = "wfstep";
                    fieldsValues[fieldsValues.length] = stepId;
                }

                var allField = dijit.byId("allFieldTB").getValue();

				if (allField != undefined && allField.length>0 ) {

                        fieldsValues[fieldsValues.length] = "catchall";
                        fieldsValues[fieldsValues.length] = allField + "*";
				}
                for (var j = 0; j < currentStructureFields.length; j++) {
                        var field = currentStructureFields[j];
            			var fieldId = selectedStruct+"."+field["fieldVelocityVarName"] + "Field";
                        var formField = document.getElementById(fieldId);
                        var fieldValue = "";

                        if(formField != null){
                                if(dojo.attr(formField.id,DOT_FIELD_TYPE) == 'select'){

                                        var tempDijitObj = dijit.byId(formField.id);
                                        fieldsValues[fieldsValues.length] = selectedStruct+"."+field["fieldVelocityVarName"];

                                        if (/\s/.test(tempDijitObj.value)) {

                                            fieldsValues[fieldsValues.length] = '"' + tempDijitObj.value + '"';
                                        } else {

                                            fieldsValues[fieldsValues.length] = tempDijitObj.value;
                                        }
                                }else if(formField.type=='select-one' || formField.type=='select-multiple') {

                                     var values = "";
                                     for (var i=0; i<formField.options.length; i++) {
                                            if (formField.options[i].selected) {
                                              fieldsValues[fieldsValues.length] = selectedStruct+"."+field["fieldVelocityVarName"];
                                              fieldsValues[fieldsValues.length] = formField.options[i].value;

                                            }
                                          }

                                }else {
                                        fieldsValues[fieldsValues.length] = selectedStruct+"."+field["fieldVelocityVarName"];
                                        fieldsValues[fieldsValues.length] = formField.value;

                                }

                        }

                }

        		for(var i=0;i < radiobuttonsIds.length ;i++ ){
                        var formField = document.getElementById(radiobuttonsIds[i]);
                        if(formField != null && formField.type=='radio') {
                            var values = "";
                                if (formField.checked) {
                                        values = formField.value;
                                        fieldsValues[fieldsValues.length] = formField.name;
                                        fieldsValues[fieldsValues.length] = values;
                                }
                        }
                }

                for(var i=0;i < checkboxesIds.length ;i++ ){
                        var formField = document.getElementById(checkboxesIds[i]);
                        if(formField != null && formField.type=='checkbox') {
                            var values = "";
                                if (formField.checked) {
                                        values = formField.value;
                                        fieldsValues[fieldsValues.length] = formField.name;
                                        fieldsValues[fieldsValues.length] = values;
                                }
                        }
                }

                if(getSelectedLanguageId() != 0){
                	fieldsValues[fieldsValues.length] = "languageId";
                	fieldsValues[fieldsValues.length] = getSelectedLanguageId();
                }

                if(getSelectedLanguageId() == ""){
                        dijit.byId('language_id').focus() ;
                        return false;
                }

                // if we have an identifier
            if(isInodeSet(document.getElementById("Identifier").value)){
            var contentId = "";
                fieldsValues[fieldsValues.length] = "identifier";
                contentId = document.getElementById("Identifier").value;
                    fieldsValues[fieldsValues.length] = contentId;
            }
                var categoriesValues = new Array ();
                var form = document.getElementById("search_form");
                var categories = document.getElementsByName("categories");

                if (categories != null) {
                        if (categories.options != null) {
                                var opts = categories.options;
                                for (var j = 0; j < opts.length; j++) {
                                        var option = opts[j];
                                        if (option.selected) {
                                                categoriesValues[categoriesValues.length] = option.value;
                                        }
                                }
                        } else {
                                for (var i = 0; i < categories.length; i++) {
                                        var catSelect = categories[i];
                                        var opts = catSelect.options;
                                        for (var j = 0; j < opts.length; j++) {
                                                var option = opts[j];
                                                if (option.selected) {
                                                        categoriesValues[categoriesValues.length] = option.value;
                                                }
                                        }
                                }
                        }
                }

                if (page == null)
                        currentPage = 1;
                else
                        currentPage = page;

                if (sortBy != null && sortBy != "undefined") {
                        if (sortBy == currentSortBy && sortBy.indexOf("desc")==-1)
                                sortBy = sortBy + " desc";
                        currentSortBy = sortBy;
                }
                else {
                        sortBy=document.getElementById('currentSortBy').value;
                }


                var filterSystemHost = false;
                if (document.getElementById("filterSystemHostCB").checked && document.getElementById("filterSystemHostTable").style.display != "none") {
                        filterSystemHost = true;
                }

                var filterLocked = false;

                if (dijit.byId("showingSelect").getValue() == "locked") {
                        filterLocked = true;
                }

                var filterUnpublish = false;
                if (dijit.byId("showingSelect").getValue() == "unpublished") {
                       filterUnpublish = true;
                }

                var showDeleted = false;
                if (dijit.byId("showingSelect").getValue() == "archived") {
                        showDeleted = true;
                }

                //dijit.byId("searchButton").attr("disabled", true);
                //dijit.byId("clearButton").attr("disabled", false);

                document.getElementById('fieldsValues').value = fieldsValues;
                document.getElementById('categoriesValues').value = categoriesValues;
                document.getElementById('showDeleted').value = showDeleted;
               // document.getElementById('currentSortBy').value = currentSortBy;
                document.getElementById('filterSystemHost').value = filterSystemHost;
                document.getElementById('filterLocked').value = filterLocked;
                document.getElementById('filterUnpublish').value = filterUnpublish;
                if(isInodeSet(structureInode) || "catchall" == structureInode){
                        var dateFrom=null;
                        var dateTo= null;
                        if((document.getElementById("lastModDateFrom").value!="")){
                                dateFrom = document.getElementById("lastModDateFrom").value;
                                var dateFromsplit = dateFrom.split("/");
                                if(dateFromsplit[0]< 10) dateFromsplit[0]= "0"+dateFromsplit[0]; if(dateFromsplit[1]< 10) dateFromsplit[1]= "0"+dateFromsplit[1];
                                dateFrom= dateFromsplit[2]+dateFromsplit[0]+dateFromsplit[1]+"000000";
                        }

                        if((document.getElementById("lastModDateTo").value!="")){
                                dateTo = document.getElementById("lastModDateTo").value;
                                var dateTosplit = dateTo.split("/");
                                if(dateTosplit[0]< 10) dateTosplit[0]= "0"+dateTosplit[0]; if(dateTosplit[1]< 10) dateTosplit[1]= "0"+dateTosplit[1];
                                dateTo= dateTosplit[2]+dateTosplit[0]+dateTosplit[1]+"235959";
                        }
                        if("catchall" == structureInode){
                            <%if(request.getAttribute("contentTypesJs")!=null){ %>
                               // setting this to an array
                                structureInode = "<%=request.getAttribute("contentTypesJs") %>";
                            <%} %>
                        }

                        ContentletAjax.searchContentlets (structureInode, fieldsValues, categoriesValues, showDeleted, filterSystemHost, filterUnpublish, filterLocked, currentPage, currentSortBy, dateFrom, dateTo, fillResults);

                }

        }

        function nextPage () {
                doSearch (currentPage + 1);
        }

        function previousPage () {
        	if(parseInt(currentPage-1) > 0)
                doSearch (currentPage - 1);
        }

        function noResults (data) {
                return "<div class='noResultsMessage'><%= LanguageUtil.get(pageContext, "No-Results-Found") %></div>";
        }

        function checkUncheckAll() {
                var checkAll = dijit.byId("checkAll");
                var check;
	            var viewCard = getViewCardEl();

                if (viewCard) {
	                viewCard.value = '';
                }

                for (var i = 0; i < cbContentInodeList.length; ++i) {
                        check = dijit.byId("checkbox" + i);
                        if(check) {
                                check.setChecked(checkAll.checked);
                        }
                }
                if (checkAll.checked) {
                        selectAllContentsMessage();
                }else{
                        clearAllContentsMessage();
                }
                togglePublish();

        }

        function selectAllContentsMessage() {
                var checkAllDijit = dijit.byId("checkAll");
                var isChecked = checkAllDijit.checked;
                var table = $('tablemessage');
                if (isChecked) {
                        var html = '' +
                                '       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + cbContentInodeList.length + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "contents-on-this-page-are-selected")) %>';
                                if (perPage < totalContents) {
                                        html += ' <a href="javascript: selectAllContents()" style="text-decoration: underline;"> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all" )) %> ' + totalContents + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "content-s" )) %>.</a>';
                                }
                        html+= '';
                        table.update(html);
                        document.getElementById("fullCommand").value = "false";
                }
        }

        function selectAllContents()
        {
                var table = $('tablemessage');
                        var html = '' +
                '       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all" )) %> ' + totalContents + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "contents-on-this-page-are-selected" )) %>' +
                '       <a href="javascript: clearAllContentsSelection()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Selection" )) %>.</a>' +
                '';
                table.update(html);
                document.getElementById("fullCommand").value = "true";
                //document.getElementById("structureInode").value="<%=structureSelected %>";
        }

        function clearAllContentsSelection() {
                dijit.byId('checkAll').attr('checked',false);
                checkUncheckAll();
                clearAllContentsMessage();
        }

        function clearAllContentsMessage()      {
                $('tablemessage').innerHTML = "";
                unCheckedInodes = "";
                document.getElementById('allUncheckedContentsInodes').value = "";
                document.getElementById("fullCommand").value = "false";
                return true;
        }

        function getHeader (field) {
                var fieldContentlet = field["fieldVelocityVarName"];
                var fieldName = field["fieldName"];
                var stVar = field["fieldStructureVarName"];
                if(fieldContentlet == '__title__'){
                	return "<a href=\"javascript: doSearch (1, 'title')\">" + fieldName + "</a>";
                }else if(fieldContentlet == '__type__'){
                	return "<a href=\"javascript: doSearch (1, 'structurename')\">" + fieldName + "</a>";
                }else if(fieldContentlet == '__wfstep__'){
                    return "<a href=\"javascript: doSearch (1, 'wfCurrentStepName')\">" + fieldName + "</a>";
                }else{
                	return "<a href=\"javascript: doSearch (1, '" + stVar + "." + fieldContentlet + "')\">" + fieldName + "</a>";
                }
        }

        /* Displays the Push Publish dialog. If the content is archived, allow
        users to ONLY do a "Remove", not a "Push" or "Push & Remove". */
        function remotePublish(objId, referrer, isArchived) {
            pushHandler.showDialog(objId, false, isArchived);
        }

        function changeView(view, skipLocalStorage = false) {
          if (!skipLocalStorage) {
            localStorage.setItem(DOTCMS_DATAVIEW_MODE, view)
          }
          state.view = view;

          let card = getViewCardEl();
          const list = getListEl();

          if (state.view === 'list') {
            const selectedInodes = getSelectedInodesFromCardView();

            if (!list.innerHTML.length) {
                fillResultsTable(state.headers, state.data);
            }

            const checkboxes = document.querySelectorAll('[name="publishInode"]')
            checkboxes.forEach((item, i) => {
                dijit.byId('checkbox' + i).setValue(selectedInodes.includes(item.value));
            })

            try {
                card.style.display = 'none';
                list.style.display = '';
            } catch (error) {}

          } else {

            // After append the dot-card-view we have to wait to get the HTML node
            if (!card || !card.items.length) {
                fillCardView(state.data)
                setTimeout(() => {
                    card = getViewCardEl();
                }, 0);
            }

            setTimeout(() => {
                card.style.display = '';
                card.value = getSelectedInodesFromList().join(',');
                list.style.display = 'none';
            }, 0);

          }
        }

        function fillCardView(data) {
          const content = data.map(i => {
            return {
              data: {
                ...i,
                title: i.__title__ // Why not `title` coming?
              },
              actions: fillActions(i)
            }
          })

          let viewCard = getViewCardEl();

          if (!viewCard) {
            viewCard = document.createElement('dot-card-view');
            viewCard.style.padding = '0 1rem';
            viewCard.style.fontSize = '16px';
            viewCard.addEventListener('selected', (e) => {
                if (e.detail.length) {
                    enableBulkAvailableActionsButton();
                } else {
                    disableBulkAvailableActionsButton();
                }
            });
			viewCard.addEventListener('cardClick', (e) => {
				openEditModal(e.detail);
			});
            dojo.byId('metaMatchingResultsDiv').appendChild(viewCard);
          }

          viewCard.items = content;
          viewCard.showVideoThumbnail = showVideoThumbnail;
        };

		function openEditModal(data){
			var inode = data.inode;
			var liveSt = data.live === "true" ? "1" : "0";
			var workingSt = data.working === "true" ? "1" : "0";
			var write = userHasWritePermission (data, userId) ? "1" : "0";
			var typeVariable = data.typeVariable;

			if (data.structureInode == '<%=calendarEventInode %>') {
				editEvent(inode, '<%=user.getUserId()%>', '<%= referer %>', liveSt, workingSt, write, typeVariable);
			}else{
				editContentlet(inode, '<%=user.getUserId()%>', '<%= referer %>', liveSt, workingSt, write, typeVariable);
			}
		};

		function fillActions(data) {
			let actions = []

			const live = data["live"] == "true";
			const working = data["working"] == "true";
			const deleted = data["deleted"] == "true";
			const locked = data["locked"] == "true";
			const liveSt = live ? "1" : "0";
			const workingSt = working ? "1" : "0";
			const permissions = data["permissions"];
			const read = userHasReadPermission(data, userId) ? "1" : "0";
			const write = userHasWritePermission(data, userId) ? "1" : "0";
			const publish = userHasPublishPermission(data, userId) ? "1" : "0";
			const contentStructureType = data["contentStructureType"];
			const structure_id = data["structureInode"];
			const hasLiveVersion = data["hasLiveVersion"];
			const typeVariable = data.typeVariable;

			const contentAdmin = new dotcms.dijit.contentlet.ContentAdmin(data.identifier, data.inode, data.languageId);
			const wfActionMapList = JSON.parse(data["wfActionMapList"]);

			if ((live || working) && (read=="1") && (!deleted)) {
				if(structure_id == '<%=calendarEventInode %>'){
					actions.push({ label: write === '1' ? '<%=LanguageUtil.get(pageContext, "Edit") %>' : '<%=LanguageUtil.get(pageContext, "View") %>',
						action: () => { editEvent(data.inode, '<%= user.getUserId() %>', '<%= referer %>', liveSt, workingSt, write, typeVariable)}
					});
				} else {
					actions.push({ label: write === '1' ? '<%=LanguageUtil.get(pageContext, "Edit") %>' : '<%=LanguageUtil.get(pageContext, "View") %>',
						action: () => { editContentlet(data.inode, '<%= user.getUserId() %>', '<%= referer %>', liveSt, workingSt, write, typeVariable)}
					});
				}
			}

			wfActionMapList.map((wfAction) => {
				actions.push({ label: wfAction.name,
					action: () => { contentAdmin.executeWfAction(wfAction.id, wfAction.assignable.toString(), wfAction.commentable.toString(), wfAction.hasPushPublishActionlet.toString(), data.inode, wfAction.moveable ? wfAction.moveable.toString() : 'false')}
				});
			});

			if (enterprise && sendingEndpoints ) {
				actions.push({ label: '<%=LanguageUtil.get(pageContext, "Remote-Publish") %>',
					action: () => { remotePublish(data.inode, '<%= referer %>', deleted )}
				});
				actions.push({ label: '<%=LanguageUtil.get(pageContext, "Add-To-Bundle") %>',
					action: () => { addToBundle(data.inode, '<%= referer %>')}
				});
			}

			if (locked && (write=="1")){
				if(structure_id == '<%=calendarEventInode %>') {
					actions.push({ label: '<%=LanguageUtil.get(pageContext, "Unlock") %>',
						action: () => { unlockEvent(data.inode, '<%= user.getUserId() %>', '<%= referer %>', liveSt, workingSt, write)}
					});
				}else{
					actions.push({ label: '<%=LanguageUtil.get(pageContext, "Unlock") %>',
						action: () => { _unlockAsset(data.inode)}
					});
				}
			}

			return actions;
		}

        function fillResultsTable(headers, data) {
                headerLength = headers.length;
                var table = getListEl();

                //Filling Headers
                var row = table.insertRow(table.rows.length);

                var th = document.createElement('th');
                th.setAttribute("width","64px");
                th.innerHTML = '&nbsp;';
                row.appendChild(th);

                for (var i = 0; i < headers.length; i++) {
                        var header = headers[i];
                        th = document.createElement('th');
                        if (i == 0) {

                                th.innerHTML = '&nbsp;';
                                if(dijit.byId("checkAll")){
                                        dijit.byId("checkAll").destroy();
                                }
                                th.setAttribute("valign","bottom");
                                th.setAttribute("nowrap","true");
                                th.innerHTML = '<input type="checkbox" dojoType="dijit.form.CheckBox" name="checkAll" id="checkAll" onclick="checkUncheckAll()">&nbsp;&nbsp;' + getHeader(header);
                                row.appendChild(th);

                                th = document.createElement('th');
                                th.style.width="32px";
                                th.innerHTML = '&nbsp;';
                                th.setAttribute("nowrap","true");
                                row.appendChild(th);

                                th = document.createElement('th');
                                th.style.width="54px";
                                th.innerHTML = '&nbsp;';
                                th.setAttribute("nowrap","true");
                                row.appendChild(th);

                                th = document.createElement('th');
                                th.style.width="32px";
                                th.innerHTML = '&nbsp;';
                                th.setAttribute("nowrap","true");
                                row.appendChild(th);


                        } else {
                        th.innHTML =
                                th.innerHTML = getHeader(header);
                                th.setAttribute("valign","bottom");
                                row.appendChild(th);
                        }
                }
                th = document.createElement('th');
				th.setAttribute("valign","bottom");
				th.style.width="120px";
                th.innerHTML = "<a href=\"javascript: doSearch (1, 'modUser')\"><%= LanguageUtil.get(pageContext, "Last-Editor") %></a>";
                row.appendChild(th);

                th = document.createElement('th');
				th.setAttribute("valign","bottom");
				th.style.width="120px";
                th.innerHTML = "<a class=\"beta\" href=\"javascript: doSearch (1, 'modDate')\"><%= LanguageUtil.get(pageContext, "Last-Edit-Date") %></a>";
                row.appendChild(th);

                th = document.createElement('th');
                row.appendChild(th);


                var languageId;
                var locale;

                var live;
                var working;
                var deleted;
                var locked;
                var liveSt;
                var workingSt;
                var permissions;
                var write;
                var publish;
                var popupMenusDiv = dojo.byId("results_table_popup_menus");
                var popupMenu = "";
                var popupMenu2 = "";
                var wfActionMapList;
                var structure_id;
                var contentStructureType;
                var typeVariable;

                cbContentInodeList = data;

                //Filling data
                for (var i = 0; i < data.length; i++) {
                    var popupMenuItems = "";
                    var row = table.insertRow(table.rows.length);

                    var cellData = data[i];
                    row.setAttribute("id","tr" + cellData.inode);

                    var cell = row.insertCell (row.cells.length);

                    var iconName = cellData.baseType !== 'FILEASSET' ?
                        cellData.contentTypeIcon : getIconName(cellData['__type__']);
                    var hasTitleImage = (cellData.hasTitleImage ==='true');

                    var modDate = cellData.modDateMilis;



                    let holderDiv = document.createElement("div");
                    holderDiv.className="listingThumbDiv";


                    let cardThumbnail = document.createElement("dot-contentlet-thumbnail");

                    cardThumbnail.iconSize="48px";
                    cardThumbnail.backgroundImage="true";
                    cardThumbnail.contentlet=cellData;

                    holderDiv.appendChild(cardThumbnail);
                    cell.appendChild(holderDiv);
                    live = cellData["live"] == "true"?true:false;
                    working = cellData["working"] == "true"?true:false;
                    deleted = cellData["deleted"] == "true"?true:false;
                    locked = cellData["locked"] == "true"?true:false;
                    liveSt = live?"1":"0";
                    workingSt = working?"1":"0";
                    permissions = cellData["permissions"];
                    read = userHasReadPermission (cellData, userId)?"1":"0";
                    write = userHasWritePermission (cellData, userId)?"1":"0";
                    publish = userHasPublishPermission (cellData, userId)?"1":"0";
                    contentStructureType = cellData["contentStructureType"];
                    typeVariable = cellData["typeVariable"];
                    structure_id = cellData["structureInode"];
                    hasLiveVersion = cellData["hasLiveVersion"];
                    holderDiv.setAttribute('data-inode', cellData["inode"]);
                    holderDiv.setAttribute('data-live', liveSt);
                    holderDiv.setAttribute('data-working', workingSt);
                    holderDiv.setAttribute('data-write', write);
                    holderDiv.setAttribute('data-typevariable', typeVariable);
                    holderDiv.addEventListener('click', function(e){
                        let dataSet =  e.currentTarget.dataset;
                        editContentlet(dataSet["inode"],'<%= user.getUserId() %>','<%= referer %>', dataSet["live"] , dataSet["working"] , dataSet["write"], dataSet["typevariable"] );
                    }, false);








                    cell.setAttribute("style","height: 85px; text-align: center;");

                    for (var j = 0; j < headers.length; j++) {
                        var header = headers[j];
                        var cell = row.insertCell (row.cells.length);
                        cell.setAttribute("align","left");

                        if (j == 0 ) {

                            let fieldVarName  = header["fieldVelocityVarName"];
                            let fieldVarTitle = cellData[fieldVarName + "_title_"];
                            fieldVarTitle     = fieldVarTitle || cellData[fieldVarName]
                            var value         = titleCell(cellData,fieldVarTitle, i);

                            if (value != null){
                                cell.innerHTML = value;
                            }

                            var cell = row.insertCell (row.cells.length);
                            cell.setAttribute("align", "center");
                            cell.innerHTML = '<dot-state-icon />';
                            var stateIcon = document.querySelector("#tr" + cellData.inode + " dot-state-icon");
                            stateIcon.state = cellData;
                            stateIcon.size = '16px';


                            var cell = row.insertCell (row.cells.length);
                            cell.setAttribute("align", "center");
                            cell.innerHTML = '<dot-badge style="white-space: nowrap" bordered="true">' + cellData.language + '</dot-badge>';

                            var cell = row.insertCell (row.cells.length);
                            cell.setAttribute("align", "center");
                            cell.innerHTML = '<dot-contentlet-lock-icon locked="' + cellData.locked + '" />';

                        } else {

                            let fieldVarName  = header["fieldVelocityVarName"];
                            let fieldVarTitle = cellData[fieldVarName + "_title_"];
                            fieldVarTitle     = fieldVarTitle || cellData[fieldVarName]
                            var value         = fieldVarTitle;

                            if (value != null){
                                cell.innerHTML = value;
                            }
                        }
                    }

                    var cell = row.insertCell (row.cells.length);
                    cell.innerHTML = cellData["modUser"];
                    cell.style.whiteSpace="nowrap";

                    var cell = row.insertCell (row.cells.length);
                    cell.setAttribute("nowrap","true");
                    cell.style.textAlign="right";
                    cell.style.whiteSpace="nowrap";
                    cell.innerHTML = cellData["modDate"];

                    var cell = row.insertCell (row.cells.length);
                    cell.innerHTML = '<span class=\"dijitIcon actionIcon content-search__action-item\" id=\"touchAction' + i + '\"></span>';

                    contentAdmin = new dotcms.dijit.contentlet.ContentAdmin(cellData.identifier,cellData.inode,cellData.languageId);

                    wfActionMapList = JSON.parse(cellData["wfActionMapList"]);

                    dijit.registry.remove("popupTr"+i);

                    if(dijit.byId("popupTr"+i)){
                        dijit.byId("popupTr"+i).destroy();
                    }

                    dijit.registry.remove("popup2Tr"+i);

                    if(dijit.byId("popup2Tr"+i)){
                        dijit.byId("popup2Tr"+i).destroy();
                    }


                    popupMenu += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popupTr" + i + "\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"tr" + cellData.inode + "\">";
                    popupMenu2 += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popup2Tr" + i + "\" leftClickToOpen=\"true\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"touchAction" + i + "\">";

                    // NEW CONTEXT MENU

                    if ((live || working) && (read=="1") && (!deleted)) {
                            if(structure_id == '<%=calendarEventInode %>'){
                                if (write=="1"){
                                popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ",'" + typeVariable + "');\"><%=LanguageUtil.get(pageContext, "Edit") %></div>";
                                }else{
                                popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ",'" + typeVariable + "');\"><%=LanguageUtil.get(pageContext, "View") %></div>";
                                }
                            }else{
                                if (write=="1"){
                                popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ",'" + typeVariable + "');\"><%=LanguageUtil.get(pageContext, "Edit") %></div>";
                                }else{
                                popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ",'" + typeVariable + "');\"><%=LanguageUtil.get(pageContext, "View") %></div>";
                                }
                            }
                    }


                    for (var k = 0; k < wfActionMapList.length; k++) {
                        var name = wfActionMapList[k].name;
                        var id = wfActionMapList[k].id;
                        var assignable = wfActionMapList[k].assignable;
                        var commentable = wfActionMapList[k].commentable;
                        var moveable = wfActionMapList[k].moveable;
                        var icon = wfActionMapList[k].icon;
                        var requiresCheckout = wfActionMapList[k].requiresCheckout;
                        var wfActionNameStr = wfActionMapList[k].wfActionNameStr;
                        var hasPushPublishActionlet = wfActionMapList[k].hasPushPublishActionlet;

                        popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\""+icon+"\" onClick=\"contentAdmin.executeWfAction('" + id + "', '" + assignable + "', '" + commentable + "', '" + hasPushPublishActionlet + "', '" + cellData.inode + "', '" + moveable + "');\">"+wfActionNameStr+"</div>";

                    }

                    if(enterprise && sendingEndpoints ) {
                            popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"sServerIcon\" onClick=\"remotePublish('" + cellData.inode + "','<%= referer %>', " + deleted + ");\"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>";

                            popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"addToBundle('" + cellData.inode + "','<%= referer %>');\"><%=LanguageUtil.get(pageContext, "Add-To-Bundle") %></div>";
                    }


                    // END NEW CONTEXT

                    if (locked && (write=="1")){
                        if(structure_id == '<%=calendarEventInode %>'){
                            popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unlockIcon\" onClick=\"unlockEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Unlock") %></div>";
                        }else{
                            popupMenuItems += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unlockIcon\" onClick=\"_unlockAsset('" + cellData.inode + "');\"><%=LanguageUtil.get(pageContext, "Unlock") %></div>";
                        }
                    }

                    popupMenu += popupMenuItems + "</div>";
                    popupMenu2 += popupMenuItems + "</div>";
                }
                popupMenusDiv.innerHTML = popupMenu + popupMenu2;


                dojo.parser.parse(dojo.byId("results_table_popup_menus"));
                dojo.parser.parse(dojo.byId("results_table"));

        }

        function getIconName(iconCode) {
            var startIndex = iconCode.indexOf('<span class') + 13;
            var endIndex = iconCode.indexOf('</span>') - 2;
            return iconCode.substring(startIndex, endIndex);
        }

        function replaceWithIcon(parentElement, iconName) {
            parentElement.innerHTML = '<dot-contentlet-icon icon="' + iconName +'" size="48px" />'
        }

        function clearSearch () {

                document.getElementById('currentSortBy').value=DOTCMS_DEFAULT_CONTENT_SORT_BY;
                dijit.byId("scheme_id").set("value",'catchall');
                dijit.byId("showingSelect").set("value", "all");
                dijit.byId("allFieldTB").set("value", "");
                dijit.byId('FolderHostSelector').set('value', "<%= conHostValue %>");
                const tree = dijit.byId('FolderHostSelector-tree');
                tree.set('selectedItem', "<%= conHostValue %>" );
                tree.collapseAll();

                var div = document.getElementById("matchingResultsBottomDiv");
                div.innerHTML = "";
                div = document.getElementById("metaMatchingResultsDiv");
                div.style.display='none';

                for (var i = 0; i < categories.length; i++) {
                        var mainCat = categories[i];
                        var selectId = mainCat["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select";
                        var selectObj = document.getElementById(selectId);
                        var options = selectObj.options;
                        for (var j = 0; j < options.length; j++) {
                                var opt = options[j];
                                opt.selected = false;
                        }
                }
                var structureInode = dijit.byId('structure_inode').value;
                var structureVelraw=dojo.byId("structureVelocityVarNames").value;
                var structInoderaw=dojo.byId("structureInodesList").value;
                var structureVel=structureVelraw.split(";");
                var structInode=structInoderaw.split(";");
                var selectedStruct="";
                for(var m2=0; m2 <= structInode.length ; m2++ ){
             if(structureInode==structInode[m2]){
                 selectedStruct=structureVel[m2];
                 }
                        }


                for (var h = 0; h < currentStructureFields.length; h++) {
                        var field = currentStructureFields[h];
                        var fieldId = selectedStruct+"."+field["fieldVelocityVarName"] + "Field";
                        var formField = document.getElementById(fieldId);
                        //DOTCMS-3232
                if(field["fieldFieldType"] == "host or folder"){
                   if(dijit.byId('FolderHostSelector')!=null){
                       dijit.byId('FolderHostSelector')._setValueAttr("<%= UtilMethods.isSet(crumbtrailSelectedHostId)? crumbtrailSelectedHostId: ""%>");
                       getHostValue();
                   }

                }
                        if(formField != null) {
                                 if(formField.type=='select-one' || formField.type=='select-multiple'){
                                          var options = formField.options;
                                          for (var j = 0; j < options.length; j++) {
                                                var opt = options[j];
                                                opt.selected = false;
                                          }
                                  } else {

                                          var dotCurrentFieldType = formField.getAttribute("dotfieldtype");
                                          if (dotCurrentFieldType && dotCurrentFieldType == "tag") {
                                              //Clean up the tag search field
                                              clearSuggestTagsForSearch();
                                              removeAllTags();
                                          } else {

                                              formField.value = "";

                                              var temp = dijit.byId(formField.id);

                                              if(temp){
                                                try{
                                                    temp.attr('value','');
                                                   temp.setDisplayedValue('');
                                                 }catch(e){console.log(e);}
                                              }
					                      }
                                  }
                        }
                }

                for(var i=0;i < radiobuttonsIds.length ;i++ ){
                        var formField = document.getElementById(radiobuttonsIds[i]);
                        if(formField != null && formField.type=='radio') {
                            var values = "";
                                if (formField.checked) {
                                        var temp = dijit.byId(formField.id);
                                        temp.attr('checked',false);
                                }
                        }
                }

                for(var i=0;i < checkboxesIds.length ;i++ ){
                        var formField = document.getElementById(checkboxesIds[i]);
                        if(formField != null && formField.type=='checkbox') {
                            var values = "";
                                if (formField.checked) {
                                        var temp = dijit.byId(formField.id);
                                        //temp.reset();
                                        temp.setValue(false);
                                }
                        }
                }
	        document.getElementById("Identifier").value = "";
	        document.getElementById("lastModDateFrom").value = "";
	        document.getElementById("lastModDateTo").value = "";

       		var showDeletedCB = dijit.byId("showDeletedCB");
	        if(showDeletedCB!=null){
	                if(showDeletedCB.checked) {
	                  showDeletedCB.setValue(false);
	                }
	        }

	        var filterSystemHostCB = dijit.byId("filterSystemHostCB");
	        if(filterSystemHostCB!=null){
	                if(filterSystemHostCB.checked) {
	                  filterSystemHostCB.setValue(false);
	                }
	        }

	        var filterLockedCB = dijit.byId("filterLockedCB");
	        if(filterLockedCB!=null){
	                if(filterLockedCB.checked) {
	                  filterLockedCB.setValue(false);
	                }
	        }

	        var filterUnpublishCB = dijit.byId("filterUnpublishCB");
	        if(filterUnpublishCB!=null){
	               if(filterUnpublishCB.checked) {
	                 filterUnpublishCB.setValue(false);
	               }
	        }

	        dwr.util.removeAllRows("results_table");
	        document.getElementById("nextDiv").style.display = "none";
	        document.getElementById("previousDiv").style.display = "none";



	        hideMatchingResults ();
            doSearch(1, DOTCMS_DEFAULT_CONTENT_SORT_BY);

        }

        function userHasReadPermission (contentlet, userId) {
                <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                        return true;
                <%} %>

                var permissions = contentlet["permissions"];
                var owner = contentlet["owner"];
                var ownerCanRead = contentlet["ownerCanRead"];
                var hasPermission = false;
                if(owner == userId && ownerCanRead.valueOf() == 'true'){
                        return true;
                }
                for (var i = 0; i < userRolesIds.length; i++) {
                        var roleId = userRolesIds[i];
                        var re = new RegExp("P" + roleId + "\\.1P");
                        if (permissions.match(re)) {
                                hasPermission = true;
                        }
                }
                return hasPermission;
        }

        function userHasWritePermission (contentlet, userId) {
                <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                        return true;
                <%} %>
                var permissions = contentlet["permissions"];
                var owner = contentlet["owner"];
                var ownerCanWrite = contentlet["ownerCanWrite"];
                var hasPermission = false;
                <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                        return true;
                <%} %>
                if(owner == userId && ownerCanWrite.valueOf() == 'true'){
                        return true;
                }
                for (var i = 0; i < userRolesIds.length; i++) {
                        var roleId = userRolesIds[i];
                        var re = new RegExp("P" + roleId + "\\.2P");
                        if (permissions.match(re)) {
                                hasPermission = true;
                        }
                }
                return hasPermission;
        }

        function userHasPublishPermission (contentlet, userId) {


                <%if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){ %>
                        return true;
                <%} %>

                var permissions = contentlet["permissions"];
                var owner = contentlet["owner"];
                var ownerCanPublish = contentlet["ownerCanPublish"];
                var hasPermission = false;
                if(owner == userId && ownerCanPublish.valueOf() == 'true'){
                        return true;
                }
                for (var i = 0; i < userRolesIds.length; i++) {
                        var roleId = userRolesIds[i];
                        var re = new RegExp("P" + roleId + "\\.4P");
                        if (permissions.match(re)) {
                                hasPermission = true;
                        }
                }
                return hasPermission;
        }



        var exportContentButton;
        function showMatchingResults (num,begin,end,totalPages) {
                        if (exportContentButton) {
                            exportContentButton.destroyRendering();
                        }

                        var div = document.getElementById("metaMatchingResultsDiv");

                        div.style.display='';

                    //Top Matching Results

                    eval("totalContents=" + num + ";");

                    let showDataViewButton = '';
                    if (!totalPages) {
                        showDataViewButton = '; opacity: 0'
                        const viewCard = getViewCardEl();
                        if (viewCard) {
                            viewCard.items = [];

                        }

                        const list = getListEl();
                        list.style.display = '';
                    }

                    let dataViewButton = "<dot-data-view-button " + showDataViewButton +"\" value=\""+ state.view +"\"></dot-data-view-button>";

                        div = document.getElementById("matchingResultsDiv")
                        var structureInode = dijit.byId('structure_inode').value;
                        var strbuff = dataViewButton + "<div class=\"contentlet-results\"><%= LanguageUtil.get(pageContext, "Showing") %> " + begin + "-" + end + " <%= LanguageUtil.get(pageContext, "of1") %> " + num + "</div>";
                        var actionPrimaryMenu = dijit.byId('actionPrimaryMenu');
                        var donwloadToExcelMenuItem = dijit.byId('donwloadToExcel');
                        if (num > 0 && structureInode != "catchall") {
                            if (!donwloadToExcelMenuItem) {
                                actionPrimaryMenu.addChild(new dijit.MenuItem({
                                    label: "<%= LanguageUtil.get(pageContext, "Export") %>",
                                    onClick: donwloadToExcel,
                                    id: 'donwloadToExcel'
                                }));
                            }
                        } else {
                            if (donwloadToExcelMenuItem) {
                                actionPrimaryMenu.removeChild(donwloadToExcelMenuItem);
                                donwloadToExcelMenuItem.destroy();
                            }

                        }

                        div.innerHTML = strbuff;
                        div.style.display = "";

                        if (totalPages) {
                            setDotSelectButton();
                        }

                        //Bottom Matching Results
                        var div = document.getElementById("matchingResultsBottomDiv")
                        var strbuff = "<table border='0' width=\"100%\"><tr><td align='center' nowrap='true'><b><%= LanguageUtil.get(pageContext, "Showing") %> " + begin + " - " + end + " <%= LanguageUtil.get(pageContext, "of1") %> " + num;
                        if(num > 0)
                        {
                                strbuff += " | <%= LanguageUtil.get(pageContext, "Pages") %> ";
                                for(i = 4;i >= 1;i--)
                                {
                                        var auxPage = currentPage - i;
                                        if(auxPage >= 1)
                                        {
                                                strbuff += "<a href='javascript:doSearch (" + auxPage + ");'> " + auxPage + "</a> ";
                                        }
                                }
                                strbuff += " " + currentPage + " ";
                                for(i = 1;i <= 4;i++)
                                {
                                        var auxPage = currentPage + i;
                                        if(auxPage <= totalPages)
                                        {
                                                strbuff += "<a href='javascript:doSearch(" + auxPage + ");'> " + auxPage + "</a> ";
                                        }
                                }
                        }
                        strbuff += "</b></td></tr></table>";
                        div.innerHTML = strbuff;
        }

        function hideMatchingResults () {
                        var div = document.getElementById("matchingResultsDiv")
                        div.style.display = "none";
        }

        function queryContentJSONPost(url, queryRaw, sortBy) {
            queryRaw = queryRaw.replace(/%27/g, "'").replace(/%22/g, '&quot;');
            var query = `{
    		    "query" : "${queryRaw}" },
	    	    "sort" : { "moddate":"${sortBy}" },
	    	    "size": 20,
	    	    "from": 0
            }`;

            var xhrArgs = {
             url: url,
             postData: query,
             headers: {
                 "Accept" : "application/json",
                 "Content-Type" : "application/json"
              },
             handleAs : "json",
             load: function(data) {
                var myWindow = window.open("", "_blank");
                data = JSON.stringify(data).replace(/[<>&\n]/g, function(x) {
                    return {
                        '<': '&lt;',
                        '>': '&gt;',
                        '&': '&amp;',
                        '\n': '<br />',
                    }[x];
                });
                myWindow.document.write(data);
             }
         }
         dojo.xhrPost(xhrArgs);

        }


        function getRestBasePostUrl(){
             return location.protocol + '//' + location.host + '/api/content/_search';
        }

        function fillQuery (counters) {
                        var restBaseUrl = location.protocol + '//' + location.host + '/api/content/render/false';
                        var restBasePostUrl = getRestBasePostUrl();

                        queryRaw = counters["luceneQueryRaw"];
                        var encodedQueryRaw = queryRaw.replace(/'/g, "%27").replace(/"/g, "%22");
                        var queryfield=document.getElementById("luceneQuery");
                        queryfield.value=queryRaw;
                        var velocityCode = counters["velocityCode"];
                        var relatedQueryByChild = counters["relatedQueryByChild"];
                        var sortBy = counters["sortByUF"];
                        var div = document.getElementById("queryResults");
                        var apicall=restBaseUrl + "/query/"+queryRaw+"/orderby/"+sortBy;
                        var test_api_xml_link="/api/content/render/false/type/xml/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);
                        var test_api_json_link="/api/content/render/false/type/json/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);
                        var apicall_urlencode=restBaseUrl + "/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);

                        var expiredInodes = counters["expiredInodes"];
                        dojo.byId("expiredInodes").value=expiredInodes;
                        dojo.byId("expireDateReset").value="";


                        div.innerHTML = "<div class='contentViewDialog' style=\"white-space: pre;\">" +

                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "frontend-query") %></div>"+
                            "<div class='contentViewQuery'><code>" + velocityCode + "</code></div>";

                        if (relatedQueryByChild == null){
                            div.innerHTML += "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "The-actual-query-") %></div>"+
                                "<div class='contentViewQuery'><code>"+queryRaw+"</code></div>";
                        } else{
                            test_api_xml_link +=  "/related/" + relatedQueryByChild;
                            test_api_json_link += "/related/" + relatedQueryByChild;
                            apicall_urlencode += "/related/" + relatedQueryByChild;
                        }

                        div.innerHTML +=
                            "<style>" +
                                ".dot-api-link {" +
                                    "align-items: center; border-radius: 2px; border: solid 1px var(--color-sec); color: var(--color-sec); " +
                                    "display: inline-flex; line-height: 1em; padding: 0.25rem 6px 0.25rem 0.5rem; text-decoration: none; " +
                                    "text-transform: uppercase; transition: background-color 150ms ease, color 150ms ease; cursor: pointer;" +
                                "}" +
                                ".dot-api-link:hover {" +
                                    "background-color: var(--color-sec);" +
                                    "color: white;" +
                                "}" +
                            "</style>" +
                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "rest-api-call-post") %></div>"+
                            "<div class='contentViewQuery'><code>" + "curl -XPOST '" + restBasePostUrl + "' \\<br/>" +
                            "-H 'Content-Type: application/json' \\<br/>" +
                            "-d '{<br/>" +
                            "<span style='margin-left: 20px'>\"query\": \"" + queryRaw + "\",</span><br/>" +
                            "<span style='margin-left: 20px'>\"sort\": \"" + sortBy + "\",</span><br/>" +
                            "<span style='margin-left: 20px'>\"limit\": 20,</span><br/>" +
                            "<span style='margin-left: 20px'>\"offset\": 0</span><br/>" +
                            "}'</code></div>" +

                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "rest-api-call-urlencoded") %></div>"+
                            "<div class='contentViewQuery'><code>"+apicall_urlencode+"</code></div>"+

                            "<div class='contentViewQuery' style='padding:20px;padding-top:10px;color:#333;'>REST API: " +
                            "<span class='dot-api-link' " +
                            "onClick=\"queryContentJSONPost(getRestBasePostUrl(), '" + encodedQueryRaw + "', '" + sortBy + "')\">API</span></a>"+
                            "</div>"+

                            "<b><%= LanguageUtil.get(pageContext, "Ordered-by") %>:</b> " + sortBy +
                            "<ul><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint2") %> " +
                            "</li><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint3") %> " +
                            "</li><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint4") %> " +
                            "<li><%= LanguageUtil.get(pageContext, "message.contentlet.hint6")%></li>"+
                            "<li><%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "message.contentlet.note1")) %></li>"+
                            "</ul></div>";

        }

        function showHideQuery () {
                dijit.byId('queryDiv').show();
        }

        function useLoadingMessage(message) {
          var loadingMessage;
          if (message) loadingMessage = message;

          dwr.engine.setPreHook(function() {
              var messageZone = $('messageZone');
              messageZone.innerHTML = loadingMessage;
              messageZone.style.display = '';
            });

          dwr.engine.setPostHook(function() {
                if ($('messageZone') != null)
                    $('messageZone').style.display = 'none';
          });
        }

        function showHideHints () {
                dijit.byId('hintsdiv').show();
        }
        //DOTCMS-3232
        function getHostValue(){
          if(!isInodeSet(dijit.byId('FolderHostSelector').attr('value'))){
            dojo.byId("hostField").value = "";
            dojo.byId("folderField").value = "";
          }else{
            var data = dijit.byId('FolderHostSelector').attr('selectedItem');
                if(data["type"]== "host"){
                        dojo.byId("hostField").value =  dijit.byId('FolderHostSelector').attr('value');
                        dojo.byId("folderField").value = "";
                }else if(data["type"]== "folder"){
                        dojo.byId("hostField").value = "";
                    dojo.byId("folderField").value =  dijit.byId('FolderHostSelector').attr('value');
            }

            conHostFolderValue = dijit.byId('FolderHostSelector').hostFolderSelectedName.value;
          }
   }

    function checkAll(check) {
        selectBox = document.getElementsByName("publishInode");
        for (i=0;i< selectBox.length;i++) {
            selectBox[i].checked = check;
        }
        togglePublish();
    }

    function togglePublish(){
        var cbArray = document.getElementsByName("publishInode");
        var showArchive =  (dijit.byId("showingSelect").getValue() == "archived");
        if(typeof event !== 'undefined' && event.shiftKey && event.target.checked){

            var hasChecked=false;
            for(i = 0;i< cbArray.length ;i++){
                if (cbArray[i].checked) {
                    hasChecked=true;
                }
                dijit.byId(cbArray[i].id).setChecked(hasChecked);
                cbArray[i].checked=true;
                if(cbArray[i].id==event.target.id){
                    break;
                }

            }
        }


        for(i = 0;i< cbArray.length ;i++){
            if (cbArray[i].checked) {
                enableBulkAvailableActionsButton()
                return;
            }
        }

        // nothing selected
        disableBulkAvailableActionsButton();
    }

    function enableBulkAvailableActionsButton() {
        dijit.byId('bulkAvailableActions').setAttribute("disabled", false);
    }

    function disableBulkAvailableActionsButton() {
        dijit.byId('bulkAvailableActions').setAttribute("disabled", true);
    }


    dojo.addOnLoad(function () {
        structureChanged(true);
        //useLoadingMessage("<i class='loadingIcon'></i> Loading");

        //DWR sync mode doesn't work in Chrome. Forcing sync with the flag 'loadingSearchFields'
        if (dojo.isChrome) {
                setTimeout("checkSearchFieldLoaded()", 50);
        } else {
                initialLoad();
        }
    });

    function checkSearchFieldLoaded() {
            if (!loadingSearchFields) {
                    initialLoad();
            } else {
                    setTimeout("checkSearchFieldLoaded()", 50);
            }
    }

    function resetHostValue() {
        if(document.getElementById('FolderHostSelector-hostFolderSelect')){
          if(document.getElementById('FolderHostSelector-hostFolderSelect').value == ""){
              dojo.byId("hostField").value = "";
                  dojo.byId("folderField").value = "";
              dijit.byId('FolderHostSelector')._resetValue();
          }
        }
    }



         function unlockSelectedContentlets(){
            //disableButtonRow();
            var form = copySearchForm()
            form.cmd.value = 'full_unlock_list';
            form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_unlock_list" /></portlet:actionURL>';


          /*if we have a date*/
                        var dateFrom= null;
                        var dateTo= null;
                        if((document.getElementById("lastModDateFrom").value!="")){
                                dateFrom = document.getElementById("lastModDateFrom").value;
                                var dateFromsplit = dateFrom.split("/");
                                if(dateFromsplit[0]< 10) dateFromsplit[0]= "0"+dateFromsplit[0]; if(dateFromsplit[1]< 10) dateFromsplit[1]= "0"+dateFromsplit[1];
                                dateFrom= dateFromsplit[2]+dateFromsplit[0]+dateFromsplit[1]+"000000";
                                form.action+= "&modDateFrom="+dateFrom;
                        }

                        if((document.getElementById("lastModDateTo").value!="")){
                                dateTo = document.getElementById("lastModDateTo").value;
                                var dateTosplit = dateTo.split("/");
                                if(dateTosplit[0]< 10) dateTosplit[0]= "0"+dateTosplit[0]; if(dateTosplit[1]< 10) dateTosplit[1]= "0"+dateTosplit[1];
                                dateTo= dateTosplit[2]+dateTosplit[0]+dateTosplit[1]+"235959";
                                form.action+= "&modDateTo="+dateTo;
                        }
            form.action+= "&structure_id=<%=structure.getInode()%>";
            form.submit();
            form.parentNode.removeChild(form);
         }

    //*************************************
    //
    //
    //  ContentAdmin Obj
    //
    //
    //*************************************




    dojo.declare("dotcms.dijit.contentlet.ContentAdmin", null, {
    	contentletIdentifier : "",
    	contentletInode : "",
    	languageID : "",
    	wfActionId:"",
    	constructor : function(contentletIdentifier, contentletInode,languageId ) {
    		this.contentletIdentifier = contentletIdentifier;
    		this.contentletInode =contentletInode;
    		this.languageId=languageId;


    	},


    	executeWfAction: function(wfId, assignable, commentable, hasPushPublishActionlet, inode, moveable ){
            this.wfActionId = wfId;
    		if(assignable == "true" || commentable == "true" || hasPushPublishActionlet == "true" || moveable === "true" ){

                let workflow = {
                  actionId:wfId,
                  inode:inode
                };

                var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Workflow-Action")%>');
                pushHandler.showWorkflowEnabledDialog(workflow, saveAssignCallBack);
                return;

    		} else{
        		    var wfActionAssign 		= "";
		    		var selectedItem 		= "";
		    		var wfConId 			= inode;
		    		var wfActionId 			= this.wfActionId;
		    		var wfActionComments 	= "";
		    		var publishDate			= "";
		    		var publishTime 		= "";
		    		var expireDate 			= "";
		    		var expireTime 			= "";
		    		var neverExpire 		= "";
		    		var whereToSend 		= "";
                    var pathToMove 			= "";
					BrowserAjax.saveFileAction(selectedItem, wfActionAssign, wfActionId, wfActionComments, wfConId, publishDate,
		    				publishTime, expireDate, expireTime, neverExpire, whereToSend, pathToMove, fileActionCallback
                    );
    		}

    	}
    });


    function saveAssignCallBack(actionId, formData) {

        var pushPublish = formData.pushPublish;
        var assignComment = formData.assignComment;

        var selectedItem = "";
        var wfConId =  pushPublish.inode;
        var comments = assignComment.comment;
        var assignRole = assignComment.assign;
        var pathToMove = assignComment.pathToMove;

        var whereToSend = pushPublish.whereToSend;
        var publishDate = pushPublish.publishDate;
        var publishTime = pushPublish.publishTime;
        var expireDate  = pushPublish.expireDate;
        var expireTime  = pushPublish.expireTime;
        var forcePush   = pushPublish.forcePush;
        var neverExpire = pushPublish.neverExpire;

        BrowserAjax.saveFileAction(selectedItem, assignRole, actionId, comments, wfConId, publishDate,
           publishTime, expireDate, expireTime, neverExpire, whereToSend, forcePush, pathToMove, fileActionCallback
        );
    }

    function fileActionCallback (response) {
        if (response.status == "success") {
            setTimeout("refreshFakeJax()", 1000);
            showDotCMSSystemMessage(response.message);
            return;
        }

        // An error happened
        refreshFakeJax();
        showDotCMSErrorMessage(response.message);
    }


	function _unpublishAsset (inode) {
		BrowserAjax.unPublishAsset(inode, function (data) { _unpublishAssetCallback(data) } );
	}

	function _unpublishAssetCallback (response) {

		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish-failed-check-you-have-the-required-permissions")) %>');
		} else {

			refreshFakeJax();
		}
	}


	function _publishAsset (inode) {
		BrowserAjax.publishAsset(inode, function (data) { _publishAssetCallback(data) } );
	}

	function _publishAssetCallback (response) {

		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-failed-check-you-have-the-required-permissions")) %>');
		} else {
			refreshFakeJax();

		}
	}

	function _archiveAsset (inode) {
		BrowserAjax.archiveAsset(inode, function (data) { _archiveAssetCallback(data) } );
	}

	function _archiveAssetCallback (response) {
		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-archive-check-you-have-the-required-permissions")) %>');
		} else {
			refreshFakeJax();
		}
	}

	function _unArchiveAsset (objId, referer) {
		BrowserAjax.unArchiveAsset(objId, _unarchiveAssetCallback);
	}

	function _unarchiveAssetCallback (response) {
		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-un-archive-check-you-have-the-required-permissions")) %>');
		} else {
			refreshFakeJax();

		}
	}

	function _copyContentlet (inode) {

		var loc = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + inode ;
		window.AjaxActionJackson.location = loc;

	}

	function _unarchiveAssetCallback (response) {
		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-un-archive-check-you-have-the-required-permissions")) %>');
		} else {
			refreshFakeJax();

		}
	}

	function _unlockAsset (inode) {
		BrowserAjax.unlockAsset(inode, function (data) { _unlockAssetCallback(data) } );
	}

	function _unlockAssetCallback (response) {
		if (!response) {
			showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Failed-to-unlock-check-you-have-the-required-permissions")) %>');
		} else {
			refreshFakeJax();
		}
	}


	function refreshFakeJax(){

		doSearch();

		setTimeout(function(){ doSearch() }, 1000);

	}

    function angularWorkflowEventCallback () {
        refreshFakeJax();
        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");
    }

    var contentAdmin ;

--></script>
