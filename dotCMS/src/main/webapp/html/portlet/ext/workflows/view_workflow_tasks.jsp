<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@ include file="/html/portlet/ext/workflows/init.jsp"%>

<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.Role"%>
<%request.setAttribute("requiredPortletAccess", "workflow"); %>
<%@ include file="/html/common/uservalidation.jsp"%>
<style type="text/css">
	@import "/html/portlet/ext/workflows/schemes/workflow.css";
</style>

<%

	WorkflowSearcher searcher = (WorkflowSearcher) session.getAttribute(com.dotmarketing.util.WebKeys.WORKFLOW_SEARCHER);
	if(searcher ==null){

		Map<String, Object>  newMap = new HashMap<String, Object>();


		newMap.putAll(request.getParameterMap());

		searcher = new WorkflowSearcher(newMap, user);

	}
	if(!searcher.isOpen() && ! searcher.isClosed()){
		searcher.setOpen(true);
	}

	Structure structure = null;


    boolean isAdministrator = APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())
                               || APILocator.getRoleAPI().doesUserHaveRole(user,RoleAPI.WORKFLOW_ADMIN_ROLE_KEY);
	List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());

    Role assignedTo  = APILocator.getRoleAPI().loadRoleById(searcher.getAssignedTo());
    Role myRole  = APILocator.getRoleAPI().getUserRole(user);

    if(assignedTo ==null){
    	assignedTo = myRole;
    }

    List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().findSchemes(false);


	Map myMap = new HashMap();

	myMap.put("struts_action", new String[] { "/ext/workflows/view_workflow_tasks" });

	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED
		.toString(), myMap);


%>


<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.util.WebKeys.WorkflowStatuses"%>
<%@ include file="/html/portlet/ext/workflows/workflows_js_inc.jsp" %>

