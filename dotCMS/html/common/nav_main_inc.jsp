<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.rest.BaseRestPortlet"%>
<%@page import="com.liferay.portal.model.Portlet"%>
<%@page import="com.dotcms.rest.WebResource"%>
<%@page import="com.dotmarketing.business.APILocator"%>

<%@page import="java.util.List"%><%@page import="com.dotmarketing.util.UtilMethods"%>
<script>
var portletTabMap = {}; // this holds a Map of portletId, tabId, used when refreshing to retrieve the proper tabId based on the portletId

</script>
<div id="menu">
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
                
                Portlet portlet = (Portlet) APILocator.getPortletAPI().findPortlet(portletIDs.get(0));
                Object object = Class.forName(portlet.getPortletClass()).newInstance();
                if(object instanceof BaseRestPortlet){
                    tabHREF =  "javascript:dotAjaxNav.show('/api/portlet/"+ portletIDs.get(0) + "/', '" + l + "');";
            	}
                
                %> 



                        <li class="dotAjaxNav<%=l %> level1 <%=(isSelectedTab) ? "Active" : ""%>">
                                <a href="<%=tabHREF %>">
                                        <div class="tabLeft">
                                                <div class="navMenu-title"><%=tabName %></div>
                                                <div class="navMenu-subtitle"><%=tabDescription %></div>
                                                <div class="navMenu-arrow">&nbsp;</div>
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






<script>

