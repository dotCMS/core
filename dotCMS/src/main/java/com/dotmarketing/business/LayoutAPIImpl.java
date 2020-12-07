/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.db.DotRunnableFlusherThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.base.Splitter;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.API;
import io.vavr.control.Try;

/**
 * @author jasontesser
 *
 */
public class LayoutAPIImpl implements LayoutAPI {

	private final LayoutFactory layoutFactory = FactoryLocator.getLayoutFactory();
	
	

	
	
	
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#addPortletsToLayout(com.dotmarketing.business.Layout, java.util.List)
	 */
	@Override
	@WrapInTransaction
	public void setPortletsToLayout(final Layout layout, final List<Portlet> portlets) throws DotDataException {
		List<String> portletIds = new ArrayList<String>();
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
	    APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	}

	@Override
	@CloseDBIfOpened
	public List<Layout> loadLayoutsForUser(final User user) throws DotDataException {
		
		final List<Role> urs = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), false);
		final Set<String> lids = new HashSet<String>();
		for (Role role : urs) {
			 lids.addAll(APILocator.getRoleAPI().loadLayoutIdsForRole(role));	
		}
		List<Layout> layouts = new ArrayList<Layout>();
		for (String lid : lids) {
			layouts.add(loadLayout(lid));
		}
		Collections.sort(layouts, new Comparator<Layout>() {
			public int compare(Layout l1, Layout l2) {
				return new Integer(l1.getTabOrder()).compareTo(new Integer(l2.getTabOrder()));
			}
		});
		return layouts;
	}

	@Override
	public boolean doesUserHaveAccessToPortlet(final String portletId, final User user) throws DotDataException {
          if(portletId==null || user==null || !user.isBackendUser()) {
              return false;
          }
		if(loadLayoutsForUser(user).stream(). anyMatch(layout -> layout.getPortletIds().contains(portletId))){
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
		Set<String> lids = new HashSet<String>();
		lids.addAll(APILocator.getRoleAPI().loadLayoutIdsForRole(role));	
		List<Layout> layouts = new ArrayList<Layout>();
		for (String lid : lids) {
			layouts.add(loadLayout(lid));
		}
		Collections.sort(layouts, new Comparator<Layout>() {
			public int compare(Layout l1, Layout l2) {
				return new Integer(l1.getTabOrder()).compareTo(new Integer(l2.getTabOrder()));
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

	    Layout layout = Try.of(() -> findLayout(GETTING_STARTED_LAYOUT_ID)).getOrElseThrow(e->new DotRuntimeException(e));
	    return layout.getPortletIds().isEmpty()  ? this.createGettingStartedLayout() : layout ;
	    

    }
	
    @WrapInTransaction
    private synchronized Layout createGettingStartedLayout() {
        final Layout layout = Try.of(() -> findLayout(GETTING_STARTED_LAYOUT_ID)).getOrElseThrow(e->new DotRuntimeException(e));
        if(!layout.getPortletIds().isEmpty()) {
            return layout;
        }
        
        Layout gettingStarted = new Layout();
        gettingStarted.setId(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        gettingStarted.setName("Getting Started");
        gettingStarted.setDescription("whatshot");
        gettingStarted.setPortletIds(Collections.singletonList("starter"));
        gettingStarted.setTabOrder(-320000);
        Try.run(() -> {
            layoutFactory.saveLayout(gettingStarted);
            layoutFactory.setPortletsToLayout(gettingStarted, Collections.singletonList("starter"));
        }).onFailure(e -> new DotRuntimeException(e));
        

        return gettingStarted;


    }
	
    @Override
	@WrapInTransaction
	public Layout addLayoutForUser(final Layout layout, final User user) {
	    Role role = user.getUserRole();	    
	    
	    Try.run(()->{
	        APILocator.getRoleAPI().addLayoutToRole(layout, role);
	        APILocator.getSystemEventsAPI().pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload());
	    }).onFailure(e->new DotRuntimeException(e));
	   
	     
	    return layout;
	}
	
	
	
	
	
	
	
}