<script type="text/javascript">
	dojo.require("dijit.form.FilteringSelect");
	dojo.require("dotcms.dojo.data.RoleReadStore");
	dojo.require("dotcms.dojo.data.RoleReadStore");
	dojo.require("dojox.layout.ContentPane");

	function doFilter () {

		var url="";

		if(!dijit.byId("showOpen").checked && !dijit.byId("showClosed").checked){
			dijit.byId("showOpen").setValue(true) ;
		}



		var container = dojo.byId("filterTasksFrm");
		var widgets = dojo.query("[widgetId]", container).map(dijit.byNode);
		dojo.forEach(widgets, function(inputElem){
			if("checkbox" == inputElem.type){
				url = url + "&" + inputElem.name +"=" +inputElem.checked ;
			}
			else{
				url = url + "&" + inputElem.name +"=" +inputElem.value ;
			}
		});

		<%if(isAdministrator) {%>
		     if(show4All) {
		    	 url = url + "&show4all=true";
		     }
		<%}%>

		refreshTaskList(url);


	}
	var lastUrlParams ;

	function refreshTaskList(urlParams){
		lastUrlParams = urlParams;
		var r = Math.floor(Math.random() * 1000000000);
		var url = "/html/portlet/ext/workflows/view_tasks_list.jsp?r=" + r + urlParams;




		var myCp = dijit.byId("workflowTaskListCp");


		if (myCp) {
			dojo.attr(myCp,"content","");
		}else{
			myCp = new dojox.layout.ContentPane({
				id : "workflowTaskListCp"
			}).placeAt("hangTaskListHere");
		}


		myCp.attr("href", url);

	}








	function doOrderBy (newOrder) {

			dojo.byId("orderBy").value= newOrder;


			var newURL = "";
			var x  =lastUrlParams.split("&");

            for (i = 0; i < x.length; i++) {
                if (x[i].indexOf("orderBy") < 0) {

                    if (x[i].length > 0) {
                        if (x[i].indexOf("&") == 0) {
                            newURL += x[i];
                        } else {
                            newURL += "&" + x[i];
                        }
                    }
                }
            }

			newURL+="&orderBy=" +newOrder;

			refreshTaskList(newURL);

	}


	var stepStore = new dojo.data.ItemFileReadStore({url:"/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax?cmd=listByScheme"});
	var emptyData = { "identifier" : "id", "label" : "name", "items": [{ name: '',id: '' }] };
	var emptyStore = new dojo.data.ItemFileReadStore({data:emptyData});
	var daysData= { "identifier" : "d", "label" : "days", "items":
		[{d:1},{d:2},{d:5},{d:10},{d:15},{d:20},{d:30},{d:40},{d:50},{d:60}]};
	var daysOldStore = new dojo.data.ItemFileReadStore({data:daysData});

	var myRoleReadStore = new dotcms.dojo.data.RoleReadStore({nodeId: "assignedTo", includeFake:false});

	dojo.ready(function(){

		if(dojo.isIE){
	    	setTimeout(function(){
	        	var randomParam = Math.floor((Math.random()*10000)+1);
	            var myRoleReadStoreURL = myRoleReadStore.url;
	            var dummyVar = new Array();
	            myRoleReadStore.url = myRoleReadStoreURL+"?randomParam="+randomParam;
	            myRoleReadStore.fetch({onComplete: dummyVar});
	        },100);
	    }


	<%if(isAdministrator){%>
        var assignedTo = new dijit.form.FilteringSelect({
            id: "assignedTo",
            name: "assignedTo",
            store: myRoleReadStore,
            searchDelay: 300,
            pageSize: 30,
            required: false,
            value: "<%=assignedTo.getId()%>",
            onClick:function(){
            	dijit.byId("assignedTo").set("displayedValue","");
            	dijit.byId("assignedTo").loadDropDown();
            },
            onChange:function(){
            	doFilter();
            }
        },
        "assignedTo");
        doFilter();
	<%}%>

		var stepId = new dijit.form.FilteringSelect({
		    id: "stepId",
		    name: "stepId",
		    store: emptyStore,
		    searchDelay:300,
		    pageSize:20,
		    required:false,
		    onChange:function(){
		    	doFilter();
		    }

		},"stepId");

		var olderThanCombo = new dijit.form.ComboBox({
	        id:"daysold",
	        name:"daysold",
	        store:daysOldStore,
	        required:false,
	        value:"",
	        searchAttr:"d"
	    },"daysold");


		doFilter();

	});

	var emptyData = {"identifier" : "id","label" : "name","items": [{ name: '', id: ''}]};




	function updateSteps(){
		var schemeId = dijit.byId("schemeId").value;
		var stepId = dijit.byId("stepId");
		stepId.store= emptyStore;
		dojo.byId("stepId").value ="";
		if(schemeId){
			var myUrl = "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax?cmd=listByScheme&schemeId=" + schemeId;
			dijit.byId("stepId").set('store',new dojo.data.ItemFileReadStore({url:myUrl}));
		}

	}

	function assignedToMe(){
		<%if(isAdministrator){%>
             disable4AllUsers();
        <%}%>

		var assignedTo = dijit.byId("assignedTo");
		assignedTo.displayedValue="";

        var currentAssignedTo = assignedTo.getValue();
        assignedTo.setValue("<%=myRole.getId()%>");

        //If the values are equals the onchange event of the FilteringSelect won't be fired, and we need it.
        if (currentAssignedTo == "<%=myRole.getId()%>") {
            doFilter();
        }
	}
	<%if(isAdministrator){%>
	    var show4All=false;
		function showTasks4AllUsers() {
			if(show4All) {
				disable4AllUsers();
			}
			else {
				var assignedTo = dijit.byId("assignedTo");
		        assignedTo.displayedValue="";

		        assignedTo.attr("disabled","true");
		        dojo.style(dojo.byId("showAllLink"),"fontWeight","bold");
		        show4All=true;
			}
			doFilter();
		}
		function disable4AllUsers() {
			show4All=false;
			var assignedTo = dijit.byId("assignedTo");
			assignedTo.attr("disabled",false);
			dojo.style(dojo.byId("showAllLink"),"fontWeight","normal");
		}
	<%}%>
	function resetFilters(){

		dijit.byId("showme").set("checked", "true");

		dijit.byId("daysold").setValue("");

		var stepId = dijit.byId("stepId");
		stepId.setValue("");

		<%if(isAdministrator){%>
	        disable4AllUsers();
	   <%}%>

		var assignedTo = dijit.byId("assignedTo");

		if(assignedTo) {
			assignedTo.displayedValue="";
			assignedTo.setValue("<%=myRole.getId()%>");
		}

		dijit.byId("keywords").setValue("");

		dijit.byId("showOpen").setValue(true);
		dijit.byId("showClosed").setValue(false);

		var schemeId = dijit.byId("schemeId");
		schemeId.setValue("");

		doFilter();
	}

	function editTask(event, inode, langId){
        event.preventDefault();
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "edit-task",
            data: {
                inode: inode,
                lang: langId
            }
        });
        document.dispatchEvent(customEvent);
	}

