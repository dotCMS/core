package com.dotmarketing.portlets.fileassets.business;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class FileAssetConverter {
	private volatile boolean flag = true;
	private volatile boolean finished = false;
	
	private List<String> succeeded =  new ArrayList<String>();
	private List<String> failed =  new ArrayList<String>();
	
	
	public List<String> getSucceeded(){
		return Collections.unmodifiableList(succeeded);
	}
	
	public List<String> getFailed(){
		return Collections.unmodifiableList(failed);
	}
	
	public synchronized void start(){
		Runnable converterRunnable = new Runnable(){
			public void run(){	
				try{
					while(flag){
						convertFileAssetsToContentlets();
					}
				}catch(Exception e){
					flag = false;
				}finally{
					flag = false;
				}
			}
		};
		Thread converterThread = new Thread(converterRunnable);
		converterThread.start();
	}


	private synchronized void convertFileAssetsToContentlets() throws DotDataException{

		DotConnect dc = new DotConnect();
		dc.setSQL("select inode from file_asset order by mod_date asc");
		List<HashMap<String, Object>> inodes = dc.loadResults();
		for (Map<String, Object> inode : inodes) {
			String id = (String) inode.get("inode");
			File file = null;
			try {
				file = APILocator.getFileAPI().find(id, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				if(!failed.contains(id))
					failed.add(id);
				Logger.debug(this, "Error getting file", e);
			} 
			if(file!=null && UtilMethods.isSet(file.getInode())){
				Logger.info(this, "Converting file to contentlet path: " + file.getPath() + " inode:" + file.getInode());
				Folder folder = null;
				Host host = null;
				try {
					Identifier ident = APILocator.getIdentifierAPI().find(file);
					if(ident!=null && InodeUtils.isSet(ident.getId())){
						host = APILocator.getHostAPI().find(ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
						folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host,APILocator.getUserAPI().getSystemUser(), false);
					}
				} catch (Exception e) {
					if(!failed.contains(id))
						failed.add(id);
					
					Logger.info(this, "Error file to contentlet path: " + file.getPath() + " inode:" + file.getInode());
					Logger.debug(this, "Error getting identifier", e);
				}
				if(folder!=null){
					java.io.File binFile = null;
					try {
						Structure faStructure = StructureCache.getStructureByInode(folder.getDefaultFileType());
						Field fieldVar = faStructure.getFieldVar(FileAssetAPI.BINARY_FIELD);
						java.io.File assetFile = APILocator.getFileAPI().getAssetIOFile(file);
						if(assetFile!=null && assetFile.exists()){
							java.io.File tempUserFolder = new java.io.File(Config.CONTEXT.getRealPath(Constants.TEMP_BINARY_PATH) + java.io.File.separator + APILocator.getUserAPI().getSystemUser().getUserId() + 
									java.io.File.separator + fieldVar.getFieldContentlet());
							if (!tempUserFolder.exists())
								tempUserFolder.mkdirs();

							binFile = new java.io.File(tempUserFolder.getAbsolutePath() + java.io.File.separator + file.getFileName());
								FileChannel ic = new FileInputStream(assetFile).getChannel();
								FileChannel oc = new FileOutputStream(binFile).getChannel();
								ic.transferTo(0, ic.size(), oc);
								ic.close();
								oc.close();
						}
					} catch (IOException e) {
						if(!failed.contains(id))
							failed.add(id);
						
						Logger.info(this, "Error file to contentlet path: " + file.getPath() + " inode:" + file.getInode());
						Logger.debug(this, "Error getting binary file", e);
					}

					if(binFile!=null && binFile.length()>0){
						HibernateUtil.startTransaction();
						try{
							Contentlet contentlet = null;
							Identifier identifier = APILocator.getIdentifierAPI().find(file.getIdentifier());
							Identifier identAux = APILocator.getIdentifierAPI().find(host, file.getPath());
							LiveCache.removeAssetFromCache(file);
							WorkingCache.removeAssetFromCache(file);
							CacheLocator.getIdentifierCache().removeFromCacheByVersionable(file);
							com.dotmarketing.menubuilders.RefreshMenus.deleteMenu(file);
							identifier.setAssetName(identifier.getAssetName()+"_copy");
							APILocator.getIdentifierAPI().save(identifier);
							if(identAux!=null && UtilMethods.isSet(identAux.getId()) && identAux.getAssetType().equals("contentlet")){
								Logger.info(this, "Identifier already exists, host = " + host.getIdentifier() + " path = " + file.getPath());
								contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identAux.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),APILocator.getUserAPI().getSystemUser(), false);
								if(contentlet!=null && UtilMethods.isSet(contentlet.getInode())){
									contentlet.setInode(null);
									contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.isSet(file.getTitle())?file.getTitle():binFile.getName());
									contentlet.setProperty(FileAssetAPI.SORT_ORDER, file.getSortOrder());
									contentlet.setProperty(FileAssetAPI.DESCRIPTION, file.getFriendlyName());
									contentlet.setProperty(FileAssetAPI.SHOW_ON_MENU, String.valueOf(file.isShowOnMenu()));
									contentlet.setProperty(FileAssetAPI.BINARY_FIELD, binFile);
									contentlet.setSortOrder(file.getSortOrder());
									contentlet = APILocator.getContentletAPI().checkin(contentlet, APILocator.getUserAPI().getSystemUser(), false);
								}
							}else{
								contentlet = new Contentlet();
								contentlet.setStructureInode(folder.getDefaultFileType());
								contentlet.setHost(host.getIdentifier());
								contentlet.setFolder(folder.getInode());
								contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.isSet(file.getTitle())?file.getTitle():binFile.getName());
								contentlet.setProperty(FileAssetAPI.SORT_ORDER, file.getSortOrder());
								contentlet.setProperty(FileAssetAPI.DESCRIPTION, file.getFriendlyName());
								contentlet.setProperty(FileAssetAPI.SHOW_ON_MENU, String.valueOf(file.isShowOnMenu()));
								contentlet.setProperty(FileAssetAPI.BINARY_FIELD, binFile);
								contentlet.setSortOrder(file.getSortOrder());
								contentlet = APILocator.getContentletAPI().checkin(contentlet, APILocator.getUserAPI().getSystemUser(), false);
							}
							APILocator.getVersionableAPI().setDeleted(contentlet, file.isArchived());
							APILocator.getVersionableAPI().setLocked(contentlet, file.isLocked(), APILocator.getUserAPI().getSystemUser());
							if(file.isWorking() && !file.isLive()){
								APILocator.getVersionableAPI().setWorking(contentlet);
							}else if(file.isLive()){
								APILocator.getVersionableAPI().setLive(contentlet);
							}
							APILocator.getVersionableAPI().setDeleted(contentlet, file.isArchived());
							APILocator.getVersionableAPI().setLocked(contentlet, file.isLocked(), APILocator.getUserAPI().getSystemUser());
							if(file.isWorking() && !file.isLive()){
								APILocator.getVersionableAPI().setWorking(contentlet);
							}else if(file.isLive()){
								APILocator.getVersionableAPI().setLive(contentlet);
							}
							APILocator.getPermissionAPI().copyPermissions(file, contentlet);
							APILocator.getVersionableAPI().setDeleted(file, true);
							HibernateUtil.commitTransaction();
							succeeded.add(file.getInode());
						}catch(Exception e){
							if(!failed.contains(id))
								failed.add(id);
							Logger.debug(this, "Error creating contentlet file asset", e);
							HibernateUtil.rollbackTransaction();
						}
					}
				}
			}
		}
		HibernateUtil.closeSession();
		flag = false;
		finished = true;
		
	}
	
	public boolean isFinished(){
		return finished;
	}
}
