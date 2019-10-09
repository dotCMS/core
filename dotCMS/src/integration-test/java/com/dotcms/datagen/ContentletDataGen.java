package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class used to create {@link Contentlet} objects for test purposes
 * @author Nollymar Longa
 *
 */
public class ContentletDataGen extends AbstractDataGen<Contentlet> {

    private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    protected String contentTypeId;
    protected Map<String, Object> properties = new HashMap<>();
    protected long languageId;
    protected String modUser = UserAPI.SYSTEM_USER_ID;
    protected List<Category> categories;
    private boolean skipValidation = false;
    private IndexPolicy policy = null;

    public ContentletDataGen(String contentTypeId) {
        this.contentTypeId = contentTypeId;
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
     * @param contentTypeId the id of the structure
     * @return ContentletDataGen with structure set
     */
    public ContentletDataGen structure(String contentTypeId) {
        this.contentTypeId = contentTypeId;
        return this;
    }

    /**
     * Sets host property to the ContentletDataGen instance. This will be used when a new {@link
     * Contentlet} instance is created
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
     * Set category to the ContentletDataGen instance.
     * @param category
     * @return
     */
    public ContentletDataGen addCategory(Category category) {
        if(!UtilMethods.isSet(this.categories)){
          this.categories = new ArrayList<>();
        }
        this.categories.add(category);
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
     * In case we need to create content with empty required fields
     * @param skipValidation
     * @return
     */
    public ContentletDataGen skipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
        return this;
    }

    /**
     * Creates a new {@link Contentlet} instance kept in memory (not persisted)
     * @return Contentlet instance created
     */
    @Override
    public Contentlet next(){

        final Contentlet contentlet = new Contentlet();
        contentlet.setFolder(folder ==null ? Folder.SYSTEM_FOLDER : folder.getInode());
        contentlet.setHost(host == null ? Host.SYSTEM_HOST : host.getIdentifier());
        contentlet.setLanguageId(languageId==0 ? APILocator.getLanguageAPI().getDefaultLanguage().getId()  : languageId);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentlet.setContentTypeId(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeId).id()).getOrNull());
        for(Entry<String, Object> element:properties.entrySet()){
            contentlet.setProperty(element.getKey(), element.getValue());
        }

        contentlet.setIndexPolicy(null != policy?policy:IndexPolicy.WAIT_FOR);
        contentlet.setIndexPolicyDependencies(null != policy?policy:IndexPolicy.WAIT_FOR);
        if(skipValidation){
            contentlet.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);
        }
        return contentlet;
    }

    /**
     * Creates a new {@link Contentlet} instance and persists it in DB
     * @return A new Contentlet instance persisted in DB
     */
    @Override
    public Contentlet nextPersisted() {
        try{
            if(UtilMethods.isSet(this.categories) && !this.categories.isEmpty()){
               return persist(next(), this.categories);
            }
            return persist(next());
        } catch (DotContentletStateException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Persists in DB a given {@link Contentlet} instance
     *
     * @param contentlet to be persisted
     * @return The persisted Contentlet instance
     */
    @Override
    public Contentlet persist(Contentlet contentlet) {
        return checkin(contentlet, null != policy?policy:IndexPolicy.FORCE);
    }

    /**
     * Persists in DB a given {@link Contentlet} instance
     * @param contentlet to be persisted
     * @param categories  The persisted Contentlet instance
     * @return
     */
    public Contentlet persist(final Contentlet contentlet, final List<Category> categories) {
        return checkin(contentlet, categories);
    }

    /**
     * Creates a new {@link Contentlet} instance and persists it in DB
     *
     * @return A new Contentlet instance persisted in DB
     */
    public Contentlet nextPersistedWithSampleTextValues() {
        try {
            return persist(nextWithSampleTextValues());
        } catch (DotContentletStateException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new {@link Contentlet} instance kept in memory (not persisted)
     *
     * @return Contentlet instance created
     */
    public Contentlet nextWithSampleTextValues() {

        try {
            final Contentlet contentlet = next();

            final Collection<Field> fields = APILocator.getContentTypeFieldAPI()
                    .byContentTypeId(contentlet.getContentTypeId());
            for (Field field : fields) {

                if (field.dataType().equals(DataTypes.TEXT)) {
                    contentlet.setStringProperty(field,
                            "TestFieldValue" + System.currentTimeMillis());
                }
            }
            return contentlet;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create Contentlet instance.", e);
        }
    }

    /**
     * Creates an instance of a {@link Contentlet} considering a existing one (Contentlet checkout)
     *
     * @param contentletBase Contentlet instance that will be used as a model for the new Contentlet
     * instance
     * @return Contentlet instance created from existing one
     */
    @WrapInTransaction
    public static Contentlet checkout(Contentlet contentletBase) {
        try {
            return APILocator.getContentletAPI().checkout(
                    contentletBase.getInode(), user, false);
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    public static Contentlet checkin(Contentlet contentlet) {
        return checkin(contentlet, IndexPolicy.FORCE);
    }

    @WrapInTransaction
    public static Contentlet checkin(Contentlet contentlet, IndexPolicy policy) {
        try{
            contentlet.setIndexPolicy(policy);
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            return contentletAPI.checkin(contentlet, user, false);
        } catch (DotContentletStateException | IllegalArgumentException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    public static Contentlet checkin(final Contentlet contentlet, final List<Category> categories) {
        try{
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            return contentletAPI.checkin(contentlet, user, false, categories);
        } catch (DotContentletStateException | IllegalArgumentException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    public static Contentlet publish(Contentlet contentlet) {
        try {
            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            contentletAPI.publish(contentlet, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return contentlet;
    }

    /**
     * Archives a given {@link Contentlet} instance
     * @param contentlet to be archived
     */
    @WrapInTransaction
    public static void archive(Contentlet contentlet) {
        try{
            contentletAPI.archive(contentlet, APILocator.systemUser(), false);
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Deletes a given {@link Contentlet} instance
     * @param contentlet to be deleted
     */
    @WrapInTransaction
    public static void delete(Contentlet contentlet) {
        try{
            contentletAPI.delete(contentlet, APILocator.systemUser(), false);
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Archives and deletes a given {@link Contentlet} instance
     * @param contentlet to be removed
     */
    @WrapInTransaction
    public static void remove(Contentlet contentlet) {
        try{
            destroy(contentlet, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void destroy(final Contentlet contentlet) {
        destroy(contentlet, true);
    }

    @WrapInTransaction
    public static void destroy(final Contentlet contentlet, final Boolean failSilently) {

        if (null != contentlet) {
            try {
                APILocator.getContentletAPI().destroy(contentlet, APILocator.systemUser(), false);
            } catch (Exception e) {
                if (failSilently) {
                    Logger.error(ContentTypeDataGen.class, "Unable to destroy Contentlet.", e);
                } else {
                    throw new RuntimeException("Unable to destroy Contentlete.", e);
                }
            }
        }
    }

    public ContentletDataGen setPolicy(final IndexPolicy policy) {
        this.policy = policy;
        return this;
    }
}