dojo.require("dojo.hash");
        //
        //
        // -------------------- AJAX NAVIGATION --------------------
        //
        //

        dojo.declare("dotcms.dijit.dotAjaxNav", null, {
                contentDiv : "dotAjaxMainDiv",
                hangerDiv : "dotAjaxMainHangerDiv",

                wfCrumbTrail : new Array(),

                constructor : function() {},

                show : function(href, tabId) {
                	
                        var r = Math.floor(Math.random() * 1000000000);
                        if (href.indexOf("?") > -1) {
                                href = href + "&r=" + r;
                        } else {
                                href = href + "?r=" + r;
                        }
                        dojo.hash(encodeURIComponent(href));

                        // if we need to update the tabs
                        if(tabId && tabId != undefined){
                                dojo.query(".level1 .Active").forEach(function(node){
                                        dojo.removeClass(node, "Active");
                                  });

                                dojo.query(".dotAjaxNav" + tabId).forEach(function(node){
                                        dojo.addClass(node, "Active");
                                  });
                        }

                },



                reload : function(){
                        if(dojo.hash()  ){
	                        var hashValue = decodeURIComponent(dojo.hash());
	                        var portletId = hashValue.split("/api/portlet/")[1];
	                        if(portletId){
		                        portletId = portletId.substring(0, portletId.indexOf("/"));
		                        dotAjaxNav.show(hashValue, portletTabMap[portletId]);
	                        }
                        }
                },


                refresh : function() {

                        var hashValue = decodeURIComponent(dojo.hash());

                        if(hashValue.indexOf("donothing")>0) {
                        	return;
                        }

                        console.log("refreshing:" + hashValue);
                        if(!hashValue || hashValue.length ==0){
                                return;
                        }

                        var myCp = dijit.byId(this.contentDiv);
                        var hanger = dojo.byId(this.hangerDiv);
                        if(!hanger){
                                return;
                        }
                        if (myCp) {
                                myCp.destroyRecursive();
                                myCp.attr("content","");
                        }

                        myCp = new dojox.layout.ContentPane({
                                id : this.contentDiv
                        }).placeAt(this.hangerDiv);


                        console.log("navigating to:" + hashValue)
                        myCp.attr("href", hashValue);
                        //myCp.refresh(); GIT-7098

                        dojo.parser.parse(this.hangerDiv);
                },

                
                refreshHTML : function(html) {



                    var myCp = dijit.byId(this.contentDiv);
                    var hanger = dojo.byId(this.hangerDiv);
                    if(!hanger){
                            return;
                    }
                    if (myCp) {
                            myCp.destroyRecursive();
                            myCp.attr("content","");
                    }

                    myCp = new dojox.layout.ContentPane({
                            id : this.contentDiv
                    }).placeAt(this.hangerDiv);


                    myCp.attr("content", html);

                    dojo.parser.parse(this.hangerDiv);
            },
            
                addCrumbtrail : function (title, urlx){
                        var entry = {title:title, url:urlx};
                        this.wfCrumbTrail[this.wfCrumbTrail.length] = entry;
                },


                resetCrumbTrail : function(){
                        this.wfCrumbTrail = new Array();
                },

                refreshCrumbtrail : function (){
                        var crumbDiv = dojo.byId("subNavCrumbUl");
                        crumbDiv.innerHTML ="";
                        // dojo.create("li",
                        // {onClick:this.show(this.wfCrumbTrail[i].url)},crumbDiv )

                        dojo.create("li", {innerHTML:"<span class='hostStoppedIcon' style='float:left;margin-right:5px;'></span><%=LanguageUtil.get(pageContext, "Global-Page")%>", id:"selectHostDiv", onClick:"window.location='/c'"},crumbDiv );
                        for( i =0;i< this.wfCrumbTrail.length;i++ ){
                                var className="showPointer";
                                if(i+1 ==this.wfCrumbTrail.length){
                                        dojo.create("li", {innerHTML:"<b>" + dotAjaxNav.wfCrumbTrail[i].title + "</b>", className:"lastCrumb"},crumbDiv );
                                }
                                else{
                                        dojo.create("li", {innerHTML:"<a href='javascript:dotAjaxNav.show(dotAjaxNav.wfCrumbTrail[" + i + "].url)'>" + dotAjaxNav.wfCrumbTrail[i].title + "</a>", className:className},crumbDiv );
                                }

                        }


                }

        });

        var dotAjaxNav = new dotcms.dijit.dotAjaxNav({});

        dojo.subscribe("/dojo/hashchange", this, function(hash){dotAjaxNav.refresh();});




        var _myWindowWidth=0;

        function smallifyMenu(){

                // move our menu out of sight for rendering....
                var m = dojo.byId("menu");

                var viewport = dijit.getViewport();
                var  screenWidth= (viewport.w -40);
                //alert(screenWidth);
                if(_myWindowWidth == screenWidth){
                        return;
                }
                _myWindowWidth = screenWidth;
                // tabW keeps track of the tab width
                var tabW = 0;
                var fattestTab =0;
                var tabs = dojo.query("li.level1");
                for(i = 0;i<tabs.length;i++){
                        var x = tabs[i];
                        var classes = dojo.attr(x, "class");
                        classes = classes.replace(" smallify", "");
                        dojo.attr(x, "class", classes);
                        width =  (dojo.coords(x)).w;
                        if(width > fattestTab){
                                fattestTab=width;
                        }
                        tabW = tabW + (dojo.coords(x)).w;
                }
                screenWidth = screenWidth - fattestTab;
                //alert(fattestTab);
                // get the top of our menu (to see if we are wrapping)
                var firstTop = (dojo.coords(tabs[0])).t;
                var lastTop = (dojo.coords(tabs[(tabs.length-1)])).t;
                if(tabW > screenWidth || lastTop > firstTop){
                        for(i = tabs.length;i>0;i--){
                                lastTop = (dojo.coords(tabs[(tabs.length-1)])).t;
                                var x = tabs[i-1];

                                var width = (dojo.coords(x)).w;
                                tabW = tabW-width;
                                var classes = dojo.attr(x, "class");

        <%--
                                alert("tab:\t" + (i-1) +
                                        "\ntabW:\t" + tabW +
                                        "\nscreenWidth:\t" + screenWidth +
                                        "\nfirstTop:\t" + firstTop +
                                        "\nlastTop:\t" + lastTop +

                                        "\nwidth:\t" + width);
        --%>

                                if(tabW > screenWidth || lastTop > firstTop){
                                       /*  classes = classes + " smallify";
                                        dojo.attr(x, "class", classes);
                                        width = (dojo.coords(x)).w;
                                        tabW = tabW+width; */
                                }else{
                                        break;
                                }
                        }
                }


        }
        dojo.addOnLoad (smallifyMenu);
        dojo.addOnLoad(function(){
            dotAjaxNav.reload();
        });
        dojo.connect(window, "onresize", this, "smallifyMenu");



</script>