package com.dotcms.rest.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.spring.portlet.PortletController;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.JSPPortlet;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.portlet.StrutsPortlet;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
@Path("/{from}/menu")
public class MenuResource {

	public enum App{CORE, CORE_WEB};

	/**
	 * /**
	 * Get the layout menus and sub-menus that the logged in a user have access 
	 * @return  a collection of menu portlet
	 * @throws NoSuchUserException If the user doesn't exist
	 * @throws DotDataException If there is a data inconsistency
	 * @throws DotSecurityException
	 * @throws LanguageException
	 * @throws ClassNotFoundException If the portet class is not assignable to PortletController or BaseRestPortlet
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public Collection<Menu> getMenus(@Context HttpServletResponse response, @PathParam("from") String from, @Context HttpServletRequest httpServletRequest) throws NoSuchUserException, DotDataException, DotSecurityException, LanguageException, ClassNotFoundException 
	{

		//TODO include user validation

		response.setHeader("Access-Control-Allow-Origin", "*");
		App appFrom = App.valueOf(from.toUpperCase());
		Collection<Menu> menus = new ArrayList<Menu>();

		HttpSession session = httpServletRequest.getSession();
		LayoutAPI api= APILocator.getLayoutAPI();
		User user = APILocator.getUserAPI().loadUserById((String) session.getAttribute(WebKeys.USER_ID));

		List<Layout> layouts = api.loadLayoutsForUser(user);

		MenuContext menuContext = new MenuContext(httpServletRequest, user, appFrom);

		for (int layoutIndex = 0; layoutIndex < layouts.size(); layoutIndex++) {
			Layout layout = layouts.get( layoutIndex );
			String tabName = LanguageUtil.get(user, layout.getName());
			String tabDescription = LanguageUtil.get(user, layout.getDescription());
			List<String> portletIds = layout.getPortletIds();

			menuContext.setLayout(layout);
			menuContext.setPortletId(portletIds.get(0));
			menuContext.setLayoutIndex(layoutIndex);

			String url = getUrl(menuContext);
			Menu menu = new Menu( tabName, tabDescription, url );

			List<MenuItem> menuItems = getMenuItems(menuContext);

			menu.setMenuItems( menuItems );
			menus.add( menu  );
		}

		return menus;
	}

	/**
	 * Get the list of menu portlet items for the specified menucontext  
	 * @param menuContext
	 * @return a list of menu item associated to this context
	 * @throws LanguageException
	 * @throws ClassNotFoundException
	 */
	private List<MenuItem> getMenuItems(MenuContext menuContext)
			throws LanguageException, ClassNotFoundException {

		List<MenuItem> menuItems = new ArrayList<>();
		List<String> portletIds = menuContext.getLayout().getPortletIds();

		for (String portletId : portletIds) {
			menuContext.setPortletId( portletId );
			String linkHREF = getUrl(menuContext);
			String linkName = LanguageUtil.get(menuContext.getUser(), "com.dotcms.repackage.javax.portlet.title." + portletId);
			boolean isAngular = isAngular( portletId );
			boolean isAjax = isAjax( portletId );

			menuItems.add ( new MenuItem(portletId, linkHREF, linkName, isAngular, isAjax) );
		}
		return menuItems;
	}

	/**
	 * Validate if the portlet is a PortletController
	 * @param portletId Id of the portlet
	 * @return true if the portlet is a PortletController portlet, false if not
	 * @throws ClassNotFoundException
	 */
	private boolean isAngular(String portletId) throws ClassNotFoundException {
		Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
		String portletClass = portlet.getPortletClass();
		Class classs = Class.forName( portletClass );
		return PortletController.class.isAssignableFrom( classs );
	}

	/**
	 * Validate if the portlet is a BaseRestPortlet
	 * @param portletId Id of the portlet
	 * @return true if the portlet is a BaseRestPortlet portlet, false if not
	 * @throws ClassNotFoundException
	 */
	private boolean isAjax(String portletId) throws ClassNotFoundException {
		Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
		String portletClass = portlet.getPortletClass();
		Class classs = Class.forName( portletClass );
		return BaseRestPortlet.class.isAssignableFrom( classs );
	}

	/**
	 * Get the url of the menucontext
	 * @param menuContext 
	 * @return the portlet url
	 * @throws ClassNotFoundException
	 */
	private String getUrl(MenuContext menuContext) throws ClassNotFoundException {
		Portlet portlet = APILocator.getPortletAPI().findPortlet( menuContext.getPortletId() );

		String portletClass = portlet.getPortletClass();
		Class classs = Class.forName( portletClass );
		App appFrom = menuContext.getAppFrom();

		Logger.debug(MenuResource.class,"### getPortletId" + menuContext.getPortletId());
		Logger.debug(MenuResource.class,"### portletClass" + portletClass);
		if(StrutsPortlet.class.isAssignableFrom( classs ) || JSPPortlet.class.isAssignableFrom( classs )) {
			PortletURLImpl portletURLImpl = new PortletURLImpl(menuContext.getHttpServletRequest(),
					menuContext.getPortletId(), menuContext.getLayout().getId(), false);
			return portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();
		}else if(BaseRestPortlet.class.isAssignableFrom( classs )) {
			return "/api/portlet/" + menuContext.getPortletId();
		}else if(PortletController.class.isAssignableFrom( classs )){
			if (App.CORE.equals( appFrom )) {
				return "/spring/portlet/" + menuContext.getPortletId();
			}else{
				return "/html/ng/p/" + menuContext.getPortletId();
			}
		}

		return null;
	}  

}
