<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.List"%>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="java.util.Collections" %>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String schemeId = request.getParameter("schemeId");
	WorkflowScheme scheme = new WorkflowScheme();

	List<WorkflowStep> steps;
	List<ContentType> contentTypes = Collections.emptyList();
	try {
		scheme = wapi.findScheme(schemeId);
		steps = wapi.findSteps(scheme);
		contentTypes =
				wapi.findContentTypesForScheme(scheme);
	} catch (Exception e) {
	}

	final String schemeShortyId = APILocator.getShortyAPI().shortify(scheme.getId());
%>

<div dojoType="dijit.form.Form" id="addEditSchemeForm" jsId="addEditSchemeForm" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfSchemeAjax" method="POST">
	<input type="hidden" id="cmd" name="cmd" value="save">
	<input type="hidden" id="schemeArchived" name="schemeArchived" value="<%=(scheme.isArchived()) ? "true" : "false"%>">
	<input type="hidden" id="schemeId" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
	<!-- START Listing Results -->
    <%if(scheme.isArchived()){%>
            <div style="padding:10px;margin-bottom:10px;text-align:center;font-weight: bold;color:maroon; ">
                <h3>
                <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archived"))%>
               </h3>
           </div>
     <%} %>
	<div class="form-horizontal" <%if(scheme.isArchived()){%>style="opacity: .6"<%} %>>


		<%if(!scheme.isNew()){%>
			<dl>
				<dt>
					<span><%=LanguageUtil.get(pageContext, "Scheme")%> <%=LanguageUtil.get(pageContext, "Id")%></span>
				</dt>
				<dd>
					<strong>
						<a onclick="this.parentNode.innerHTML='<%=scheme.getId()%>'; return false;" href="#"><%=schemeShortyId %></a>
					</strong>
				</dd>
			</dl>
			<dl>
				<dt>
					<span><%=LanguageUtil.get(pageContext, "Variable")%></span>
				</dt>
				<dd>
					<input type="text" name="schemeVariable" id="schemeVariable"
						   dojoType="dijit.form.ValidationTextBox"
						   value="<%=scheme.getVariableName()%>" readonly="true"
						   maxlength="255" style="width:250px;">
				</dd>
			</dl>
		<%}%>
		<dl>
			<dt>
				<label for="schemeName" class="required" for=""><%=LanguageUtil.get(pageContext, "Name")%></label>
			</dt>
			<dd>
				<input type="text" name="schemeName" id="schemeName"
					   dojoType="dijit.form.ValidationTextBox"  required="true"
					   value="<%=UtilMethods.webifyString(scheme.getName())%>" <%if(scheme.isArchived()){%>readonly="true"<%} %>
					   maxlength="255" style="width:250px;<%if(scheme.isArchived()){%>;text-decoration:line-through;<%}%>">
			</dd>
		</dl>

		<dl>
			<dt>
				<label for="schemeDescription"><%=LanguageUtil.get(pageContext, "Description")%></label>
			</dt>
			<dd>
				<input type="textarea" name="schemeDescription"
					   id="schemeDescription" dojoType="dijit.form.Textarea" <%if(scheme.isArchived()){%>readonly="true"<%} %>
					   value="<%=UtilMethods.webifyString(scheme.getDescription())%>" style="width:250px; height:100px;min-height:100px;max-height:100px;">
			</dd>
		</dl>

		
			<dl>
				<dt>
					<label style="<%if(scheme.isNew()){%>color:silver<%}%>"><%if(!contentTypes.isEmpty()) { %><%=contentTypes.size() %><%} %> <%=LanguageUtil.get(pageContext, "structures")%></label>
				</dt>
	
				<dd class="wf-content-types">
	
		            <div style="margin:auto;width:250px;height:147px;overflow: auto; border:1px solid <%if(!scheme.isNew()){%>silver<%}else{%>#dddddd<% }%>">
		                   <%for(final ContentType contentType : contentTypes) { %>
			                    <div style="" class="structure-content-type-listing" onclick="window.parent.location='/dotAdmin/#/content-types-angular/edit/<%=contentType.id()%>'" >
			                      
			                            <%=contentType.name()%>
			                 
			                    </div>
		                   <% }%>
		                   <%if(contentTypes.isEmpty()) { %>
		                      <div style="padding:10px; <%if(scheme.isNew()){%>color:#dddddd<%}%>"> 
		                          <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "none"))%>
		                      </div>
		                   <%} %>
		            </div>
				</dd>		
			</dl>


	</div>

	<div class="buttonRow scheme">
        <div>
		<%if(!scheme.isNew()){%>
			<button dojoType="dijit.form.Button" onClick='schemeAdmin.copyScheme("<%=UtilMethods.webifyString(scheme.getId())%>", "<%=UtilMethods.webifyString(scheme.getName())%>")' iconClass="saveIcon" type="button">
				<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy"))%>
			</button>
			
			<%if(scheme.isArchived()){%>
	        <button dojoType="dijit.form.Button" onClick='schemeAdmin.unArchiveScheme("<%=UtilMethods.webifyString(scheme.getId())%>")' iconClass="archiveIcon"  type="button">
	            <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive"))%>
	        </button>
			<button dojoType="dijit.form.Button" onClick='schemeAdmin.deleteScheme("<%=UtilMethods.webifyString(scheme.getId())%>")' iconClass="deleteIcon" style="background:black;color:white" type="button">
				<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete"))%>
			</button>
			<%}else{%>
	        <button dojoType="dijit.form.Button" onClick='schemeAdmin.archiveScheme("<%=UtilMethods.webifyString(scheme.getId())%>")' iconClass="archiveIcon"  type="button">
	            <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive"))%>
	        </button>
			<%} %>
		<%}%>
        </div>
        <div>
		    <button dojoType="dijit.form.Button"
		        onClick='schemeAdmin.hideAddEdit()' class="dijitButtonFlat" type="button">
		            <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		    </button>
		    <button dojoType="dijit.form.Button" onClick='schemeAdmin.saveAddEdit()' iconClass="saveIcon" type="button">
		        <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		    </button>
        </div>
	</div>

</div>
