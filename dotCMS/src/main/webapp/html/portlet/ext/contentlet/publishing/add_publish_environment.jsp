<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
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

	dojo.ready( function(){	        
		if(dojo.isIE){
	    	setTimeout(function(){
	        	var randomParam = Math.floor((Math.random()*10000)+1);
	            var myRoleReadStoreURL = myRoleReadStore.url;
	            var dummyVar = new Array();
	            myRoleReadStore.url = myRoleReadStoreURL+"?randomParam="+randomParam;
	            myRoleReadStore.fetch({onComplete: dummyVar});
	        },100);
	    }
	});
	
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
            maxHeight:400,
			width:200,
            pageSize:30,
            autoComplete:false,
            searchDelay:300,
            required:false,
            onClick:function(){
            	dijit.byId("whoCanUseSelect").set("displayedValue","");
            	dijit.byId("whoCanUseSelect").loadDropDown();
            },
            value : 0
        },
        "actionWhoCanUseSelect");

		permissionSelect.watch('displayedValue', function(property, oldValue, newValue) {
			if (newValue && dijit.byId("whoCanUseSelect").isValid()) {
				addSelectedToWhoCanUse();
			}
		});

		whoCanUse = new Array()

		<% if(currentEnvironment!=null) {

			Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(currentEnvironment, PermissionAPI.PERMISSION_READ);%>
			<%for(Role tmpRole :  roles){%>
				addToWhoCanUse("<%=tmpRole.getId()%>",
						"<%=(tmpRole.getName().toLowerCase().contains("anonymous")) ? LanguageUtil.get(pageContext, "current-user") + " (" + LanguageUtil.get(pageContext, "Everyone") + ")" : tmpRole.getName()+ ((tmpRole.isSystem()) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : "")%>");
			<% }%>

			refreshWhoCanUse();

		<% }%>


	});

</script>

<div dojoType="dijit.form.Form"  name="formSaveEnvironment"  id="formSaveEnvironment" onsubmit="return false;">
	<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(currentEnvironment.getId())) %>">
	<div class="form-horizontal">
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "publisher_Environment_Name") %>:</dt>
			<dd>
				<input type="text" dojoType="dijit.form.ValidationTextBox"
					name="environmentName"
					id="environmentName"
					style="width:200px;"
					value="<%=UtilMethods.webifyString(String.valueOf(currentEnvironment.getName())) %>"
					promptMessage="<%= LanguageUtil.get(pageContext, "publisher_Environment_name_required") %>"
					/>
			</dd>
		</dl>
		<dl>
			<dt><%= LanguageUtil.get(pageContext, "publisher_Environment_Push_Mode") %>:</dt>
			<dd>
				<div class="checkbox">
					<input dojoType="dijit.form.RadioButton" type="radio" name="pushType" value="pushToOne" checked="<%=!currentEnvironment.getPushToAll()%>" id="pushToOne" />
					<label for="pushToOne"><%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_One") %></label>
				</div>
				<div class="checkbox">
					<input dojoType="dijit.form.RadioButton" type="radio" name="pushType" value="pushToAll"  checked="<%=currentEnvironment.getPushToAll()%>"  id="pushToAll" />
					<label for="pushToAll"><%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_All") %></label>
				</div>
			</dd>
		</dl>
		<dl>
			<dt><%=LanguageUtil.get(pageContext, "publisher_Environment_Who_Can_Send_To_Env")%>:</dt>
			<dd>
				<input id="actionWhoCanUseSelect" />
				<div class="who-can-use">
					<table class="who-can-use__list" id="whoCanUseTbl" style="overflow-y: scroll;display: block;overflow: auto;"></table>
				</div>
				<input type="hidden" name="whoCanUse" id="whoCanUse">
			</dd>
		</dl>
	</div>

	<div class="buttonRow">
		<button dojoType="dijit.form.Button" type="submit" id="save" onclick="saveEnvironment()"><%= LanguageUtil.get(pageContext, "Save") %></button>
		<button dojoType="dijit.form.Button" onClick="backToEnvironmentList(false)" id="closeSave" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
	</div>

</div>
