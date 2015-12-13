<%@page import="com.dotmarketing.portlets.personas.business.PersonaAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.liferay.util.Validator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESContentletAPIImpl"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ include file="/html/common/init.jsp" %>
<%

Object c = LicenseUtil.getLevel();
%>
<%if( LicenseUtil.getLevel() < 200){ %>
	<div class="portlet-wrapper">
		<div class="subNavCrumbTrail">
			<ul id="subNavCrumbUl">
				<li class="lastCrumb">
					<a href="#" ><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.PERSONAS_PORTLET")%></a>
				</li>

			</ul>
			<div class="clear"></div>
		</div>
		<jsp:include page="/WEB-INF/jsp/personas_portlet/not_licensed.jsp"></jsp:include>

	</div>
<%return;}%>
<%
	String defaultPersonaSt = "";
	Structure defaultPersona = APILocator.getPersonaAPI().getDefaultPersonaStructure();
	defaultPersonaSt = defaultPersona.getInode();
%>





<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<%@taglib prefix="portlet" uri="/WEB-INF/tld/liferay-portlet.tld"%>

<script type="text/javascript">
	dojo.require("dotcms.dojo.data.StructureReadStore");
	<% com.dotmarketing.beans.Host myHost =  WebAPILocator.getHostWebAPI().getCurrentHost(request); %>
	
	function addPersona(){//need to add the referer
		showPersonaPopUp();
	}

	function showPersonaPopUp(){
		var dialog = dijit.byId("addPersonaDialog");
		if(dialog){
			dialog.destroyRecursive(true);
		}
		var personaDialog = new dijit.Dialog({
			id : "addPersonaDialog",
			title: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.persona.ui.addpersona.dialog")) %>",
			style: "width: 420px; height:130px; overflow: auto"
		});
		var dialogPersona = getPersonaDialog();

        dialogPersona = dojo.string.substitute(dialogPersona, { stInode:'<%=defaultPersonaSt%>'});
        personaDialog.attr("content", dialogPersona);
        personaDialog.show();
	}

	function getPersonaDialog(){
        return "<div>"+
                "<div style='margin:8px 5px;'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.persona.ui.select.the.type.of.persona.you.wish.to.create")) %>:</div>" +
                "<span dojoType='dotcms.dojo.data.StructureReadStore' jsId='personaStructureStore' dojoId='personaStructureStoreDojo' structureType='<%=Structure.STRUCTURE_TYPE_PERSONA%>'></span>"+
                "<select id='defaultPersonaType' name='defaultPersonaType' dojoType='dijit.form.FilteringSelect' style='width:200px;' store='personaStructureStore' searchDelay='300' pageSize='15' autoComplete='false' ignoreCase='true' labelAttr='name' searchAttr='name'  value='${stInode}' invalidMessage='<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>'></select>"+
                "<button dojoType='dijit.form.Button' iconClass='addIcon' id='selectedPersonaButton' onclick='getSelectedPersona();'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "modes.Select")) %></button>" +
                "</div>";
    }

    function getSelectedPersona() {
		var selected = dijit.byId('defaultPersonaType');
		window.location = "/c/portal/layout?p_l_id=71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=new&selectedStructure=" + selected + "&_EXT_11_referer=%23%2Fapi%2Fportlet%2FPERSONAS_PORTLET%2F%3Fr%3D57220346";
	}

</script>

<div class="portlet-wrapper">
	<div id="PersonaDiv" style="text-align:center;padding:20px;" >
		<input name="searchPersona" id="searchPersona" type="text" value="<%= LanguageUtil.get(pageContext, "search") %>" size="40"   dojoType="dijit.form.TextBox"  />
		<input type="checkbox" dojoType="dijit.form.CheckBox" id="hideInactivePersonas" onclick="" >
	        <%= LanguageUtil.get(pageContext, "message.persona.ui.hideInactivePersonas") %>
		<button type="button" id="addPersona" onClick="addPersona()" dojoType="dijit.form.Button" iconClass="plusIcon" value="Add Persona">
			<%= LanguageUtil.get(pageContext, "message.persona.ui.addPersona") %>
		</button>
	</div>
	<table class="listingTable" style="width:70%;">	
		<tr>
			<th colspan="3">					
				<strong></strong><h2>Personas</h2>
			</th>
		</tr>
		<!-- Result Personas -->
		<tr>
			<td width="100">Img</td>
			<td>Name</td>
			<td width="100"><input type="checkbox" dojoType="dijit.form.CheckBox" id="isActive" onclick="" ></td>
		</tr>
		
	</table>
</div>