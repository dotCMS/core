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

<div class="portlet-wrapper">
	<div id="PersonaDiv" style="text-align:center;padding:20px;" >
		<input name="searchPersona" id="searchPersona" type="text" value="Search..." size="40"   dojoType="dijit.form.TextBox"  />
		<input type="checkbox" dojoType="dijit.form.CheckBox" id="hideInactivePersonas" onclick="" >
	        <%= LanguageUtil.get(pageContext, "hideInactivePersonas") %>
		<button type="button" id="addPersona" onClick="" dojoType="dijit.form.Button" iconClass="plusIcon" value="Add Persona">
			<%= LanguageUtil.get(pageContext, "addPersona") %>
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
			<td>Name Of The Persona</td>
			<td width="100">On/Off</td>
		</tr>
		
	</table>
</div>

