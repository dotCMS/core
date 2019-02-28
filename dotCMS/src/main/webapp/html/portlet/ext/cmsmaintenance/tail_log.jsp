<%@page import="java.io.File"%>
<%

	String regex = com.dotmarketing.util.Config.getStringProperty("TAIL_LOG_FILE_REGEX");
	if(!com.dotmarketing.util.UtilMethods.isSet(regex)){
		regex=".*";
	}
    String logPath = com.dotmarketing.util.Config.getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
	logPath = com.dotmarketing.util.FileUtil.getAbsolutlePath(logPath);
    if (!logPath.endsWith(java.io.File.separator)) {
        logPath = logPath + java.io.File.separator;
    }
	File[] files = com.liferay.util.FileUtil.listFileHandles(logPath, true);
	java.util.regex.Pattern pp = java.util.regex.Pattern.compile(regex);
	java.util.List<File> l = new java.util.ArrayList<File>();
	for(File x : files){
		if(pp.matcher(x.getName()).matches()){
			l.add(x);
		}
	}
	// http://jira.dotmarketing.net/browse/DOTCMS-6271
	// put matched files set to an array with exact size and then sort them
	files = l.toArray(new File[l.size()]);
	java.util.Arrays.sort(files);




	com.liferay.portal.model.User uu=null;
	try {
		uu = com.liferay.portal.util.PortalUtil.getUser(request);
	} catch (Exception e) {
		response.sendError(403);
		return;
	}
	try {
		if (!com.dotmarketing.business.APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", uu)) {
			response.sendError(403);
			return;
		}
	} catch (Exception e2) {
		com.dotmarketing.util.Logger.error(this.getClass(), e2.getMessage(), e2);
		response.sendError(403);
		return;
	}


%>

<script type="text/javascript">

	function reloadTail(){
		var x = dijit.byId("fileName").getValue();
		dojo.byId("tailingFrame").src='/dotTailLogServlet/?fileName='+x;

	}

	function doPopup(){
			var x = dijit.byId("fileName").getValue();
			dijit.byId("fileName").setValue("");
			var newwin = window.open("/html/portlet/ext/cmsmaintenance/tail_log_popup.jsp?fileName=" + x, "tailwin", "status=1,toolbars=1,resizable=1,scrollbars=1,height=600,width=800");
			newwin.focus();
	}

	dojo.ready(function(){

		if(self != top){
			dojo.style(dojo.byId("popMeUp"), "display", "block");
		}


		<%if(request.getParameter("fileName")!= null){
			String selectedFileNameStr = com.dotmarketing.util.UtilMethods.xmlEscape(request.getParameter("fileName")).replace(logPath + File.separator, "");
			selectedFileNameStr = selectedFileNameStr.replace("\\", "\\\\");
		%>
		dijit.byId("fileName").attr("displayedValue","<%=selectedFileNameStr%>");
		<%}%>

	});

	function doManageLogs() {

		dijit.byId('logman_dia').show();

	}

    function checkUncheck () {
        var x = dijit.byId( "checkAllCkBx" ).checked;
        dojo.query( ".taskCheckBox" ).forEach( function ( node ) {
            dijit.getEnclosingWidget(node).set("checked", x);
        } );
    }


    /**
     * Will search for all the current logs and it will populate the table with those logs details
     */
    function getCurrentLogs () {
        var xhrArgs = {

            url:"/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.LogConsoleAjaxAction/cmd/getLogs/",

            handleAs:"json",
            handle:function ( data, ioArgs ) {

                if ( data.response == "error" ) {
                    showDotCMSSystemMessage( data.message, true );
                } else {
                    //If everything its ok lets populate the table with the returned logs details
                    populateTable( data.logs );
                }
            }
        };
        dojo.xhrPost( xhrArgs );
        dijit.byId( "checkAllCkBx" ).set('checked',false);
    }

    /**
     * Will enable/disable the selected logs
     */
    function enableDisableLogs () {
        //Find the list of checked logs details
        var selectedLogs = "";
        dojo.query( ".taskCheckBox" ).forEach( function ( node ) {
            if ( node.checked ) {
                selectedLogs += node.value + ",";
            }
        } );

        var xhrArgs = {

            url:"/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.LogConsoleAjaxAction/cmd/enabledDisabledLogs/selection/" + selectedLogs,

            handleAs:"json",
            handle:function ( data, ioArgs ) {

                if ( data.response == "error" ) {
                    showDotCMSSystemMessage( data.message, true );
                } else {
                    //If everything its ok lets populate the table with the returned logs details
                    populateTable( data.logs );
                }
            }
        };
        dojo.xhrPost( xhrArgs );
        dijit.byId( "checkAllCkBx" ).set('checked',false);
    }

    /**
     * Populate the logs table with a given logs details array
     * @param logsData logs details array
     */
    var populateTable = function ( logsData ) {

        //First we need to remove the old rows
        var currentItems = dojo.query( ".logsTableRow" );
        if ( currentItems.length ) {
            for ( i = 0; i < currentItems.length; i++ ) {
                dojo.destroy( currentItems[i] );
            }
        }

        //Now lets add the new ones
        for ( i = 0; i < logsData.length; i++ ) {

            var logDetail = logsData[i];

            var name = logDetail.name;
            var enabled = logDetail.enabled;
            var description = logDetail.description;

            //Creating the html code....
            var tdCheckbox = '<td><input name="logs" class="taskCheckBox" dojoType="dijit.form.CheckBox" type="checkbox" name="logs" value="' + name + '" id="' + name + '" /></td>';
            var tdStatus;
            if ( enabled == 1 ) {
                tdStatus = '<td><span class="liveIcon"></span></td>';
            } else {
                tdStatus = '<td><span class="archivedIcon"></span></td>';
            }
            var tdName = '<td>' + name + '</td>';
            var tdDescription = '<td><strong>' + description + '</strong></td>';

            //And building the node...
            var tableNode = dojo.byId( "logsTable" );
            var headerNode = dojo.byId( "logsTableHeader" );
            var newTr = dojo.create( "tr", {
                innerHTML:tdCheckbox + tdStatus + tdName + tdDescription,
                className:"logsTableRow",
                id:"tr-" + name
            }, tableNode );

            dojo.parser.parse(tableNode);
        }

    };

    function destroyCheckboxNodes() {
        dojo.query(".taskCheckBox").forEach(function (node) {
            console.log(dijit.getEnclosingWidget(node));
            dijit.getEnclosingWidget(node).destroy();
        });
    }


    dojo.ready(function() {
        var dialog = dijit.byId("logman_dia");
    	dojo.connect(dialog, "onShow", null, getCurrentLogs);
    	dojo.connect(dialog, "onCancel", null, destroyCheckboxNodes);
    });

