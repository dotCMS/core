<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ include file="/html/common/init.jsp" %>
<tiles:useAttribute id="tilesContent" name="content" classname="java.lang.String" />
<tiles:useAttribute id="tilesPortletSubNav" name="portlet_sub_nav" classname="java.lang.String" />
<%
        boolean inPortal = (request.getAttribute("org.dotcms.variables.inPortlets") != null);
        boolean inPopupIFrame = UtilMethods.isSet(ParamUtil.getString(request, WebKeys.POPUP)) || (UtilMethods.isSet(ParamUtil.getString(request, WebKeys.IN_FRAME)) && "true".equals(ParamUtil.getString(request, WebKeys.IN_FRAME))) || (UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME)) && (Boolean) request.getSession().getAttribute(WebKeys.IN_FRAME));
		boolean isAjaxIframe = UtilMethods.isSet(ParamUtil.getString(request, WebKeys.AJAX_PORTLET)) && "true".equals(ParamUtil.getString(request, WebKeys.AJAX_PORTLET));
		
        request.setAttribute("org.dotcms.variables.inPortlets", "true"); 
        
%>

<%if(inPortal ) {%>
    <div class="portlet-wrapper">
        <% if (Validator.isNotNull(tilesPortletSubNav) ) {%>
            <liferay:include page="<%= Constants.TEXT_HTML_DIR + tilesPortletSubNav %>" flush="true" />
        <%}%>
        <jsp:include page="<%= Constants.TEXT_HTML_DIR + tilesContent %>"></jsp:include>
    </div>
        
<%}else{ %>
        <%@ include file="/html/common/top_inc.jsp" %>
        <style>
                body{
                        background: white;
                }
        </style>
        
        <%if(isAjaxIframe){ %>
            <script>
				var portletTabMap = {}; 
			</script>
			
	        <div id="menu" class="navbar" style="display:none;">
				<ul class="level1 horizontal" id="root">
					<script>portletTabMap['<%=ParamUtil.getString(request, "p_p_id")%>'] = '0';</script>
				    <li class="dotAjaxNav0 level1 active">
				    	<a href="javascript:dotAjaxNav.show("/api/portlet/<%=ParamUtil.getString(request, "p_p_id")%>/","0")">
					        <div class="tabLeft">
								<div class="navMenu-title"></div>
								<div class="navMenu-subtitle"></div>
							</div>
						</a>                               
					</li>            
				</ul>
			</div>
            <%@ include file="/html/common/rest_portlet_navigation_js.jsp" %>
            <%@ include file="/html/common/messages_inc.jsp" %>
                
            <div id="dotAjaxMainHangerDiv">
            	<div id="dotAjaxMainDiv" dojoType="dojox.layout.ContentPane" style="overflow: visible;">
                </div>
            </div>
            
		    <script type="text/javascript">
		        dotAjaxNav.show("/api/portlet/<%=ParamUtil.getString(request, "p_p_id")%>/","0");
		    </script>
        <%} else { %>
        	<%@ include file="/html/common/messages_inc.jsp" %>
        	<jsp:include page="<%= Constants.TEXT_HTML_DIR + tilesContent %>"></jsp:include>
        <%} %>
        <%@ include file="/html/common/bottom_inc.jsp" %>
<%} %>
