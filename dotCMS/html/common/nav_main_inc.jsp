<%@page import="java.util.List"%><%@page import="com.dotmarketing.util.UtilMethods"%>
<div id="menu">
	<ul class="level1 horizontal" id="root">

	<%for(int l=0;l< layouts.length ;l++){
		String tabName =LanguageUtil.get(pageContext, LanguageUtil.get(pageContext, layouts[l].getName())); 
		String tabDescription = (!UtilMethods.isSet(layouts[l].getDescription())) ? "&nbsp;" :layouts[l].getDescription() ;
		if(!tabDescription.equals("&nbsp;")){
		 	tabDescription = LanguageUtil.get(pageContext,tabDescription) ;
		 }
		
		
		List<String> portletIDs = layouts[l].getPortletIds();
		boolean isSelectedTab = (layout != null && layouts !=null && layout.getId().equals(layouts[l].getId()));
		PortletURLImpl portletURLImpl = new PortletURLImpl(request, portletIDs.get(0), layouts[l].getId(), false);
		String tabHREF = portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();%>
		
		
		
			<li class="level1 <%=(isSelectedTab) ? "Active" : ""%>">
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
							portletURLImpl = new PortletURLImpl(request, portletIDs.get(i), layouts[l].getId(), false);			
							String linkHREF = portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();
							String linkName = LanguageUtil.get(pageContext,"javax.portlet.title." + portletIDs.get(i)); 
							
							
							if("EXT_LICENSE_MANAGER".equals(portletIDs.get(i))){
								request.setAttribute("licenseManagerPortletUrl", linkHREF);
							}
							
							
							
							
							
							%>
							
							<li class="level2 dotCMS_<%=portletIDs.get(i)%>"><a href="<%=linkHREF %>"><span></span><%=linkName %></a></li>
						<%} %>
					</ul>
				<%}%>
			</li>
		<%}%>
	</ul>
</div>

<script>
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
					classes = classes + " smallify";
					dojo.attr(x, "class", classes);
					width = (dojo.coords(x)).w;
					tabW = tabW+width;
				}else{
					break;
				}
			}
		}
	
	
	}
	dojo.addOnLoad (smallifyMenu);
	dojo.connect(window, "onresize", this, "smallifyMenu");


</script>