<%@page import="com.dotmarketing.common.reindex.ReindexThread"%>
<%
    final ReindexThread reindexThread = ReindexThread.getInstance();
    String isWorkingMsg = "The current Reindex Thread instance is null";
    if (null != reindexThread) {
        isWorkingMsg = String.valueOf(ReindexThread.getInstance().isWorking());
    }
%>
<ul>
    <li>Was current Reindex Thread working? <%= isWorkingMsg %></li>
    <li>Current Reindex Thread instance: <%= reindexThread %></li>
</ul>
<%
    try {
        ReindexThread.getInstance().startThread();
        %><h3>Reindex Thread as been restarted successfully!</h3><%
    } catch (final Throwable t) {
        %><h1>ATTENTION! An error occurred when restarting the Reindex Thread</h1>
        <p><%= t.getMessage() %></p>
        <p><%= t.toString() %></p>
<%
    }
%>
