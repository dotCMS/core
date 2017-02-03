/**
 * 
 */
package com.dotmarketing.plugin.business;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.model.Plugin;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5c
 * The PluginAPI is intended to provide access/information on the different plugins. It can also provide
 * the plugins the ability to retrieve their property values.  The methods starting with
 * load are expected to goto cache first and then database and methods starting with find will 
 * typically hit database always.
 */
public interface PluginAPI {

	/**
	 * finds all plugins in the database.  These plugins may or MAY NOT be deployed
	 * @return
	 * @throws DotDataException 
	 */
	public List<Plugin> findPlugins() throws DotDataException;
	
	/**
	 * Load a plugin by its primary key(ID).  The primary key of a Plugin
	 * is it's fully qualified name in the plugin directory. 
	 * @param id
	 * @return
	 * @throws DotDataException 
	 */
	public Plugin loadPlugin(String id) throws DotDataException;
	
	/**
	 * 
	 * @param plugin
	 * @throws DotDataException
	 */
	public void save(Plugin plugin) throws DotDataException;	
	
	/**
	 * 
	 * @param plugin
	 * @throws DotDataException
	 */
	public void delete(Plugin plugin) throws DotDataException;
	
	/**
	 * Deletes all properties for a specified plugin
	 * @param pluginId
	 * @throws DotDataException
	 */
	public void deletePluginProperties(String pluginId) throws DotDataException;
	
	/**
	 * 
	 * @param pluginId
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	public String loadProperty(String pluginId, String key) throws DotDataException;
	
	/**
	 * 
	 * @param pluginId
	 * @param key
	 * @param value
	 * @throws DotDataException
	 */
	public void saveProperty(String pluginId, String key, String value) throws DotDataException;
	
	/**
	 * loads a property from the plugin.config file of the specified plugin
	 * @param pluginId
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	public String loadPluginConfigProperty(String pluginId, String key) throws DotDataException;
	
	/**
	 * loads all keys from the plugin.config file of the specified plugin
	 * @param pluginId
	 * @return
	 * @throws DotDataException
	 */
	public List<String> loadPluginConfigKeys(String pluginId) throws DotDataException;
	
	/**
	 * The path where the plugin jars live
	 * @param directory
	 * @throws IOException - if directory doesn't exist
	 */
	public void setPluginJarDir(File directory) throws IOException;
	
	/**
	 * The path where the plugin jars live
	 * @param directory
	 */
	public File getPluginJarDir();
	
	/**
	 * The deployedPluginOrder is set at startup of the dotCMS when the app is 
	 * starting up the plugins. 
	 * sets the list of the plugin ids 
	 * @param pluginIds
	 */	
	public void setDeployedPluginOrder(List<String> pluginIds);
	
	/**
	 * The deployedPluginOrder is set at startup of the dotCMS when the app is 
	 * starting up the plugins. 
	 * @return
	 */
	public List<String> getDeployedPluginOrder();
	
	/**
	 * The loadBackEndFiles is set at startup of the dotCMS when the app is 
	 * starting up the plugins.
	 * sets or update the plugin backend file(s) if exist on the specified host(s)
	 * @param pluginId
	 * @throws IOException 
	 */
	public void loadBackEndFiles(String pluginId) throws DotDataException, IOException;
	
}
