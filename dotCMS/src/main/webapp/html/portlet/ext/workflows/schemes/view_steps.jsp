<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page
	import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page
	import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String schemeId  = request.getParameter("schemeId");
	WorkflowScheme scheme          = wapi.findScheme(schemeId);
	final List<WorkflowStep> steps = wapi.findSteps(scheme);

	pageContext.setAttribute("scheme",scheme);
%>

<script type="text/javascript">



	dragula([document.querySelectorAll('.wfStepInDrag'), document.getElementById('wfStepInDragContainer')], {
		  moves: function (el, source, handle, sibling) {

			  return handle.classList.contains('handle'); 
		  },
		  accepts: function (el, target, source, sibling) {
		    return target ==document.getElementById('wfStepInDragContainer');
		  },
		  direction: 'horizontal',             // Y axis is considered when determining where an element would be dropped
		  revertOnSpill: true,              // spilling will put the element back where it was dragged from, if this is true
		  ignoreInputTextSelection: true     // allows users to select input text, see details below
		}).on('drop', function (el) {

			let stepID = el.id.split("stepID").join("");
			let sibblings = el.parentNode.children;
			let index = Array.from(sibblings).indexOf(el);

			try {
				if (sibblings && ((sibblings.length - 1) - index === 0)) {
					this.cancel();
					return;
				}
			} catch (e) {
				console.error(e);
			}
			colorMeNot();
			stepAdmin.reorderStep(stepID, index);
		 });
	
	
		var arr = new Array(<%=steps.size()%>);
		<%for(WorkflowStep step : steps){ %>
			arr.push(document.getElementById("jsNode<%=step.getId()%>"));
		<%}%>
		
		
	  dragula(arr, {
		  moves: function (el, source, handle, sibling) {

			  return el.classList.contains('wf-action-wrapper'); 
		  },
		  accepts: function (el, target, source, sibling) {

		    return true;
		  },
		  revertOnSpill: true,    
		  copy: true,                       // elements are moved by default, not copied
		  copySortSource: true,             // elements in copy-source containers can be reordered
		  
		}).on('drop', function (ele) {
			colorMeNot();
			actionAdmin.copyOrReorderAction(ele);
		 });


	function colorMe(clazz) {
		var x = document.getElementsByClassName(clazz);
		for (i = 0; i < x.length; i++) {
			if(x[i].className.indexOf("makeMeBlue")==-1){
				x[i].className += ' makeMeBlue';
			}
		}
	}

	function colorMeNot() {
		var x = document.getElementsByClassName("makeMeBlue");
		
		for (i = 0; i < x.length; i++) {
			colorMeNotEle(x[i]);
		}
	}

	function colorMeNotEle(ele) {
		var cName = ele.className;
		
		while (cName.indexOf(" makeMeBlue") > -1) {
			cName = cName.replace(" makeMeBlue", "");
		}
		ele.className = cName;
	}

    dojo.ready(function(){
        var schemeId = '<%=scheme.getId()%>';
        var whoCanUseFilteringSelect = new dijit.form.FilteringSelect({
                id: "whoCanUseSelect",
                name: "whoCanUseSelect",
                store: myRoleReadStoreFilter,
                pageSize:30,
                searchDelay:300,
                style: "width: 80%",
                required:false,
                onClick:function(){
                    var select = dijit.byId("whoCanUseSelect");
                    select.set("displayedValue","");
                    select.loadDropDown();
                },onChange:function (item) {

                    var select = dijit.byId("whoCanUseSelect");
                    var roleId = select.getValue();
                    stepAdmin.filterSteps(schemeId, roleId);

                }
            },
            "filterByWhoCanUseSelect");
        dijit.byId("whoCanUseSelect").set("displayedValue","All");
	});

</script>




<div class="portlet-toolbar">
    <div class="portlet-toolbar__actions-primary">
	<div class="showPointer">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<div>

			
			<div class="showPointer" onClick="schemeAdmin.show()"  style="float: left;">
			 <h2 style="border-bottom:dotted 1px white;"><%=LanguageUtil.get(pageContext, "Workflow-Schemes")%></h2>
			 <p>&nbsp;</p>
			</div> 
			
			<div style="float: left;">
			&nbsp; &rarr; &nbsp;
			</div>
			<div onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');" style="float: left;">
			 <h2 style="border-bottom:dotted 1px gray;"><%=UtilMethods.webifyString(scheme.getName())%></h2>
			 <p><%=UtilMethods.webifyString(scheme.getDescription())%></p>
			</div>
            <div style="float: left;">&nbsp;
            <span class="editIcon"></span>
            </div>
	
			
		</div>
	</div>
    </div>
	<div class="portlet-toolbar__info">
		<div class="inline-form">
			<input id="filterByWhoCanUseSelect"/>
			<label font-size:85%; for="filterByWhoCanUseSelect"><%=LanguageUtil.get(pageContext, "Filter-By-Who-Can-Use")%>
			</label>
		</div>
	</div>

	<div class="portlet-toolbar__actions-secondary">
		<!-- ADD STEP -->
		<button dojoType="dijit.form.Button"
				onClick="schemeAdmin.exportScheme('<%=scheme.getId()%>');return false;" iconClass="addIcon">
			<%=LanguageUtil.get(pageContext, "Export-Workflow-Scheme")%>
		</button>
	   <!-- ADD STEP -->
	</div>
</div>



<!-- Workflow Steps -->
<div class="board-wrapper">
	<div class="board-main-content">
		<div class="board-canvas">
			<div class="" id="wfStepInDragContainer">

				<jsp:include page="view_steps_filtered.jsp">
					<jsp:param name="scheme" value="#{scheme}" />
				</jsp:include>
				
			</div>
		</div>
	</div>
</div>
