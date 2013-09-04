<%@page import="java.io.File"%>
<%

	String regex = com.dotmarketing.util.Config.getStringProperty("TAIL_LOG_FILE_REGEX");
	if(!com.dotmarketing.util.UtilMethods.isSet(regex)){
		regex=".*";
	}
	String logPath = com.dotmarketing.util.FileUtil.getAbsolutlePath(com.dotmarketing.util.Config.getStringProperty("TAIL_LOG_LOG_FOLDER"));
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
		if (!com.dotmarketing.business.APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", uu)) {
			response.sendError(403);
			return;
		}
	} catch (Exception e2) {
		com.dotmarketing.util.Logger.error(this.getClass(), e2.getMessage(), e2);
		response.sendError(403);
		return;
	}


%>


<style type="text/css">
	
	body,html{ height: 100%; }
	
	#tailingFrame{
		border:1px solid silver;
		overflow: auto;
		height:100%;
		width:100%;

	}
	#tailContainer {
		margin-top:10px;
		margin-bottom:30px;
		height:80%;
		width:94%;
		position: relative;
		top: 40px;;
	    left: 3%;
	}
	#headerContainer{
		position: relative;
		width:94%;
	   	left: 3%;
		border:0px solid silver;
		padding-top:10px;
		padding-left:10px;
	}

	#popMeUp{
		float:right;
	}

	#logman_dia {
	   width:640px;
       height:480px;
	}
</style>

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


		<%if(request.getParameter("fileName")!= null){%>
		dijit.byId("fileName").attr("displayedValue","<%=com.dotmarketing.util.UtilMethods.xmlEscape(request.getParameter("fileName")).replace(logPath + File.separator, "")%>");
		<%}%>

	});

	function doManageLogs() {

		dijit.byId('logman_dia').show();

	}

    function checkUncheck () {

        var x = dijit.byId( "checkAllCkBx" ).checked;
        dojo.query( ".taskCheckBox" ).forEach( function ( node ) {
            node.checked = x;
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
        }

    };

    dojo.ready(function() {
    	dojo.connect(dijit.byId("logman_dia"),"onShow",null,getCurrentLogs);
    });

</script>




	<div id="headerContainer">
		<%=com.liferay.portal.language.LanguageUtil.get(pageContext, "Tail")%>:
		<select name="fileName" dojoType="dijit.form.FilteringSelect" ignoreCase="true" id="fileName" style="width:250px;" onchange="reloadTail();">
			<option value=""></option>
			<%for(File f: files){ %>

					<option value="<%= f.getPath()%>"><%= f.getPath().replace(logPath + File.separator, "")%></option>

			<%} %>
		</select>
		&nbsp; &nbsp;
		<%=com.liferay.portal.language.LanguageUtil.get(pageContext, "Follow") %> <input type='checkbox' id='scrollMe' dojoType="dijit.form.CheckBox" value=1 checked="true" />
		            <button dojoType="dijit.form.Button" onClick="doPopup()"  iconClass="detailView"  value="popup" name="popup" >
                <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"popup") %>
            </button>
		<div id="popMeUp">
            <button dojoType="dijit.form.Button" onClick="doManageLogs()"  iconClass="detailView"  value="popup" name="popup" >
                <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"LOG_Manager") %>
            </button>
		</div>

	</div>

	<div id="tailContainer">
		<iframe id="tailingFrame" src="/html/blank.jsp"></iframe>
	</div>


   <div id="logman_dia" dojoType="dijit.Dialog">
        <div id="search" title="<%= com.liferay.portal.language.LanguageUtil.get(pageContext, "LOG_activity") %>" ></div>

		<div style="width:620px;height:300px;">
		    <table class="listingTable" id="logsTable" align="center">
		        <tr id="logsTableHeader">
		            <th width="5%"><input type="checkbox" dojotype="dijit.form.CheckBox" id="checkAllCkBx" onclick="checkUncheck()"></input></th>
		            <th nowrap="nowrap" width="5%" style="text-align:center;">Status</th>
		            <th nowrap="nowrap" width="32%" style="text-align:center;">Log Name</th>
		            <th nowrap="nowrap" width="58%" style="text-align:center;">Log Description</th>
		        </tr>
		    </table>
		</div>
		<div>&nbsp;</div>		
		<div class="buttonRow">
		    <button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" onClick="enableDisableLogs()"> <%= com.liferay.portal.language.LanguageUtil.get(pageContext, "LOG_button") %> </button>
		    <button dojoType="dijit.form.Button" iconClass="resetIcon" name="refreshButton" onClick="getCurrentLogs ()"> Refresh </button>
		</div>

   </div>

