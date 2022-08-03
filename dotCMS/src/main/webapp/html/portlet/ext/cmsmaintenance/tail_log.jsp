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

    var dataLogPrintedElem = null;
    var keywordLogInput = null;
    var logViewerFiltering = false;

    // TEMP - BEGIN
    function countLogViewerLines() {
        var splitParam = '<br>';
        var linesCount = dataLogPrintedElem.innerHTML.split(splitParam).length;
        document.querySelector('.logViewerLinesCount').value = linesCount;
    }
    // TEMP - END

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

        var dataLogSourceElem = document.getElementById('tailingFrame');
        var iDoc = dataLogSourceElem.contentWindow || dataLogSourceElem.contentDocument;

        iDoc.document.addEventListener("logUpdated", (e) => {
            // Only triggering if "newContent" has a value, cuz there can be calls from BE with empty data
            // for the purpose of just to keep the connection alive
            if (e.detail.newContent.length > 0) {
                updateLogViewerData(e.detail.newContent);

                var jsScriptTags = document.getElementById('tailingFrame').contentDocument.body?.querySelectorAll('script')
                for (var i = 0, total = jsScriptTags.length; i < total; i++) {
                    jsScriptTags[i].parentNode.removeChild(jsScriptTags[i]);
                }
            }
        })
    }
    
    // Function called on every fiter keydown event
    function filterLog(event) {
        const ignoredKeys = ["ArrowLeft", "ArrowUp", "ArrowDown", "ArrowRight"];

        var log = document.getElementById('tailingFrame').contentDocument.body?.innerHTML;

        // If keyword is greater than 2 characters, then filtering is applied
        logViewerFiltering = keywordLogInput.value.length > 2;

        if (event.key === 'Enter' && logViewerFiltering) {
            excludeLogRowsActive = true;
            log = performLogViewerMark(log, excludeNoMatchingRows);
        } else if (!ignoredKeys.includes(event.key) && logViewerFiltering) {
            excludeLogRowsActive = false;
            log = performLogViewerMark(log);
        }

        dataLogPrintedElem.innerHTML = null;
        dataLogPrintedElem.insertAdjacentHTML('beforeend', log);
        
    }

    // Function that gets called on every new log update
    function updateLogViewerData(newContent) {
        if (logViewerFiltering) {
            excludeLogRowsActive ? 
            newContent = performLogViewerMark(newContent, excludeNoMatchingRows) : newContent = performLogViewerMark(newContent);
        }

        dataLogPrintedElem.insertAdjacentHTML('beforeend', '<p style="margin:0">' + newContent + '</p>');

        if (document.querySelector('#scrollMe').checked) {
            scrollLogToBottom();
        }
    }

    // Function that adds to the log content SPAN Html Tags used for highlight
    function addLogViewerKeywordMatchHighlight(log) {
        var keyword = keywordLogInput.value;

        var regEx = new RegExp( keyword, "ig");
        return log.replaceAll(regEx, highlightTagBegin + "$&" + highlightTagEnd);
    }

    function performLogViewerMark(newContent, callback) {
        var log;
        log = addLogViewerKeywordMatchHighlight(newContent);
        
        if (callback) {
            log = callback(log);
        }

        return log;
    }

    // Function that gets called when pressed "Enter" key to exclude no matching rows
    function excludeNoMatchingRows(log) {
        var splitParam = '<br>';
        return log.split(splitParam).filter((row) => row.indexOf('highlighKeywordtMatchLogViewer') !== -1).join(splitParam) + splitParam;
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
            <input class="logViewerLinesCount" onClick="countLogViewerLines()" />
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
    <iframe id="tailingFrame" src="/html/blank.jsp" style="display: block; min-height: 250px;" class="log-files__iframe"></iframe>
    <div class="logViewerPrinted" style="flex-grow: 1;"></div>
</div>
