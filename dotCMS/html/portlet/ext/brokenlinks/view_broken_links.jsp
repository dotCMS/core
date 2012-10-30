
<%@ page contentType="text/html" isELIgnored="false" %>
<%@ page import="com.dotmarketing.portlets.checkurl.util.CheckURLAccessibilityUtil"%>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.business.*" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.cache.StructureCache"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="javax.portlet.WindowState"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ include file="/html/portlet/ext/brokenlinks/init.jsp" %>
<%@ include file="/html/portlet/ext/brokenlinks/common_js.jsp" %>
<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />
<%@ include file="/html/common/messages_inc.jsp" %>
<%
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/brokenlinks/view_broken_links"});
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);	
%>
<liferay:include page="/html/portlet/ext/brokenlinks/sub_nav.jsp" />
		<fmt:setLocale value="<%=Locale.getDefault()%>"/>
        <div class="buttonBoxLeft">
	        <strong><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_LEFT_TITLE)%></strong>
	        <span id="structSelectBox"></span>
        </div>

        <div class="buttonBoxRight">
             <div id="addNewMenu"></div>
        </div>
<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox">
	<!-- RIGHT COLUMN -->
	<div region="center" splitter="true">
		<c:set value="20" var="pageSize"/>
		<c:set value="${listCBLSize}" var="rLen"/>
		<div id="contentWrapper" style="overflow-y:auto; overflow-x:auto;margin:35px 0 0 0;">
			<div style="" id="matchingResultsDiv">
				<div class="yui-gb portlet-toolbar">
					<br />
					<%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_LEFT_TEXT)%>
					<br />						
					<div style="text-align: center;" class="yui-u" id="tablemessage"> </div>
				</div>
			</div>
		    <c:choose>
		        <c:when test="${empty param.s}">
		            <c:set var="rowStart" value="1"/>
		        </c:when>
		        <c:otherwise>
		            <c:set var="rowStart" value="${param.s}"/>
		        </c:otherwise>
		    </c:choose>
		 
		    <c:choose>
		        <c:when test="${empty param.e}">
		            <c:set var="rowEnd" value="${pageSize}"/>
		        </c:when>
		        <c:otherwise>
		            <c:set var="rowEnd" value="${param.e}"/>
		        </c:otherwise>
		    </c:choose>
			
			<table class="listingTable" id="results_table">
				<tbody>
					<tr>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_ACTION)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_TITLE)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_FIELD_NAME)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_OWNER)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_LEU)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_NAME)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_HOST)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_LMD)%></th>
						<th><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_LINK)%></th>						
					</tr>
					<c:choose>
						<c:when test="${rLen>0}">
							<c:forEach begin="${rowStart-1}" step="1" end="${rowEnd-1}" items="${listCBL}" var="cbl" varStatus="ctrl">							
								<tr class="alternate_1_no_pointer">
									<td><a href="javascript: editContentlet('${cbl.inode}','<%=user.getUserId()%>','<%= referer %>',1,1,1);" title="Edit Content"><img src="/html/images/icons/pencil.png" alt="edit content" /></a></td>
									<td><c:out value="${cbl.title}"/></td>
									<td><c:out value="${cbl.fieldName}"/></td>
									<td><c:out value="${cbl.ownerFirstName}"/>&nbsp;<c:out value="${cbl.ownerLastName}"/></td>
									<td><c:out value="${cbl.modUserFirstName}"/>&nbsp;<c:out value="${cbl.modUserLastName}"/></td>
									<td><c:out value="${cbl.structureName}"/></td>
									<td><c:out value="${cbl.structureHost}"/></td>
									<td><fmt:formatDate value="${cbl.modDate}" /></td>
									<td>
										<c:forEach items="${cbl.brokenLinks}" var="bl">
											<strong>
												<c:choose>
													<c:when test="${bl.linkType=='I'}">
														Internal Broken Link
													</c:when>
													<c:otherwise>
														External Broken Link
													</c:otherwise>
												</c:choose>
											</strong>
											<ul>
												<li><c:out value="${bl.link}" /></li>
												<li><c:out value="${bl.title}" /></li>
											</ul>
											<br />
										</c:forEach>
									</td>									
								</tr>
							</c:forEach>
					</tbody>
				</table>
				
				<!-- START PAGINATION -->				
				<c:choose>
					<c:when test="${rLen lt rowEnd}">
						<c:set var="rCurrEnd" value="${rLen}"/>
					</c:when>
					<c:otherwise>
						<c:set var="rCurrEnd" value="${rowEnd}"/>
					</c:otherwise>
				</c:choose>
				<div class="yui-u first" style="float: left; margin: 5px 0 5px 6px"> showing ${rowStart}-${rCurrEnd} of ${rLen}</div>
				<div class="yui-u first" style="float: right; margin: 5px 6px 5px 0">
					<c:if test="${rowStart gt 1}">
						<a href="<%=referer%>&amp;s=${rowStart-pageSize}&amp;e=${rowEnd-pageSize}">< prev</a>
					</c:if>
					<c:forEach var="pStart" varStatus="status" begin="0" end="${rLen-1}" step="${pageSize}">
						<span>
	               			<a href="<%=referer%>&amp;s=${pStart+1}&amp;e=${pStart+pageSize}">${status.count}</a>&nbsp;
	            		</span>
	        		</c:forEach>	        						
	        		<c:if test="${rowEnd lt rLen}">	            						
	            		<a href="<%=referer%>&amp;s=${rowStart+pageSize}&amp;e=${rowEnd+pageSize}">next ></a>
	        		</c:if>
	        	</div>	
	        	<!-- END PAGINATION -->	
	        		 
	    		</c:when>
				<c:otherwise>
					<tr>
						<td colspan="100">
							<div class="noResultsMessage"><%=LanguageUtil.get(Locale.getDefault(),CheckURLAccessibilityUtil.BROKEN_LINKS_PORTLET_DETAIL_TABLE_EMPTY_LIST)%></div>			
						</td>
					</tr>										
					</tbody>
					</table>
					<br /><br />
					
				</c:otherwise>
			</c:choose>
		</div>			
	</div>
	<!-- END RIGHT COLUMN -->
</div>