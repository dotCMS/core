package com.dotmarketing.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.util.PluginUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class PluginLoader {
	
	private PluginAPI pluginAPI; 
	
	public PluginLoader() {
		pluginAPI = APILocator.getPluginAPI();
	}
	
	public void loadPlugins(String rootPath, String pluginPath) {
		List<File> pluginJars = PluginUtil.getPluginJars(rootPath, pluginPath);
		List<String> pluginIds = new ArrayList<String>();
		
		//initialize the pluginAPI
		PluginAPI pluginAPI = APILocator.getPluginAPI();
		pluginAPI.setDeployedPluginOrder(pluginIds);
		try {
			pluginAPI.setPluginJarDir(new File(pluginPath));
		} catch (IOException e) {
			Logger.fatal(this, "ERROR : while initializing pluginAPI", e);
			return;
		}
		
		List<Plugin> plugins = null;
		try {
			plugins = pluginAPI.findPlugins();
		} catch (DotDataException e) {
			Logger.fatal(this, "ERROR : when loading the installed plugins", e);
			return;
		}
		
		for (File f : pluginJars) {
			String id = f.getName();
			try{
				id = PluginUtil.getPluginNameFromJar(f.getName());	
				Plugin plugin = null;
				
				if (plugins != null) {
					for (Plugin tempPlugin: plugins) {
						if (tempPlugin.getId().equals(id)) {
							plugin = tempPlugin;
							break;
						}
					}
				}
				
				boolean newPlugin = false;
				JarFile jar;
				Manifest mf;
				try {
					jar = new JarFile(f.getPath());
					mf=jar.getManifest();
				} catch (IOException e1) {
					Logger.fatal(this, "Method loadPlugins : Error deploying plugin id:" + id, e1);
					continue;
				}
				Attributes attrs=mf.getMainAttributes();
				if(plugin == null || !UtilMethods.isSet(plugin.getId())){
					plugin = new Plugin();
					newPlugin = true;
					plugin.setFirstDeployedDate(Calendar.getInstance().getTime());
					plugin.setId(id);
				}
				String version = attrs.getValue("Plugin-Version");
				if(!UtilMethods.isSet(version)){
					Logger.fatal(this, "Method loadPlugins : Error deploying plugin id:" + id);
					continue;
				}
				if(!newPlugin){
					if(plugin.getPluginVersion().equalsIgnoreCase(version)){
						setUpHooks(id);
						loadPluginProperties(jar, plugin.getId());
						pluginIds.add(id);
						pluginAPI.setDeployedPluginOrder(pluginIds);
						continue;
					}
				}
				
				plugin.setPluginVersion(version);
				String oldVersion = plugin.getPluginVersion();
				plugin.setLastDeployedDate(Calendar.getInstance().getTime());
				String pluginName = attrs.getValue("Plugin-Name") ==  null ? "":attrs.getValue("Plugin-Name");
				String author = attrs.getValue("Author") ==  null ? "":attrs.getValue("Author");
				plugin.setAuthor(author);
				plugin.setPluginName(pluginName);
				try{
					pluginAPI.save(plugin);
				} catch (DotDataException e2) {
					Logger.fatal(this, "Method loadPlugins : Error deploying plugin id:" + id, e2);
					continue;
				}
				loadPluginProperties(jar, plugin.getId());
				String deployClass = attrs.getValue("Deploy-Class");
				PluginDeployer deployer=null;
				if (deployClass!=null && deployClass.length()>0) {
					try {
						Object o=Class.forName(deployClass).newInstance();
						if (o instanceof PluginDeployer) {
							deployer=(PluginDeployer)o;
							if(newPlugin)
								deployer.deploy();
							else
								deployer.redeploy(oldVersion);
						}
					} catch (InstantiationException e) {
						Logger.debug(PluginLoader.class,"InstantiationException: " + id + " " + e.getMessage(),e);
						continue;
					} catch (IllegalAccessException e) {
						Logger.debug(PluginLoader.class,"IllegalAccessException: " + id + " " + e.getMessage(),e);
						continue;
					} catch (ClassNotFoundException e) {
						Logger.debug(PluginLoader.class,"ClassNotFoundException: " + id + " " + e.getMessage(),e);
						continue;
					}
				}
				setUpHooks(id);
				pluginIds.add(id);
				pluginAPI.setDeployedPluginOrder(pluginIds);
				pluginAPI.loadBackEndFiles(plugin.getId());
				
			}catch (Exception e) {
				Logger.fatal(this, "ERROR DEPLOYING A PLUGIN : " + id, e);
			}
		}
	}
	
	private void setUpHooks(String pluginId) throws InstantiationException, IllegalAccessException, ClassNotFoundException, DotDataException{

		PluginAPI pluginAPI = APILocator.getPluginAPI();
		Interceptor conI = (Interceptor)APILocator.getContentletAPIntercepter();
		String conPreHooks = pluginAPI.loadPluginConfigProperty(pluginId, "contentletapi.prehooks");
		String conPostHooks = pluginAPI.loadPluginConfigProperty(pluginId, "contentletapi.posthooks");
		if(UtilMethods.isSet(conPreHooks)){
			String[] pres = conPreHooks.split(",");
			for (String string : pres) {
				conI.addPreHook(string);
			}
		}
		if(UtilMethods.isSet(conPostHooks)){
			String[] post = conPostHooks.split(",");
			for (String string : post) {
				conI.addPostHook(string);
			}
		}
	}
	
	private void loadPluginProperties(JarFile jar, String pluginId) throws IOException, DotDataException{
		Map<String, String> props = PluginUtil.loadPropertiesFromFile(jar);
		String reload = props.get("reload.force");
		if(!UtilMethods.isSet(reload) || new Boolean(reload)){
			pluginAPI.deletePluginProperties(pluginId);
			for (String key : props.keySet()) {
				pluginAPI.saveProperty(pluginId, key, props.get(key));
			}
		}
	}
}
