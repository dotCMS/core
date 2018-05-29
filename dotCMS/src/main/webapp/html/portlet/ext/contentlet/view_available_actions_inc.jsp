
<script language="Javascript">

    function doShowAvailableActions() {

        dijit.byId('workflowActionsDia').show();

        var data = {};

        var xhrArgs = {
            url: "/html/portlet/ext/contentlet/view_available_actions.jsp",
            handleAs: "json",
            postData: data,
            load: function(data) {

            },
            error: function(error){
               showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-available-actions-error")%>", true);
            }
        }

        dojo.xhrPost(xhrArgs);

    }

</script>

<style>
    #workflowActionsDia {
        width: 600px;
        height: 570px;
    }

    #workflowActionsDia .listingTable {
        width: 99%;
    }

    .workflowActionsOption .dijitButton {
        width: 130px;
        text-align: center;
    }
</style>

<div dojoType="dijit.Dialog" id="workflowActionsDia"
     title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bulk Actions" )) %>'>
    <div style="overflow:auto;">
        <table class="sTypeTable"
               style="width:90%;  border-collapse: separate; border-spacing: 10px 15px;margin-bottom:10px;">
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

    </div>
</div>
