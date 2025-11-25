/**
 * 
 */
package com.dotmarketing.business;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.base.Splitter;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import static com.dotmarketing.business.PermissionAPI.PermissionableType.CONTENTLETS;
import static com.dotmarketing.business.PermissionAPI.PermissionableType.HTMLPAGES;

/**
 * @author jasontesser
 *
 */
public class LayoutAPIImpl implements LayoutAPI {

	private final LayoutFactory layoutFactory = FactoryLocator.getLayoutFactory();

	private static void accept(Throwable e) {
		throw new DotRuntimeException(e);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#addPortletsToLayout(com.dotmarketing.business.Layout, java.util.List)
	 */
	@Override
	@WrapInTransaction
	public void setPortletsToLayout(final Layout layout, final List<Portlet> portlets) throws DotDataException {
		List<String> portletIds = new ArrayList<>();
		for(Portlet p : portlets) {
			portletIds.add(p.getPortletId());
		}
		
		layoutFactory.setPortletsToLayout(layout, portletIds);
		APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#loadLayout(java.lang.String)
	 */
	@Override
	@CloseDBIfOpened
	public Layout loadLayout(final String layoutId) throws DotDataException {
		return layoutFactory.loadLayout(layoutId);
	}


  @Override
  @CloseDBIfOpened
  public Optional<Layout> resolveLayout(final HttpServletRequest request) {

    Layout layout = Try.of(() -> loadLayout(request.getParameter("p_l_id"))).getOrNull();
    if (layout == null || UtilMethods.isNotSet(layout.getId())) {
      final String referer = request.getHeader("referer");
      if (referer != null && referer.indexOf("?") > -1) {
        for (String x : Splitter.on('&').trimResults().split(referer.split("\\?")[1])) {
          if (x.startsWith("p_l_id=")) {
            layout = Try.of(() -> loadLayout(x.replace("p_l_id=", ""))).getOrNull();
            break;
          }
        }
      }
    }
    if(layout==null) {
      final String lastLayout = (String) request.getSession().getAttribute(WebKeys.LAYOUT_PREVIOUS);
      layout = Try.of(() -> loadLayout(lastLayout)).getOrNull();
    
    }

    if(layout!=null) {
      request.getSession().setAttribute(WebKeys.LAYOUT_PREVIOUS, layout.getId());
    }
    
    return Optional.ofNullable(layout);

  }

	@Override
	@CloseDBIfOpened
	public Optional<LayoutsRoles> findLayoutByRole(final Layout layout, final Role role) {

		return FactoryLocator.getRoleFactory().findLayoutsRole(layout, role);
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#loadLayout(java.lang.String)
	 */
	@Override
	@CloseDBIfOpened
	public Layout findLayout(final String layoutId) throws DotDataException {
		return layoutFactory.findLayout(layoutId);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#removeLayout(com.dotmarketing.business.Layout)
	 */
	@Override
	@WrapInTransaction
	public void removeLayout(final Layout layout) throws DotDataException {
		layoutFactory.removeLayout(layout);
        //Send a websocket event to notificate a layout change  
        APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#saveLayout(com.dotmarketing.business.Layout)
	 */
	@Override
	@WrapInTransaction
	public void saveLayout(Layout layout) throws LayoutNameAlreadyExistsException, DotDataException {
		Layout oldLayout = layoutFactory.findLayoutByName(layout.getName());
		if(UtilMethods.isSet(oldLayout) && UtilMethods.isSet(oldLayout.getId()) && !oldLayout.getId().equals(layout.getId()))
			throw new LayoutNameAlreadyExistsException("Layout with name: " + layout.getName() + " already exists in the system, " +
					"cannot save a new layout using the same name");
		
		
		
		layoutFactory.saveLayout(layout);
		APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}

	@WrapInTransaction
	@Override
	public void setPortletIdsToLayout(Layout layout, List<String> portletIds) throws DotDataException {
		layoutFactory.setPortletsToLayout(layout, portletIds);
	    APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload(
				Map.of("toolgroup", layout, "menuItems", portletIds.toArray())));
	}

	@Override
	@CloseDBIfOpened
	public List<Layout> loadLayoutsForUser(final User user) throws DotDataException {
		
		final List<Role> urs = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), false);
		final Set<String> lids = new HashSet<>();
		for (Role role : urs) {
			 lids.addAll(APILocator.getRoleAPI().loadLayoutIdsForRole(role));	
		}
		List<Layout> layouts = new ArrayList<>();
		for (String lid : lids) {
			layouts.add(loadLayout(lid));
		}
		Collections.sort(layouts, new Comparator<Layout>() {
			public int compare(Layout l1, Layout l2) {
				return Integer.valueOf(l1.getTabOrder()).compareTo(Integer.valueOf(l2.getTabOrder()));
			}
		});
		return layouts;
	}


	/* This method is used to check if the user has access to edit the page portlet.
	 * All the users should have access to Edit Page, regardless of the assigned portlets.
	 * To determine if the user has access to edit page, we check if the user can edit HTMLPAGES or CONTENTLETS
	 */
	private boolean doesUserHaveAccessEditPagePortlet(User user) throws DotDataException {
		final PermissionAPI permAPI = APILocator.getPermissionAPI();
		return permAPI.doesUserHavePermissions(HTMLPAGES, PermissionAPI.PERMISSION_EDIT, user) ||
				permAPI.doesUserHavePermissions(CONTENTLETS, PermissionAPI.PERMISSION_EDIT, user);
	}

	@Override
	public boolean doesUserHaveAccessToPortlet(final String portletId, final User user) throws DotDataException {
		if(portletId==null || user==null || !user.isBackendUser()) {
		  return false;
		}
		if(loadLayoutsForUser(user).stream(). anyMatch(layout -> layout.getPortletIds().contains(portletId))){
			return true;
		}
		if("edit-page".equals(portletId) && doesUserHaveAccessEditPagePortlet(user)){
			return true;
		}
		return APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
	}

	@Override
	@CloseDBIfOpened
	public List<Layout> findAllLayouts() throws DotDataException {
		return layoutFactory.findAllLayouts();
	}

	@Override
	public List<Layout> loadLayoutsForRole(Role role) throws DotDataException {
		Set<String> lids = new HashSet<>();
		lids.addAll(APILocator.getRoleAPI().loadLayoutIdsForRole(role));	
		List<Layout> layouts = new ArrayList<>();
		for (String lid : lids) {
			layouts.add(loadLayout(lid));
		}
		Collections.sort(layouts, new Comparator<Layout>() {
			public int compare(Layout l1, Layout l2) {
				return Integer.valueOf(l1.getTabOrder()).compareTo(Integer.valueOf(l2.getTabOrder()));
			}
		});
		return layouts;	
	}

	public Layout findLayoutByName(String name) throws DotDataException {
		return layoutFactory.findLayoutByName(name);
	}
	

	@CloseDBIfOpened
	@Override
    public Layout findGettingStartedLayout() {
	    final Layout layout = Try.of(() -> findLayoutByName(GETTING_STARTED_LAYOUT_NAME)).getOrElseThrow(e->new DotRuntimeException(e));
	    return layout.getPortletIds().isEmpty()  ? this.createGettingStartedLayout() : layout ;
    }
	
    @WrapInTransaction
    private synchronized Layout createGettingStartedLayout() {
        final Layout gettingStarted = new Layout();
        gettingStarted.setId(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        gettingStarted.setName(LayoutAPI.GETTING_STARTED_LAYOUT_NAME);
        gettingStarted.setDescription("whatshot");
        gettingStarted.setPortletIds(List.of("starter"));
        gettingStarted.setTabOrder(-320000);
        Try.run(() -> {
            layoutFactory.saveLayout(gettingStarted);
            layoutFactory.setPortletsToLayout(gettingStarted, List.of("starter"));
        }).onFailure(LayoutAPIImpl::accept);

        return gettingStarted;
    }
	
    @Override
	@WrapInTransaction
	public void addLayoutForUser(final Layout layout, final User user) throws DotDataException {
		if(user==null || UtilMethods.isNotSet(user.getUserId())){
			Logger.error(this.getClass(),"User is null");
			throw new DotDataException("User is null");
		}
		if(layout==null || UtilMethods.isNotSet(layout.getId())){
			Logger.error(this.getClass(),"ToolGroup is not valid");
			throw new DotDataException("ToolGroup is not valid");
		}
		if(UtilMethods.isSet(findLayoutByRole(layout,user.getUserRole()).get().getLayoutId())){
			Logger.info(this.getClass(),"ToolGroup is already set");
			return;
		}

		APILocator.getRoleAPI().addLayoutToRole(layout, user.getUserRole());
	    APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}
	
	
	
	
	
	
	
}
