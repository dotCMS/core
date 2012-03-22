/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

/**
 * @author jasontesser
 *
 */
public class LayoutAPIImpl implements LayoutAPI {

	private LayoutFactory lf = FactoryLocator.getLayoutFactory();
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#addPortletsToLayout(com.dotmarketing.business.Layout, java.util.List)
	 */
	public void setPortletsToLayout(Layout layout, List<Portlet> portlets) throws DotDataException {
		List<String> portletIds = new ArrayList<String>();
		for(Portlet p : portlets) {
			portletIds.add(p.getPortletId());
		}
		lf.setPortletsToLayout(layout, portletIds);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#loadLayout(java.lang.String)
	 */
	public Layout loadLayout(String layoutId) throws DotDataException {
		return lf.loadLayout(layoutId);
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#loadLayout(java.lang.String)
	 */
	public Layout findLayout(String layoutId) throws DotDataException {
		return lf.findLayout(layoutId);
	}
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#removeLayout(com.dotmarketing.business.Layout)
	 */
	public void removeLayout(Layout layout) throws DotDataException {
		lf.removeLayout(layout);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#saveLayout(com.dotmarketing.business.Layout)
	 */
	public void saveLayout(Layout layout) throws LayoutNameAlreadyExistsException, DotDataException {
		Layout oldLayout = lf.findLayoutByName(layout.getName());
		if(oldLayout != null && !oldLayout.getId().equals(layout.getId()))
			throw new LayoutNameAlreadyExistsException("Layout with name: " + layout.getName() + " already exists in the system, " +
					"cannot save a new layout using the same name");
		
		
		
		lf.saveLayout(layout);
	}

	public void setPortletIdsToLayout(Layout layout, List<String> portletIds) throws DotDataException {
		lf.setPortletsToLayout(layout, portletIds);
	}

	public List<Layout> loadLayoutsForUser(User user) throws DotDataException {
		
		List<Role> urs = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), false);
		Set<String> lids = new HashSet<String>();
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

	public boolean doesUserHaveAccessToPortlet(String portletId, User user) throws DotDataException {
		List<Layout> layouts = loadLayoutsForUser(user);
		boolean hasAccess = false;
		for (Layout layout : layouts) {
			if(layout.getPortletIds().contains(portletId)){
				hasAccess = true;
				break;
			}
		}
		return hasAccess;
	}

	public List<Layout> findAllLayouts() throws DotDataException {
		return lf.findAllLayouts();
	}

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
	
}
