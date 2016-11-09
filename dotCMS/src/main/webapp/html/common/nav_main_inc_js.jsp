<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script type="text/javascript">

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
                                dojo.query(".level1 .active").forEach(function(node){
                                        dojo.removeClass(node, "active");
                                  });

                                dojo.query(".dotAjaxNav" + tabId).forEach(function(node){
                                        dojo.addClass(node, "active");
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

                        logger.debug("refreshing:" + hashValue);
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

                        dojo.style(hanger, "min-height", "400px");

                        logger.debug("navigating to:" + hashValue)
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

        dojo.subscribe("/dojo/hashchange", this, function(hash){
        	dotAjaxNav.refresh();
        	dojo.style(dojo.body(), "visibility", "visible");
        });




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