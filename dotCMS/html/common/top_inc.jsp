<%--

THE INTENT OF THIS FILE IS A COMMON HEADER THAT SHOULD 
BE ABLE TO BE INCLUDED IN ALL JSP PAGES, EVEN "BLANK ONES"
PLEASE KEEP ALL PORTAL SPECIFIC CODE, JS AND MARKUP OUT OF 
THIS FILE AND ITS INCLUDES

--%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

	String dojoPath = Config.getStringProperty("path.to.dojo");
	if(!UtilMethods.isSet(dojoPath)){
		// Change dojopath in dotmarketing-config.properties!
		response.sendError(500, "No dojo path variable (path.to.dojo) set in the property file");
	}
	String agent = request.getHeader("User-Agent");
	response.setHeader("Cache-Control","no-store");
	response.setHeader("Pragma","no-cache");
	response.setHeader("Expires","01 Jan 2000 00:00:00 GMT");

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:bi="urn:bi" xmlns:csp="urn:csp">
<head>
	<meta http-equiv="x-ua-compatible" content="IE=edge" >
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	
	<meta content="no-cache" http-equiv="Cache-Control" />
	<meta content="no-cache" http-equiv="Pragma" />
	<meta content="0" http-equiv="Expires" />
	<meta name="Expire" content="Now" />
	
	<link rel="shortcut icon" href="//www.dotcms.com/global/favicon.ico" type="image/x-icon" />
	<title><%= DOTCMS_PORTAL_PAGE_TITLE %></title>
	 
	<style type="text/css">
		@import "/html/common/css.jsp?b=<%= ReleaseInfo.getVersion() %>"; 
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css?b=<%= ReleaseInfo.getVersion() %>";
        @import "<%=dojoPath%>/dijit/themes/dmundra/Grid.css?b=<%= ReleaseInfo.getVersion() %>";
        @import "<%=dojoPath%>/dojox/widget/Calendar/Calendar.css?b=<%= ReleaseInfo.getVersion() %>";
        @import "/html/js/dotcms/dijit/image/image_tools.css?b=<%= ReleaseInfo.getVersion() %>";

    </style>
	
	<!--[if IE]>
		<link rel="stylesheet" type="text/css" href="/html/css/iehacks.css" />
	<![endif]--> 
    
    <%
    String dojoLocaleConfig = "locale:'en-us'";    
    if(locale != null){
    	dojoLocaleConfig = "locale:'"+locale.getLanguage() + "-" + locale.getCountry().toLowerCase() + "',";    	
    }    
    %>
   	    
   	<script type="text/javascript">
       	djConfig={
               parseOnLoad: true,
               useXDomain: false,
               isDebug: false,
               <%=dojoLocaleConfig%>
               modulePaths: { dotcms: "/html/js/dotcms" }
       };
	   
	   	function isInodeSet(x){
			return (x && x != undefined && x!="" && x.length>15);
		}
   	</script>

	<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/html/common/javascript.jsp?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/engine.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/util.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/TemplateAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/HostAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/ContainerAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/RoleAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/BrowserAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/UserAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>
	<script type="text/javascript" src="/dwr/interface/InodeAjax.js?b=<%= ReleaseInfo.getVersion() %>"></script>

    <script type="text/javascript">
		dojo.require("dijit.Dialog");
		dojo.require("dijit.form.Button");
		dojo.require("dijit.form.CheckBox");
		dojo.require("dijit.form.DateTextBox");
		dojo.require("dijit.form.FilteringSelect");
		dojo.require("dijit.form.TextBox");
		dojo.require("dijit.form.ValidationTextBox");
		dojo.require("dijit.form.Textarea");
		dojo.require("dijit.Menu");
		dojo.require("dijit.MenuItem");
		dojo.require("dijit.MenuSeparator");
		dojo.require("dijit.ProgressBar");
		dojo.require("dijit.PopupMenuItem");
		dojo.require('dijit.layout.TabContainer');
		dojo.require('dijit.layout.ContentPane');
		dojo.require('dojox.layout.ContentPane');
		dojo.require("dijit.layout.BorderContainer");
		dojo.require("dijit.TitlePane");
		dojo.require("dijit.Tooltip");
		dojo.require("dojo.parser");
		dojo.require("dojo.fx");
		dojo.require("dotcms.dojo.data.UsersReadStore");
		dojo.require("dojox.form.DropDownSelect");
		dojo.require("dojox.json.query");
		dojo.require("dijit.form.NumberTextBox");
		dojo.require("dijit.form.TimeTextBox");
		dojo.require("dotcms.dijit.image.ImageEditor");
		dojo.require("dojox.widget.Calendar");
		dojo.require("dojo.date.locale");
		dojo.require("dojox.form.Uploader");
        dojo.require("dojox.form.uploader.FileList");
        dojo.require("dojox.form.uploader.plugins.HTML5");
        dojo.require("dijit.form.ComboBox");
   		dojo.require("dijit.TooltipDialog");
		dojo.require("dojox.grid.EnhancedGrid");
		dojo.require("dojox.grid.enhanced.plugins.Menu");
		dojo.require("dojo.data.ItemFileReadStore");
		dojo.require("dijit.form.Form");
		dojo.require("dojo.io.script");
		
		dojo.addOnLoad(function () {
			dojo.global.DWRUtil = dwr.util;
			dojo.global.DWREngine = dwr.engine;
			dwr.engine.setErrorHandler(DWRErrorHandler);
			dwr.engine.setWarningHandler(DWRErrorHandler);
		});

		function DWRErrorHandler(msg, e) {
			console.log(msg, e);
		}
		var dojoDom=dojo.require("dojo.dom");
		var dojoDomGeometry=dojo.require("dojo.dom-geometry");
		var dojoStyle=dojo.require("dojo.dom-style");
		dojo.coords = function(elem,xx) {
            var mb=dojoDomGeometry.getMarginBox(elem,dojoStyle.getComputedStyle(elem));
            var abs=dojoDomGeometry.position(elem,xx);
            mb.x=abs.x;
            mb.y=abs.y;
            mb.w=abs.w;
            mb.h=abs.h;
            return mb;
        };
		
	</script>
	<% String dotBackImage = (!UtilMethods.isSet(company.getHomeURL()) || "localhost".equals(company.getHomeURL())) ? "/html/images/backgrounds/bg-3.jpg" : company.getHomeURL();%>
	<style>
		.imageBG{background-color:<%= company.getSize() %>;background-image:url(<%= dotBackImage %>);background-repeat:no-repeat;background-position:top center;background-size:100% auto;height:75px;position:absolute;top:0;left:0;width:100%;z-index:-2;}
	</style>

	
</head>

<%if(UtilMethods.isSet(request.getParameter("popup")) || UtilMethods.isSet(request.getAttribute("popup")) || UtilMethods.isSet(request.getParameter("in_frame"))){ %>
	<body class="dmundra" style="background:white url()">
<%}else{ %>
	<body class="dmundra" style="visibility:hidden">
		<div class="imageBG"></div>
		<div class="bannerBG"></div>
<%} %>

