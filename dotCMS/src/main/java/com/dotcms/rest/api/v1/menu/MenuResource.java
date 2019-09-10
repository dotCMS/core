package com.dotcms.rest.api.v1.menu;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.AccessControlAllowOrigin;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates the Menu Resource
 * Created by freddyrodriguez on 18/5/16.
 * @author freddyrodriguez
 * @author jsanca
 */
@Path("/v1/menu")
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

	/**
	 * Get the layout menus and sub-menus that the logged in a user have access
	 *
	 * @return a collection of menu portlet
	 * @throws NoSuchUserException    If the user doesn't exist
	 * @throws DotDataException       If there is a data inconsistency
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
	public Response getMenus(@Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws LanguageException, ClassNotFoundException
	{


		Response res;
		final Collection<Menu> menus = new ArrayList<Menu>();


		try {

	    final User user = new WebResource.InitBuilder(this.webResource)
	    .requestAndResponse(httpServletRequest, httpServletResponse)
	    .requiredBackendUser(true)
	    .rejectWhenNoUser(true)
	    .init().getUser();
	    
			final List<Layout> layouts = this.layoutAPI.loadLayoutsForUser(user);
			final MenuContext menuContext = new MenuContext(httpServletRequest, user);

			for (int layoutIndex = 0; layoutIndex < layouts.size(); layoutIndex++) {

				Layout layout = layouts.get(layoutIndex);
				String tabName = LanguageUtil.get(user, layout.getName());
				String tabIcon = StringEscapeUtils.escapeHtml(StringEscapeUtils
						.escapeJavaScript(LanguageUtil.get(user, layout.getDescription())));
				List<String> portletIds = layout.getPortletIds();

				if (null != portletIds && portletIds.size() > 0) {

					menuContext.setLayout(layout);
					menuContext.setPortletId(portletIds.get(0));
					menuContext.setLayoutIndex(layoutIndex);

					final String url = this.menuHelper.getUrl(menuContext);
					final Menu menu = new Menu(tabName, tabIcon, url, layout);

					final List<MenuItem> menuItems = this.menuHelper.getMenuItems(menuContext);

					menu.setMenuItems(menuItems);
					menus.add(menu);
				}
			}

			res = Response.ok(new ResponseEntityView(menus)).build(); // 200

		} catch (DotDataException | NoSuchUserException e) {

			res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      res = ExceptionMapperUtil.createResponse(new ForbiddenException(e), Response.Status.INTERNAL_SERVER_ERROR);

    }

		return res; //menus;
	} // getMenus.

} // E:O:F:MenuResource.
