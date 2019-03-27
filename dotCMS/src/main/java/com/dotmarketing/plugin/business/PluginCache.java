package com.dotmarketing.plugin.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.plugin.model.PluginProperty;

//This interface should have default package access
public abstract class PluginCache implements Cachable{

	abstract protected com.dotmarketing.plugin.model.Plugin add(com.dotmarketing.plugin.model.Plugin plugin);

	abstract protected com.dotmarketing.plugin.model.Plugin get(String pluginId);
	
	abstract protected PluginProperty addProperty(PluginProperty pluginProperty);
	
	abstract protected PluginProperty getProperty(String pluginId, String propertyKey);

	abstract public void clearCache();
	
	abstract protected void clearPluginCache();
	
	abstract protected void clearPropertyCache();

	abstract protected void removePlugin(String pluginId);

	abstract protected void removePluginProperty(PluginProperty pluginProperty);
}