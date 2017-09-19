/**
 * 
 */
package com.dotmarketing.plugin.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.model.PluginProperty;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5c
 *
 */
public class PluginAPIImpl implements PluginAPI {

	private PluginFactory pluginFac;
	private File pluginJarDir;
	private List<String> deployedPluginOrder;

	public PluginAPIImpl() {
		pluginFac = FactoryLocator.getPluginFactory();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#delete(com.dotmarketing.plugin.model.Plugin)
	 */
	@WrapInTransaction
	public void delete(Plugin plugin) throws DotDataException {
		pluginFac.delete(plugin);
	}

	@WrapInTransaction
	public void deletePluginProperties(String pluginId) throws DotDataException {
		pluginFac.deletePluginProperties(pluginId);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#loadPlugin(java.lang.String)
	 */
	@CloseDBIfOpened
	public Plugin loadPlugin(String id) throws DotDataException {
		return pluginFac.loadPlugin(id);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#loadPlugins()
	 */
	@CloseDBIfOpened
	public List<Plugin> findPlugins() throws DotDataException {
		return pluginFac.findPlugins();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#loadProperty(java.lang.String, java.lang.String)
	 */
	@CloseDBIfOpened
	public String loadProperty(String pluginId, String key)	throws DotDataException {
		PluginProperty pp = pluginFac.loadProperty(pluginId, key);
		if(pp!= null){
			return pp.getCurrentValue();
		}else{
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#save(com.dotmarketing.plugin.model.Plugin)
	 */
	@WrapInTransaction
	public void save(Plugin plugin) throws DotDataException {
		pluginFac.save(plugin);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.plugin.business.PluginAPI#saveProperty(java.lang.String, java.lang.String, java.lang.String)
	 */
	@WrapInTransaction
	public void saveProperty(String pluginId, String key, String value)	throws DotDataException {
		PluginProperty pp = pluginFac.loadProperty(pluginId, key);
		if(pp != null && UtilMethods.isSet(pp.getPluginId())){
			pp.setOriginalValue(pp.getCurrentValue());
			pp.setCurrentValue(value);
		}else{
			pp = new PluginProperty();
			pp.setPropkey(key);
			pp.setPluginId(pluginId);
			pp.setOriginalValue(value);
			pp.setCurrentValue(value);
		}
		pluginFac.saveProperty(pp);
	}

	public List<String> loadPluginConfigKeys(String pluginId) throws DotDataException {
		List<String> result = new ArrayList<String>();
		try{
			JarFile jar = new JarFile(new File(pluginJarDir.getPath() + File.separator + "plugin-" + pluginId));
			JarEntry entry = jar.getJarEntry("conf/plugin-controller.properties");
			Properties props = new Properties();
			InputStream in = jar.getInputStream(entry);
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key =  en.nextElement().toString().trim();
				result.add(key);
			}
			return result;
		}catch (NullPointerException e){
			return result;
		}catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(),e);
		}
	}

	public String loadPluginConfigProperty(String pluginId, String key)	throws DotDataException {
		try{
			JarFile jar = new JarFile(new File(pluginJarDir.getPath() + File.separator + "plugin-" + pluginId + ".jar"));
			JarEntry entry = jar.getJarEntry("conf/plugin-controller.properties");
			Properties props = new Properties();
			InputStream in = jar.getInputStream(entry);
			props.load(in);
			return props.get(key).toString();
		}catch (NullPointerException e){
			return "";
		}catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(),e);
		}
	}

	public List<String> getDeployedPluginOrder() {
		return deployedPluginOrder;
	}

	public File getPluginJarDir() {
		return pluginJarDir;
	}

	public void setDeployedPluginOrder(List<String> pluginIds) {
		this.deployedPluginOrder = pluginIds;
	}

	public void setPluginJarDir(File directory) throws IOException {
		if(!directory.exists()){
			throw new IOException("The directory doesn't exist");
		}
		this.pluginJarDir = directory;		
	}

	@CloseDBIfOpened
	public void loadBackEndFiles(String pluginId) throws IOException, DotDataException{
		try{
			
			HostAPI hostAPI = APILocator.getHostAPI();
			
			User systemUser = APILocator.getUserAPI().getSystemUser();
			JarFile jar = new JarFile(new File(pluginJarDir.getPath() + File.separator + "plugin-" + pluginId + ".jar"));
			List<Host> hostList = new ArrayList<Host>();

			String hosts = loadPluginConfigProperty(pluginId, "hosts.name");
			if(UtilMethods.isSet(hosts)){
				for(String hostname : hosts.split(",")){	
					Host host = hostAPI.findByName(hostname, systemUser, false);
					hostList.add(host);
				}
			}else{
				Host host = hostAPI.findDefaultHost(systemUser, false);
				hostList.add(host);
			}

			Enumeration resources = jar.entries();
			while(resources.hasMoreElements()){

				JarEntry entry = (JarEntry) resources.nextElement();
				// find the files inside the dotcms folder in the jar to copy on backend with this reg expression ("dotcms\\/.*\\.([^\\.]+)$")
				if(entry.getName().matches("dotcms\\/.*\\.([^\\.]+)$") ){

					String filePathAndName=entry.getName().substring(7);
					String filePath = "";
					if(filePathAndName.lastIndexOf("/") != -1){
						filePath = filePathAndName.substring(0, filePathAndName.lastIndexOf("/"));
					}
					String fileName = filePathAndName.substring(filePathAndName.lastIndexOf("/")+1);
					String pluginFolderPath = "/plugins/"+pluginId;

					Logger.debug(this,"files in dotcms:"+filePathAndName+"\n");
					//Create temporary file with the inputstream
					InputStream input = jar.getInputStream(entry);
					File temporaryFile = new File("file.temp");
					OutputStream output= Files.newOutputStream(temporaryFile.toPath());
					byte buf[]=new byte[1024];
					int len;
					while((len=input.read(buf))>0){
						output.write(buf,0,len);
					}
					output.close();
					input.close();

					for(Host host : hostList){

						Folder folder = APILocator.getFolderAPI().findFolderByPath(pluginFolderPath + "/" + filePath,host,APILocator.getUserAPI().getSystemUser(),false);
						if( !InodeUtils.isSet(folder.getInode())){			
							folder = APILocator.getFolderAPI().createFolders(pluginFolderPath + "/" + filePath, host,APILocator.getUserAPI().getSystemUser(),false);
						}
						//GetPrevious version if exists
						Identifier currentId = APILocator.getIdentifierAPI().find(host, pluginFolderPath+"/"+filePathAndName);
						if(currentId!=null && InodeUtils.isSet(currentId.getId()) && currentId.getAssetType().equals("contentlet")){
							Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(currentId.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(),false);
							if(cont!=null && InodeUtils.isSet(cont.getInode())){
								APILocator.getFileAssetAPI().fromContentlet(cont);
								cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(fileName));
								cont.setFolder(folder.getInode());
								cont.setHost(host.getIdentifier());
								cont.setBinary(FileAssetAPI.BINARY_FIELD, temporaryFile);
								APILocator.getContentletAPI().checkin(cont, APILocator.getUserAPI().getSystemUser(),false);
								APILocator.getVersionableAPI().setWorking(cont);
								APILocator.getVersionableAPI().setLive(cont);

							}
						}else{
							Contentlet cont = new Contentlet();
							cont.setStructureInode(folder.getDefaultFileType());
							cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(fileName));
							cont.setFolder(folder.getInode());
							cont.setHost(host.getIdentifier());
							cont.setBinary(FileAssetAPI.BINARY_FIELD, temporaryFile);
							APILocator.getContentletAPI().checkin(cont, APILocator.getUserAPI().getSystemUser(),false);
							APILocator.getVersionableAPI().setWorking(cont);
							APILocator.getVersionableAPI().setLive(cont);

						}
			

					}

					temporaryFile.delete();

				}
			}

		}catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			//throw new IOException("The directory doesn't exist");
		}catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			//throw new DotDataException(e.getMessage(),e);
		}
	}

}
