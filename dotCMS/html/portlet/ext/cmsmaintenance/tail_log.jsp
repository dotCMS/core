<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%

	String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX");
	if(!UtilMethods.isSet(regex)){
		regex=".*";
	}
	String[] files = FileUtil.listFiles(Config.CONTEXT.getRealPath(Config.getStringProperty("TAIL_LOG_LOG_FOLDER")));
	Pattern p = Pattern.compile(regex);  
	List<String> l = new ArrayList<String>();
	for(String x : files){
		if(p.matcher(x).matches()){  
			l.add(x);   
		}
	}
	// http://jira.dotmarketing.net/browse/DOTCMS-6271
	// put matched files set to an array with exact size and then sort them
	files = l.toArray(new String[l.size()]);
	Arrays.sort(files);
	


	
	
	try {
		user = com.liferay.portal.util.PortalUtil.getUser(request);
	} catch (Exception e) {
		response.sendError(403);
		return;
	}
	try {
		if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
			response.sendError(403);
			return;
		}
	} catch (Exception e2) {
		Logger.error(this.getClass(), e2.getMessage(), e2);
		response.sendError(403);
		return;
	}

	
%>

<%request.setAttribute("popup", "true"); %>
<%@ include file="/html/common/top_inc.jsp" %>

<style>
	#tailingFrame{
		border:1px solid silver;
		overflow: auto;
		height:100%;
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
		display:none;
	}
	
	#logman_dia {
	   width:640px;
       height:480px;
	}
</style>

<script>

	function reloadTail(){
		var x = dijit.byId("fileName").getValue();
		dojo.byId("tailingFrame").src='/dotTailLogServlet/?fileName='+x;

	}

	function doPopup(){
			var x = dijit.byId("fileName").getValue();
			dijit.byId("fileName").setValue("");
			var newwin = window.open("/html/portlet/ext/cmsmaintenance/tail_log.jsp?fileName=" + x, "tailwin", "status=1,toolbars=1,resizable=1,scrollbars=1,height=600,width=800");
			newwin.focus();
	}

	dojo.ready(function(){

		if(self != top){
			dojo.style(dojo.byId("popMeUp"), "display", "block");
		}

			
		<%if(request.getParameter("fileName")!= null){%>
			dijit.byId("fileName").setValue("<%=UtilMethods.xmlEscape(request.getParameter("fileName"))%>");
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
		<%=LanguageUtil.get(pageContext, "Tail")%>: 
		<select name="fileName" dojoType="dijit.form.FilteringSelect" ignoreCase="true" id="fileName" style="width:250px;" onchange="reloadTail();">
			<option value=""></option>
			<%for(String f: files){ %>

					<option value="<%= f%>"><%= f%></option>

			<%} %>
		</select>
		&nbsp; &nbsp; 
		<%=LanguageUtil.get(pageContext, "Follow") %> <input type='checkbox' id='scrollMe' dojoType="dijit.form.CheckBox" value=1 checked="true" />
		            <button dojoType="dijit.form.Button" onClick="doPopup()"  iconClass="detailView"  value="popup" name="popup" >
                <%= LanguageUtil.get(pageContext,"popup") %>
            </button>
		<div id="popMeUp">
            <button dojoType="dijit.form.Button" onClick="doManageLogs()"  iconClass="detailView"  value="popup" name="popup" >
                <%= LanguageUtil.get(pageContext,"LOG_Manager") %>
            </button>
		</div>
		
	</div>
	
	<div id="tailContainer">
		<iframe id="tailingFrame" src="/html/blank.jsp"></iframe>
	</div>

    
   <div id="logman_dia" dojoType="dijit.Dialog">
        <div id="search" title="<%= LanguageUtil.get(pageContext, "LOG_activity") %>" >
		
		<div style="width:90%;margin:auto;">
		    <table class="listingTable" id="logsTable" align="center">
		        <tr id="logsTableHeader">
		            <th><input width="5%" type="checkbox" dojoType="dijit.form.CheckBox" id="checkAllCkBx" value="true" onClick="checkUncheck()" /></th>
		            <th nowrap="nowrap" width="5%" style="text-align:center;">Status</th>
		            <th nowrap="nowrap" width="32%" style="text-align:center;">Log Name</th>
		            <th nowrap="nowrap" width="58%" style="text-align:center;">Log Description</th>
		        </tr>
		    </table>
		</div>
		<div>&nbsp;</div>
		
		<div class="buttonRow">
		    <button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" onclick="enableDisableLogs()"> <%= LanguageUtil.get(pageContext, "LOG_button") %> </button>
		    <button dojoType="dijit.form.Button" iconClass="resetIcon" name="refreshButton" onclick="getCurrentLogs()"> Refresh </button>
		</div>
		
   </div>

   