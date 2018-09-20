<%@ page import="com.dotcms.publisher.environment.bean.Environment" %>
<%@ page import="java.util.List" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
    //Verify if we have set environments on session
    String selectedEnvKey = com.dotmarketing.util.WebKeys.SELECTED_ENVIRONMENTS + request.getSession().getAttribute("USER_ID");
	String selectedBundleKey = com.dotmarketing.util.WebKeys.SELECTED_BUNDLE + request.getSession().getAttribute("USER_ID");

    List<Environment> lastSelectedEnvironments = null;
    if ( request.getSession().getAttribute(selectedEnvKey) != null ) {
        lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute(selectedEnvKey);
    }

    //Verify if we have set a bundle on session
    com.dotcms.publisher.bundle.bean.Bundle lastSelectedBundle = null;
    if ( request.getSession().getAttribute( selectedBundleKey ) != null ) {
        lastSelectedBundle = (com.dotcms.publisher.bundle.bean.Bundle) request.getSession().getAttribute( selectedBundleKey );
    }
%>
<script type="text/javascript">

    var lastSelectedEnvironments = [];
    <%if (lastSelectedEnvironments != null) {

        int i = 0;
        for (Environment environment: lastSelectedEnvironments) {
            String id = environment.getId();
            String name = environment.getName();
    %>

            var entry = {name:'<%=name%>',id:'<%=id%>'};
            lastSelectedEnvironments[<%=i++%>] = entry;
    <%}%>

    <%}%>

    var lastSelectedBundle = {};
    <%if (lastSelectedBundle != null) {

        String id = lastSelectedBundle.getId();
        String name = lastSelectedBundle.getName();%>

        lastSelectedBundle = {name: '<%=UtilMethods.escapeSingleQuotes(name)%>', id: '<%=id%>'};
    <%}%>

    sessionStorage.setItem("lastSelectedEnvironments",JSON.stringify(lastSelectedEnvironments));
    sessionStorage.setItem("lastSelectedBundle",JSON.stringify(lastSelectedBundle));

</script>