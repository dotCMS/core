<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>

<%
	final WorkflowAPI wapi = APILocator.getWorkflowAPI();

	final String schemeId = request.getParameter("schemeId");
	final WorkflowScheme scheme = wapi.findScheme(schemeId);
	final List<WorkflowStep> steps = wapi.findSteps(scheme);
	final List<ContentType> contentTypes = wapi.findContentTypesForScheme(scheme);

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

				if ("true" == el.dataset.first || index == 0) { // the first one can not be moved or can not drop any step to the first one.

					this.cancel();
					return;
				}

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
                style: "width: 80%; margin-left: 5px;",
                required:false,
                onClick:function(){
                    var whoCanUseSelect = dijit.byId("whoCanUseSelect");
                    whoCanUseSelect.set("displayedValue","");
                    whoCanUseSelect.loadDropDown();
                },onChange:function (item) {

                    var whoCanUseSelect = dijit.byId("whoCanUseSelect");
                    var roleId = whoCanUseSelect.getValue();
                    var contentTypeSelect = dijit.byId("contentTypeSelect");

                    //We clean up the value displayed in the content type box in any of the following cases
				    // No need to have a content type if we're showing it all (meaning no filters are applied)
                    if('' == roleId){ //When selcting 'All' we clear contentType
                       contentTypeSelect.set("displayedValue","");
					} else {
                        var whoCanUseDisplayedVal = whoCanUseSelect.get("displayedValue");
                        if (whoCanUseDisplayedVal) {
                            //if we pick a special role we clean the content type box
                            if (whoCanUseDisplayedVal.indexOf('Anyone who can') >= 0) {
                                contentTypeSelect.set("displayedValue","");
                            }
                        }
					}
                    var contentTypeId = contentTypeSelect.getValue();
                    stepAdmin.filterSteps(schemeId, roleId, contentTypeId);

                }
            },
            "filterByWhoCanUseSelect");
        dijit.byId("whoCanUseSelect").set("displayedValue","All");

        var stateStore = new dojo.store.Memory({
            data: [
                <%
                   final Iterator<ContentType> contentTypeIterator = contentTypes.iterator();
				   while (contentTypeIterator.hasNext()){
                      final ContentType contentType = contentTypeIterator.next();
                      %>
                       {name:"<%=contentType.name()%>" , id:"<%=contentType.id()%>" }
				      <%
				      if (contentTypeIterator.hasNext()){
                         %>,<%
				      }
				   }
                %>
            ]
        });

        var filterByContentTypeSelect = new dijit.form.FilteringSelect({
                id: "contentTypeSelect",
                name: "contentTypeSelect",
                store: stateStore,
                pageSize:30,
                searchDelay:300,
                style: "width: 80%; margin-left: 5px;",
                required:false,
                onClick:function(){

                },onChange:function (item) {

                    var whoCanUseSelect = dijit.byId("whoCanUseSelect");
                    var roleId = whoCanUseSelect.getValue();
                    var contentTypeSelect = dijit.byId("contentTypeSelect");
                    var contentTypeId = contentTypeSelect.getValue();

                    stepAdmin.filterSteps(schemeId, roleId, contentTypeId);

                }
            },
            "filterByContentTypeSelect");

	});

</script>




<div class="portlet-toolbar">

	    <div style="float:left">
			<input type="hidden" name="cmd" value="save">
			<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
			<div onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');" style="float: left;" class="showPointer" >
			     <h2 style="border-bottom:dotted 1px gray;"><%=UtilMethods.webifyString(scheme.getName())%></h2>
			     <!--  <%=UtilMethods.webifyString(scheme.getDescription())%> -->
			</div>
	        <div style="float: left;">&nbsp;
	            <span class="showPointer" href="#" onclick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');"><i class="fa fa-pencil" aria-hidden="true"></i></span>
	        </div>
		</div>
		<div style="float:left">
			<div class="inline-form">
				<label style="display: flex;" font-size:85%; for="filterByWhoCanUseSelect"><%=LanguageUtil.get(pageContext, "Filter-By-Who-Can-Use")%>:
				    <input id="filterByWhoCanUseSelect"/>
				</label> &nbsp; 
				<label style="display: flex;" font-size:85%; for="filterByContentTypeSelect"><%=LanguageUtil.get(pageContext, "Filter-By-Content-Type")%>:
					<input id="filterByContentTypeSelect"/>
				</label>
			</div>
		</div>
	
		<div style="float:right">
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
<div style="display:block; padding-left:35px;padding-bottom:10px;color: gray;"><%=UtilMethods.webifyString(scheme.getDescription())%></div>
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
