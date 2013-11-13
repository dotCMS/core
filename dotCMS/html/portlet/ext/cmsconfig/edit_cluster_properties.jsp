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

		var html = "<table class='listingTable' style='width:90%'>";

		for(var key in properties){
			var value = properties[key];
			html += "<tr><td class='left_td' style='font-size:11px'>"+key+"</td><td><input style='width: 95%; font-size:11px' type='text' data-dojo-type='dijit/form/TextBox' id='"+key+"+' name='"+key+"+' value="+value+"></input></td></tr>"

		}

		html += "</table>"

		var nodeDiv = dojo.create("div",
				{ innerHTML: html
				});
		dojo.place(nodeDiv, dojo.byId("propertiesDiv"))

		var form = dojo.byId("propertiesForm");


		dojo.connect(form, "onSubmit", function(event){

		    // Stop the submit event since we want to control form submission.
		    dojo.stopEvent(event);

		    // The parameters to pass to xhrPost, the form, how to handle it, and the callbacks.
		    // Note that there isn't a url passed.  xhrPost will extract the url to call from the form's
		    //'action' attribute.  You could also leave off the action attribute and set the url of the xhrPost object
		    // either should work.

		    var xhrArgs = {
		      form: dojo.byId("propertiesForm"),
		      handleAs: "text",
		      load: function(data){
		        dojo.byId("response").innerHTML = "Form posted.";
		      },
		      error: function(error){
		        // We'll 404 in the demo, but that's okay.  We don't have a 'postIt' service on the
		        // docs server.
		        dojo.byId("response").innerHTML = "Form posted.";
		      }
		    }
		    // Call the asynchronous xhrPost
		    dojo.byId("response").innerHTML = "Form being sent..."
		    var deferred = dojo.xhrPost(xhrArgs);
		  });

	});



</script>

<style>
	.myTable {margin:20px;padding:10px;}
	.myTable tr td{padding:5px;vertical-align: top;}
	#addressRow {}
	#portRow {display:none;}

</style>

<div>
	<form action="/api/cluster/updateESConfigProperties/" id="propertiesForm" method="post">
		<div style="height: auto; width:500px" >
			<div style="height: 400px; overflow-y: scroll"  >
				<div id='propertiesDiv'></div>
			</div>

			<div align="center">
				<button style="padding-top: 10px; padding-bottom: 10px;" dojoType="dijit.form.Button"
					iconClass="saveIcon"
					type="submit"><%=LanguageUtil.get(pageContext, "Save")%></button>
				<button style="padding-top: 10px; padding-bottom: 10px;" dojoType="dijit.form.Button"
					onClick='bundles.modifyExtraPackages()' iconClass="cancelIcon"
					type="button"><%=LanguageUtil.get(pageContext, "Cancel")%></button>
			</div>
		</div>
	</form>
</div>
