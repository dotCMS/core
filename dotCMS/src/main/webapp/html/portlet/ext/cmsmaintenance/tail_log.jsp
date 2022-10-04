<%@page import="java.io.File"%>
<%@ page import="com.dotmarketing.servlets.taillog.TailLogServlet" %>
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
    .highlightKeywordMatchLogViewer{
        background-color: yellow;
        color: #000;
    }

    .keepAlive{
        display: none;
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
    }

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
                attachLogIframeEvents();
            }
        }, 200);


	}

    /**********************/
    /* Log Viewer - BEGIN */

    const ON_SCREEN_PAGES = 3;

    const DROP_PAGES_PERCENT = 30;

    const MIN_KEYWORD_LENGTH = 2;

    const MAX_VISITED_PAGES = 100;

    const BATCH_SIZE = 10;

    /**
     *
     * @param src expects the iframe
     * @param dest expects the div where we want to render the log/highlight etc..
     * @returns {{fetchNextPage: fetchNextPage, filterByKeyword: filterByKeyword, updateView: updateView, setFollowing: setFollowing, fetchPriorPage: fetchPriorPage}}
     * @constructor
     */
    const LogViewManager = ({src, dest}) => {

        let pagesOnScreen = 0;

        let currentPageIndex = 0;

        let following = true;

        let keyword = null;

        let matchLinesOnlyView = false;

        function _dropOldestPages(){
            if (pagesOnScreen >= ON_SCREEN_PAGES) {

                const pagesToDrop = Math.round((pagesOnScreen * DROP_PAGES_PERCENT) * .01);
                for (let i = 1; i <= pagesToDrop; i++) {
                    const first = dest.querySelector(`.log:first-child`);
                    console.log("First:: " + first);
                    if (!first) {
                        break;
                    }
                        const firstPageId = first.dataset.page;
                        //console.log("FirstPageID:: " + firstPageId);
                        let pageToDrop = dest.querySelectorAll(`.page${firstPageId}`);
                        //console.log("PageToDrop ::: " + pageToDrop);
                        if (pageToDrop.length > 0) {
                            //Remove all items on that page
                            pageToDrop.forEach((elem) => elem.remove());
                            pagesOnScreen--;
                        }
                        console.log(' page  : ' + firstPageId + ' dropped!');
                }
            }
        }

        function _fetchPriorPage(){
            //if following back to load prior pages we should stop feeding the log (adding new stuff to it)
            const first = dest.querySelector(`.log:first-child`);
            if(first){
                let firstPageId = first.dataset.page;
                firstPageId--;
                const list = src.document.body.querySelectorAll(`.page${firstPageId}`);
                if (list.length > 0) {
                    //iterate backwards to preserve the original order
                    for (let i = list.length - 1; i >= 0; i--) {
                        const elem = list[i];
                        dest.insertAdjacentHTML('afterbegin', _applyHighlight(elem.outerHTML));
                    }
                    pagesOnScreen++;
                    //Move the scroll just a tiny bit, so we have room to fire the event again.
                    dest.scrollTop = dest.scrollTop + 10;
                }
            }
        }

        function _dropNewestPages() {
            if (pagesOnScreen >= ON_SCREEN_PAGES) {

                const pagesToDrop = Math.round((pagesOnScreen * DROP_PAGES_PERCENT) * .01);

                for (let i = 1; i <= pagesToDrop; i++) {

                    const last = dest.querySelector(`.log:last-child`)
                    if (!last) {
                        break;
                    }
                        let lastPageId = last.dataset.page;
                        //console.log("LastPageID:: " + lastPageId);
                        const pageToDrop = dest.querySelectorAll(`.page${lastPageId}`);
                        //console.log("PageToDrop ::: " + pageToDrop);
                        if (pageToDrop.length > 0) {
                            //Remove all items on that page
                            pageToDrop.forEach((elem) => elem.remove());
                            pagesOnScreen--;
                        }
                        console.log(' page  : ' + lastPageId + ' dropped!');
                }
            }
        }

        function _fetchNextPage(){

            const last = dest.querySelector(`.log:last-child`);
            if(last){
                let lastPageId = last.dataset.page;
                //console.log(" Last page id is  ::: " + lastPageId);
                lastPageId++;
                const list = src.document.body.querySelectorAll(`.page${lastPageId}`);
                if (list.length > 0) {
                    list.forEach((elem)=>{
                        dest.insertAdjacentHTML('beforeend', _applyHighlight(elem.outerHTML));
                    });
                    pagesOnScreen++;
                }
            }
        }

        function _updateView(e){

            const pageId = parseInt(e.detail.pageId);
            //initialize var so we know what page we're on

            if(currentPageIndex === 0){
                currentPageIndex = pageId;
                pagesOnScreen = 0;
            } else {
                if (pageId > currentPageIndex) {
                    currentPageIndex = pageId;
                    pagesOnScreen++;
                }
            }
            const newContent = e.detail.newContent;

            if (_isFiltering()) {
                dest.insertAdjacentHTML('beforeend', _applyHighlight(newContent));
            } else {
                dest.insertAdjacentHTML('beforeend', newContent);
            }

        }

        function _applyFilter() {
            _removeHighlight();
            if (_isFiltering()) {
                dest.innerHTML = _applyHighlight(dest.innerHTML);
            }
        }

        function _applyHighlight(newContent) {
            const regEx = new RegExp(keyword, "ig");
            return newContent.replaceAll(regEx,
                '<span class="highlightKeywordMatchLogViewer">$&</span>');
        }

        function _removeHighlight() {
            dest.querySelectorAll(".highlightKeywordMatchLogViewer").forEach(
                el => el.replaceWith(...el.childNodes));
        }

        function _isFiltering(){
            return keyword != null && keyword.length > MIN_KEYWORD_LENGTH;
        }

        function _scrollDownToBottom() {
            dest.scrollTop = dest.scrollHeight;
        }

        function _matchingLinesOnlyView(){

            if (_isFiltering()) {

                let ms1 = Date.now();

                const first = dest.querySelector(`.log:first-child`);
                const last = dest.querySelector(`.log:last-child`);

                if(!first || !last){
                    console.warn('no items are loaded in the view.');
                    return;
                }
                //first and last on-screen items
                const firstPageId = first.dataset.page;
                const lastPageId = last.dataset.page;

                let buffer = [];
                //Filter lines matching the keyword using the pages currently on screen
                for(let i = firstPageId; i <= lastPageId; i++){
                    const list = src.document.body.querySelectorAll(`.page${i}`);
                    const matches = _matchingLines(list);
                    if(matches && matches.length > 0){
                        buffer = buffer.concat(matches);
                    }
                }

                dest.replaceChildren();

                buffer.forEach(value => {
                    dest.insertAdjacentHTML('beforeend',value);
                });

                buffer.length = 0;

                dest.innerHTML = _applyHighlight(dest.innerHTML);

                //Grab Pages before the current first line
                //These must go before the ones we just loaded

                let priorLinesBuffer = [];
                let pageId = firstPageId;
                for(let i=BATCH_SIZE; i < MAX_VISITED_PAGES; i+= BATCH_SIZE) {
                    let morePagesAvailable = _fetchPriorLinesOnly(pageId, BATCH_SIZE, priorLinesBuffer);
                    if(morePagesAvailable === false){
                        break;
                    }
                    pageId -= BATCH_SIZE;
                }

                priorLinesBuffer.reverse().forEach(value => {
                    dest.insertAdjacentHTML('afterbegin', value );
                });

                priorLinesBuffer.length = 0;

                //Grab Pages after the current last line

                pageId = lastPageId;
                let nextLinesBuffer = [];
                for(let i=BATCH_SIZE; i < MAX_VISITED_PAGES; i += BATCH_SIZE) {
                    let morePagesAvailable = _fetchNextLinesOnly(pageId, BATCH_SIZE, nextLinesBuffer);
                    if(morePagesAvailable === false){
                        break;
                    }
                    pageId += BATCH_SIZE;
                }

                nextLinesBuffer.forEach(value => {
                    dest.insertAdjacentHTML('beforeend',value);
                });

                nextLinesBuffer.length = 0;


                matchLinesOnlyView = true;

                let ms2 = Date.now();

                console.log( "duration in ms is ::: " +  (ms2 - ms1));
            }
        }

        function _fetchPriorLinesOnly(startFromPageId, numPages, buffer){
             const stopAtPageId = (startFromPageId - numPages);
             for(let i = startFromPageId; i >= stopAtPageId; i--){
                 const list = src.document.body.querySelectorAll(`.page${i}`);
                 if(!list || list.length === 0){
                     return false;
                 }
                 const matches = _matchingLines(list);
                 if(matches && matches.length > 0){
                     buffer = buffer.concat(matches);
                 }
             }
             return true;
        }

        function _fetchNextLinesOnly(startFromPageId, numPages, buffer){
            const stopAtPageId = (startFromPageId + numPages);
            for(let i = startFromPageId; i <= stopAtPageId; i++){
                const list = src.document.body.querySelectorAll(`.page${i}`);
                if(!list || list.length === 0){
                    return false;
                }
                const matches = _matchingLines(list);
                if(matches && matches.length > 0){
                    buffer = buffer.concat(matches);
                }
            }
            return true;
        }

        function _matchingLines(nodes){
            const regEx = new RegExp( keyword, "i");
            let matches = [];
            if(nodes && nodes.length > 0){
                nodes.forEach(el => {
                    const html = el.innerHTML;
                    const lines = html.split('<br>').filter((row)=> regEx.test(row)).join('<br>');
                    if(lines){
                        const classes = el.classList.value;
                        const page = el.dataset['page'];
                        const logNum = el.dataset['lognumber'];
                        const p = `<p class="${classes}" data-page="${page}" data-lognumber="${logNum}" style="margin:0"> ${lines} </p>`;
                        matches.push(p);
                    }
                });
            }
            return matches;
        }

        return ({

            setFollowing : (value) => {
                following = value;
            },

            filterByKeyword : (value, showMatchingLinesOnly) => {
                keyword = value;
                if(showMatchingLinesOnly){
                    _matchingLinesOnlyView();
                } else {
                    _applyFilter();
                }
            },

            fetchPriorPage : () => {
                if(following){
                    return;
                }
                _fetchPriorPage();
                _dropNewestPages();
            },

            fetchNextPage : () => {
                if(following){
                    return;
                }
                _fetchNextPage();
                _dropOldestPages();
            },

            updateView : (e) => {
                if(!following){
                    return;
                }
                _updateView(e);
                _scrollDownToBottom();
                _dropOldestPages();
            }

        });
    }

    let logViewManager = null;



    function attachLogIframeEvents() {

        if(logViewManager){
            console.warn("logView is already initialized.");
            return;
        }

        const followCheck = document.getElementById('scrollMe');
        const keywordInput = document.querySelector('#keywordLogFilterInput');
        const dataLogSourceElem = document.getElementById('tailingFrame');
        const logView = document.querySelector('.logViewerPrinted');
        const iDoc = dataLogSourceElem.contentWindow || dataLogSourceElem.contentDocument;


        logViewManager = LogViewManager({src:iDoc,dest:logView});

        followCheck.addEventListener("change",(e)=>{
            logViewManager.setFollowing(e.currentTarget.checked);
        });

        iDoc.document.addEventListener("logUpdated", (e) => {
            logViewManager.updateView(e);
        });

        let lastScrollTop = 0;

        logView.addEventListener("scroll", (e)=>{

            const div = e.target;
            const scrollTop = div.scrollTop;
            const scrollPercentage = computeScrollPercentage(div.scrollHeight, scrollTop);

           // console.log(" scroll % :: " + scrollPercentage);

            if (scrollTop < lastScrollTop) {
                //We're scrolling up
                const followCheck = dijit.byId('scrollMe');
                if (followCheck.checked) {
                    //This does guarantee that we need to scroll back up at least till the scroll top is at the 94% of the div height
                    //This is makes the check-box stay checked until we have scrolled back a bit more
                    if (scrollPercentage <= 91) {
                        //This does not fire the change event
                        followCheck.setValue(false);
                        //Therefore, we need to explicitly indicate we want to stop following the events
                        logViewManager.setFollowing(false);
                    }
                }
            }

            lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;

            if(scrollPercentage === 0) {
                //We're at the top of the container.
                logViewManager.fetchPriorPage();
                return;
            }

            if( scrollPercentage === 95 ){
                //we're at the bottom of the scroll
                logViewManager.fetchNextPage();
            }

        });

        const debounce = (callback, time = 300, interval) => (...args) => {
            clearTimeout(interval, interval = setTimeout(() => callback(...args), time));
        };

        const ignoredKeys = ["ArrowLeft", "ArrowUp", "ArrowDown", "ArrowRight"];

        function processKeyEvent(e){

            if(ignoredKeys.includes(e.key)){
               return;
            }

            const input = e.target;
            if(input.value.length > 2){
                logViewManager.filterByKeyword(input.value, e.key === 'Enter');
            } else {
                logViewManager.filterByKeyword(null);
            }
        }

        keywordInput.addEventListener("keyup", debounce(processKeyEvent, 300));

    }

    /**
     * Simple Rule of 3 to compute how much (Percent wise) of the total scroll height we have covered with the scroll bar
     * @param scrollHeight
     * @param scrollTop
     * @returns {number}
     */
    function computeScrollPercentage(scrollHeight, scrollTop){
        return Math.round(scrollTop * 100 / scrollHeight);
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
                <input type="checkbox" id="scrollMe" dojoType="dijit.form.CheckBox" value=1 checked  />
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
    <iframe id="tailingFrame" src="/html/blank.jsp" style="display: none;" class="log-files__iframe" ></iframe>
    <div class="logViewerPrinted" style="flex-grow: 1;"></div>
</div>
