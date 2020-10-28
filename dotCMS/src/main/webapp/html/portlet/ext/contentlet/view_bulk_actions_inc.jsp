<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods" %>
<script language="Javascript">

    /**
     *
     *
     */
    function toggleFailDetails() {
        var node = dojo.byId('fail-details');
        if( node.style.display === "none" ){
            node.style.display = "block";
        }else{
            node.style.display = "none";
        }
        return false;
    }
    
    /**
     *
     */
    function doShowAvailableActions() {

        dojo.byId('bulkActionsContainer').innerHTML = '';

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

    }

    /**
     *
     */
    function renderSingleAction(action){
        var actionSingleTemplate
            = '<tr class="workflowActionsOption"> '
            + '   <td style="">&nbsp;&nbsp;'+action.workflowAction.name+'</td> '
            + '     <td style="width:140px;text-align: right"> '
            + '     <button dojoType="dijit.form.Button" class="dijitButton wfAction" '
            + '             data-acction-id="'+action.workflowAction.id+'" '
            + '             data-action-commentable="'+action.workflowAction.commentable+'" '
            + '             data-action-assignable="'+action.workflowAction.assignable+'" '
            + '             data-action-pushPublish="'+action.pushPublish+'"  '
            + '             data-action-condition="'+action.conditionPresent+'" >'+action.count+' content(s)</button>'
            + '   </td>'
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

        if(!steps || steps.length == 0){
          return '';
        }

        for(var i=0; i < steps.length; i++){
          actionRows += renderActions(steps[i]);
        }

        if(actionRows === ''){
           return '';
        }

        var schemeTemplate
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
            + '  <tr>'
            + '     <th colspan="2" class="sTypeHeader wfScheme" data-scheme-id="'+scheme.id+'" >'+scheme.name+'</th>'
            + '  </tr>'
            + actionRows
            + '</table>';
        return schemeTemplate;
    }

    function emptyActionsMarkup() {
        var emptyLabel = `<%=LanguageUtil.get(pageContext, "No-Available-Actions")%>`;
        var empty
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
            + '  <tr>'
            + '     <th colspan="2" class="sTypeHeader" ></th>'
            + '  </tr>'
            + '  <tr>'
            + '     <td> '+emptyLabel+' </td>'
            + '  </tr>'
            + '</table>';
        return empty;
    }

    function errorMarkup() {
        var errorMessage= `<%=LanguageUtil.get(pageContext, "Available-actions-error")%>`;
        var empty
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
            + '  <tr>'
            + '     <th colspan="2" class="sTypeHeader" ></th>'
            + '  </tr>'
            + '  <tr>'
            + '     <td> '+errorMessage+' </td>'
            + '  </tr>'
            + '</table>';
        return empty;
    }

    function noFailsMarkup() {
        var emptyLabel = `<%=LanguageUtil.get(pageContext, "No-Failed-Actions")%>`;
        var empty
            = '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
            + '  <tr>'
            + '     <th colspan="2" class="sTypeHeader" ></th>'
            + '  </tr>'
            + '  <tr>'
            + '     <td> '+emptyLabel+' </td>'
            + '  </tr>'
            + '</table>';
        return empty;
    }

    /**
     *
     * @param entity
     * @returns {string}
     */
    function actionsSummaryMarkup(entity){
        var markup = '';
        var schemes = entity.schemes;
        if(schemes.length == 0){
            markup = emptyActionsMarkup();
        } else {
            for(var i=0; i < schemes.length; i++){
                markup += renderSchemeInfo(schemes[i]);
            }
        }
        return markup;
    }

    /**
     *
     * @param entity
     * @returns {string}
     */
    function actionsExecutionSummarytMarkup(entity) {

        var skipsCount = entity.skippedCount;
        var successCount = entity.successCount;
        var failsCount = entity.fails.length;

        var resultsLabel = '<%=LanguageUtil.get(pageContext, "Results")%>';
        var sucessLabel = '<%=LanguageUtil.get(pageContext, "Successul")%>';
        var failsLabel = '<%=LanguageUtil.get(pageContext, "Fails")%>';
        var skipsLabel = '<%=LanguageUtil.get(pageContext, "Skips")%>';

        var exceptionLabel = '<%=LanguageUtil.get(pageContext, "Exception")%>';

        var summaryTableMarkup =
        '<div>' +
          '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
            +
            '<tr>' +
            ' <th colspan="2" class="sTypeHeader" >' + resultsLabel + '</th>' +
            ' <th></th>' +
            '</tr>' +
            '<tr>' +
              '<td> ' + sucessLabel + ':&nbsp;</td>' +
              '<td> ' + successCount + ' </td>' +
            '</tr>' +
            '<tr>' +
              '<td> ' + failsLabel + ':&nbsp;</td>' +
              '<td><a href="#" onclick="toggleFailDetails();" > ' + failsCount + ' </a></td>' +
            '</tr>' +
            '<tr>' +
              '<td> ' + skipsLabel + ':&nbsp;</td>' +
              '<td> ' + skipsCount + ' </td>' +
            '</tr>' +
          '</table>' +
        '</div>';

        var detailsMarkup = '';

        if (failsCount == 0) {
            detailsMarkup = '<div id="fail-details" style="display:none;">' +
                noFailsMarkup() +
            '</div>';
        } else {

            var markupDetailEntries = '';
            for (var i = 0; i < entity.fails.length; i++) {
                var fail = entity.fails[i];
                markupDetailEntries +=
                    '<tr>' +
                      '<td colspan="2"> ' + fail.inode + '  &nbsp;</td>' +
                      '<td> ' + fail.errorMessage + ' </td>' +
                    '</tr>';
            }

            detailsMarkup =
            '<div id="fail-details" style="display:none;">' +
                '<table class="sTypeTable" style="width:90%; border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">'
                +
                '<tr>' +
                ' <th colspan="2" class="sTypeHeader" > iNode </th>' +
                ' <th class="sTypeHeader" >' + exceptionLabel + '</th>' +
                '</tr>' +
                   markupDetailEntries +
            '</div>';
        }
        return summaryTableMarkup + detailsMarkup;
    }

    function showPopupIfRequired(buttonElement) {

        var commentable = dojo.attr(buttonElement, 'data-action-commentable');
        var assignable = dojo.attr(buttonElement, 'data-action-assignable');
        var pushPublish = dojo.attr(buttonElement, 'data-action-pushPublish');
        var condition = dojo.attr(buttonElement, 'data-action-condition');

        var popupRequired = (commentable == 'true' || assignable == 'true' || pushPublish == 'true' || condition == 'true' );
        if(!popupRequired){
           return false;
        }

        var actionId = dojo.attr(buttonElement, 'data-acction-id');

        var inode = null;
        var selectedInodes = getSelectedInodes();
        if (Array.isArray(selectedInodes) && selectedInodes.length > 0) {
            inode = selectedInodes[0];
        }

        let workflow = {
            actionId:actionId,
            inode:inode
        };
        var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Workflow-Action")%>');
        pushHandler.showWorkflowEnabledDialog(workflow, fireActionCallback, true);
        return true;
    }


    function fireActionCallback(actionId, formData){

        var pushPusblishFormData = formData.pushPublish;
        var assignComment = formData.assignComment;

        //Just a sub set of the fields can be sent
        //Any unexpected additional field on this structure will upset the rest endpoint.
        var pushPublish = {
            whereToSend:pushPusblishFormData.whereToSend,
            publishDate:pushPusblishFormData.publishDate,
            publishTime:pushPusblishFormData.publishTime,
            expireDate:pushPusblishFormData.expireDate,
            expireTime:pushPusblishFormData.expireTime,
            neverExpire:pushPusblishFormData.neverExpire,
            forcePush:pushPusblishFormData.forcePush
        };

        var data = {
            assignComment:assignComment,
            pushPublish:pushPublish
        };

        return fireAction(actionId, data);
    }

    function fireAction(actionId, popupData) {
        fireActionLoadingIndicator();
        var selectedInodes = getSelectedInodes();
        if(!selectedInodes){
            return;
        }

        var assignComment = null;

        if((typeof popupData != "undefined") && (typeof popupData.assignComment != "undefined")){
            assignComment = popupData.assignComment;
        }

        var pushPublish = null;
        if((typeof popupData != "undefined") && (typeof popupData.pushPublish != "undefined")){
            pushPublish = popupData.pushPublish;
        }

        var additionalParams = {
            assignComment:assignComment,
            pushPublish:pushPublish
        };

        var data ;
        if(Array.isArray(selectedInodes)){
            data = {
                "workflowActionId":actionId,
                "contentletIds":selectedInodes,
                "additionalParams":additionalParams
            };
        } else {
            data = {
                "workflowActionId":actionId,
                "query":selectedInodes,
                "additionalParams":additionalParams
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
                const entity = data ? data.entity : null; bulkWorkflowActionCallback(entity);
            },
            error: function(error){
                dojo.byId('bulkActionsContainer').innerHTML = `<%=LanguageUtil.get(pageContext, "Available-actions-error")%>`;
            }
        };

        dojo.xhrPut(xhrArgs);
        return true;
    }

    function fireActionLoadingIndicator(){
        dojo.byId('bulkActionsContainer').innerHTML = `<%=LanguageUtil.get(pageContext, "Applying")%>`;
    }

    function bulkWorkflowActionCallback(data) {
        if(data){
            var summary = actionsExecutionSummarytMarkup(data);
            dojo.byId('bulkActionsContainer').innerHTML = summary;
        } else {
            showDotCMSSystemMessage(`<%=LanguageUtil.get(pageContext, "Available-actions-error")%>`, true);
        }
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

        dojo.byId('bulkActionsContainer').innerHTML = `<%=LanguageUtil.get(pageContext, "dot.common.message.loading")%>`;
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
                           var buttonElement = e.target;
                           var popupShown = showPopupIfRequired(buttonElement);
                           if(popupShown){
                               return;
                           }
                           var actionId = dojo.attr(buttonElement, 'data-acction-id');
                           fireAction(actionId);
                       }
                    );
                    dijit.byId('workflowActionsDia').show();
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
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Refresh")) %>
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
