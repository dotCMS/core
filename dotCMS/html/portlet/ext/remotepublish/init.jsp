<%@ page import="com.dotcms.publisher.environment.bean.Environment" %>
<%@ page import="java.util.List" %>
<%
    //Verify if we have set environments on session
    List<Environment> lastSelectedEnvironments = null;
    if ( request.getSession().getAttribute( com.dotmarketing.util.WebKeys.SELECTED_ENVIRONMENTS ) != null ) {
        lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( com.dotmarketing.util.WebKeys.SELECTED_ENVIRONMENTS );
    }

    //Verify if we have set a bundle on session
    com.dotcms.publisher.bundle.bean.Bundle lastSelectedBundle = null;
    if ( request.getSession().getAttribute( com.dotmarketing.util.WebKeys.SELECTED_BUNDLE ) != null ) {
        lastSelectedBundle = (com.dotcms.publisher.bundle.bean.Bundle) request.getSession().getAttribute( com.dotmarketing.util.WebKeys.SELECTED_BUNDLE );
    }
%>
<script type="text/javascript">

    window.lastSelectedEnvironments = new Array();
    <%if (lastSelectedEnvironments != null) {

        int i = 0;
        for (Environment environment: lastSelectedEnvironments) {
            String id = environment.getId();
            String name = environment.getName();%>

            var entry = {name:'<%=name%>',id:'<%=id%>'};
            window.lastSelectedEnvironments[<%=i++%>] = entry;
    <%}%>

    <%}%>

    window.lastSelectedBundle = {};
    <%if (lastSelectedBundle != null) {

        String id = lastSelectedBundle.getId();
        String name = lastSelectedBundle.getName();%>

        window.lastSelectedBundle = {name: '<%=name%>', id: '<%=id%>'};
    <%}%>
</script>