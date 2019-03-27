/**
 * 
 */
package com.dotmarketing.plugin.business;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.model.PluginProperty;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author jasontesser
 *
 */
public class PluginFactoryDBImpl extends PluginFactory {

	private PluginCache cache = CacheLocator.getPluginCache();
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#delete(com.dotmarketing.plugin.model.Plugin)
	 */
	@Override
	protected void delete(Plugin plugin) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from plugin_property where plugin_id = ?");
		dc.addParam(plugin.getId());
		dc.getResult();
		dc.setSQL("delete from plugin where id = ?");
		dc.addParam(plugin.getId());
		dc.getResult();
		cache.removePlugin(plugin.getId());
		cache.clearPropertyCache();
	}
	
	@Override
	protected void deletePluginProperties(String pluginId) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from plugin_property where plugin_id = ?");
		dc.addParam(pluginId);
		dc.getResult();
		cache.clearPropertyCache();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#loadPlugin(java.lang.String)
	 */
	@Override
	protected Plugin loadPlugin(String id) throws DotDataException{
		Plugin plugin = cache.get(id);
		if(plugin != null){
			return plugin;
		}
		HibernateUtil hu = new HibernateUtil(Plugin.class);
		try {
			plugin = (Plugin)hu.load(id);
			if(plugin != null && UtilMethods.isSet(plugin.getId())){
				cache.removePlugin(plugin.getId());
				cache.add(plugin);
			}
			return plugin;
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#loadPlugins()
	 */
	@Override
	protected List<Plugin> findPlugins() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Plugin.class);
		try{
			hu.setQuery("from Plugin");
			List<Plugin> plugins =  (List<Plugin>)hu.list();
			for (Plugin plugin : plugins) {
				cache.removePlugin(plugin.getId());
				cache.add(plugin);
			}
			return plugins;
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#loadProperty(java.lang.String, java.lang.String)
	 */
	@Override
	protected PluginProperty loadProperty(String pluginId, String key) throws DotDataException {
		PluginProperty pluginProp = cache.getProperty(pluginId, key);
		if(pluginProp != null){
			return pluginProp;
		}
		HibernateUtil hu = new HibernateUtil(PluginProperty.class);
		hu.setQuery("from PluginProperty where plugin_id = ? and propkey = ?");
		hu.setParam(pluginId);
		hu.setParam(key);
		pluginProp = (PluginProperty)hu.load();
		if(pluginProp != null && UtilMethods.isSet(pluginProp.getPluginId())){
			cache.addProperty(pluginProp);
		}
		return pluginProp;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#save(com.dotmarketing.plugin.model.Plugin)
	 */
	@Override
	protected void save(Plugin plugin) throws DotDataException {
		HibernateUtil.saveOrUpdate(plugin);
		if(UtilMethods.isSet(plugin.getId())){
			cache.removePlugin(plugin.getId());
			cache.add(plugin);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginFactory#saveProperty(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	protected void saveProperty(PluginProperty pluginProperty) throws DotDataException {
		HibernateUtil.saveOrUpdate(pluginProperty);
		if(UtilMethods.isSet(pluginProperty.getPluginId()) && UtilMethods.isSet(pluginProperty.getPropkey())){
			cache.removePluginProperty(pluginProperty);
			cache.addProperty(pluginProperty);
		}
	}

}
