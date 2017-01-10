<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@ include file="/html/portlet/ext/workflows/init.jsp"%>

<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.*"%>
<%@page import="com.dotmarketing.cms.factories.*"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.workflows.struts.*"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.beans.WebAsset"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils"%>
<%@page import="com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.beans.WebAsset"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.liferay.portal.NoSuchRoleException"%>
<%@page import="com.dotmarketing.business.Role"%>
<%request.setAttribute("requiredPortletAccess", "EXT_21"); %>
<%@ include file="/html/common/uservalidation.jsp"%>
<style type="text/css">
	@import "/html/portlet/ext/workflows/schemes/workflow.css";
	#filterTasksFrm dt{height:30px;}
	#filterTasksFrm dd{height:30px;}
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

	function editTask(id,langId){
		var url = "<portlet:actionURL windowState="maximized"><portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" /><portlet:param name="cmd" value="view" /><portlet:param name="taskId" value="REPLACEME" /><portlet:param name="language" value="LANGUAGE" /></portlet:actionURL>";
		url = url.replace("REPLACEME", id);
		url = url.replace("LANGUAGE", langId);
		window.location=url;
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


		actionStore.fetch({query: {id:actionId}, onComplete:function(item){
			if(item[0].assignable =="true" || item[0].commentable == "true"){
				var dia = dijit.byId("contentletWfDialog");
    			if(dia){
    				dia.destroyRecursive();

    			}
    			dia = new dijit.Dialog({
    				id			:	"contentletWfDialog",
    				title		: 	"<%=LanguageUtil.get(pageContext, "Workflow-Actions")%>",
    				style		:	"width:520px;height:400px;"
    				});


  				var myCp = dijit.byId("contentletWfCP");
    			if(myCp){
    				myCp.destroyRecursive();

    			}
    			myCp = new dojox.layout.ContentPane({
    				id 			: "contentletWfCP",
    				style		:	"width:500px;height:400px;margin:auto;"
    			}).placeAt("contentletWfDialog");

    			dia.show();
    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + actionId);
    			return;
			}
			else{
				contentAdmin.saveAssign();
			}
		}});
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

<style>
	 #container {
	   display: table;
	   }
	 #table-row  {
	   display: table-row;
	   }
	 #cell-left, #cell-right{
	   display: table-cell;
	   }
</style>
<liferay:box top="/html/common/box_top.jsp"
bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "Filtered-Tasks") %>' />


<!-- START Button Row -->
	<div class="buttonBoxLeft"><h3><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.EXT_21")%></h3></div>

<!-- END Button Row -->

<!-- START Split Box -->
<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="height:100px;">

<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 370px; overflow-x: scroll; overflow-y: hidden; white-space: nowrap;" class="lineRight">
		<div style="margin-top:48px;">
			<div  id="filterTasksFrm">
				<input type="hidden" name="cmd" value="filterTasks">
				<input type="hidden" name="orderBy" id="orderBy" value="mod_date desc">
				<dl>
					<dt><%=LanguageUtil.get(pageContext, "Keywords")%>:</dt>
					<dd><input type="text" dojoType="dijit.form.TextBox" name="keywords" id="keywords" value="<%=UtilMethods.webifyString(searcher.getKeywords())%>" /></dd>
					<dt><%=LanguageUtil.get(pageContext, "Assigned-To")%>:</dt>
					<dd>
					<div id="container">
					  <div id="table-row">
						  <div id="cell-left">
							<input type="hidden" id="assignedTo" name="assignedTo" value="<%=myRole.getId() %>" />
						  </div>
						  <div id="cell-right">
							<%if(isAdministrator) { %>
							<input type="radio" dojoType="dijit.form.RadioButton" id="showAllLink" name="assignedto" onclick="showTasks4AllUsers()"><%=LanguageUtil.get(pageContext, "all") %> </input>
	                        <%} %>
	                      </div>
					  </div>
					  <div id="table-row">
					  	 <div id="cell-left"> </div>
					     <div id="cell-right">
                       		 <input type="radio" dojoType="dijit.form.RadioButton" id="showme" name="assignedto" checked="true" onclick="assignedToMe()"><%=LanguageUtil.get(pageContext, "me") %></input>
						</div>
					 </div>
					</div>
					</dd>
					<dt><%=LanguageUtil.get(pageContext, "Older_than_(days)") %></dt>
					<dd>
					   <input type="text" id="daysold" name="daysold"/>
					</dd>
					<dt><%=LanguageUtil.get(pageContext, "Scheme")%>:</dt>
					<dd>
						<select name="schemeId" id="schemeId" dojoType="dijit.form.FilteringSelect" value="<%=UtilMethods.webifyString(searcher.getSchemeId())%>" onChange="updateSteps();doFilter();">
							<option value=""></option>
							<%for(WorkflowScheme scheme : schemes) {%>
								<option value="<%=scheme.getId()%>"  <%=(scheme.getId().equals(searcher.getSchemeId())) ? "selected": ""%>><%=scheme.getName()%></option>
							<%} %>
						</select>


					</dd>
					<dt><%=LanguageUtil.get(pageContext, "Step")%>:</dt>
					<dd>

						<input type="hidden" id="stepId" name="stepId"  />



					</dd>

					<dt><%=LanguageUtil.get(pageContext, "Show")%>:</dt>
					<dd>
						<input dojoType="dijit.form.CheckBox" <%if(searcher.isOpen()){%> checked='checked' <%}%> type="checkbox" name="open" value="true" id="showOpen" onclick="doFilter()" /> <label for="showOpen"><%=LanguageUtil.get(pageContext, "open-tasks")%></label><br/>
						<input dojoType="dijit.form.CheckBox" <%if(searcher.isClosed()){%> checked='checked' <%}%> type="checkbox" name="closed" value="true" id="showClosed"  onclick="doFilter()"  /> <label for="showClosed"><%=LanguageUtil.get(pageContext, "resolved-tasks")%></label><br/>
					</dd>
				</dl>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" iconClass="searchIcon" name="filterButton" type="submit" onclick="doFilter()"> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %></button>
					<button dojoType="dijit.form.Button" name="resetButton"  iconClass="resetIcon" onclick="resetFilters()"><%=LanguageUtil.get(pageContext, "reset")%></button>
				</div>
			</div>
		</div>
	</div>
<!-- END Left Column -->


<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center" style="margin-top:37px;">
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
