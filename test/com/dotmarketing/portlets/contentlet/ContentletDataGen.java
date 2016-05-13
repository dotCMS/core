package com.dotmarketing.portlets.contentlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

public class ContentletDataGen {
	
	private static final HostAPI hostAPI = APILocator.getHostAPI();
	private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static final FolderAPI folderAPI = APILocator.getFolderAPI();
	private static final User user;
	private static final Host defaultHost;
	private static final String folderPath = "/testfolder" + UUIDGenerator.generateUuid();
	private static final Folder folder;

	private String structureInode = HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE;
	private Map<String, String> properties = new HashMap<>();
	private long languageId;
	
    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
            folder = folderAPI.createFolders(folderPath, defaultHost, user, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public Contentlet next(){
    	Contentlet contentlet = new Contentlet();
       	contentlet.setFolder(folder.getInode());
       	contentlet.setHost(defaultHost.getIdentifier());
       	contentlet.setLanguageId(languageId);
    	for(Entry<String, String> element:properties.entrySet()){
    		contentlet.setProperty(element.getKey(), element.getValue());
    	}
 
    	contentlet.setStructureInode(structureInode);
  
    	return contentlet;
    }
    
    public Contentlet nextPersisted() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException{
    	return persist(next());
    }
    
    public Contentlet persist(Contentlet contentlet) throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException{
    	return contentletAPI.checkin(contentlet, user, false);
    }
    
    public void archive(Contentlet contentlet) throws DotContentletStateException, DotDataException, DotSecurityException{
    	contentletAPI.archive(contentlet, user, false);
    }
    
    public void delete(Contentlet contentlet) throws DotContentletStateException, DotDataException, DotSecurityException{
    	contentletAPI.delete(contentlet, user, false);
    }
    
    public void remove(Contentlet contentlet) throws DotContentletStateException, DotDataException, DotSecurityException{
    	this.archive(contentlet);
    	this.delete(contentlet);
    	folderAPI.delete(folder, user, false);
    }
    
    public ContentletDataGen languageId(long languageId){
    	this.languageId = languageId;
    	return this;
    }
    
    public ContentletDataGen structureInode(String structureInode){
    	this.structureInode = structureInode;
    	return this;
    }
    
    public ContentletDataGen setProperty(String key, String value){
    	this.properties.put(key, value);
    	return this;
    }
    
    public ContentletDataGen removeProperty(String key){
    	this.properties.remove(key);
    	return this;
    }
    
}
