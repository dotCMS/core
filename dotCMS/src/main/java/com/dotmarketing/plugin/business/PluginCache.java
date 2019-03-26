package com.dotmarketing.plugin.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.plugin.model.PluginProperty;

// This interface should have default package access
public abstract class PluginCache implements Cachable {

  protected abstract com.dotmarketing.plugin.model.Plugin add(
      com.dotmarketing.plugin.model.Plugin plugin);

  protected abstract com.dotmarketing.plugin.model.Plugin get(String pluginId);

  protected abstract PluginProperty addProperty(PluginProperty pluginProperty);

  protected abstract PluginProperty getProperty(String pluginId, String propertyKey);

  public abstract void clearCache();

  protected abstract void clearPluginCache();

  protected abstract void clearPropertyCache();

  protected abstract void removePlugin(String pluginId);

  protected abstract void removePluginProperty(PluginProperty pluginProperty);
}
