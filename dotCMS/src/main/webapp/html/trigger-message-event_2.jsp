
<%@page import="java.util.*"%>
<%@ page import="com.dotmarketing.exception.DotDataException" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.api.system.event.*" %>
<%
    if (Boolean.parseBoolean(request.getParameter("create"))) {
        String message = request.getParameter("message");
        String title = request.getParameter("title");
        String width = request.getParameter("width");
        String height = request.getParameter("height");
        String code = request.getParameter("code");
        String lang = request.getParameter("lang");

        Map map = new HashMap();
        map.put("body", message);
        map.put("title", title);
        map.put("width", width);
        map.put("height", height);
        map.put("code", code);
        map.put("lang", lang);

        try {

            final SystemEvent systemEvent = new SystemEvent(SystemEventType.LARGE_MESSAGE, new Payload(map, Visibility.GLOBAL , null));
            APILocator.getSystemEventsAPI().push(systemEvent);
        } catch (DotDataException e) {
        }
    } else {
%>


body:
<br>
<textarea id="message" type="text" style="height:200px;width: 300px">Hello World</textarea>
<br>
Title: <input id="title" type="text"/>
<br>
Width: <input id="width" type="text"/>
<br>
Height: <input id="height" type="text"/>
<br>
Code: <textarea id="code" type="text" style="height:200px;width: 300px"></textarea>
<br>
lang: <input id="lang" type="text"/>
<br>
<button onclick="send()">Send</button>

<script>
    function send() {
        var message = document.getElementById('message').value;
        var title = document.getElementById('title').value;
        var width = document.getElementById('width').value;
        var code = document.getElementById('code').value;
        var lang = document.getElementById('lang').value;
        var height = document.getElementById('height').value;

        var Http = new XMLHttpRequest();
        var url='/html/trigger-message-event.jsp';
        Http.open("POST", url);
        Http.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        Http.send('create=true&message=' + message + '&title=' + title + '&width=' + width + '&height=' + height + '&code=' + code + '&lang=' + lang);

        Http.onreadystatechange=(e) => {
            console.log(Http.responseText)
        }
    }
</script>
<%}%>