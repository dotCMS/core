package com.dotmarketing.plugin;

/**
 * A plugin can implement this interface to determine what to do on deploy and redeploy
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5c
 */
public interface PluginDeployer {

	/**
	 * This method will execute the first time the plugin deploys
	 * @return
	 */
	public boolean deploy();
	
	/**
	 * If the manifest version of the plugin is higher then
	 * the current build number of the plugin is higher.
	 * @return
	 */
	public boolean redeploy(String version);
}