</script>

<div class="portlet-toolbar" id="headerContainer">
    <div class="portlet-toolbar__actions-primary">
        <div class="inline-form">
            <label for="fileName"><%=com.liferay.portal.language.LanguageUtil.get(pageContext, "Tail")%>:</label>
            <select name="fileName" dojoType="dijit.form.FilteringSelect" ignoreCase="true" id="fileName" style="width:250px;" onchange="reloadTail();">
                <option value=""></option>
                <%for(File f: files){%>
                    <option value="<%= f.getPath().replace(logPath, "")%>"><%= f.getPath().replace(logPath, "")%></option>
                <%} %>
            </select>
            <div class="checkbox">
                <input type="checkbox" id="scrollMe" dojoType="dijit.form.CheckBox" value=1 checked="true" />
                <label for="scrollMe">
                    <%=com.liferay.portal.language.LanguageUtil.get(pageContext, "Follow") %>
                </label>
            </div>
            <button dojoType="dijit.form.Button" onClick="doPopup()" value="popup" name="popup">
                <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"popup") %>
            </button>
        </div>
    </div>
    <div class="portlet-toolbar__actions-secondary">
        <div id="popMeUp">
            <button dojoType="dijit.form.Button" onClick="doManageLogs()"  value="popup" name="popup" >
                <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"LOG_Manager") %>
            </button>
        </div>
    </div>
</div>

<div id="tailContainer" class="log-files__container">
    <iframe id="tailingFrame" src="/html/blank.jsp" class="log-files__iframe"></iframe>
</div>

<div id="logman_dia" dojoType="dijit.Dialog">
    <div id="search" title="<%= com.liferay.portal.language.LanguageUtil.get(pageContext, "LOG_activity") %>" ></div>
    <div style="width:620px">
        <table class="listingTable" id="logsTable" align="center">
            <tr id="logsTableHeader">
                <th width="5%"><input type="checkbox" dojotype="dijit.form.CheckBox" id="checkAllCkBx" onclick="checkUncheck()" /></th>
                <th nowrap="nowrap" width="5%" style="text-align:center;">Status</th>
                <th nowrap="nowrap" width="32%" style="text-align:center;">Log Name</th>
                <th nowrap="nowrap" width="58%" style="text-align:center;">Log Description</th>
            </tr>
        </table>
    </div>
    <div class="buttonRow">
        <button dojoType="dijit.form.Button" name="filterButton" onClick="enableDisableLogs()"><%= com.liferay.portal.language.LanguageUtil.get(pageContext, "LOG_button") %></button>
        <button dojoType="dijit.form.Button" name="refreshButton" onClick="getCurrentLogs ()">Refresh</button>
    </div>
</div>

