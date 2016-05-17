package com.dotcms.datagen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.util.UUIDGenerator;

/**
 * Class used to create {@link Contentlet} objects for test purposes
 * @author Nollymar Longa
 *
 */
public class ContentletDataGen extends AbstractDataGen<Contentlet>{

	private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static final FolderAPI folderAPI = APILocator.getFolderAPI();
	private static final String folderPath = "/testfolder" + UUIDGenerator.generateUuid();
	private static Folder folder;

	protected String structureInode;
	protected Map<String, String> properties = new HashMap<>();
	protected long languageId;
	
    static {
        try {
            folder = folderAPI.createFolders(folderPath, defaultHost, user, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates a new {@link Contentlet} instance kept in memory (not persisted)
     * @return Contentlet instance created
     */
    @Override
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
    
    
    /**
     * Creates an instance of a {@link Contentlet} considering a existing one (Contentlet checkout)
     * @param contentletBase Contentlet instance that will be used as a model for the new Contentlet instance
     * @return Contentlet instance created from existing one
     */
    public Contentlet next(Contentlet contentletBase){
    	try{
	    	return APILocator.getContentletAPI().checkout(
	    			contentletBase.getInode(), user, false);
    	} catch (DotContentletStateException | DotDataException | DotSecurityException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * Creates a new {@link Contentlet} instance and persists it in DB
     * @return A new Contentlet instance persisted in DB
     */
    @Override
    public Contentlet nextPersisted() {
    	try{
    		return persist(next());
    	} catch (DotContentletStateException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * Persists in DB a given {@link Contentlet} instance
     * @param contentlet to be persisted
     * @return The persisted Contentlet instance
     */
    @Override
    public Contentlet persist(Contentlet contentlet) {
    	try{
    		return contentletAPI.checkin(contentlet, user, false);
    	} catch (DotContentletStateException | IllegalArgumentException | DotDataException | DotSecurityException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * Archives a given {@link Contentlet} instance
     * @param contentlet to be archived
     */
    public void archive(Contentlet contentlet) {
    	try{
    		contentletAPI.archive(contentlet, user, false);
    	} catch (DotContentletStateException | DotDataException | DotSecurityException e) {
			throw new RuntimeException(e);
		}
    }
    
    
    /**
     * Deletes a given {@link Contentlet} instance
     * @param contentlet to be deleted
     */
    public void delete(Contentlet contentlet) {
    	try{
    		contentletAPI.delete(contentlet, user, false);
    	} catch (DotContentletStateException | DotDataException | DotSecurityException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * Archives and deletes a given {@link Contentlet} instance
     * @param contentlet to be removed
     */
    @Override
    public void remove(Contentlet contentlet) {
    	try{
	    	this.archive(contentlet);
	    	this.delete(contentlet);
    	} catch (DotContentletStateException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * Sets languageId property to the ContentletDataGen instance. 
     * This will be used when a new {@link Contentlet} instance is created
     * @param languageId
     * @return ContentletDataGen with languageId set
     */
    public ContentletDataGen languageId(long languageId){
    	this.languageId = languageId;
    	return this;
    }
    
    /**
	 * Sets structureInode property to the ContentletDataGen instance. This will
	 * be used when a new {@link Contentlet} instance is created
	 * 
	 * @param structureInode
	 * @return ContentletDataGen with structureInode set
	 */
    public ContentletDataGen structureInode(String structureInode){
    	this.structureInode = structureInode;
    	return this;
    }
    
    /**
     * Sets properties to the ContentletDataGen instance. 
     * This will be used when a new {@link Contentlet} instance is created
     * @param key
     * @param value
     * @return ContentletDataGen with a new property set
     */
    public ContentletDataGen setProperty(String key, String value){
    	this.properties.put(key, value);
    	return this;
    }
    
    /**
     * Removes an existing property from the ContentletDataGen instance.
     * @param key
     * @return ContentletDataGen without the given property
     */
    public ContentletDataGen removeProperty(String key){
    	this.properties.remove(key);
    	return this;
    }
    
}
