/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.InodeUtils;

/**
 * @author Jason Tesser
 *
 */
public class LayoutFactoryImpl extends LayoutFactory {

	private LayoutCache lc = CacheLocator.getLayoutCache();
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutFactory#addPortletsToLayout(com.dotmarketing.business.Layout, java.util.List)
	 */
	@Override
	protected void setPortletsToLayout(Layout layout, List<String> portletIds) throws DotDataException {
		lc.remove(layout);
		int count = 1;
		deletePortletsFromLayout(layout);
		for (String portletId : portletIds) {
			PortletsLayouts pl = new PortletsLayouts();
			pl.setLayoutId(layout.getId());
			pl.setPortletId(portletId);
			pl.setPortletOrder(count);
			HibernateUtil.save(pl);
			count++;
		}
		layout.setPortletIds(portletIds);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutFactory#loadLayout(java.lang.String)
	 */
	@Override
	protected Layout loadLayout(String layoutId) throws DotDataException {
		Layout l = null;
		if(layoutId ==null || layoutId.length() <1){
			return null;
		}
		l = lc.get(layoutId);
		if(l == null || !InodeUtils.isSet(l.getId())){
			HibernateUtil hu = new HibernateUtil(Layout.class);
			hu.setQuery("from com.dotmarketing.business.Layout where id = ?");
			hu.setParam(layoutId);
			l = (Layout)hu.load();
		
			if(l != null && InodeUtils.isSet(l.getId())){
				populatePortlets(l);
				lc.add(layoutId, l);
			}
		}
		return l;
	}
	

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutFactory#loadLayout(java.lang.String)
	 */
	@Override
	protected Layout findLayout(String layoutId) throws DotDataException {
		Layout l = null;

		HibernateUtil hu = new HibernateUtil(Layout.class);
		hu.setQuery("from com.dotmarketing.business.Layout where id = ?");
		hu.setParam(layoutId);
		l = (Layout)hu.load();
	
		return l;
	}
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutFactory#removeLayout(com.dotmarketing.business.Layout)
	 */
	@Override
	protected void removeLayout(Layout layout) throws DotDataException {
		lc.remove(layout);

		deletePortletsFromLayout(layout);
		deleteRolesFromLayout(layout);
		HibernateUtil.delete(layout);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.LayoutFactory#saveLayout(com.dotmarketing.business.Layout)
	 */
	@Override
	protected void saveLayout(Layout layout) throws DotDataException {
		lc.remove(layout);
		HibernateUtil.save(layout);
		populatePortlets(layout);
		lc.add(layout.getId(), layout);
	}

	@SuppressWarnings("unchecked")
	private void populatePortlets(Layout layout) throws DotDataException{
		List<String> layouts = lc.getPortlets(layout);
		if(layouts !=null && layouts.size() > 0){
			layout.setPortletIds(layouts);
			return;
		}
		HibernateUtil hu = new HibernateUtil(PortletsLayouts.class);
		hu.setQuery("from com.dotmarketing.business.PortletsLayouts where layout_id = ? order by portlet_order");
		hu.setParam(layout.getId());
		List<PortletsLayouts> pls = hu.list();
		List<String> pids = new ArrayList<String>();
		if(pls != null && pls.size()>0){
			for (PortletsLayouts pl : pls) {
				pids.add(pl.getPortletId());
			}
			
		}
		layout.setPortletIds(pids);
		lc.addPortlets(layout, pids);
	}
	
	private void deletePortletsFromLayout(Layout layout) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from cms_layouts_portlets where layout_id = ?");
		dc.addParam(layout.getId());
		dc.loadResult();
	}

	@SuppressWarnings("unchecked")
	private void deleteRolesFromLayout(Layout layout) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL("select role_id from layouts_cms_roles where layout_id = ?");
		dc.addParam(layout.getId());
		List l =dc.loadResults();
		
		dc.setSQL("delete from layouts_cms_roles where layout_id = ?");
		dc.addParam(layout.getId());
		dc.loadResult();
		
		for(int i =0;i<l.size();i++){
			Map m= (Map) l.get(i);
			CacheLocator.getRoleCache().removeLayoutsOnRole((String) m.get("role_id"));
		}
		
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<Layout> findAllLayouts() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Layout.class);
		hu.setQuery("from com.dotmarketing.business.Layout order by tab_order");
		List<Layout> layouts = hu.list();
		for(Layout l : layouts) {
			this.populatePortlets(l);
		}
		return layouts;
	}

	@Override
	protected Layout findLayoutByName(String name) throws DotDataException {

		HibernateUtil hu = new HibernateUtil(Layout.class);
		hu.setQuery("from com.dotmarketing.business.Layout where layout_name = ?");
		hu.setParam(name);
		return (Layout)hu.load();
	
	}
	
}
