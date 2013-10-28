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

		// ES Cluster Nodes Status
		var properties;

		xhrArgs = {
			url : "/api/cluster/getESConfigProperties/",
			handleAs : "json",
			sync: true,
			load : function(data) {
				properties = data;
			},
			error : function(error) {
				targetNode.innerHTML = "An unexpected error occurred: " + error;
			}
		}

		deferred = dojo.xhrGet(xhrArgs);

		var html = "<table class='listingTable' style='background:white; width:auto'>"
			 + "<tr>"
		     + "<th style='font-size: 8pt;' width='30%'><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Status") %></th> "
		     + "<th style='font-size: 8pt;'><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Node_Status") %></th>"
		     + "</tr>";

		for(var key in properties){
			console.log(key)
			var value = properties[key];
			html += "<tr><td class='left_td'>"+key+"</td><td>"+value+"</td></tr>"

		}

		html += "</table>"

		var nodeDiv = dojo.create("div",
				{ innerHTML: html
				});
		dojo.place(nodeDiv, dojo.byId("propertiesDiv"))

	});

</script>

<style>
	.myTable {margin:20px;padding:10px;}
	.myTable tr td{padding:5px;vertical-align: top;}
	#addressRow {}
	#portRow {display:none;}

</style>

<div style="margin:auto;">
	<div id='propertiesDiv'></div>
</div>