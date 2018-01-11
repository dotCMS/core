<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.List"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="java.util.*" %>

<%
    java.util.Map params = new java.util.HashMap();
    params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets"});
    String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
    String containerIdentifier = (String) request.getParameter("container_id");
    String dataAdd = (String) request.getParameter("add");
    User user = PortalUtil.getUser(request);
    Container container = (Container) APILocator.getVersionableAPI().findWorkingVersion(containerIdentifier, user, false);
    List<Structure> structuresInContainer = APILocator.getContainerAPI().getStructuresInContainer(container);
%>

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

        .portlet-sidebar-wrapper {
            width: 200px;
            margin-right: 16px;
        }

        .portlet-toolbar__add-contentlet {
            margin-left: auto;
        }
    </style>

    <script type="text/javascript">
        function addNewContentlet() {
            var href;
            var structureInode = dijit.byId('structuresSelect+1').value;

            // TODO: We have to add a condition for when is an Event
            var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
            href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
            href += "<portlet:param name='cmd' value='new' />";
            href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
            href += "<portlet:param name='inode' value='' />";
            href += "</portlet:actionURL>";
            href += "&selectedStructure=" + structureInode ;
            href += "&lang=" + getSelectedLanguageId();
            window.location = href;
        }

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

        function displayStructure(structureInode) {
		    contentSelector.displayStructureFields(structureInode);
        }
        
        function getSelectedLanguageId () {
            var obj = dijit.byId("langcombo+1");
            return obj && obj.value;
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

    <script type="text/javascript">
        dojo.require("dotcms.dijit.form.ContentSelector");

        dojo.addOnLoad(function () {
            contentSelector.show();
            var baseTypeToAdd = "<%= dataAdd %>";

            if (baseTypeToAdd === "content") {
                contentSelector.containerStructures = [
                    <%
                        for (Structure structure: structuresInContainer) {
                    %>
                    {
                        "inode": "<%=structure.id()%>",
                        "name": "<%=structure.getName()%>"
                    },
                    <%
                        }
                    %>
                ];

                contentSelector._fillStructures();
            } else if (baseTypeToAdd === "form") {
                contentSelector.displayStructureFields("4d21b6d8-1711-4ae6-9419-89e2b1ae5a06");
            } else if (baseTypeToAdd === "widget") {
                    // TODO: we need to get all the widgets and build the object
                    contentSelector.containerStructures = [
                    {
                        "inode": "4316185e-a95c-4464-8884-3b6523f694e9",
                        "name": "Document Listing"
                    },
                    {
                        "inode": "33f08cca-b1d0-4b77-a3e4-1a2c475fadc2",
                        "name": "Photo Carousel"
                    }
                ];

                contentSelector._fillStructures();
            }
        })
    </script>
</head>
<body>
<div jsId="contentSelector" onContentSelected="contentSelected" dojoType="dotcms.dijit.form.ContentSelector"></div>
</body>
</html>