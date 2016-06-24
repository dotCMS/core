package com.dotcms.datagen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * Class used to create {@link Contentlet} objects for test purposes
 * @author Nollymar Longa
 *
 */
public class ContentletDataGen extends AbstractDataGen<Contentlet> {

	private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();

	protected String structureId;
	protected Map<String, Object> properties = new HashMap<>();
	protected long languageId;

    public ContentletDataGen(String structureId) {
        this.structureId = structureId;
    }

    /**
     * Sets languageId property to the ContentletDataGen instance. 
     * This will be used when a new {@link Contentlet} instance is created
     * @param languageId the id of the language
     * @return ContentletDataGen with languageId set
     */
    public ContentletDataGen languageId(long languageId){
    	this.languageId = languageId;
    	return this;
    }
    
    /**
	 * Sets the structure that will be the type of the  {@link Contentlet} created by this data-gen.
	 * 
	 * @param structureId the id of the structure
	 * @return ContentletDataGen with structure set
	 */
    public ContentletDataGen structure(String structureId){
    	this.structureId = structureId;
    	return this;
    }

	/**
	 * Sets host property to the ContentletDataGen instance. This will
	 * be used when a new {@link Contentlet} instance is created
	 *
	 * @param host the host
	 * @return the datagen with host set
	 */
	public ContentletDataGen host(Host host) {
		this.host = host;
		return this;
	}

    /**
     * Sets folder property to the ContentletDataGen instance. This will
     * be used when a new {@link Contentlet} instance is created
     *
     * @param folder the inode of the folder
     * @return ContentletDataGen with folder set
     */
    public ContentletDataGen folder(Folder folder) {
        this.folder = folder;
        return this;
    }
    
    /**
     * Sets properties to the ContentletDataGen instance. 
     * This will be used when a new {@link Contentlet} instance is created
     * @param key the key
     * @param value the value
     * @return ContentletDataGen with a new property set
     */
    public ContentletDataGen setProperty(String key, Object value){
    	this.properties.put(key, value);
    	return this;
    }
    
    /**
     * Removes an existing property from the ContentletDataGen instance.
     * @param key the key
     * @return ContentletDataGen without the given property
     */
    @SuppressWarnings("unused")
    public ContentletDataGen removeProperty(String key){
    	this.properties.remove(key);
    	return this;
    }

    /**
     * Creates a new {@link Contentlet} instance kept in memory (not persisted)
     * @return Contentlet instance created
     */
    @Override
    public Contentlet next(){
        Contentlet contentlet = new Contentlet();
        contentlet.setFolder(folder.getInode());
        contentlet.setHost(host.getIdentifier());
        contentlet.setLanguageId(languageId);
        for(Entry<String, Object> element:properties.entrySet()){
            contentlet.setProperty(element.getKey(), element.getValue());
        }

        contentlet.setStructureInode(structureId);

        return contentlet;
    }


    /**
     * Creates an instance of a {@link Contentlet} considering a existing one (Contentlet checkout)
     * @param contentletBase Contentlet instance that will be used as a model for the new Contentlet instance
     * @return Contentlet instance created from existing one
     */
    public static Contentlet checkout(Contentlet contentletBase){
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
        return checkin(contentlet);
    }

    public static Contentlet checkin(Contentlet contentlet) {
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
    public static void archive(Contentlet contentlet) {
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
    public static void delete(Contentlet contentlet) {
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
    public static void remove(Contentlet contentlet) {
        try{
            archive(contentlet);
            delete(contentlet);
        } catch (DotContentletStateException e) {
            throw new RuntimeException(e);
        }
    }
    
}
