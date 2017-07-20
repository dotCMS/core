<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.web.HostWebAPI"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.URLUtils"%>
<%@page import="com.dotmarketing.util.URLUtils.ParsedURL"%>

<%@page import="com.liferay.util.ParamUtil"%>
<%@page import="com.dotmarketing.portlets.common.bean.CrumbTrailEntry"%>
<%@page import="java.util.ArrayList"%>
<%

	boolean inPopupIFrame = UtilMethods.isSet(ParamUtil.getString(request, WebKeys.POPUP)) ||(UtilMethods.isSet(ParamUtil.getString(request, WebKeys.IN_FRAME)) && "true".equals(ParamUtil.getString(request, WebKeys.IN_FRAME)));
    boolean isAngularFrame = (UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME)) && (Boolean)request.getSession().getAttribute(WebKeys.IN_FRAME)) && UtilMethods.isSet(request.getSession().getAttribute(WebKeys.FRAME)) && !UtilMethods.isSet(ParamUtil.getString(request, WebKeys.HIDE_SUBNAV));


		UserAPI userAPI = APILocator.getUserAPI();
		HostWebAPI hostApi = WebAPILocator.getHostWebAPI();
	
		Boolean dontDisplayAllHostsOption = (Boolean) request
				.getAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS);
		if (dontDisplayAllHostsOption == null) {
			dontDisplayAllHostsOption = false;
		}
	
		boolean showHostSelector = request.getAttribute("SHOW_HOST_SELECTOR") != null;
		String hostId = (String)request.getAttribute("_crumbHost");
	
		List<CrumbTrailEntry> crumbTrailEntries = (request
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS) != null) ? (List<CrumbTrailEntry>) request
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)
				: new ArrayList<CrumbTrailEntry>();
				
	
	    if(!UtilMethods.isSet(hostId)){
		   hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	    }
		Host currentHost = null;
		String hostName = null;
	
		try{
			currentHost = hostApi.find(hostId, user, false);
			hostName = currentHost.getTitle();
		}
		catch(Exception e){
			try{
				currentHost=hostApi.findDefaultHost(user, false);
				hostName = currentHost.getTitle();
			}
			catch(Exception ex){
				com.dotmarketing.util.Logger.error(this.getClass(), "user does not have a default host");
			}
		}
		
	
	
		String _browserCrumbUrl = null;
		boolean canManageHosts = false;
		String _hostManagerUrl = null;
	
		if(layouts ==null){
			List<Layout> userHasLayouts = (List<Layout>) APILocator.getLayoutAPI().loadLayoutsForUser(user);
			layouts = userHasLayouts.toArray(new Layout[userHasLayouts.size()]);
		}
		// if we have a host, get the url for the browser
		for (int i = 0; i < layouts.length; i++) {
			List<String> portletIDs = layouts[i].getPortletIds();
			for (String x : portletIDs) {
				if ("site-browser".equals(x)) {
					_browserCrumbUrl = new PortletURLImpl(request, x, layouts[i].getId(), false).toString();
				}
				if ("sites".equals(x)) {
					canManageHosts = true;
					_hostManagerUrl = new PortletURLImpl(request, x, layouts[i].getId(), false).toString();
				}
			}
		}
	
%>


<div class="clear"></div>