/*	dojo.ready(function(){

	updateSteps();
	})
*/



	function checkAll(){
		var x = dijit.byId("checkAllCkBx").checked;

		dojo.query(".taskCheckBox").forEach(function(node){
			dijit.byNode(node).setValue(x);
		})
	}



	function excuteWorkflowAction(){
		var actionId = dijit.byId("performAction").getValue();

		if(! actionId || actionId.length <1){
			showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Please-select-an-action")%>", true);
			return;
		}

		dojo.byId("wfActionId").value =actionId;

		var hasChecks = false;
		var cons ="";
		dojo.query(".taskCheckBox").forEach(function(node){
			var check = dijit.byNode(node);
			if(check.getValue()){
				cons = cons +  check.id + ",";
				hasChecks=true;
			}
		});


		if(!hasChecks){
			showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Please-select-a-task")%>", true);
			return;

		}

		dojo.byId("wfCons").value=cons;

		actionStore.fetch({
			query: {id: actionId}, onComplete: function (item) {
				if (item[0].assignable == "true" || item[0].commentable == "true"
						|| item[0].pushPublish == "true") {
					let workflow = {
						actionId: actionId,
						inode: ''
					};
					var pushHandler = new dotcms.dojo.push.PushHandler(
							'<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');
					debugger;
					pushHandler.showWorkflowEnabledDialog(workflow, fireActionCallback, true);
				} else {
					contentAdmin.saveAssign();
				}
			}
		});
	}

	function getTaskSelectedInodes() {
		var ids = [];
		dojo.query(".taskCheckBox").forEach(function (node) {
			let check = dijit.byNode(node);
			if (check.getValue()) {
				let inode = check.attr('data-action-inode');
				ids.push(inode);
			}
		});
		return ids;
	}

	function fireActionCallback(actionId, formData){
		debugger
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

		let data = {
			assignComment:assignComment,
			pushPublish:pushPublish
		};
		let fireResult = fireAction(actionId, data);
		if(fireResult){
		  doFilter();
		}
		return fireResult;
	}

	function fireAction(actionId, popupData) {
		debugger
		let selectedInodes = getTaskSelectedInodes();
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

		var data = {
			"workflowActionId":actionId,
			"contentletIds":selectedInodes,
			"additionalParams":additionalParams
		};

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
				if(data && data.entity){
					console.log(data.entity);
				} else {
					showDotCMSSystemMessage(`<%=LanguageUtil.get(pageContext, "Available-actions-error")%>`, true);
				}
			},
			error: function(error){
				this.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-excute-workflows")%>");
			}
		};

		dojo.xhrPut(xhrArgs);
		return true;

	}

	var contentAdmin = {
		saveAssign:function(){

			if(dojo.byId("taskAssignmentAux")){
				dojo.byId("wfActionAssign").value=dijit.byId('taskAssignmentAux').getValue();
			}
			if(dojo.byId("taskCommentsAux")){
				dojo.byId("wfActionComments").value=dijit.byId('taskCommentsAux').getValue();
			}
			if(dojo.byId("contentletWfDialog")){
				dijit.byId("contentletWfDialog").hide();
			}

			dojo.xhrPost({
				form : "executeTasksFrm",
				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {

							showDotCMSSystemMessage(dataOrError, true);
						} else {
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Saved")%>");

						}
					} else {
						this.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-excute-workflows")%>");

					}
					doFilter();
				}
			});
		}
	};

</script>


<liferay:box top="/html/common/box_top.jsp"
bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "Filtered-Tasks") %>' />


<!-- START Button Row -->
<!-- <%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.workflow")%> -->

<!-- END Button Row -->

