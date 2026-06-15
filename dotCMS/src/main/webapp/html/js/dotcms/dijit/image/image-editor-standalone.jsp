<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%
    String dojoPath = Config.getStringProperty("path.to.dojo");
    String inode = UtilMethods.isSet(request.getParameter("inode")) ? request.getParameter("inode") : "";
    String tempId = UtilMethods.isSet(request.getParameter("tempId")) ? request.getParameter("tempId") : "";
    String variable = UtilMethods.isSet(request.getParameter("variable")) ? request.getParameter("variable") : "fileAsset";
    String fieldName = UtilMethods.isSet(request.getParameter("fieldName")) ? request.getParameter("fieldName") : variable;

    User user = PortalUtil.getUser(request);

    if (user == null || LicenseLevel.COMMUNITY.level == LicenseUtil.getLevel()) {
        response.getWriter().println("Unauthorized");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>dotCMS Image Editor</title>
    <link rel="stylesheet" type="text/css" href="/html/css/dot_admin.css">
    <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dmundra/dmundra.css">
    <link rel="stylesheet" type="text/css" href="<%=dojoPath%>/dijit/themes/dmundra/Grid.css">
    <link rel="stylesheet" type="text/css" href="/html/js/dotcms/dijit/image/image_tools.css">
    <script type="text/javascript">
        djConfig = {
            parseOnLoad: true,
            useXDomain: false,
            isDebug: false,
            modulePaths: { "dotcms": "/html/js/dotcms" }
        };
    </script>
    <script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js"></script>
    <script type="text/javascript">
        dojo.require("dotcms.dijit.image.ImageEditor");

        var editorVariable = "<%=variable%>";
        var editorInode = "<%=inode%>";
        var editorTempId = "<%=tempId%>";
        var editorFieldName = "<%=fieldName%>";

        window.contentAdmin = {
            contentletInode: editorInode
        };

        function forwardImageEditorMessage(type, tempFile) {
            var payload = {
                source: "dot-image-editor",
                type: type
            };

            if (tempFile) {
                payload.tempFile = tempFile;
            }

            window.parent.postMessage(payload, location.origin);
        }

        document.addEventListener("binaryField-tempfile-" + editorVariable, function(event) {
            forwardImageEditorMessage("tempfile", event.detail.tempFile);
        });

        document.addEventListener("binaryField-close-image-editor-" + editorVariable, function() {
            forwardImageEditorMessage("close");
        });

        dojo.ready(function() {
            var imageEditor = new dotcms.dijit.image.ImageEditor({
                inode: editorInode,
                tempId: editorTempId,
                variable: editorVariable,
                fieldName: editorFieldName,
                binaryFieldId: editorVariable,
                focalPoint: "0.0,0.0"
            });

            imageEditor.execute();
        });
    </script>
</head>
<body class="dotcms"></body>
</html>
