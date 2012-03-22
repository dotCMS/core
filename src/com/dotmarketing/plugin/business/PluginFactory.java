package com.dotmarketing.plugin.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.model.PluginProperty;

public abstract class PluginFactory {

	/**
	 * Loads all plugins in the system
	 * @return
	 * @throws DotDataException 
	 */
	protected abstract List<Plugin> findPlugins() throws DotDataException;
	
	/**
	 * Load a plugin by its primary key(ID).  The primary key of a Plugin
	 * is it's fully qualified name in the plugin directory. 
	 * @param id
	 * @return
	 * @throws DotDataException 
	 */
	protected abstract Plugin loadPlugin(String id) throws DotDataException;
	
	/**
	 * 
	 * @param plugin
	 * @throws DotDataException
	 */
	protected abstract void save(Plugin plugin) throws DotDataException;	
	
	/**
	 * 
	 * @param plugin
	 * @throws DotDataException
	 */
	protected abstract void delete(Plugin plugin) throws DotDataException;
	
	/**
	 * Deletes all properties for a specified plugin
	 * @param pluginId
	 * @throws DotDataException
	 */
	protected abstract void deletePluginProperties(String pluginId) throws DotDataException;
	
	/**
	 * 
	 * @param pluginId
	 * @param key
	 * @return
	 * @throws DotDataException
	 */
	protected abstract PluginProperty loadProperty(String pluginId, String key) throws DotDataException;
	
	/**
	 * 
	 * @param pluginId
	 * @param key
	 * @param value
	 * @throws DotDataException
	 */
	protected abstract void saveProperty(PluginProperty pluginProperty) throws DotDataException;
}
