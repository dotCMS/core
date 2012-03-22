<%@ page import="javax.portlet.WindowState" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Relationship" %>
<%@ page import="com.dotmarketing.portlets.structure.struts.RelationshipForm" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.RelationshipFactory" %>
<%@ include file="/html/portlet/ext/structure/init.jsp" %>
<%	
 String referer = request.getParameter("referer");
 RelationshipForm relationshipForm = (RelationshipForm) request.getAttribute("RelationshipForm");
 Relationship relationship = RelationshipFactory.getRelationshipByInode(relationshipForm.getInode());
 boolean disabled = false;
 if(relationship.isFixed()){
	 disabled = true;
 }
 
List<Structure> structures = (List<Structure>) request.getAttribute(com.dotmarketing.util.WebKeys.Relationship.STRUCTURES_LIST);
 
%>
<script language="javascript">

function cancel()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/view_relationships' />";
	href = href + "</portlet:actionURL>";
	document.location = href;
}

function saveRelationship()
{
	var form = document.getElementById("relationshipForm");
	var action = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	action = action + "<portlet:param name='struts_action' value='/ext/structure/edit_relationship' />";
	action = action + "<portlet:param name='<%=com.liferay.portal.util.Constants.CMD%>' value='<%=com.liferay.portal.util.Constants.ADD%>' />";
	action = action + "</portlet:actionURL>";
	form.action = action;
	form.submit();
}

// DOTCMS - 4028
var structureInodes = [ 
<% 
for (Structure structure: structures) {
%>
	"<%= structure.getInode()%>",
<%
}
%>
];

var structureVelVars = [ 
<% 
for (Structure structure: structures) {
%>
	"<%= structure.getVelocityVarName()%>",
<%
}
%>
];



function structuresChanged() {
	var parentRelationName = dijit.byId("parentRelationName").attr('value');
	var childRelationName = dijit.byId("childRelationName").attr('value');
	var parentStructureInode = dijit.byId("parentStructureInode").attr('value');
	var parentStructureName = dijit.byId("parentStructureInode").attr('displayedValue');
	var childStructureInode = dijit.byId("childStructureInode").attr('value');
	var childStructureName = dijit.byId("childStructureInode").attr('displayedValue');
	
	for ( var i = 0; i < structureInodes.length; i++) {
		if(parentStructureInode == structureInodes[i]){
			parentStructureName = structureVelVars[i];
			break;
		}
	}
	
	for ( var i = 0; i < structureInodes.length; i++) {
		if(childStructureInode == structureInodes[i]){
			childStructureName = structureVelVars[i];
			break;
		}
	}
		
	dijit.byId("parentRelationName").attr('value', "Parent " + parentStructureName);
	dijit.byId("childRelationName").attr('value', "Child " + childStructureName);
	
}

function relationNameChanged() {
	var parentRelationName = dijit.byId("parentRelationName").attr('value');
	var childRelationName = dijit.byId("childRelationName").attr('value');
	var parentStructureInode = dijit.byId("parentStructureInode").attr('value');
	var parentStructureName = dijit.byId("parentStructureInode").attr('displayedValue');
	var childStructureInode = dijit.byId("childStructureInode").attr('value');
	var childStructureName = dijit.byId("childStructureInode").attr('displayedValue');
	var row = document.getElementById("childRequiredRow");
	var label = document.getElementById("parentRequiredLabel");
	var cardinalitySelect = document.getElementById("cardinality");
	
	<%if(disabled){%>
	  cardinalitySelect.disabled = true;
	<%}else{%>
	  cardinalitySelect.disabled = false;
	<%}%>

	
	
}


/*function setRequired(required){
    var form = document.getElementById(required);
    var requiredValue = form.value;
    if(!form.checked){
        form.value = "false";
        form.checked = false;
    }else{
        form.value = "true";
        form.checked = true;
   }
}*/


</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add/Edit-Relationship")) %>' />
	<%@ include file="/html/common/messages_inc.jsp" %>

<html:form action="/ext/structure/edit_relationship" method="post" styleId="relationshipForm">
    
    <fieldset>
 
        <html:hidden property="inode" />
        <html:hidden property="relationTypeValue" />
        <html:hidden property="fixed" />
        <input type="hidden" name="referer"  value="<%=referer%>" >
        
          <dl>
                <dt><span class="required"></span> <span class="mRed"><%= LanguageUtil.get(pageContext, "Required-Fields") %></span>
                    <%= LanguageUtil.get(pageContext, "Parent-Structure") %>:
                </dt>
                <dd>
                    <select dojoType="dijit.form.FilteringSelect" name="parentStructureInode" id="parentStructureInode" onchange="relationNameChanged(); structuresChanged(); "  disabled="<%=disabled%>" value="<%= UtilMethods.isSet(relationshipForm.getParentStructureInode()) ? relationshipForm.getParentStructureInode() : "" %>">
