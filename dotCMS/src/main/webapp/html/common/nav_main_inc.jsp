<%@page import="com.dotcms.spring.portlet.PortletController"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.rest.BaseRestPortlet"%>
<%@page import="com.liferay.portal.model.Portlet"%>
<%@page import="com.dotcms.rest.WebResource"%>
<%@page import="com.dotmarketing.business.APILocator"%>

<%@page import="java.util.List"%><%@page import="com.dotmarketing.util.UtilMethods"%>
<script>
var portletTabMap = {}; // this holds a Map of portletId, tabId, used when refreshing to retrieve the proper tabId based on the portletId

</script>
<div id="menu" class="navbar">
        <ul class="level1 horizontal" id="root">

        <%for(int l=0;l< layouts.length ;l++){
                String tabName =LanguageUtil.get(pageContext, LanguageUtil.get(pageContext, layouts[l].getName()));
                String tabDescription = (!UtilMethods.isSet(layouts[l].getDescription())) ? "&nbsp;" :layouts[l].getDescription() ;
                if(!tabDescription.equals("&nbsp;")){
                         tabDescription = LanguageUtil.get(pageContext,tabDescription) ;
                 }

                List<String> portletIDs = layouts[l].getPortletIds();
                if(portletIDs ==null || portletIDs.size() ==0){
                	continue;
                }
                // fill
               	%><script>portletTabMap['<%=portletIDs.get(0)%>'] = <%=l%></script><%

                boolean isSelectedTab = (layout != null && layouts !=null && layout.getId().equals(layouts[l].getId()));
                PortletURLImpl portletURLImpl = new PortletURLImpl(request, portletIDs.get(0), layouts[l].getId(), false);
                String tabHREF = portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();

                Portlet portlet = APILocator.getPortletAPI().findPortlet(portletIDs.get(0));
                try {
                    Object object = Class.forName(portlet.getPortletClass()).newInstance();
                    if ( object instanceof BaseRestPortlet ) {
                        tabHREF = "javascript:dotAjaxNav.show('/api/portlet/" + portletIDs.get(0) + "/', '" + l + "');";
                    } else if ( object instanceof PortletController ) {
                        tabHREF = "/spring/portlet/" + portletIDs.get(0);
                    }
                } catch (Exception e) {
                    com.dotmarketing.util.Logger.error(this.getClass(), "Exception on portlet: " + (portlet == null ? "[null]" : portlet.getPortletClass()), e);
                }

                %>



                        <li class="dotAjaxNav<%=l %> level1 <%=(isSelectedTab) ? "active" : ""%>">
                                <a href="<%=tabHREF %>">
                                        <div class="tabLeft">
                                                <div class="navMenu-title"><%=tabName %></div>
                                                <div class="navMenu-subtitle"><%=tabDescription %></div>
                                        </div>
                                </a>
                                <%if( portletIDs.size()>1){%>
                                        <span class="tabRight"></span>
                                        <ul class="level2 dropdown">
                                                <%for(int i=0;i< portletIDs.size() ;i++){
                                                        Portlet p = (Portlet) APILocator.getPortletAPI().findPortlet(portletIDs.get(i));


                                                     	%><script>portletTabMap['<%=portletIDs.get(i)%>'] = <%=l%></script><%


                                                        portletURLImpl = new PortletURLImpl(request, portletIDs.get(i), layouts[l].getId(), false);
                                                        String linkHREF = portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();
                                                        String linkName = LanguageUtil.get(pageContext,"com.dotcms.repackage.javax.portlet.title." + portletIDs.get(i));


                                                        if("9".equals(portletIDs.get(i))){
                                                                request.setAttribute("licenseManagerPortletUrl", linkHREF + "&tab=licenseTab");
                                                        }
                                                        try{
                                                        	Object obj = Class.forName(p.getPortletClass()).newInstance();
	                                                        if(obj instanceof BaseRestPortlet){
	                                                                linkHREF =  "javascript:dotAjaxNav.show('/api/portlet/"+ portletIDs.get(i) + "/', '" + l + "');";
	                                                        }else if(obj instanceof PortletController ){
                                                                linkHREF =  "/spring/portlet/" + portletIDs.get(i);
                                                           }
                                                        }
                                                        catch(Exception e){
                                                        	//Logger.error(this.getClass(),"error in portlet nav:" + e.getMessage());
                                                        }

                                                        %>



														<%if(!"EXT_6".equals(portletIDs.get(i))){%>
                                                        	<li class="level2 dotCMS_<%=portletIDs.get(i)%>"><a href="<%=linkHREF %>"><span></span><%=linkName %></a></li>
                                                		<%} %>
                                                <%} %>
                                        </ul>
                                <%}%>
                        </li>
                <%}%>
        </ul>
</div>






<%@ include file="/html/common/nav_main_inc_js.jsp" %>
