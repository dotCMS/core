<%@page import="java.io.File"%>
<%@ page import="com.dotcms.rest.api.v1.taillog.TailLogResource" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<script type="text/javascript" src="/html/js/sse.js"></script>

<style>
    #Logging #tailContainer {
        height: 600px;
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
        min-height: 600px;
    }
    .highlightKeywordMatchLogViewer{
        background-color: yellow;
        color: #000;
    }
    .keepAlive{
        display: none;
    }
</style>

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
	List<File> files = new ArrayList<>(com.liferay.util.FileUtil.getFilesByPattern(new File(logPath), "*.*"));
	java.util.regex.Pattern pp = java.util.regex.Pattern.compile(regex);
	java.util.List<File> l = new java.util.ArrayList<File>();
	for(File x : files){
		if(pp.matcher(x.getName()).matches()){
			l.add(x);
		}
	}
	// http://jira.dotmarketing.net/browse/DOTCMS-6271
	// put matched files set to an array with exact size and then sort them
    files = new ArrayList<>(l);
    files.sort(File::compareTo);


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

    let source = null;

    function reloadTail(){

    		const fileName = dijit.byId("fileName").getValue();

            if(fileName) {
                const url = '/api/v1/logs/' + fileName + '/_tail';
                console.log(url);

                closeSource(source);

                console.log('Opening new connection to '+url);

                source = new SSE(url, null);

                attachLogIframeEvents(source);

                dijit.byId("downloadLog").attr("disabled", false);
                source.stream();
            } else {
                dijit.byId("downloadLog").attr("disabled", true);
                closeSource(source);
            }

            // Reset values from Log Viewer container and input filter value
            document.querySelector('.logViewerPrinted').innerHTML = '';
            document.querySelector('#keywordLogFilterInput').value = '';
    	}

    function closeSource(source){
        if(null != source){
            try{
                source.close();
                console.log('Prior Connection Successfully closed.');
            }catch (e){
                console.log(e);
            }
        }
    }

    /**********************/
    /* Log Viewer - BEGIN */

    const ON_SCREEN_PAGES = 3;

    const DROP_PAGES_PERCENT = 30;

    const MIN_KEYWORD_LENGTH = 2;

    const PAGE_SIZE = <%=Config.getIntProperty("TAIL_LOG_LINES_PER_PAGE",10)%>;

    const MAX_VISITED_PAGES = PAGE_SIZE * 10 * 4;

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

        let filterMatchFound = false;

        let matchLinesOnlyView = false;

        let updatingView = false;


        /**
         * As we move (forward) through the contents loaded into the view (Div)
         * We need to dispose old pages to avoid saturating the div with a ton of content
         * We simply load a small number of pages and drop the old ones
         * @private
         */
        function _dropOldestPages(){
            if (pagesOnScreen >= ON_SCREEN_PAGES) {

                const pagesToDrop = Math.round((pagesOnScreen * DROP_PAGES_PERCENT) * .01);
                for (let i = 1; i <= pagesToDrop; i++) {
                    const first = dest.querySelector(`.log:first-child`);
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
                if (matchLinesOnlyView) {
                    const lines = _fetchPriorLinesOnly(firstPageId);
                    if (lines && lines.buffer.length > 0) {
                        for (let i = lines.buffer.length - 1; i >= 0; i--) {
                            const value = list[i];
                            dest.insertAdjacentHTML('afterbegin', _applyHighlight(value));
                        }
                        lines.buffer.length = 0;
                    }
                } else {
                    const list = src.document.body.querySelectorAll(`.page${firstPageId}`);
                    console.log(`loading prior page with id ${firstPageId}  and list with ${list.length} elements. `);
                    if (list.length > 0) {
                        //iterate backwards to preserve the original order
                        for (let i = list.length - 1; i >= 0; i--) {
                            const elem = list[i];
                            if(true === _isFiltering()){
                                dest.insertAdjacentHTML('afterbegin', _applyHighlight(elem.outerHTML));
                            } else {
                                dest.insertAdjacentHTML('afterbegin', elem.outerHTML);
                            }
                        }
                        pagesOnScreen++;
                    }
                }
            }
        }

        /**
         * As we move (backwards) through the contents loaded into the view (Div)
         * We need to dispose the newest pages to avoid saturating the div with a ton of content
         * We simply load a small number of pages and drop the newer ones
         * @private
         */
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

        /**
         * if scrolling down we need to be able to load contents that lay up ahead
         * @private
         */
        function _fetchNextPage() {
            const last = dest.querySelector(`.log:last-child`);
            if (last) {
                let lastPageId = last.dataset.page;
                lastPageId++;
                if (matchLinesOnlyView) {
                    const lines = _fetchNextLinesOnly(lastPageId);
                    if (lines && lines.buffer.length > 0) {
                        lines.buffer.forEach((value) => {
                            dest.insertAdjacentHTML('beforeend', _applyHighlight(value));
                        });
                        lines.buffer.length = 0;
                    }
                } else {
                    const list = src.document.body.querySelectorAll(`.page${lastPageId}`);
                    console.log(`loading next page with id ${lastPageId}  and list with ${list.length} elements. `);
                    if (list.length > 0) {
                        list.forEach((elem) => {
                            if (true === _isFiltering()) {
                                dest.insertAdjacentHTML('beforeend', _applyHighlight(elem.outerHTML));
                            } else {
                                dest.insertAdjacentHTML('beforeend', elem.outerHTML);
                            }
                        });
                        pagesOnScreen++;
                    }
                }
            }
        }

        /**
         * Append incoming content into the view
         * @param data
         * @private
         */
        function _updateView(data) {

            const pageId = parseInt(data.pageId);
            //initialize var so we know what page we're on

            if (currentPageIndex === 0) {
                currentPageIndex = pageId;
                pagesOnScreen = 0;
            } else {
                if (pageId > currentPageIndex) {
                    currentPageIndex = pageId;
                    pagesOnScreen++;
                }
            }
            const newContent = data.lines;

            if (matchLinesOnlyView) {
                const element = document.createElement('p');
                element.innerHTML = newContent;
                const matches = _matchingLines([element]);
                matches.forEach(value => {
                    dest.insertAdjacentHTML('beforeend', _applyHighlight(value));
                });
            } else {
                if (true === _isFiltering()) {
                    dest.insertAdjacentHTML('beforeend', _applyHighlight(newContent));
                } else {
                    dest.insertAdjacentHTML('beforeend', newContent);
                }
            }

        }

        /**
         * First: Reset the view Removing any previously applied  filter
         * Then: if filtering then apply the highlight
         * @private
         */

        function _applyFilter() {
            _removeHighlight();
            if (_isFiltering()) {
                dest.innerHTML = _applyHighlight(dest.innerHTML);
                filterMatchFound = (dest.querySelector(".highlightKeywordMatchLogViewer") != null);
            }
        }

        /**
         * Adds the style that highlights contents
         * @param newContent
         * @returns {*}
         * @private
         */
        function _applyHighlight(newContent) {
            const regExp = new RegExp(`(?!(class|style)+=\")(?!margin:0)${keyword}`, 'ig');
            return newContent.replaceAll(regExp,
                '<span class="highlightKeywordMatchLogViewer">$&</span>');
        }

        /**
         * Destroy the style that was added to highlight contents
         * @private
         */
        function _removeHighlight() {
            filterMatchFound = false;
            dest.querySelectorAll(".highlightKeywordMatchLogViewer").forEach(
                el => el.replaceWith(...el.childNodes));
        }

        /**
         * This indicates if a valid keyword was provided
         * @returns {boolean}
         * @private
         */
        function _isFiltering(){
            return keyword != null && keyword.length > MIN_KEYWORD_LENGTH;
        }

        /**
         * Move the scroll down to the bottom
         * @private
         */
        function _scrollDownToBottom() {
            dest.scrollTop = dest.scrollHeight;
        }

        /**
         * When enter key is pressed we need to build a view when only the lines matching the search criteria (keyword) are shown
         * @private
         */
        function _matchingLinesOnlyView(){

            if (true === _isFiltering()) {

                let t1 = Date.now();

                const first = dest.querySelector(`.log:first-child`);
                const last = dest.querySelector(`.log:last-child`);

                if(!first || !last){
                    console.error(' No items are loaded in the view.');
                    return;
                }
                //first and last on-screen items
                const firstPageId = parseInt(first.dataset.page);
                const lastPageId = parseInt(last.dataset.page);


                //Grab Pages before the current first line
                //These means pages not loaded into the view that are in the iframe before the first line shown in the div
                //These must go before the ones we just loaded

                let maxVisitedPages = MAX_VISITED_PAGES;

                let priorLinesBuffer = [];
                let pageId = firstPageId - 1;
                for(let i=PAGE_SIZE; i <= maxVisitedPages; i+= PAGE_SIZE) {
                    let lines = _fetchPriorLinesOnly(pageId);
                    if(lines.buffer && lines.buffer.length > 0) {
                        priorLinesBuffer = priorLinesBuffer.concat(lines.buffer);
                    }
                    if(lines.hasMorePages === false){
                        break;
                    }

                    //This is an edge case on which we show a dialog  that allows breaking long search that hasn't returned any matches.
                    if( i === maxVisitedPages && lines.buffer.length === 0){
                        const keepLooking = confirm(`No matches have been found so far across ${i} prior pages. You want to continue looking?`);
                        if(true === keepLooking){
                           maxVisitedPages = maxVisitedPages * 2;
                        } else
                            break;
                    }

                    pageId -= PAGE_SIZE;

                }


                let midLinesBuffer = [];
                //Filter lines matching the keyword using the pages currently on screen
                for(let i = firstPageId; i <= lastPageId; i++){
                    const list = src.document.body.querySelectorAll(`.page${i}`);
                    const matches = _matchingLines(list);
                    if(matches && matches.length > 0){
                        midLinesBuffer = midLinesBuffer.concat(matches);
                    }
                }

                //Grab Pages after the current last line
                //These means pages not loaded into the view that are in the iframe after the last line currently shown in the div

                maxVisitedPages = MAX_VISITED_PAGES;

                pageId = lastPageId + 1;
                let nextLinesBuffer = [];

                for(let i=PAGE_SIZE; i <= maxVisitedPages; i += PAGE_SIZE) {
                    let lines = _fetchNextLinesOnly(pageId);
                    if(lines.buffer && lines.buffer.length > 0) {
                        nextLinesBuffer = nextLinesBuffer.concat(lines.buffer);
                    }
                    if(lines.hasMorePages === false){
                        break;
                    }

                    //This is an edge case on which we show a dialog  that allows breaking long search that hasn't returned any matches.
                    if( i === maxVisitedPages && lines.buffer.length === 0){
                        const keepLooking = confirm(`No matches have been found so far across ${i} of the most recent pages. You want to continue looking?`);
                        if(true === keepLooking){
                            maxVisitedPages = maxVisitedPages * 2;
                        } else
                            break;
                    }

                    pageId += PAGE_SIZE;
                }

                let buffer = [].concat(priorLinesBuffer, midLinesBuffer, nextLinesBuffer);

                dest.replaceChildren();

                buffer.forEach(value => {
                    dest.insertAdjacentHTML('beforeend',value);
                });

                buffer.length = 0;

                dest.innerHTML = _applyHighlight(dest.innerHTML);

                //Tell the Manager we're only showing lines matching the search criteria
                matchLinesOnlyView = true;

                let t2 = Date.now();

                console.log( "duration in ms is ::: " +  (t2 - t1));
            }
        }

        /**
         * The method is meant to load pages not loaded yet and apply the lines filtering
         * When filtering and showing only matching lines this method should be called to load the matching line
         * @param startFromPageId
         * @returns {{hasMorePages: boolean, buffer: *[]}}
         * @private
         */
        function _fetchPriorLinesOnly(startFromPageId){

            const lines = {
                buffer:[],
                hasMorePages: true
            };

            let buffer = [];
            const stopAtPageId = (startFromPageId - PAGE_SIZE);
            for(let i = startFromPageId; i >= stopAtPageId; i--){
                const list = src.document.body.querySelectorAll(`.page${i}`);
                if(!list || list.length === 0){
                    lines.hasMorePages = false;
                    break;
                }
                const matches = _matchingLines(list);
                if(matches && matches.length > 0){
                    buffer = [].concat(matches, buffer);
                }
            }
            lines.buffer.concat(buffer);
            return lines;
        }

        /**
         * The method is meant to load pages not loaded yet and apply the lines filtering
         * When filtering and showing only matching lines this method should be called to load the matching line
         * @param startFromPageId
         * @returns {{hasMorePages: boolean, buffer: *[]}}
         * @private
         */
        function _fetchNextLinesOnly(startFromPageId){

            const lines = {
                buffer:[],
                hasMorePages: true
            };

            const stopAtPageId = (startFromPageId + PAGE_SIZE);
            for(let i = startFromPageId; i <= stopAtPageId; i++){
                const list = src.document.body.querySelectorAll(`.page${i}`);
                if(!list || list.length === 0){
                    lines.hasMorePages = false;
                    break;
                }
                const matches = _matchingLines(list);
                if(matches && matches.length > 0){
                    lines.buffer = lines.buffer.concat(matches);
                }
            }
            return lines;
        }

        /**
         * This takes an HTMLNodeList and filters out all nodes where keyword does not have a match
         * Returns a list with brand new HTMLNodes of type `p`
         * @param nodes
         * @returns {*[Node]}
         * @private
         */
        function _matchingLines(nodes){
            const regEx = new RegExp(`(?!(class|style)+=\")(?!margin:0)${keyword}`, 'i');
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

        /**
         * Once a search criteria has been cleared we need to recreate content and start somewhere
         * That's what this method does. it shows the last three existing pages on the iframe
         * @private
         */
        function _loadDefaultView() {

            dest.replaceChildren();
            //this should give me the last paragraph
            const last = src.document.body.lastElementChild.previousSibling;
            if (!last) {
                console.log("I can not recreate view at the moment.");
                return
            }

            pagesOnScreen = 0;
            const lastPageId = last.dataset.page;
            const stopAtPage = lastPageId - ON_SCREEN_PAGES;
            for (let pageId = lastPageId; pageId >= stopAtPage; pageId--) {
                const list = src.document.body.querySelectorAll(`.page${pageId}`);
                if (list && list.length > 0) {
                    for (let i = list.length - 1; i >= 0; i--) {
                        const elem = list[i];
                        dest.insertAdjacentHTML('afterbegin', elem.outerHTML);
                    }
                    pagesOnScreen++;
                }
            }
            matchLinesOnlyView = false;
            _scrollDownToBottom();
        }

        // Return our public API
        return ({
            setFollowing : (value) => {
                following = value;
            },

            filterByKeyword: (value, showMatchingLinesOnly) => {
                if (updatingView) {
                    return;
                }
                updatingView = true;
                try {
                    keyword = value;
                    if (showMatchingLinesOnly && filterMatchFound) {
                        _matchingLinesOnlyView();
                    } else {
                        if (true === matchLinesOnlyView && null == keyword) {
                            _loadDefaultView();
                        }
                        _applyFilter();
                    }
                } finally {
                    updatingView = false;
                }
            },

            fetchPriorPage: () => {

                if (updatingView) {
                    return;
                }
                updatingView = true;
                try {
                    _fetchPriorPage();
                    _dropNewestPages();
                } finally {
                    updatingView = false;
                }
            },

            fetchNextPage: () => {

                if (updatingView) {
                    return;
                }
                updatingView = true;
                try {
                    _fetchNextPage();
                    _dropOldestPages();
                } finally {
                    updatingView = false;
                }
            },
            updateView: (data) => {
                if (!following) {
                    return;
                }
                if (updatingView) {
                    return;
                }
                updatingView = true;
                try {
                    _updateView(data);
                    _dropOldestPages();
                    _scrollDownToBottom();
                } finally {
                    updatingView = false;
                }
            }

        });
    }

    let logViewManager = null;

    /**
     * Here we handle
     * @param e scroll in general, for fetching prior and next pages
     */
    function scrollHandler(e){

        const div = e.target;
        const scrollPercentage = computeScrollPercentage(div.scrollHeight, div.scrollTop);

        //console.log(" scroll % :: " + scrollPercentage);

        if(scrollPercentage === 0) {
            //We're at the top of the container.
            logViewManager.fetchPriorPage();
            return;
        }

        if( scrollPercentage === 95 ){
            //we're at the bottom of the scroll
            logViewManager.fetchNextPage();
        }
    }

    const debounce = (callback, time = 300, interval) => (...args) => {
        clearTimeout(interval, interval = setTimeout(() => callback(...args), time));
    };

    const ignoredKeys = ["ArrowLeft", "ArrowUp", "ArrowDown", "ArrowRight"];

    function keyUpHandler(e){

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

    function attachLogIframeEvents(sseSource) {

        const followCheck = document.getElementById('scrollMe');
        const keywordInput = document.querySelector('#keywordLogFilterInput');
        const dataLogSourceElem = document.getElementById('tailingFrame');
        const logView = document.querySelector('.logViewerPrinted');
        const iDoc = dataLogSourceElem.contentWindow || dataLogSourceElem.contentDocument;

        iDoc.document.body.innerHTML = '';

        logViewManager = LogViewManager({src:iDoc,dest:logView});

        followCheck.addEventListener("change",(e)=>{
            logViewManager.setFollowing(e.currentTarget.checked);
        });

        sseSource.addEventListener('success', function(e) {

            // Assuming we receive JSON-encoded data payloads:
            const data = JSON.parse(e.data);
            iDoc.document.body.insertAdjacentHTML('beforeend', data.lines);

            logViewManager.updateView(data);
        });

        sseSource.addEventListener('keepAlive', function(e) {

            // Assuming we receive JSON-encoded data payloads:
            const data = JSON.parse(e.data);
            console.log("keepAlive :: " + data.keepAlive);
        });

        sseSource.addEventListener('failure', function(e) {
            // process error
            console.error(e);
        });

        logView.addEventListener("scroll", scrollHandler);

        keywordInput.addEventListener("keyup", debounce(keyUpHandler, 300));

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
