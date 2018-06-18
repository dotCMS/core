<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<script language="Javascript">

    /**
     *
     */
    function doShowAvailableActions() {

        dojo.byId('bulkActionsContainer').innerHTML = '';

            <% if(enterprise){ %>
               // if there are selected items and we're running an enterprise verision the we can bother retriving the available actions
                var selectedInodes = getSelectedInodes();
                if (!selectedInodes) {
                    return;
                }

                var data;
                if (Array.isArray(selectedInodes) && selectedInodes.length > 0) {
                    data = {"contentletIds": selectedInodes};
                } else {
                    data = {"query": selectedInodes}; //No. it's not a bug. This variable sometimes holds a query.
                }
                getAvailableBulkActions(data);
               //adjust dialog size to show the actions section
               dojo.style(dijit.byId('workflowActionsDia').domNode, {height:'575px'});
        <%}else{%>
                //adjust dialog size to hide the actions section
                dojo.style(dijit.byId('workflowActionsDia').domNode, {height:'141px'});
            <%}%>
        dijit.byId('workflowActionsDia').show();
    }

    /**
     *
     */
    function renderSingleAction(action){
        var actionSingleTemplate
            = '<tr class="workflowActionsOption">\n'
            + '   <td style="">&nbsp;&nbsp;'+action.workflowAction.name+'</td>\n'
            + '     <td style="width:140px;text-align: right">\n'
            + '     <button dojoType="dijit.form.Button" class="dijitButton wfAction" data-acction-id="'+action.workflowAction.id+'" >'+action.count+' content(s)</button>\n'
            + '   </td>\n'
            + '</tr>';
        return actionSingleTemplate;
    }

    /**
     *
     * @param stepWrapper
     * @returns {string}
     */
    function renderActions(stepWrapper){
        var step = stepWrapper.step;
        var actions = stepWrapper.actions;
        var actionsMarkup='';
            for(var i=0; i < actions.length; i++){
                actionsMarkup += renderSingleAction(actions[i]);
            }
        return actionsMarkup;
    }

    /**
     *
     * @param schemeWrapper
     * @returns {string}
     */
    function renderSchemeInfo(schemeWrapper){
        var actionRows = '';
        var scheme = schemeWrapper.scheme;
        var steps = schemeWrapper.steps;
        for(var i=0; i < steps.length; i++){
          actionRows += renderActions(steps[i]);
        }

        var schemeTemplate
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">\n'
            + '  <tr>\n'
            + '     <th colspan="2" class="sTypeHeader wfScheme" data-scheme-id="'+scheme.id+'" >'+scheme.name+'</th>\n'
            + '  </tr>\n'
            + actionRows
            + ' </table>';
        return schemeTemplate;
    }

    function emptyRecordsMarkup() {
        var emptyLabel = '<%=LanguageUtil.get(pageContext, "No-Available-Actions")%>';
        var empty
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">\n'
            + '  <tr>\n'
            + '     <th colspan="2" class="sTypeHeader" ></th>\n'
            + '  </tr>\n'
            + '  <tr>\n'
            + '     <td> '+emptyLabel+' </td>\n'
            + '  </tr>\n'
            + '</table>';
        return empty;
    }

    function errorMarkup() {
        var errorMessage= '<%=LanguageUtil.get(pageContext, "Available-actions-error")%>';
        var empty
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">\n'
            + '  <tr>\n'
            + '     <th colspan="2" class="sTypeHeader" ></th>\n'
            + '  </tr>\n'
            + '  <tr>\n'
            + '     <td> '+errorMessage+' </td>\n'
            + '  </tr>\n'
            + '</table>';
        return empty;
    }

    /**
     *
     * @param entity
     * @returns {string}
     */
    function actionsSummaryMarkup(entity){
        var markUp = '';
        var schemes = entity.schemes;
        if(schemes.length == 0){
            markUp = emptyRecordsMarkup();
        } else {
            for(var i=0; i < schemes.length; i++){
                markUp += renderSchemeInfo(schemes[i]);
            }
        }
        return markUp;
    }

    /**
     *
     * @param entity
     * @returns {string}
     */
    function actionsExecutionSummarytMarkup(entity){

        var failures = entity.failsCount;
        var skips = entity.skippedCount;
        var success = entity.successCount;

        var resultsLabel = '<%=LanguageUtil.get(pageContext, "Results")%>';
        var sucessLabel = '<%=LanguageUtil.get(pageContext, "Successul")%>';
        var failsLabel = '<%=LanguageUtil.get(pageContext, "Fails")%>';
        var skipsLabel = '<%=LanguageUtil.get(pageContext, "Skips")%>';

        var markUp =
       '<div>' +
          '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">' +
                '<tr>' +
                  ' <th colspan="2" class="sTypeHeader" >'+resultsLabel+'</th>' +
                  ' <th> </th>' +
                '</tr>' +
            '<tr>' +
                '<td> '+sucessLabel+':&nbsp;</td>' +
                '<td> '+success+' </td>' +
            '</tr>' +
            '<tr>' +
                '<td> '+failsLabel+':&nbsp;</td>' +
                '<td> '+failures+' </td>' +
            '</tr>' +
            '<tr>' +
                '<td> '+skipsLabel+':&nbsp;</td>' +
                '<td> '+skips+' </td>' +
            '</tr>' +
          '</table>' +
       '</div>';
       return markUp;
    }

    function fireAction(e) {
        dojo.byId('bulkActionsContainer').innerHTML = '<%=LanguageUtil.get(pageContext, "Applying")%>';
        var buttonElement = e.toElement;
        var actionId = dojo.attr(buttonElement, 'data-acction-id');

        var selectedInodes = getSelectedInodes();
        if(!selectedInodes){
            return;
        }

        var data ;
        if(Array.isArray(selectedInodes)){
            data = {
                "workflowActionId":actionId,
                "contentletIds":selectedInodes
            };
        } else {
            data = {
                "workflowActionId":actionId,
                "query":selectedInodes
            };
        }

        var dataAsJson = dojo.toJson(data);
        var xhrArgs = {
            url: "/api/v1/workflow/contentlet/actions/bulk/fire",
            postData: dataAsJson,
            handleAs: "json",
            headers : {
                'Accept' : 'application/json',
                'Content-Type' : 'application/json;charset=utf-8',
            },
            load: function(data) {
                if(data){
                    var entity = (data.entity ? data.entity : data);
                    var summary = actionsExecutionSummarytMarkup(entity);
                    dojo.byId('bulkActionsContainer').innerHTML = summary;
                } else {
                    showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Available-actions-error")%>", true);
                }
            },
            error: function(error){
                dojo.byId('bulkActionsContainer').innerHTML = '<%=LanguageUtil.get(pageContext, "Available-actions-error")%>';
            }
        }

        dojo.xhrPut(xhrArgs);
        return true;
    }

    function getAvailableBulkActions(data){

       var closeHandle = dojo.connect(dijit.byId('workflowActionsDia'), "hide",
            function(){
                fakeAjaxCallback();
                if(closeHandle){
                    dojo.disconnect(closeHandle);
                }
            }
        );

        dojo.byId('bulkActionsContainer').innerHTML = 'Loading...';
        var dataAsJson = dojo.toJson(data);
        var xhrArgs = {
            url: "/api/v1/workflow/contentlet/actions/bulk",
            postData: dataAsJson,
            handleAs: "json",
            headers : {
                'Accept' : 'application/json',
                'Content-Type' : 'application/json;charset=utf-8',
            },
            load: function(data) {
                if(data && data.entity){
                    var markUp = actionsSummaryMarkup(data.entity);
                    dojo.byId('bulkActionsContainer').innerHTML = markUp;
                    dojo.query(".wfAction").onclick(
                       function(e){
                           fireAction(e);
                       }
                    );
                } else {
                    dojo.byId('bulkActionsContainer').innerHTML = errorMarkup();
                    console.error('No data was returned.');
                }
            },
            error: function(error){
                dojo.byId('bulkActionsContainer').innerHTML = errorMarkup();
                console.error(error);
            }
        }
        dojo.xhrPost(xhrArgs);
    }

    function addToBundleSelectedContentletsProxy(){
        addToBundleSelectedContentlets();
        dijit.byId('workflowActionsDia').hide();
        return true;
    }

    function pushPublishSelectedContentletsProxy(){
        pushPublishSelectedContentlets();
        dijit.byId('workflowActionsDia').hide();
        return true;
    }

    function unlockSelectedContentletsProxy(){
        unlockSelectedContentlets();
        dijit.byId('workflowActionsDia').hide();
        return true;
    }

    function reindexSelectedContentletsProxy() {
        reindexSelectedContentlets();
        dijit.byId('workflowActionsDia').hide();
        return true;
    }