<!-- START Split Box -->
<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer">

<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 200px;" class="portlet-sidebar-wrapper" >
		<div class="portlet-sidebar">
			
			<!-- START Filters -->
			<div  id="filterTasksFrm">
				
				<input type="hidden" name="cmd" value="filterTasks">
				<input type="hidden" name="orderBy" id="orderBy" value="mod_date desc">
				
				<dl class="vertical">
					
					<dt><label><%=LanguageUtil.get(pageContext, "Keywords")%>:</label></dt>
					<dd><input type="text" dojoType="dijit.form.TextBox" name="keywords" id="keywords" value="<%=UtilMethods.webifyString(searcher.getKeywords())%>" /></dd>
					<div class="clear"></div>
					
					<dt><label><%=LanguageUtil.get(pageContext, "Assigned-To")%>:</label></dt>
					<dd>
						<input type="hidden" id="assignedTo" name="assignedTo" value="<%=myRole.getId() %>" />
						<div style="padding: 5px 3px;">
							<%if(isAdministrator) { %>
								<span style="display:inline-block;margin-right:8px;"><input type="radio" dojoType="dijit.form.RadioButton" id="showAllLink" name="assignedto" onclick="showTasks4AllUsers()"> <%=LanguageUtil.get(pageContext, "all") %></input></span>
							<%} %>
							<input type="radio" dojoType="dijit.form.RadioButton" id="showme" name="assignedto" checked="true" onclick="assignedToMe()"> <%=LanguageUtil.get(pageContext, "me") %></input>
						</div>
					</dd>
					<div class="clear"></div>
					
					
					
					
					<dt><label><%=LanguageUtil.get(pageContext, "Older_than_(days)") %></label></dt>
					<dd><input type="text" id="daysold" name="daysold"/></dd>
					<div class="clear"></div>
					
					<dt><label><%=LanguageUtil.get(pageContext, "Scheme")%>:</label></dt>
					<dd>
						<select name="schemeId" id="schemeId" dojoType="dijit.form.FilteringSelect" value="<%=UtilMethods.webifyString(searcher.getSchemeId())%>" onChange="updateSteps();doFilter();">
							<option value=""></option>
							<%for(WorkflowScheme scheme : schemes) {%>
								<option value="<%=scheme.getId()%>"  <%=(scheme.getId().equals(searcher.getSchemeId())) ? "selected": ""%>><%=scheme.getName()%></option>
							<%} %>
						</select>
					</dd>
					<div class="clear"></div>
					
					<dt><label><%=LanguageUtil.get(pageContext, "Step")%>:</label></dt>
					<dd><input type="hidden" id="stepId" name="stepId"  /></dd>
					<div class="clear"></div>

					<dt><label><%=LanguageUtil.get(pageContext, "Show")%>:</label></dt>
					<dd>
						<input dojoType="dijit.form.CheckBox" <%if(searcher.isOpen()){%> checked='checked' <%}%> type="checkbox" name="open" value="true" id="showOpen" onclick="doFilter()" /> <label for="showOpen"><%=LanguageUtil.get(pageContext, "open-tasks")%></label><br/>
						<div class="portlet-sidebar__input-spacer"></div>
						<input dojoType="dijit.form.CheckBox" <%if(searcher.isClosed()){%> checked='checked' <%}%> type="checkbox" name="closed" value="true" id="showClosed"  onclick="doFilter()"  /> <label for="showClosed"><%=LanguageUtil.get(pageContext, "resolved-tasks")%></label><br/>
					</dd>
					<div class="clear"></div>
				</dl>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" type="submit" onclick="doFilter()"> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %></button>
					<button dojoType="dijit.form.Button" name="resetButton" iconClass="resetIcon" class="dijitButtonFlat" onclick="resetFilters()"><%=LanguageUtil.get(pageContext, "reset")%></button>
				</div>
			</div>
		</div>
	</div>
<!-- END Left Column -->


<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		<div id="hangTaskListHere">


		</div>
	</div>
<!-- END Right Column -->

</div>
<!-- END Split Box -->

</liferay:box>


<script type="text/javascript">
dojo.ready(resizeBrowser);
</script>

<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("keywords"));
		  t.stop();
		}
		t.start();
	});
</script>
