/**
 * 
 */
package com.dotmarketing.viewtools;

import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.plugin.model.Plugin;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.7
 *
 */
public class PluginWebAPI implements ViewTool{

	private PluginAPI pAPI;
	
	public void init(Object arg0) {
		pAPI = APILocator.getPluginAPI();
	}

	/**
	 * finds all plugins in the database.  These plugins may or MAY NOT be deployed
	 * @return
	 * @throws DotDataException 
	 */
	public List<Plugin> findPlugins() throws DotDataException {
		return pAPI.findPlugins();
	}

	/**
	 * Load a plugin by its primary key(ID).  The primary key of a Plugin
	 * is it's fully qualified name in the plugin directory. 
	 * @param id
	 * @return
	 * @throws DotDataException 
	 */
	public Plugin loadPlugin(String id) throws DotDataException {
		return pAPI.loadPlugin(id);
	}

	/**
	 * 
	 * @param pluginId
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	public List<String> loadPluginConfigKeys(String pluginId) throws DotDataException {
		return pAPI.loadPluginConfigKeys(pluginId);
	}

	/**
	 * loads a property from the plugin.config file of the specified plugin
	 * @param pluginId
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	public String loadPluginConfigProperty(String pluginId, String key)	throws DotDataException {
		return pAPI.loadPluginConfigProperty(pluginId, key);
	}

	/**
	 * loads all keys from the plugin.config file of the specified plugin
	 * @param pluginId
	 * @return
	 * @throws DotDataException
	 */
	public String loadProperty(String pluginId, String key)	throws DotDataException {
		return pAPI.loadProperty(pluginId, key);
	}
	
}
