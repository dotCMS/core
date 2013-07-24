<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator" %>
<%@page import="com.dotmarketing.util.URLEncoder" %>

<script type="text/javascript" src="/html/portlet/ext/cmsconfig/js/main.js" ></script>

<%
    if ( layout == null ) {

        List<Layout> myLayouts = APILocator.getLayoutAPI().loadLayoutsForUser( user );

        for ( Layout l : myLayouts ) {
            List<String> ports = l.getPortletIds();
            if ( ports.contains( "9" ) ) {
                layout = l;
                layoutId = l.getId();
                break;
            }
            layout = l;
            layoutId = l.getId();
        }
    }
    if ( user == null ) {
        response.setStatus( 403 );
        return;
    }
    boolean userIsAdmin = false;
    if ( APILocator.getRoleAPI().doesUserHaveRole( user, APILocator.getRoleAPI().loadCMSAdminRole() ) ) {
        userIsAdmin = true;
    }

    String referer = new URLEncoder().encode( "/c/portal/layout?p_l_id=" + layoutId + "&p_p_id=9&" );
%>