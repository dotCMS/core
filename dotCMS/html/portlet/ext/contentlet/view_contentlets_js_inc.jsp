<%@page import="java.text.SimpleDateFormat"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.cache.StructureCache"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>

<%boolean canReindex= APILocator.getRoleAPI().doesUserHaveRole(user,APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))|| com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());%>

        dojo.require("dojox.dtl.filter.strings");
        dojo.require("dijit.form.FilteringSelect");
        dojo.require("dijit.form.MultiSelect");
        dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");

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

        var structureInode;
        var currentStructureFields;
        var currentPage = 1;
        var currentSortBy;
        var setDotFieldTypeStr = "";
        var DOT_FIELD_TYPE = "dotFieldType";
        var cbContentInodeList = new Array();
        var totalContents = 0;
        var perPage = <%= com.dotmarketing.util.Config.getIntProperty("PER_PAGE") %>;
        var headerLength = 0;
        var headers;
        var userRolesIds = new Array ();
        var selectedStructureVarName = '';

        var enterprise = <%=LicenseUtil.getLevel() > 199%>;

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
                language = new Array(<%= language.getId() %>, "<%= language.getLanguageCode() %>", "<%= language.getCountryCode() %>", "<%= language.getLanguage() %>", "<%= language.getCountry() %>");
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
				resizeAdvancedSearch();
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
		        duration: 500
		    }).play();
			dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "Hide") %>";
			dojo.cookie("ShAdDi", end, { });
		}



		function toggleAdvancedSearchDiv(){
			// how tall are we
			var showing = dojo.getStyle(dojo.byId('advancedSearchOptions'),'height');

			// resize
			if("0px" == showing || 0 ==showing){
				dojo.cookie("ShAdDi", "0", { });
				dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "Hide") %>";
				resizeAdvancedSearch();
			// hide
			}else{

				dojo.animateProperty({
			        node: dojo.byId("advancedSearchOptions"),
			        properties: {
			            height: {start: showing, end: 0, unit: "px"},
			        },
			        duration: 500
			    }).play();
				dojo.cookie("ShAdDi", 0, { });
				dojo.byId("toggleDivText").innerHTML="<%= LanguageUtil.get(pageContext, "Advanced") %>";
			}
		}






		/**
			focus on search box
		**/
		require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
			dojo.require('dojox.timing');
			t = new dojox.timing.Timer(500);
			t.onTick = function(){
			  focusUtil.focus(dom.byId("allFieldTB"));
			  t.stop();
			}
			t.start();
		});





        function fillResults(data) {
        	//console.log("searching:" +  amISearching);
        	if(amISearching <0){
        		amISearching=0;
        	}
        	else if(amISearching>0){
        		amISearching--;
        	}

        	if(amISearching>0){
        		return;
        	}

            var counters = data[0];
            var hasNext = counters["hasNext"];
            var hasPrevious = counters["hasPrevious"];
            var total = counters["total"];
            var begin = counters["begin"];
            var end = counters["end"];
    		var totalPages = counters["totalPages"];

            headers = data[1];

            for (var i = 3; i < data.length; i++) {
                    data[i - 3] = data[i];
            }
            data.length = data.length - 3;

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
                            showMatchingResults (0,0,0,0);
                            fillQuery (counters);
                            dijit.byId("searchButton").attr("disabled", false);
                            //dijit.byId("clearButton").setAttribute("disabled", false);
                    }

                    return;
            }

            fillResultsTable (headers, data);
            showMatchingResults (total,begin,end,totalPages);
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
            amISearching--;

        }



        function titleCell (data,text, x) {
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

                var editRef ='';

                if(selectedStructureVarName == 'calendarEvent'){
              editRef = " editEvent('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ") ";
            }else{
              editRef = " editContentlet('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ") ";
            }

            var ref = "<table class='contentletInnerTable'><tr>";
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
                ref+=   "<a  href=\"javascript: " + editRef + "\">";
                ref+=   text;
                ref+=   "</a>";
                ref+=   "</td>";
                ref+=   "</tr></table>";
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

                var editRef = '';

            if(selectedStructureVarName == 'calendarEvent'){
              editRef = " editEvent('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ") ";
            }else{
              editRef = " editContentlet('" + inode + "','<%=user.getUserId()%>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ") ";
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



                                result = result + "<input onchange='doSearch()' type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" value=\""
                                                        + actual_option[1] + "\" id=\"" + selectedStruct + "." + fieldContentlet + "Field"+ counter_checkbox
                                                        + "\" name=\"" + selectedStruct + "." + fieldContentlet + "\"";
                                for(var j = 0;j < lastChecked.length; j++){
                                                if(lastChecked[j] == actual_option[1]){
                                                result = result + "checked = \"checked\"";
                                        }
                                }
                                result = result + "><label for='"+myD+"'> " + actual_option[0] + "</label><br>\n";
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
                                result = result + "<input onchange='doSearch()' type=\"radio\" dojoType=\"dijit.form.RadioButton\" value=\""
                                                        + actual_option[1] + "\" id=\"" + selectedStruct+"."+ fieldContentlet + "Field"+ counter_radio
                                                        + "\" name=\"" + selectedStruct+ "." + fieldContentlet + "\"";
                                        if(value == actual_option[1]){
                                        result = result + "checked = \"checked\"";
                                }
                                result = result + "><label for='" + myD+ "'>" + actual_option[0] + "</label><br>\n";
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
                                result = result+"<select onchange='doSearch()' dojoType='dijit.form.FilteringSelect' id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" style=\"width:160px;\" name=\"" + selectedStruct+"."+ fieldContentlet + "\">\n<option value=\"\"></option>";

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
                        dijit.registry.remove(selectedStruct+"."+ fieldContentlet +"Field");
                        var result="<table style='width:210px;' border=\"0\">";
                        result = result + "<tr><td style='padding:0px;'>";
                        result = result +"<textarea onchange=\"setTimeout(doSearch, 500);\" value=\""+value+"\" dojoType=\"dijit.form.Textarea\" id=\"" + selectedStruct+"."+ fieldContentlet + "Field\" name=\"" + selectedStruct+"."+ fieldContentlet + "Field\" cols=\"20\" rows=\"2\" onkeyup=\"suggestTagsForSearch(this,'"+ selectedStruct+"."+ fieldContentlet + "suggestedTagsDiv');\" style=\"border-color: #7F9DB9; border-style: solid; border-width: 1px; font-family: Verdana, Arial,Helvetica; font-size: 11px; height: 50px; width: 160px;\"></textarea><br/><span style=\"font-size:11px; color:#999;\"><%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></span></td></tr>";
                        result = result + "<tr><td valign=\"top\" style='padding:0px;'>";
                        result = result + "<div id=\"" + selectedStruct+"." + fieldContentlet + "suggestedTagsDiv\" style=\"height: 50px; font-size:10px;font-color:gray; width: 146px; border:1px solid #ccc;overflow: auto;\"></div><span style=\"font-size:11px; color:#999;\"><%= LanguageUtil.get(pageContext, "Suggested-Tags") %></span><br></td></tr></table>";

                        setDotFieldTypeStr = setDotFieldTypeStr
                                                                        + "dojo.attr("
                                                                        + "'" + selectedStruct + "." + fieldContentlet + "Field" + "'"
                                                                        + ",'" + DOT_FIELD_TYPE + "'"
                                                                        + ",'" + type + "');";

                    return result;
          }//http://jira.dotmarketing.net/browse/DOTCMS-3232
          else if(type=='host or folder'){
                  // Below code is used to fix the "widget already registered error".

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
                  var fieldValue = hostId;
                  <% if(UtilMethods.isSet(conFolderValue)){%>
                        fieldValue = '<%= conFolderValue %>';
                  <%}%>

                  var result = "<div onchange='doSearch()' id=\"FolderHostSelector\" style='width270px' dojoType=\"dotcms.dijit.form.HostFolderFilteringSelect\" includeAll=\"true\" onClick=\"resetHostValue();\" onChange=\"getHostValue();\" "
                                                +" hostId=\"" + hostId + "\" value = \"" + fieldValue + "\"" + "></div>";

          hasHostFolderField = true;


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
          }


          else{
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




        function addNewContentlet(structureInode){
			if(structureInode == undefined || structureInode==""){
        		structureInode = dijit.byId('structure_inode').value;
        	}
			if(structureInode == undefined || structureInode == "_all"){
				dijit.byId("selectStructureDiv").show();
				return;
			}
          else if(selectedStructureVarName == 'calendarEvent'){

                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/calendar/edit_event' />";
                href += "<portlet:param name='cmd' value='new' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "<portlet:param name='inode' value='' />";
                href += "</portlet:actionURL>";
                href += "&selectedStructure=" + structureInode ;
                href += "&lang=" + getSelectedLanguageId();
                window.location=href;
          }else{

                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
                href += "<portlet:param name='cmd' value='new' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "<portlet:param name='inode' value='' />";
                href += "</portlet:actionURL>";
                href += "&selectedStructure=" + structureInode ;
                href += "&lang=" + getSelectedLanguageId();
                window.location=href;
          }
        }

        function donwloadToExcel(){
                var structureInode = dijit.byId('structure_inode').value;

                if(structureInode ==""){
                        dijit.byId('structure_inode').focus() ;
                        return false;
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

                for (var j = 0; j < currentStructureFields.length; j++) {
                        var field = currentStructureFields[j];
            var fieldId = selectedStruct+"."+field["fieldVelocityVarName"] + "Field";
                        var formField = document.getElementById(fieldId);
                        var fieldValue = "";

                        if(formField != null){
                                                                if(dojo.attr(formField.id,DOT_FIELD_TYPE) == 'select'){

                                        var tempDijitObj = dijit.byId(formField.id);
                                        fieldsValues[fieldsValues.length] = selectedStruct+"."+field["fieldVelocityVarName"];
                                        fieldsValues[fieldsValues.length] = tempDijitObj.value;

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

                if( getSelectedLanguageId() != 0 ){
                	fieldsValues[fieldsValues.length] = "languageId";
                	fieldsValues[fieldsValues.length] = getSelectedLanguageId();
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
                var categories = form.categories;
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

                var showDeleted = false;
                if (dijit.byId("showingSelect").getValue() == "archived") {
                        showDeleted = true;
                }

                document.getElementById('fieldsValues').value = fieldsValues;
                document.getElementById('categoriesValues').value = categoriesValues;
                document.getElementById('showDeleted').value = showDeleted;
                document.getElementById('currentSortBy').value = currentSortBy;

                var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
                href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
                href += "<portlet:param name='cmd' value='export' />";
                href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
                href += "</portlet:actionURL>";
                href += "&expStructureInode="+structureInode+"&expFieldsValues="+fieldsValues+"&expCategoriesValues="+categoriesValues+"&showDeleted="+showDeleted;

                /*if we have a date*/
                        var dateFrom= null;
                        var dateTo= null;
                        if((document.getElementById("lastModDateFrom").value!="")){
                                dateFrom = document.getElementById("lastModDateFrom").value;
                                var dateFromsplit = dateFrom.split("/");
                                if(dateFromsplit[0]< 10) dateFromsplit[0]= "0"+dateFromsplit[0]; if(dateFromsplit[1]< 10) dateFromsplit[1]= "0"+dateFromsplit[1];
                                dateFrom= dateFromsplit[2]+dateFromsplit[0]+dateFromsplit[1]+"000000";
                                href+= "&modDateFrom="+dateFrom;
                        }

                        if((document.getElementById("lastModDateTo").value!="")){
                                dateTo = document.getElementById("lastModDateTo").value;
                                var dateTosplit = dateTo.split("/");
                                if(dateTosplit[0]< 10) dateTosplit[0]= "0"+dateTosplit[0]; if(dateTosplit[1]< 10) dateTosplit[1]= "0"+dateTosplit[1];
                                dateTo= dateTosplit[2]+dateTosplit[0]+dateTosplit[1]+"235959";
                                href+= "&modDateTo="+dateTo;
                        }

                window.location.href=href;

        }

        var dialog;
        function closeDialog() {
            dialog.hide();
        }
        function publishReset() {
            var rdate=dijit.byId('resetDate').get('value');
            var rtime=dijit.byId('resetTime').get('value');
            dojo.byId('expireDateReset').value=dijit.byId('resetDate')+dijit.byId('resetTime');
            closeDialog();
            publishSelectedContentlets();
        }

        function publishSelectedContentlets(){

            if(dojo.byId('expireDateReset').value=='') {
                var expiredInodes=dojo.byId("expiredInodes").value.split(",");
                var selectedInodes=dojo.query("input[name='publishInode']")
                                       .filter(function(x){return x.checked;})
                                       .map(function(x){return x.value;})
                                       .filter(function(x){return expiredInodes.indexOf(x)!=-1;});

                if(selectedInodes.length>0) {
                    dialog=new dijit.Dialog({
                        title: "dotCMS",
                        content: "<p><%=LanguageUtil.get(pageContext, "message.contentlet.resetexpire") %></p>"+
                                 "<p>"+
                                 "<input type='text' dojoType='dijit.form.DateTextBox' id='resetDate' constraints= '{min:<%=new SimpleDateFormat("yyyy-MM-dd").format(new Date())%>}'/>"+
                                 "<input type='text' dojoType='dijit.form.TimeTextBox' id='resetTime'/>"+
                                 "</p>"+
                                 "<button dojoType='dijit.form.Button' onClick='publishReset()'><%=LanguageUtil.get(pageContext, "Publish") %></button>"+
                                 "<button dojoType='dijit.form.Button' onClick='closeDialog()'><%=LanguageUtil.get(pageContext, "Cancel") %></button>",
                        style: "width:400px",
                        onHide: function() { this.destroyRecursive(); }
                    });
                    dialog.show();
                    return;
                }
            }

                disableButtonRow();
                var form = document.getElementById("search_form");
                form.cmd.value = 'full_publish_list';
                form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_publish_list" /></portlet:actionURL>';
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
                form.action += "&selected_lang=" + getSelectedLanguageId();
                submitForm(form);
        }

        function pushPublishSelectedContentlets() {
        	var selectedInodes=dojo.query("input[name='publishInode']")
                                       .filter(function(x){return x.checked;})
                                       .map(function(x){return x.value;});

			pushHandler.showDialog(selectedInodes);
        }

        function addToBundleSelectedContentlets() {
        	var selectedInodes=dojo.query("input[name='publishInode']")
                                       .filter(function(x){return x.checked;})
                                       .map(function(x){return x.value;});

			pushHandler.showAddToBundleDialog(selectedInodes, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');

        }

        function unPublishSelectedContentlets(){
                disableButtonRow();
          var form = document.getElementById("search_form");
            form.cmd.value = 'full_unpublish_list';
                form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_unpublish_list" /></portlet:actionURL>';
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
                form.action += "&selected_lang=" + getSelectedLanguageId();
                submitForm(form);
        }

        function archiveSelectedContentlets(){
                disableButtonRow();
            var form = document.getElementById("search_form");
            form.cmd.value = 'full_archive_list';
                form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_archive_list" /></portlet:actionURL>';


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
                submitForm(form);
        }

        function reindexSelectedContentlets(){
                disableButtonRow();
                var form = document.getElementById("search_form");
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
                submitForm(form);
        }

        function unArchiveSelectedContentlets(){
                disableButtonRow();
            var form = document.getElementById("search_form");
            form.cmd.value = 'full_unarchive_list';
                form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_unarchive_list" /></portlet:actionURL>';

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
                submitForm(form);
        }

        function deleteSelectedContentlets(){
                disableButtonRow();
                if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.confirm.delete")) %>')){
                         var form = document.getElementById("search_form");
                form.cmd.value = 'full_delete_list';
                        form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_delete_list" /></portlet:actionURL>';

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
                        submitForm(form);
                }
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

                StructureAjax.getStructureSearchFields (structureInode,
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
                                return field["fieldName"] + ":";
                        }
             }
        }



        function fillFields (data) {
                currentStructureFields = data;
                var htmlstr = "";
                var hasHostField = false;
                for(var i = 0; i < data.length; i++) {
                        var type = data[i]["fieldFieldType"];
                        if(type=='category' || type=='hidden'){
                                continue;
                        }
                        if(type=='host or folder'){
                           hasHostField = true;
                        }
                        htmlstr += "<dl>";
                        htmlstr += "<dt>" + fieldName(data[i]) + "</dt>";
                        htmlstr += "<dd>" + renderSearchField(data[i]) + "</dd>";
                        htmlstr += "</dl>";
                        htmlstr += "<div class='clear'></div>";
                }
                $('search_fields_table').update(htmlstr);
                <%if(APILocator.getPermissionAPI().doesUserHavePermission(APILocator.getHostAPI().findSystemHost(), PermissionAPI.PERMISSION_READ, user, true)){%>
                  if(hasHostField){
                     dojo.byId("filterSystemHostTable").style.display = "";
                  }else{
                     dojo.byId("filterSystemHostTable").style.display = "none";
                  }
           <%}%>

                dojo.parser.parse(dojo.byId("search_fields_table"));
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
                                var selectObj = "<select dojoType='dijit.form.MultiSelect' class='width-equals-200' multiple='true' name=\"categories\" id=\"" + selectId + "\"></select>";

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

        function doSearch (page, sortBy) {
                // Wait for the "HostFolderFilteringSelect" widget to end the values updating process before proceeding with the search, if necessary.
                if (dijit.byId('FolderHostSelector') && dijit.byId('FolderHostSelector').attr('updatingSelectedValue')) {
                        setTimeout("doSearch (" + page + ", '" + sortBy + "');", 250);
                } else {
                		if(dijit.byId('structure_inode'))
                        	doSearch1 (page, sortBy);
                        else
                        	setTimeout("doSearch (" + page + ", '" + sortBy + "');", 250);
                }
        }

		var amISearching = 0;


        function doSearch1 (page, sortBy) {

        		var structureInode = "";


	            if(dijit.byId('structure_inode')) {
	              structureInode  = dijit.byId('structure_inode').getValue();
	            }
                if(structureInode ==""){
                        dijit.byId('structure_inode').focus() ;
                        return false;
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
                var allField = dijit.byId("allFieldTB").getValue();

				if (allField != undefined && allField.length>0 ) {

                        fieldsValues[fieldsValues.length] = "_all";
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
                                        fieldsValues[fieldsValues.length] = tempDijitObj.value;

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

                // if we have an identifier
            if(isInodeSet(document.getElementById("Identifier").value)){
            var contentId = "";
                fieldsValues[fieldsValues.length] = "identifier";
                contentId = document.getElementById("Identifier").value;
                    fieldsValues[fieldsValues.length] = contentId;
            }
                var categoriesValues = new Array ();
                var form = document.getElementById("search_form");
                var categories = form.categories;
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

                if (sortBy != null) {
                        if (sortBy == currentSortBy)
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

                dijit.byId("searchButton").attr("disabled", true);
                //dijit.byId("clearButton").attr("disabled", false);

                document.getElementById('fieldsValues').value = fieldsValues;
                document.getElementById('categoriesValues').value = categoriesValues;
                document.getElementById('showDeleted').value = showDeleted;
                document.getElementById('currentSortBy').value = currentSortBy;
                document.getElementById('filterSystemHost').value = filterSystemHost;
                document.getElementById('filterLocked').value = filterLocked;
                document.getElementById('filterUnpublish').value = filterUnpublish;
				//console.log(fieldsValues);
                if(isInodeSet(structureInode) || "_all" == structureInode){
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
                        amISearching++;
                        ContentletAjax.searchContentlets (structureInode, fieldsValues, categoriesValues, showDeleted, filterSystemHost, filterUnpublish, filterLocked, currentPage, currentSortBy, dateFrom, dateTo, fillResults);

                }

        }

        function nextPage () {
                doSearch (currentPage + 1);
        }

        function previousPage () {
                doSearch (currentPage - 1);
        }

        function noResults (data) {
                return "<div class='noResultsMessage'><%= LanguageUtil.get(pageContext, "No-Results-Found") %></div>";
        }

        function checkUncheckAll() {
                var checkAll = dijit.byId("checkAll");
                var check;

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
                var checkAll = document.getElementById("checkAll");
                var table = $('tablemessage');
                if (checkAll.checked) {
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
                $('tablemessage').innerHTML = " &nbsp ";
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
                }else{
                	return "<a href=\"javascript: doSearch (1, '" + stVar + "." + fieldContentlet + "')\">" + fieldName + "</a>";
                }
        }

        function fillResultsTable (headers, data) {
                headerLength = headers.length;
                var table = document.getElementById("results_table");

                //Filling Headers
                var row = table.insertRow(table.rows.length);

                var th = document.createElement('th');
                th.setAttribute("width","5%");
                th.innerHTML = '&nbsp;';
                row.appendChild(th);

                th = document.createElement('th');
                th.setAttribute("width","5%");
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
                                th.innerHTML = '<input type="checkbox" dojoType="dijit.form.CheckBox" name="checkAll" id="checkAll" onclick="checkUncheckAll()">&nbsp;&nbsp;' + getHeader(header);
                                row.appendChild(th);
                        } else {
                        th.innHTML =
                                th.innerHTML = getHeader(header);
                                row.appendChild(th);
                        }
                }
                th = document.createElement('th');
                th.setAttribute("style","text-align:center;");
                th.innerHTML = "<a href=\"javascript: doSearch (1, 'modUser')\"><%= LanguageUtil.get(pageContext, "Last-Editor") %></a>";
                row.appendChild(th);

                th = document.createElement('th');
                th.setAttribute("style","text-align:center;");
                th.innerHTML = "<a class=\"beta\" href=\"javascript: doSearch (1, 'modDate')\"><%= LanguageUtil.get(pageContext, "Last-Edit-Date") %></a>";
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
                var popupMenusDiv = document.getElementById("results_table_popup_menus");
                var popupMenus = "";
                var workflowMandatory;
                var wfActionMapList;
                var structure_id;
                var contentStructureType;

                //Filling data
                for (var i = 0; i < data.length; i++) {
                        var row = table.insertRow(table.rows.length);

                        row.setAttribute("height","30");
                        row.setAttribute("valign","top");
                        var cellData = data[i];
                        row.setAttribute("id","tr" + cellData.inode);



                        var cell = row.insertCell (row.cells.length);
                        cell.style.whiteSpace="nowrap";

                        cell.innerHTML = statusDataCell(cellData, i);
                        for (var j = 0; j < headers.length; j++) {
                                var header = headers[j];
                                var cell = row.insertCell (row.cells.length);
                                //console.log(headers[j]);
                                cell.setAttribute("align","center");
                                if (j == 0) {
                                        languageId = cellData["languageId"];
                                        locale = "";

                                        for (var n = 0; n < languages.length; ++n) {
                                                if (languages[n][0] == languageId) {
                                                        locale = "<img src=\"/html/images/languages/" + languages[n][1] + "_" + languages[n][2] + ".gif\" width=\"16px\" height=\"11px\" />";
                                                        //locale = languages[n][1] + "_" + languages[n][2];
                                                        break;
                                                }
                                        }

                                        if (locale == "")
                                                locale = "&nbsp;";

                                        cell.innerHTML = locale;
                                        var cell = row.insertCell (row.cells.length);
                                        var value = titleCell(cellData,cellData[header["fieldVelocityVarName"]], i);

                                } else {
                                        var value = cellData[header["fieldVelocityVarName"]];
                                }
                                cell.setAttribute("class","titleCellDiv");
                                cell.setAttribute("className","titleCellDiv");
                                if (value != null)
                                        cell.innerHTML = value;
                        }
                        var cell = row.insertCell (row.cells.length);
                        cell.setAttribute("nowrap","true");
                        cell.innerHTML = cellData["modUser"];
                        cell.style.whiteSpace="nowrap";
                        cell.style.textAlign="center";
                        var cell = row.insertCell (row.cells.length);
                        cell.setAttribute("nowrap","true");
                        cell.style.textAlign="right";
                        cell.style.whiteSpace="nowrap";
                        cell.innerHTML = cellData["modDate"];

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
                        workflowMandatory = cellData["workflowMandatory"];
                        contentStructureType = cellData["contentStructureType"];
                        structure_id = cellData["structureInode"];

                        contentAdmin = new dotcms.dijit.contentlet.ContentAdmin(cellData.identifier,cellData.inode,cellData.languageId);

                        wfActionMapList = JSON.parse(cellData["wfActionMapList"]);

                        dijit.registry.remove("popupTr"+i);

                		if(dijit.byId("popupTr"+i)){
                                dijit.byId("popupTr"+i).destroy();
                        }


                        popupMenus += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popupTr" + i + "\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"tr" + cellData.inode + "\">";


                        // NEW CONTEXT MENU

                        if ((live || working) && (read=="1") && (!deleted)) {
                                if(selectedStructureVarName == 'calendarEvent'){
                                  if (write=="1"){
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Edit") %></div>";
                                  }else{
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "View") %></div>";
                              }
                                }else{
                                  if (write=="1"){
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Edit") %></div>";
                                  }else{
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"editIcon\" onClick=\"editContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "View") %></div>";
                              }
                            }
                        }


                        for (var k = 0; k < wfActionMapList.length; k++) {
							var name = wfActionMapList[k].name;
							var id = wfActionMapList[k].id;
							var assignable = wfActionMapList[k].assignable;

							var commentable = wfActionMapList[k].commentable;
							var icon = wfActionMapList[k].icon;
							var requiresCheckout = wfActionMapList[k].requiresCheckout;
							var wfActionNameStr = wfActionMapList[k].wfActionNameStr;
							var hasPushPublishActionlet = wfActionMapList[k].hasPushPublishActionlet;

							if (!cellData.inode && requiresCheckout || (locked && write=="1") && requiresCheckout) {
								popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\""+icon+"\" onClick=\"contentAdmin.executeWfAction('" + id + "', '" + assignable + "', '" + commentable + "', '" + hasPushPublishActionlet + "', '" + cellData.inode + "');\">"+wfActionNameStr+"</div>";

							}else if(!requiresCheckout)  {
								popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\""+icon+"\" onClick=\"contentAdmin.executeWfAction('" + id + "', '" + assignable + "', '" + commentable + "', '" + hasPushPublishActionlet + "', '" + cellData.inode + "');\">"+wfActionNameStr+"</div>";

							}
						}

						if (!live && (publish=="1") && (!deleted) && workflowMandatory=="false"){
                          if(selectedStructureVarName == 'calendarEvent'){
                            popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"publishIcon\" onClick=\"publishEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Publish") %></div>";
                          }else{
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"publishIcon\" onClick=\"publishContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Publish") %></div>";
                          }
                        }

                        if (working && publish=='1' && !deleted && workflowMandatory=="false") {
				    		if(live) {
				    			popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"republishIcon\" onClick=\"publishContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Republish") %></div>";
							} else {
								//popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"publishIcon\" onClick=\"publishContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Publish") %></div>";
							}
						}

						if(enterprise && sendingEndpoints && workflowMandatory=="false") {
								popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"sServerIcon\" onClick=\"remotePublish('" + cellData.inode + "','<%= referer %>');\"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>";
								popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"addToBundle('" + cellData.inode + "','<%= referer %>');\"><%=LanguageUtil.get(pageContext, "Add-To-Bundle") %></div>";
						}


						if (live && (publish=="1") && workflowMandatory=="false"){
                          if(selectedStructureVarName == 'calendarEvent'){
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unpublishIcon\" onClick=\"unpublishEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Unpublish") %></div>";
                          }else{
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unpublishIcon\" onClick=\"unpublishContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Unpublish") %></div>";
                          }
                        }

						// END NEW CONTEXT MENU


                        if ((!live) && working && (publish=="1") && workflowMandatory=="false") {
                           if(selectedStructureVarName == 'calendarEvent'){
                             if (!deleted){
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"archiveIcon\" onClick=\"deleteEvent('" + cellData.inode + "','','" + escape('<%= referer %>') + "');\"><%=LanguageUtil.get(pageContext, "Archive") %></div>";
                                 }else{
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unarchiveIcon\" onClick=\"unarchiveEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Un-Archive") %></div>";
                             }
                           }else{
                                if (!deleted){
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"archiveIcon\" onClick=\"deleteContentlet('" + cellData.inode + "','','" + escape('<%= referer %>') + "'" + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Archive") %></div>";
                                }else{
                                        popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unarchiveIcon\" onClick=\"unarchiveContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Un-Archive") %></div>";
                            }
                           }
                        }
                        if ((live || working) && (write=="1") && (!deleted) && workflowMandatory=="false"){
                          if(selectedStructureVarName == 'calendarEvent'){
                            popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"copyIcon\" onClick=\"copyEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Copy") %></div>";
                          }else{
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"copyIcon\" onClick=\"copyContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Copy") %></div>";
                          }
                        }

                        if (locked && (write=="1") && workflowMandatory=="false"){
                          if(selectedStructureVarName == 'calendarEvent'){
                            popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unlockIcon\" onClick=\"unlockEvent('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ");\"><%=LanguageUtil.get(pageContext, "Unlock") %></div>";
                          }else{
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"unlockIcon\" onClick=\"unlockContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Unlock") %></div>";
                          }
                        }
                        //if (deleted && (write == "1"))
                                //popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"deleteIcon\" onClick=\"fullDeleteContentlet('" + cellData.inode + "','<%= user.getUserId() %>','<%= referer %>'," + liveSt + "," + workingSt + "," + write + ", '" + contentStructureType + "', '" + structure_id + "');\"><%=LanguageUtil.get(pageContext, "Delete-Contentlet") %></div>";
                        popupMenus += "</div>";
                }

                popupMenusDiv.innerHTML = popupMenus;


                dojo.parser.parse(dojo.byId("results_table_popup_menus"));
                dojo.parser.parse(dojo.byId("results_table"));

        }

        function clearSearch () {
     			dijit.byId("showingSelect").set("value", "all");
     			dijit.byId("allFieldTB").set("value", "");

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
                                          formField.value = "";
                                          var temp = dijit.byId(formField.id);
                                          temp.attr('value','');
                                          if(temp){
					                        try{
					                           temp.setDisplayedValue('');
					                         }catch(e){console.log(e);}
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

	        amISearching =0;

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

        		//disallow publishing if workflow is mandatory
       	 		if(contentlet["workflowMandatory"] && contentlet["workflowMandatory"]!= "false"){
       	 			return false;
       	 		}
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




        function showMatchingResults (num,begin,end,totalPages) {


                        var div = document.getElementById("metaMatchingResultsDiv");
                        div.style.display='';

                    //Top Matching Results

                    eval("totalContents=" + num + ";");

                        div = document.getElementById("matchingResultsDiv")
                        var structureInode = dijit.byId('structure_inode').value;
                        var strbuff = "<div class=\"yui-gb portlet-toolbar\"><div class=\"yui-u first\"><%= LanguageUtil.get(pageContext, "showing") %> " + begin + "-" + end + " <%= LanguageUtil.get(pageContext, "of1") %> " + num + "</div><div id=\"tablemessage\" class=\"yui-u\" style=\"text-align:center;\">&nbsp</div><div class=\"yui-u\" style=\"text-align:right;\">";
                        if(num >0 && structureInode != "_all"){
                                strbuff+= "<a href='javascript:donwloadToExcel();'><%= LanguageUtil.get(pageContext, "Export") %></a> <a href='javascript:donwloadToExcel();'><img src='/icon?i=csv.xls' border='0' alt='export results' align='absbottom'></a>";
                        }
                        strbuff+= "</div></div>";
                        div.innerHTML = strbuff;
                        div.style.display = "";

                        //Bottom Matching Results
                        var div = document.getElementById("matchingResultsBottomDiv")
                        var strbuff = "<table border='0' width=\"100%\"><tr><td align='center' nowrap='true'><b><%= LanguageUtil.get(pageContext, "showing") %> " + begin + " - " + end + " <%= LanguageUtil.get(pageContext, "of1") %> " + num;
                        if(num > 0)
                        {
                                strbuff += " | <%= LanguageUtil.get(pageContext, "pages") %> ";
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

        function fillQuery (counters) {
                        <%
                        String restBaseUrl="http://"+
                           APILocator.getHostAPI().find((String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID), user, false).getHostname()+
                           ((request.getLocalPort()!=80) ? ":"+request.getLocalPort() : "")+
                           "/api/content/render/false";
                        %>
                        var queryRaw = counters["luceneQueryRaw"];
                        var queryfield=document.getElementById("luceneQuery");
                        queryfield.value=queryRaw;
                        var queryFrontend = counters["luceneQueryFrontend"];
                        var sortBy = counters["sortByUF"];
                        var div = document.getElementById("queryResults");
                        var apicall="<%= restBaseUrl %>/query/"+queryRaw+"/orderby/"+sortBy;
                        var test_api_xml_link="/api/content/render/false/type/xml/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);
                        var test_api_json_link="/api/content/render/false/type/json/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);
                        var apicall_urlencode="<%= restBaseUrl %>/query/"+encodeURI(queryRaw)+"/orderby/"+encodeURI(sortBy);

                        var expiredInodes = counters["expiredInodes"];
                        dojo.byId("expiredInodes").value=expiredInodes;
                        dojo.byId("expireDateReset").value="";


                        div.innerHTML ="<div class='contentViewDialog'>" +

                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "frontend-query") %></div>"+
                            "<div class='contentViewQuery'>#foreach($con in $dotcontent.pull(\"" + queryFrontend + "\",10,\"" + sortBy + "\"))<br/>...<br/>#end</div>" +
                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "The-actual-query-") %></div>"+
                            "<div class='contentViewQuery'>"+queryRaw+"</div>" +
                            "<div class='contentViewTitle'><%= LanguageUtil.get(pageContext, "rest-api-call-urlencoded") %></div>"+
                            "<div class='contentViewQuery'>"+apicall_urlencode+"</div>"+
                            "<div class='contentViewQuery' style='padding:20px;padding-top:10px;color:#333;'>REST API: " +

	                            "<a href='" + test_api_xml_link +"' target='_blank'><%= LanguageUtil.get(pageContext, "xml") %></a>"+
	                            "&nbsp;|&nbsp;"+
	                            "<a href='" + test_api_json_link +"' target='_blank'><%= LanguageUtil.get(pageContext, "json") %></a>"+

                            "</div>"+


                            "<b><%= LanguageUtil.get(pageContext, "Ordered-by") %>:</b> " + sortBy +
                            "<ul><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint2") %> " +
                            "</li><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint3") %> " +
                            "</li><li><%= LanguageUtil.get(pageContext, "message.contentlet.hint4") %> " +
                            "<li><%= LanguageUtil.get(pageContext, "message.contentlet.hint5") %></li>"+
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

    function togglePublish() {
        var cbArray = document.getElementsByName("publishInode");
        var showArchive =  (dijit.byId("showingSelect").getValue() == "archived");

        var cbCount = cbArray.length;
        for(i = 0;i< cbCount ;i++){
            if (cbArray[i].checked) {
                if (showArchive) {
                    dijit.byId('unArchiveButton').setAttribute("disabled", false);
                    dijit.byId('deleteButton').setAttribute("disabled", false);
                    dijit.byId('archiveUnlockButton').setAttribute("disabled", false);
                    <%=(canReindex?"dijit.byId('archiveReindexButton').setAttribute(\"disabled\", false);":"") %>
                } else {
                    dijit.byId('archiveButton').setAttribute("disabled", false);
                    dijit.byId('publishButton').setAttribute("disabled", false);

                    	<% if ( enterprise ) { %>
                    		if(typeof dijit.byId('addToBundleButton') !== "undefined") {
	                    	<% 	if ( sendingEndpoints ) { %>
	                    			dijit.byId('pushPublishButton').setAttribute("disabled", false);
	                    	 <% } %>
							}
                    		dijit.byId('addToBundleButton').setAttribute("disabled", false);

                    	<% } %>

                    dijit.byId('unPublishButton').setAttribute("disabled", false);
                    dijit.byId('unlockButton').setAttribute("disabled", false);
                    <%=(canReindex?"dijit.byId('reindexButton').setAttribute(\"disabled\", false);":"") %>
                }
                return;
            }

        }

        // nothing selected
       	if (showArchive) {
                    dijit.byId("unArchiveButton").setAttribute("disabled", true);
                    dijit.byId("deleteButton").setAttribute("disabled", true);
                    dijit.byId("archiveUnlockButton").setAttribute("disabled", true);
                    <%=(canReindex?"dijit.byId('archiveReindexButton').setAttribute(\"disabled\", true);":"") %>
        } else {
                    dijit.byId('archiveButton').setAttribute("disabled", true);
                    dijit.byId('publishButton').setAttribute("disabled", true);

                    <% if ( enterprise ) { %>
                    		if(typeof dijit.byId('addToBundleButton') !== "undefined") {
	                    	<% 	if ( sendingEndpoints ) { %>

	                    			dijit.byId('pushPublishButton').setAttribute("disabled", true);
	                    	 <% } %>
	                    	 }
                    			dijit.byId('addToBundleButton').setAttribute("disabled", true);

                    	<% } %>

                    dijit.byId('unPublishButton').setAttribute("disabled", true);
                    dijit.byId("unlockButton").setAttribute("disabled", true);
                     <%=(canReindex?"dijit.byId('reindexButton').setAttribute(\"disabled\", true);":"") %>
        }
    }
    function displayArchiveButton(){

        var showArchive =  (dijit.byId("showingSelect").getValue() == "archived");
        if (showArchive) {
            document.getElementById("archiveButtonDiv").style.display="";
            document.getElementById("unArchiveButtonDiv").style.display="none";
        } else {
            document.getElementById("archiveButtonDiv").style.display="none";
            document.getElementById("unArchiveButtonDiv").style.display="";
        }
        togglePublish();
    }



        dojo.addOnLoad(function () {
        structureChanged(true);
        useLoadingMessage("<i class='loadingIcon'></i> Loading");

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

        function  resizeBrowser(){
                var viewport = dijit.getViewport();
                var viewport_height = viewport.h;

                var  e =  dojo.byId("borderContainer");
                dojo.style(e, "height", viewport_height -150+ "px");

                var  e =  dojo.byId("filterWrapper");
                dojo.style(e, "height", viewport_height -195+ "px");

                var  e =  dojo.byId("contentWrapper");
                dojo.style(e, "height", viewport_height -230+ "px");

                dijit.byId('borderContainer').resize()
        }

        //dojo.addOnLoad(resizeBrowser);
        dojo.connect(window, "onresize", this, "resizeBrowser");

        function disableButtonRow() {

                if(dijit.byId("unArchiveButton"))
                        dijit.byId("unArchiveButton").attr("disabled", true);

                if(dijit.byId("deleteButton"))
                        dijit.byId("deleteButton").attr("disabled", true);

                if(dijit.byId("archiveReindexButton"))
                        dijit.byId("archiveReindexButton").attr("disabled", true);

                if(dijit.byId("archiveUnlockButton"))
                        dijit.byId("archiveUnlockButton").attr("disabled", true);

                if(dijit.byId("publishButton"))
                        dijit.byId("publishButton").attr("disabled", true);

                if(dijit.byId("pushPublishButton"))
                        dijit.byId("pushPublishButton").attr("disabled", true);

                if(dijit.byId("addToBundleButton"))
                        dijit.byId("addToBundleButton").attr("disabled", true);

                if(dijit.byId("unPublishButton"))
                        dijit.byId("unPublishButton").attr("disabled", true);

                if(dijit.byId("archiveButton"))
                        dijit.byId("archiveButton").attr("disabled", true);

                if(dijit.byId("reindexButton"))
                        dijit.byId("reindexButton").attr("disabled", true);

                if(dijit.byId("unlockButton"))
                        dijit.byId("unlockButton").attr("disabled", true);


        }

         function unlockSelectedContentlets(){
            disableButtonRow();
            var form = document.getElementById("search_form");
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
                submitForm(form);
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


    	executeWfAction: function(wfId, assignable, commentable, hasPushPublishActionlet, inode ){
    		this.wfActionId=wfId;
    		if(assignable == 'true' || commentable == 'true' || hasPushPublishActionlet == 'true'){
    			var dia = dijit.byId("contentletWfDialog");
    			if(dia){
    				dia.destroyRecursive();

    			}
    			dia = new dijit.Dialog({
    				id			:	"contentletWfDialog",
    				title		: 	"<%=LanguageUtil.get(pageContext, "Workflow-Actions")%>",
					style		:	"min-width:500px;min-height:250px;"
    				});



    			var myCp = dijit.byId("contentletWfCP");
    			if (myCp) {
    				myCp.destroyRecursive(true);
    			}

    			myCp = new dojox.layout.ContentPane({
    				id 			: "contentletWfCP",
    				style		:	"minwidth:500px;min-height:250px;margin:auto;"
    			}).placeAt("contentletWfDialog");

    			dia.show();
    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId + "&inode=" + inode);
    			return;
    		}
    		else{
        		    var wfActionAssign 		= "";
		    		var selectedItem 		= "";
		    		var wfConId 			= inode;
		    		var wfActionId 			= this.wfActionId;
		    		var wfActionComments 	= "";
		    		var publishDate			="";
		    		var publishTime 		= "";
		    		var expireDate 			= "";
		    		var expireTime 			="";
		    		var neverExpire 		="";
		    		var whereToSend 		="";
					BrowserAjax.saveFileAction(selectedItem,wfActionAssign,wfActionId,wfActionComments,wfConId, publishDate,
		    				publishTime, expireDate, expireTime, neverExpire, whereToSend, fileActionCallback);
    		}

    	},

    	saveAssign : function(){

    		var assignRole = (dijit.byId("taskAssignmentAux"))
			? dijit.byId("taskAssignmentAux").getValue()
				: (dojo.byId("taskAssignmentAux"))
					? dojo.byId("taskAssignmentAux").value
							: "";

			if(!assignRole || assignRole.length ==0 ){
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Assign-To-Required")%>");
				return;
			}

			var comments = (dijit.byId("taskCommentsAux"))
				? dijit.byId("taskCommentsAux").getValue()
					: (dojo.byId("taskCommentsAux"))
						? dojo.byId("taskCommentsAux").value
								: "";

    		var wfActionAssign 		= assignRole;
    		var selectedItem 		= "";
    		var wfConId 			= dojo.byId("wfConId").value;
    		var wfActionId 			= this.wfActionId;
    		var wfActionComments 	= comments;

    		var dia = dijit.byId("contentletWfDialog").hide();

    		// BEGIN: PUSH PUBLISHING ACTIONLET
			var publishDate = (dijit.byId("wfPublishDateAux"))
				? dojo.date.locale.format(dijit.byId("wfPublishDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
					: (dojo.byId("wfPublishDateAux"))
						? dojo.date.locale.format(dojo.byId("wfPublishDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"})
								: "";

			var publishTime = (dijit.byId("wfPublishTimeAux"))
				? dojo.date.locale.format(dijit.byId("wfPublishTimeAux").getValue(),{timePattern: "H-m", selector: "time"})
					: (dojo.byId("wfPublishTimeAux"))
						? dojo.date.locale.format(dojo.byId("wfPublishTimeAux").value,{timePattern: "H-m", selector: "time"})
								: "";


			var expireDate = (dijit.byId("wfExpireDateAux"))
				? dijit.byId("wfExpireDateAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
					: (dojo.byId("wfExpireDateAux"))
						? dojo.byId("wfExpireDateAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
								: "";

			var expireTime = (dijit.byId("wfExpireTimeAux"))
				? dijit.byId("wfExpireTimeAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireTimeAux").getValue(),{timePattern: "H-m", selector: "time"}) : ""
					: (dojo.byId("wfExpireTimeAux"))
						? dojo.byId("wfExpireTimeAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireTimeAux").value,{timePattern: "H-m", selector: "time"}) : ""
								: "";
			var neverExpire = (dijit.byId("wfNeverExpire"))
				? dijit.byId("wfNeverExpire").getValue()
					: (dojo.byId("wfNeverExpire"))
						? dojo.byId("wfNeverExpire").value
								: "";

			var whereToSend = (dijit.byId("whereToSend"))
				? dijit.byId("whereToSend").getValue()
					: (dojo.byId("whereToSend"))
						? dojo.byId("whereToSend").value
								: "";
			// END: PUSH PUBLISHING ACTIONLET


    		BrowserAjax.saveFileAction(selectedItem,wfActionAssign,wfActionId,wfActionComments,wfConId, publishDate,
    				publishTime, expireDate, expireTime, neverExpire, whereToSend, fileActionCallback);

    	}

    });

    function fileActionCallback (response) {
    	doSearch();
    	showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Workflow-executed")) %>');
	}

    var contentAdmin ;
