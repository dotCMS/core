package com.dotcms.rest.api.v1.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.spring.portlet.PortletController;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.JSPPortlet;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.portlet.StrutsPortlet;

/**
 * Encapsulates the Menu Resource
 * Created by freddyrodriguez on 18/5/16.
 * @author freddyrodriguez
 * @author jsanca
 */
@Path("/v1/{from}/menu")
public class MenuResource implements Serializable {

	private final LayoutAPI layoutAPI;
	private final UserAPI userAPI;
	private final WebResource webResource;
	private final MenuHelper menuHelper;

	public MenuResource() {

		this(APILocator.getLayoutAPI(), APILocator.getUserAPI(),
				MenuHelper.INSTANCE, new WebResource(new ApiProvider()));
	}

	@VisibleForTesting
	public MenuResource(final LayoutAPI layoutAPI,
						final UserAPI userAPI,
						final MenuHelper menuHelper,
						final WebResource webResource) {

		this.layoutAPI   = layoutAPI;
		this.userAPI     = userAPI;
		this.menuHelper  = menuHelper;
		this.webResource = webResource;
	}

	public enum App{CORE, CORE_WEB};

	/**
	 *
	 * Get the layout menus and sub-menus that the logged in a user have access
	 * @return  a collection of menu portlet
	 * @throws NoSuchUserException If the user doesn't exist
	 * @throws DotDataException If there is a data inconsistency
	 * @throws DotSecurityException
	 * @throws LanguageException
	 * @throws ClassNotFoundException If the portet class is not assignable to PortletController or BaseRestPortlet
	 */
	@GET
	@JSONP
	@NoCache
	@AccessControlAllowOrigin
	@InitRequestRequired
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response getMenus(@PathParam("from") final String from,
							 @Context final HttpServletRequest httpServletRequest) throws DotSecurityException, LanguageException, ClassNotFoundException
	{

		this.webResource.init(true, httpServletRequest, true);

		Response res = null;
		final Collection<Menu> menus = new ArrayList<Menu>();
		final HttpSession session = httpServletRequest.getSession();

		try {

			final App appFrom = App.valueOf(from.toUpperCase());
			final User user = this.userAPI.loadUserById((String) session.getAttribute(WebKeys.USER_ID));
			final List<Layout> layouts = this.layoutAPI.loadLayoutsForUser(user);

			final MenuContext menuContext = new MenuContext(httpServletRequest, user, appFrom);

			for (int layoutIndex = 0; layoutIndex < layouts.size(); layoutIndex++) {

				Layout layout = layouts.get( layoutIndex );
				String tabName = LanguageUtil.get(user, layout.getName());
				String tabDescription = LanguageUtil.get(user, layout.getDescription());
				List<String> portletIds = layout.getPortletIds();

				menuContext.setLayout(layout);
				menuContext.setPortletId(portletIds.get(0));
				menuContext.setLayoutIndex(layoutIndex);

				final String url = this.menuHelper.getUrl(menuContext);
				final Menu menu = new Menu( tabName, tabDescription, url );

				final List<MenuItem> menuItems = this.menuHelper.getMenuItems(menuContext);

				menu.setMenuItems( menuItems );
				menus.add( menu  );
			}

			res = Response.ok(new ResponseEntityView(menus)).build(); // 200
		} catch (DotDataException | NoSuchUserException e) {

			res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return res; //menus;
	} // getMenus.

} // E:O:F:MenuResource.
