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

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;

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
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutAPI#loadLayout(java.lang.String)
	 */
	@Override
	@CloseDBIfOpened
	public Layout loadLayout(final String layoutId) throws DotDataException {
		return layoutFactory.loadLayout(layoutId);
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
	}

	@WrapInTransaction
	@Override
	public void setPortletIdsToLayout(Layout layout, List<String> portletIds) throws DotDataException {
		layoutFactory.setPortletsToLayout(layout, portletIds);
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
	
}
