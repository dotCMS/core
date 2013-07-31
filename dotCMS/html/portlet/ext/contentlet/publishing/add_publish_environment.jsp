<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Role"%>
<%
	String identifier = request.getParameter("id");
	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
	Environment currentEnvironment = eAPI.findEnvironmentById(identifier);
	if(currentEnvironment ==null){
		currentEnvironment = new Environment();
		currentEnvironment.setName("");
		currentEnvironment.setPushToAll(Boolean.FALSE);
	}

%>

<script type="text/javascript">
	require(["dojo/parser", "dijit/form/SimpleTextarea", "dotcms/dojo/data/RoleReadStore",  "dijit/form/FilteringSelect"]);

	var myRoleReadStore = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect", includeFake:true});


    function saveEnvironment() {

        var form = dijit.byId("formSaveEnvironment");

        dijit.byId("environmentName").setAttribute('required', true);


        if (form.validate()) {

            var xhrArgs = {
                url: "/DotAjaxDirector/com.dotcms.publisher.environment.ajax.EnvironmentAjaxAction/cmd/addEnvironment",
                form: dojo.byId("formSaveEnvironment"),
                handleAs: "text",
                load: function (data) {
                    if (data.indexOf("FAILURE") > -1) {

                        alert(data);
                    }
                    else {
                        backToEnvironmentList();
                    }
                },
                error: function (error) {
                    alert(error);
                }
            };

            var deferred = dojo.xhrPost(xhrArgs);
        }
    }


	function enableSave(){
		var environmentNameValue = dojo.byId("environmentName").getValue();

		if(environmentNameValue && environmentNameValue.length > 0){
			dijit.byId("save").setAttribute('disabled',false);
		}else{

			dijit.byId("save").setAttribute('disabled',true);
		}

	}


	dojo.ready( function(){

		var permissionSelect = new dijit.form.FilteringSelect({
            id: "whoCanUseSelect",
            name: "whoCanUseSelect",
            store: myRoleReadStore,

            pageSize:30,
            searchDelay:300,
            required:false,
            onClick:function(){
            	dijit.byId("whoCanUseSelect").set("displayedValue","");
            	dijit.byId("whoCanUseSelect").loadDropDown();
            },
            value : 0
        },
        "actionWhoCanUseSelect");

		dojo.connect(dijit.byId("whoCanUseSelect"), 'onChange', function(event){
            addSelectedToWhoCanUse();
     	 });

		whoCanUse = new Array()

		<% if(currentEnvironment!=null) {

			Set<Role> roles = APILocator.getPermissionAPI().getReadRoles(currentEnvironment);%>
			<%for(Role tmpRole :  roles){%>
				addToWhoCanUse("<%=tmpRole.getId()%>",
						"<%=(tmpRole.getName().toLowerCase().contains("anonymous")) ? LanguageUtil.get(pageContext, "current-user") + " (" + LanguageUtil.get(pageContext, "Everyone") + ")" : tmpRole.getName()+ ((tmpRole.isSystem()) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : "")%>");
			<% }%>

			refreshWhoCanUse();

		<% }%>


	});

</script>

<style>
	.myTable {margin:20px;padding:10px;}
	.myTable tr td{padding:5px;vertical-align: top;}
	#addressRow {}
	#portRow {display:none;}

</style>

<div style="margin:auto;">
	<div dojoType="dijit.form.Form"  name="formSaveEnvironment"  id="formSaveEnvironment" onsubmit="return false;">
		<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(currentEnvironment.getId())) %>">
		<table class="myTable" border=0 style="margin: auto" align="center">
			<tr>
				<td align="right" width="40%">
					<%= LanguageUtil.get(pageContext, "publisher_Environment_Name") %>:
				</td>
				<td>
					<input type="text" dojoType="dijit.form.ValidationTextBox"
							  name="environmentName"
							  id="environmentName"
							  style="width:200px;"
							  value="<%=UtilMethods.webifyString(String.valueOf(currentEnvironment.getName())) %>"
							  promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Environment_name_required") %>"
							  />
				</td>
			</tr>
			<tr>
				<td align="right"><%=LanguageUtil.get(pageContext, "publisher_Environment_Who_Can_Send_To_Env")%>:</td>
				<td>
					<input id="actionWhoCanUseSelect" />
					<div class="wfWhoCanUseDiv">
						<table class="listingTable" id="whoCanUseTbl">
						</table>
					</div>

					<input type="hidden" name="whoCanUse" id="whoCanUse" value="">

				</td>
			</tr>
			<tr>
				<td align="right" width="40%">
					<%= LanguageUtil.get(pageContext, "publisher_Environment_Push_Mode") %>:
				</td>
				<td >

					<input dojoType="dijit.form.RadioButton" type="radio" name="pushType" value="pushToOne" checked="<%=!currentEnvironment.getPushToAll()%>" id="pushToOne" />
					<label for="pushToOne"><%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_One") %></label>

					&nbsp;
					&nbsp;


					<input dojoType="dijit.form.RadioButton" type="radio" name="pushType" value="pushToAll"  checked="<%=currentEnvironment.getPushToAll()%>"  id="pushToAll" />
					<label for="pushToAll"><%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_All") %></label>
				</td>
			</tr>

		</table>

		<table align="center">
			<tr>
				<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
					<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon"  onclick="saveEnvironment()"><%= LanguageUtil.get(pageContext, "Save") %></button>
					&nbsp;
					<button dojoType="dijit.form.Button" onClick="backToEnvironmentList(false)" id="closeSave" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>

			    </td>
		    </tr>
	   </table>
	</div>
</div>