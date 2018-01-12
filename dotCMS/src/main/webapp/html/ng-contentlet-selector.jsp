
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.List"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Contentlet Search</title>

    <style type="text/css">
        @import "/html/js/dojo/custom-build/dijit/themes/dijit.css";
        @import "/html/css/dijit-dotcms/dotcms.css";

        body {
            background-color: #fff;
        }

        body,
        .contentlet-search,
        .related-content-form {
            width: 100%;
            height: 100%;
        }
    </style>

    <script type="text/javascript">
        function contentSelected(content) {
            if (ngEditContentletEvents) {
                ngEditContentletEvents.next({
                    event: "select",
                    data: {
                        inode: content.inode,
                        identifier: content.identifier,
                        type: "type"
                    }
                })
            }
        }

        function isInodeSet(x) {
            return (x && x != undefined && x != "" && x.length > 15);
        }

        djConfig = {
            parseOnLoad: true,
            i18n: "/html/js/dojo/custom-build/custom-build/build/",
            useXDomain: false,
            isDebug: false,
            locale: "en-us",
            modulePaths: { dotcms: "/html/js/dotcms" }
        };
    </script>

    <script type="text/javascript" src="/html/js/log4js/log4javascript.js"></script>
    <script type="text/javascript" src="/html/js/log4js/dotcms-log4js.js"></script>
    <script type="text/javascript" src="/html/js/dojo/custom-build/dojo/dojo.js"></script>
    <script type="text/javascript" src="/html/js/dojo/custom-build/build/build.js"></script>
    <script type="text/javascript" src="/html/common/javascript.jsp"></script>
    <script type="text/javascript" src="/dwr/engine.js"></script>
    <script type="text/javascript" src="/dwr/util.js"></script>
    <script type="text/javascript" src="/dwr/interface/LanguageAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/StructureAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/ContentletAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/BrowserAjax.js"></script>

    <%
        String containerIdentifier = (String) request.getParameter("container_id");
        User user = PortalUtil.getUser(request);
        Container container = (Container) APILocator.getVersionableAPI().findWorkingVersion(containerIdentifier, user, false);
        List<Structure> structuresInContainer = APILocator.getContainerAPI().getStructuresInContainer(container);
    %>

    <script type="text/javascript">
        dojo.require("dotcms.dijit.form.ContentSelector");

        dojo.addOnLoad(function () {
            contentSelector.show();
            contentSelector.containerStructures = [
                <%
                    for (Structure structure: structuresInContainer) {
                %>
                {
                    "inode": '<%=structure.id()%>',
                    "name": '<%=structure.getName()%>'
                },
                <%
                    }
                %>
            ];

            contentSelector._fillStructures();
        })


    </script>
</head>
<body>
<div jsId="contentSelector" onContentSelected="contentSelected" dojoType="dotcms.dijit.form.ContentSelector"></div>
</body>
</html>