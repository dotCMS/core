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
<script src='/html/js/hrJS/hrJS.custom.min.js'></script>
<style>
    #Logging #tailContainer {
        height: 100%;
    }
    span[data-hr]{
        background-color: yellow;
        color: #000;
    }
    .logViewerPrinted {
        background-color: #000;
        color: #d6d6d6;
        font-family: Andale Mono, monospace;
        font-size: 12px;
        overflow: scroll;
        padding: 1rem;
        white-space: pre;
    }
    .highlighKeywordtMatchLogViewer{
        background-color: yellow;
        color: #000;
    }
</style>
<script type="text/javascript">

    function disableFollowOnScrollUp() {
        var iframe = dojo.byId('tailingFrame');
        var logWindow = iframe.contentWindow;
        var lastScrollTop = 0;
        var followCB = dojo.byId("scrollMe");
        var cbWidget = dijit.getEnclosingWidget(followCB);
        setTimeout(() => {
            logWindow.addEventListener("scroll", function(){
                var st = logWindow.pageYOffset || logWindow.document.documentElement.scrollTop;
                //scroll up
                if (st < lastScrollTop && cbWidget.get('checked')){
                    cbWidget.set('checked', false);
                }
                lastScrollTop = st <= 0 ? 0 : st; // For Mobile or negative scrolling
            }, false);
        }, 200)
    };

	function doPopup(){
			var x = dijit.byId("fileName").getValue();
			dijit.byId("fileName").setValue("");
			var newwin = window.open("/html/portlet/ext/cmsmaintenance/tail_log_popup.jsp?fileName=" + x, "tailwin", "status=1,toolbars=1,resizable=1,scrollbars=1,height=800,width=1000");
			newwin.focus();
	}

	dojo.ready(function(){
		<%if(request.getParameter("fileName")!= null){
			String selectedFileNameStr = com.dotmarketing.util.UtilMethods.xmlEscape(request.getParameter("fileName")).replace(logPath + File.separator, "");
			selectedFileNameStr = selectedFileNameStr.replace("\\", "\\\\");
		%>
		dijit.byId("fileName").attr("displayedValue","<%=selectedFileNameStr%>");
		<%}%>
	});

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
            var tdCheckbox = '<td><input name="logs" class="taskCheckBox" dojoType="dijit.form.CheckBox" type="checkbox" name="logs" value="' + name + '" /></td>';
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

	function reloadTail(){
		var x = dijit.byId("fileName").getValue();
        if(x) {
		    dojo.byId("tailingFrame").src='/dotTailLogServlet/?fileName='+x;
            disableFollowOnScrollUp();
            dijit.byId("downloadLog").attr("disabled", false);
        } else {
            dijit.byId("downloadLog").attr("disabled", true);
        }

        // Reset values from Log Viewer container and input filter value
        document.querySelector('.logViewerPrinted').innerHTML = '';
        document.querySelector('#keywordLogFilterInput').value = '';

        // Wait until the Iframe has loaded so the binding of events can be applied
        var iframeContentInterval = setInterval(() => { 
            var contentLoaded = document.getElementById('tailingFrame').contentDocument.body?.innerHTML;

            if (contentLoaded) {
                clearInterval(iframeContentInterval);
                attachLogIframeEvents()
            }
        }, 200);

        
	}

    /**********************/
    /* Log Viewer - BEGIN */

    var highlightTagBegin = '<span class="highlighKeywordtMatchLogViewer">';
    var highlightTagEnd = "</span>";
    var attachedFilterLogEvents = false;
    var excludeLogRowsActive = false;
    var logRawContent = '';
    var logViewerMarkedText = false;

    var dataLogPrintedElem = null;
    var keywordLogInput = null;

    function attachLogIframeEvents() {
        if (!attachedFilterLogEvents) {
            var debounce = (callback, time = 300, interval) => (...args) => {
                clearTimeout(interval, interval = setTimeout(() => callback(...args), time));
            }

            dataLogPrintedElem = document.querySelector('.logViewerPrinted');

            keywordLogInput = document.querySelector('#keywordLogFilterInput');
            keywordLogInput.addEventListener("keyup", debounce(filterLog, 300));

            // Flag to avoid binding multiple debounce events on filter input
            attachedFilterLogEvents = true;
        }

        logRawContent = '';

        var dataLogSourceElem = document.getElementById('tailingFrame');
        var iDoc = dataLogSourceElem.contentWindow || dataLogSourceElem.contentDocument;

        iDoc.document.addEventListener("logUpdated", (e) => {
            // Only triggering if "newContent" has a value, cuz there can be calls from BE with empty data
            // for the purpose of just to keep the connection alive
            if (e.detail.newContent.length > 0) {
                updateLogViewerData(e.detail.newContent);
            }
        })
    }
    
    // Function called on every fiter keydown event
    function filterLog(event) {
        const ignoredKeys = ["ArrowLeft", "ArrowUp", "ArrowDown", "ArrowRight"];

        if (event.key === 'Enter') {
            excludeLogRowsActive = true;
            performLogViewerMark(excludeNoMatchingRows);
        } else if (!ignoredKeys.includes(event.key)) {

            // if previously the rows were excluded when filtering then a copy of the whole content needs to be restablished
            if (excludeLogRowsActive) {
                dataLogPrintedElem.innerHTML = logRawContent;
            }

            excludeLogRowsActive = false;
            performLogViewerMark();
        }
    }

    // Function that gets called on every new log update
    function updateLogViewerData(newContent) {
        dataLogPrintedElem.innerHTML += newContent;
        logRawContent += newContent;

        excludeLogRowsActive ? performLogViewerMark(excludeNoMatchingRows) : performLogViewerMark();

        if (document.querySelector('#scrollMe').checked) {
            scrollLogToBottom();
        }
    }

    // Function that cleans the log content from SPAN Html Tag used for highlight
    function removeLogViewerKeywordMatchHighlight(log) {
        logViewerMarkedText = false;
        return log.replaceAll(highlightTagBegin, '').replaceAll(highlightTagEnd, '');
    }

    // Function that adds to the log content SPAN Html Tags used for highlight
    function addLogViewerKeywordMatchHighlight(log, keyword) {

        // if log content was previously highlighted(dirty) then we need to clean it first
        if (logViewerMarkedText) { log = removeLogViewerKeywordMatchHighlight(log); }
        
        for (let index = 0, len = log.length; index < len; index++) {
            index = log.toLocaleLowerCase().indexOf(keyword, index);
            
            if (index === -1) {
                break;
            }else{
                // If keyword match found, add initial SPAN tag
                log = log.slice(0, index) + highlightTagBegin + log.slice(index);
                // move index to last position of keyword match
                index = index + highlightTagBegin.length + keyword.length
                // Add ending SPAN tag
                log = log.slice(0, index) + highlightTagEnd + log.slice(index);

                // Flag to mark the log content as highlighted(dirty)
                logViewerMarkedText = true;
            }
        }

        return log;

    }

    function performLogViewerMark(callback) {
        var keyword = keywordLogInput.value;
        var log = dataLogPrintedElem.innerHTML;

        // If keyword is greater than 2 characters, then filtering is applied
        if (keyword && keyword.length > 2) {
            log = addLogViewerKeywordMatchHighlight(log, keyword);
            
            if (callback) {
                log = callback(log);
            }

        }

        dataLogPrintedElem.innerHTML = log;
    }

    // Function that gets called when pressed "Enter" key to exclude no matching rows
    function excludeNoMatchingRows(log) {
        // The "splitParam" can change depending if it's comming from a DOM Element or a JS variable
        var splitParam = excludeLogRowsActive ? '<br>' : '<br />';
        var filteredData = log.split(splitParam);
        var excludedRows = filteredData.filter((row) => row.indexOf('highlighKeywordtMatchLogViewer') !== -1)
        var joined = excludedRows.join(splitParam) + splitParam;
        return joined;
    }

    function scrollLogToBottom() {
        dataLogPrintedElem.scrollTop = dataLogPrintedElem.scrollHeight;
    }

    /* Log Viewer - END */
    /********************/

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
            <input dojoType="dijit.form.TextBox" id="keywordLogFilterInput" placeholder="<%=com.liferay.portal.language.LanguageUtil.get(pageContext, "Filter")%>" type="text" style="width: 200px">
        </div>
    </div>
    <div class="portlet-toolbar__actions-secondary">
        <button dojoType="dijit.form.Button" onClick="doPopup()" value="popup" name="popup">
            <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"popup") %>
        </button>
        <button dojoType="dijit.form.Button" onclick="location.href='/api/v1/maintenance/_downloadLog/' + document.getElementById('fileName').value"  id="downloadLog" value="download" name="download" disabled>
            <%= com.liferay.portal.language.LanguageUtil.get(pageContext,"Download") %>
        </button>
    </div>
</div>

<div id="tailContainer" class="log-files__container" style="display: flex; flex-direction: column;">
    <iframe id="tailingFrame" src="/html/blank.jsp" style="display: none" class="log-files__iframe"></iframe>
    <div class="logViewerPrinted" style="flex-grow: 1;"></div>
</div>