</script>

<style>
    #workflowActionsDia {
        width: 600px;
        height: 575px;
    }

    #workflowActionsDia .listingTable {
        width: 99%;
    }

    .workflowActionsOption .dijitButton {
        width: 130px;
        text-align: center;
    }

    #bulkActionsContainer {
        overflow:auto;
        height: 470px;
    }
</style>

<div dojoType="dijit.Dialog" id="workflowActionsDia"
     title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Available-actions" )) %>'>
    <div>
        <table class="sTypeTable"
               style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">

            <tr class="workflowActionsOption">
                <%if(enterprise){%>
                <td style="width:140px;text-align: right">
                    <button id="addToBundleButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: addToBundleSelectedContentletsProxy">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>
                    </button>
                </td>
                <% if ( sendingEndpoints ) { %>
                <td style="width:140px;text-align: right">
                  <button id="pushPublishButton"  dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: pushPublishSelectedContentletsProxy">
                       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>
                  </button>
                </td>
                <% } %>
                <%}%>
                <td style="width:140px;text-align: right">
                    <button id="unlockButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: unlockSelectedContentletsProxy">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>
                    </button>
                </td>
                <%if(canReindexContentlets){%>
                <td style="width:140px;text-align: right">
                    <button id="reindexButton" dojoType="dijit.form.Button" class="dijitButton" data-dojo-props="onClick: reindexSelectedContentletsProxy">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reindex")) %>
                    </button>
                </td>
                <%}%>
            </tr>
        </table>
    </div>
    <div id="bulkActionsContainer">
        <%-- Begin of sample markup --- (This markup is here just for dev purposes.  It really gets generated on the fly with Javascript) --%>
        <table class="sTypeTable"
               style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">
            <tr>
                <th colspan="2" class="sTypeHeader">Document Management</th>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Publish</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">10 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Unpublish</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">8 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Tweet This!</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">17 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Unpublish</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">8 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Full Delete</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">17 content(s)</button>
                </td>
            </tr>
        </table>

        <table class="sTypeTable"
               style="width:90%;  border-collapse: separate; border-spacing: 10px 15px;">
            <tr class="sTypeHeader">
                <th colspan="2">System Workflow</th>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Publish</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">14 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Save</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">22 content(s)</button>
                </td>
            </tr>
            <tr class="workflowActionsOption">
                <td style="">&nbsp;&nbsp;Save as Draft</td>
                <td style="width:140px;text-align: right">
                    <button dojoType="dijit.form.Button" class="dijitButton">22 content(s)</button>
                </td>
            </tr>
        </table>

        <%-- End of sample markup --%>
    </div>
</div>