<%					
					for (Structure structure: structures) {
%>
						<option value="<%= structure.getInode() %>"><%= structure.getName() %></option>
<%
					}
%>
                    </select>
                </dd>
                    
                <dt><span class="required"></span> <%= LanguageUtil.get(pageContext, "Parent-Relation-Name") %>:</dt>
                <dd><input type="text" dojoType="dijit.form.TextBox" name="parentRelationName" id="parentRelationName" onkeyup="relationNameChanged()" readonly="<%=disabled%>" value="<%= UtilMethods.isSet(relationshipForm.getParentRelationName()) ? relationshipForm.getParentRelationName() : "" %>" /></dd>
                    
                <dt id="parentRequiredLabel"><%= LanguageUtil.get(pageContext, "Is-a-Parent-Required") %>:</dt>
                <dd><input type="checkbox" dojoType="dijit.form.CheckBox" name="parentRequired" id="parentRequired" disabled="<%=disabled%>" <%-- onclick="setRequired('parentRequired');" --%> <%= relationshipForm.isParentRequired() ? "checked" : "" %> /></dd>


                <dt><%= LanguageUtil.get(pageContext, "Child-Structure") %>:</dt>
                <dd>
                    <select dojoType="dijit.form.FilteringSelect" name="childStructureInode" id="childStructureInode" onchange="relationNameChanged(); structuresChanged();" disabled="<%=disabled%>" value="<%= UtilMethods.isSet(relationshipForm.getChildStructureInode()) ? relationshipForm.getChildStructureInode() : "" %>">
<%
					for (Structure structure: structures) {
%>
                        <option value="<%= structure.getInode() %>"><%= structure.getName() %></option>
<%
					}
%>
                    </select>
                </dd>   
                
                <dt><span class="required"></span> <%= LanguageUtil.get(pageContext, "Child-Relation-Name") %>:</dt>
                <dd><input type="text" dojoType="dijit.form.TextBox" name="childRelationName" id="childRelationName" onkeyup="relationNameChanged()" styleClass="form-text" readonly="<%=disabled%>" value="<%= UtilMethods.isSet(relationshipForm.getChildRelationName()) ? relationshipForm.getChildRelationName() : "" %>" /></dd>
                
                <dt id="childRequiredRow"><%= LanguageUtil.get(pageContext, "Is-a-Child-Required") %>:</dt>
                <dd><input type="checkbox" dojoType="dijit.form.CheckBox" name="childRequired" id="childRequired" disabled="<%=disabled%>" <%-- onclick="setRequired('childRequired');" --%> <%= relationshipForm.isChildRequired() ? "checked" : "" %> /></dd>


            <dt><%= LanguageUtil.get(pageContext, "Relation") %>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="cardinality" id="cardinality" disabled="<%=disabled%>" value="<%= relationshipForm.getCardinality() %>">
                    <option value="<%= ((Integer)com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()).toString() %>"><%= LanguageUtil.get(pageContext, "One-to-Many") %></option>
                    <option value="<%= ((Integer)com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()).toString() %>"><%= LanguageUtil.get(pageContext, "Many-to-Many") %></option>
                </select>
            </dd>
    
            <dt>&nbsp;</dt>
            <dd>
                <%if(!disabled){%>
                <button dojoType="dijit.form.Button" onCLick="saveRelationship();return false;" iconClass="saveIcon">
                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
                </button>
        
                <button dojoType="dijit.form.Button" onCLick="cancel();return false;" iconClass="cancelIcon" >
                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
                </button>
                <%}else{ %>
                
                <button dojoType="dijit.form.Button" onCLick="cancel();return false;" iconClass="cancelIcon">
                   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
                </button>
                <%} %>
            </dd>
      </dl> 
 </fieldset>
   
   
</html:form>
</liferay:box>
<script language="javascript">
	dojo.addOnLoad(relationNameChanged);
	dojo.addOnLoad (function(){//DOTCMS-5407
		if(dojo.isIE){
			enableDisable(<%=disabled%>);
		}
	});
	function enableDisable(isDisabled){		
		dijit.byId("parentStructureInode").attr('disabled',isDisabled);		
		dijit.byId("childStructureInode").attr('disabled',isDisabled);
		dijit.byId("parentRequired").attr('disabled',isDisabled);		
		dijit.byId("childRequired").attr('disabled',isDisabled);
		dijit.byId("cardinality").attr('disabled',isDisabled);
		dijit.byId("parentRelationName").attr('readOnly',isDisabled);
		dijit.byId("childRelationName").attr('readOnly',isDisabled);		
	}
</script>
