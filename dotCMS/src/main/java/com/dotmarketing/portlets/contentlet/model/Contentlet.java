package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.RelationshipUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a content unit in the system. Ideally, every single domain object
 * in dotCMS will be represented as a Contentlet in the near future given the
 * flexibility they allow and the consistency goal that will be achieved in
 * terms of code, push publishing, versioning, and so on.
 *
 * @author Jason Tesser
 * @author David Tores
 *
 */
public class Contentlet implements Serializable, Permissionable, Categorizable, Versionable, Treeable, Ruleable  {

    private static final long serialVersionUID = 1L;
    public static final String TITTLE_KEY = "title";
    public static final String HAS_TITLE_IMAGE_KEY = "hasTitleImage";
    public static final String INODE_KEY = "inode";
    public static final String LANGUAGEID_KEY = "languageId";
    public static final String STRUCTURE_INODE_KEY = "stInode";
    public static final String STRUCTURE_NAME_KEY = "stName";
    public static final String CONTENT_TYPE_KEY = "contentType";
    public static final String BASE_TYPE_KEY = "baseType";
    public static final String LAST_REVIEW_KEY = "lastReview";
    public static final String NEXT_REVIEW_KEY = "nextReview";
    public static final String REVIEW_INTERNAL_KEY = "reviewInternal";
    public static final String DISABLED_WYSIWYG_KEY = "disabledWYSIWYG";
    public static final String LOCKED_KEY = "locked";
    public static final String ARCHIVED_KEY = "archived";
    public static final String LIVE_KEY = "live";
    public static final String WORKING_KEY = "working";
    public static final String MOD_DATE_KEY = "modDate";
    public static final String MOD_USER_KEY = "modUser";
    public static final String OWNER_KEY = "owner";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String SORT_ORDER_KEY = "sortOrder";
    public static final String HOST_KEY = "host";
    public static final String HOST_NAME = "hostName";
    public static final String FOLDER_KEY = "folder";
	public static final String NULL_PROPERTIES = "nullProperties";
	public static final String WORKFLOW_ACTION_KEY = "wfActionId";
	public static final String WORKFLOW_ASSIGN_KEY = "wfActionAssign";
	public static final String WORKFLOW_COMMENTS_KEY = "wfActionComments";
	public static final String WORKFLOW_BULK_KEY = "wfActionBulk";
    public static final String DOT_NAME_KEY = "__DOTNAME__";

    public static final String TITLE_IMAGE_KEY="titleImage";

    public static final String URL_MAP_FOR_CONTENT_KEY = "URL_MAP_FOR_CONTENT";



    public static final String DONT_VALIDATE_ME = "_dont_validate_me";
    public static final String DISABLE_WORKFLOW = "__disable_workflow__";

    // means the contentlet is being used on unit test mode.
	public static final String IS_TEST_MODE = "_is_test_mode";

	/**
	 * Flag to avoid to trigger the workflow again on the checkin when it is already in progress.
	 */
	public static final String WORKFLOW_IN_PROGRESS = "__workflow_in_progress__";
	public static final String IS_COPY_CONTENTLET = "_is_copy_contentlet";
	public static final String SOURCE_CONTENTLET_ASSET_NAME = "_source_contentlet_assetName";

    public static final String WORKFLOW_PUBLISH_DATE = "wfPublishDate";
    public static final String WORKFLOW_PUBLISH_TIME = "wfPublishTime";
    public static final String WORKFLOW_EXPIRE_DATE = "wfExpireDate";
    public static final String WORKFLOW_EXPIRE_TIME = "wfExpireTime";
    public static final String WORKFLOW_NEVER_EXPIRE = "wfNeverExpire";
	public static final String TEMP_BINARY_IMAGE_INODES_LIST = "tempBinaryImageInodesList";
	public static final String RELATIONSHIP_KEY = "__##relationships##__";

	private transient ContentType contentType;
    protected Map<String, Object> map = new ContentletHashMap();

	private boolean lowIndexPriority = false;

    private transient ContentletAPI contentletAPI;
    private transient UserAPI userAPI;
	private transient IndexPolicy indexPolicy = IndexPolicy.DEFER;
	private transient IndexPolicy indexPolicyDependencies = IndexPolicy.DEFER;

	private transient boolean needsReindex = false;

	private transient Map<String, List<String>> relatedIds;

	/**
	 * Returns true if this contentlet needs reindex
	 * @return true if needs reindex
	 */
	@JsonIgnore
	@com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
	public boolean needsReindex() {
		return needsReindex;
	}

	/**
	 * Call this method when you want to mark the current contentlet as dirty, usually useful to determine if reindex or not in upon layers or next stages
	 */
	public void markAsDirty () {
		this.needsReindex = true;
	}

	/**
	 * When a content is reindex mark it as an indexed.
	 */
	public void markAsReindexed () {
		this.needsReindex = false;
	}

	/**
	 * Get the indexing policy for the contentlet @see {@link IndexPolicy}
	 * @return IndexPolicy
	 */
	public IndexPolicy getIndexPolicy() {

		return (null == this.indexPolicy)?
				IndexPolicy.DEFER:indexPolicy;
	}

	/**
	 * This method sets IndexPolicy, it could be:
	 *
	 * <ul>
	 * <li>DEFER, you do not care about when is gonna be reindex your content, usually usefull on batch processing.</li>
	 * <li>WAIT_FOR, you want to wait until the content is ready to be searchable.</li>
	 * <li>FORCE, you want to force the content searchable immediate, however this policy is not highly scalable.</li>
	 * </ul>
	 * @param indexPolicy
	 */
	public void setIndexPolicy(final IndexPolicy indexPolicy) {

		if (null != indexPolicy) {
			this.indexPolicy = indexPolicy;
		}
	}

	public IndexPolicy getIndexPolicyDependencies() {

		return (null == this.indexPolicyDependencies)?
				IndexPolicy.DEFER:indexPolicyDependencies;
	}

	/**
	 * This method sets IndexPolicy for the dependencies (relationships and categories), it could be:
	 *
	 * <ul>
	 * <li>DEFER, you do not care about when is gonna be reindex your content, usually usefull on batch processing.</li>
	 * <li>WAIT_FOR, you want to wait until the content is ready to be searchable.</li>
	 * <li>FORCE, you want to force the content searchable immediate, however this policy is not highly scalable.</li>
	 * </ul>
	 * @param indexPolicy
	 */
	public void setIndexPolicyDependencies(final IndexPolicy indexPolicy) {

		if (null != indexPolicy) {
			this.indexPolicyDependencies = indexPolicy;
		}
	}

	@Override
    public String getCategoryId() {
    	return getInode();
    }

    /**
     * Create a contentlet based on a map (makes a copy of it)
     * @param map
     */
    public Contentlet(final Map<String, Object> map) {
		this.map = new ContentletHashMap();
    	this.map.putAll(map);
		this.indexPolicy = IndexPolicy.DEFER;
    }

	/**
	 * Create a contentlet based on a map (makes a copy of it)
	 * @param contentlet
	 */
	public Contentlet(final Contentlet contentlet) {
		this(contentlet.getMap());
		if (null != contentlet.getIndexPolicy()) {
			this.indexPolicy = contentlet.getIndexPolicy();
		}
	}

    /**
     * Default class constructor.
     */
    public Contentlet() {
		setInode("");
		setIdentifier("");
		setLanguageId(0);
		setContentTypeId("");
		setSortOrder(0);
		setDisabledWysiwyg(new ArrayList<>());
		this.indexPolicy = IndexPolicy.DEFER;
		getWritableNullProperties();
	}

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public String getTitle(){
    	try {

    		//Verifies if the content type has defined a title field
			Optional<com.dotcms.contenttype.model.field.Field> fieldFound = this.getContentType().fields().stream().
					filter(field -> field.variable().equals(TITTLE_KEY)).findAny();


			if (fieldFound.isPresent()) {
				return map.get(TITTLE_KEY)!=null?map.get(TITTLE_KEY).toString():null;
			}

			String title = getContentletAPI().getName(this, getUserAPI().getSystemUser(), false);
			map.put(TITTLE_KEY, title);

    	    return title;
		} catch (Exception e) {
			Logger.debug(this,"Unable to get title for contentlet, id: " + getIdentifier(), e);
			return  "";
		}
	}

    @Override
    public String getVersionId() {
    	return getIdentifier();
    }

    @Override
    public String getVersionType() {
    	return new String("content");
    }

    @Override
    public void setVersionId(String versionId) {
    	setIdentifier(versionId);
    }

    @Override
	public String getInode() {
		final String inode = (String) map.get(INODE_KEY);

		if(inode==null) {
			return "";
		}

		return inode;
	}

    /**
     *
     * @param inode
     */
    public void setInode(String inode) {
        map.put(INODE_KEY, inode);
    }

    /**
     *
     * @return
     */
    public long getLanguageId() {
    	return ConversionUtils.toLong(map.get(LANGUAGEID_KEY), 0l);
    }

    /**
     *
     * @param languageId
     */
    public void setLanguageId(long languageId) {
        map.put(LANGUAGEID_KEY, languageId);
    }

    /**
     *
     * @return
     */
    public String getContentTypeId() {
      return (String) map.get(STRUCTURE_INODE_KEY);
    }

    /**
     * @deprecated as of 4.1
     * use instead:
     * {@link #getContentTypeId()}
     */
    public String getStructureInode() {
        return (String) map.get(STRUCTURE_INODE_KEY);
    }

	/**
	 * @deprecated Please use the {@link #setContentTypeId(String)} method.
	 * @param structureInode
	 */
    @Deprecated
    public void setStructureInode(String structureInode) {
    	map.put(STRUCTURE_INODE_KEY, structureInode);
    }

	/**
	 * Assigns a specific Content Type to this Contentlet object.
	 *
	 * @param id
	 *            - The Content Type ID.
	 */
    public void setContentTypeId(String id) {
    	map.put(STRUCTURE_INODE_KEY, id);
    }
    /**
     *
     * @param type
     */
    public void setContentType(final ContentType type) {
        map.put(STRUCTURE_INODE_KEY, type.id());
    }
	/**
	 * @deprecated As of dotCMS 4.1.0. Please use the following approach:
	 * <pre>
	 *             {@link #getContentType()}
	 *             </pre>
	 */
	public Structure getStructure() {
		final ContentType type = this.getContentType();
		return null != type?
				new StructureTransformer(getContentType()).asStructure():null;
	}

    /**
     *
     * @return
     */
    public boolean hasAssetNameExtension() {
        boolean hasExtension = false;
        if(null != getContentType()){
           hasExtension = (getContentType().baseType() == BaseContentType.HTMLPAGE || getContentType().baseType() == BaseContentType.FILEASSET );
		}
        return hasExtension;
    }

    /**
     *
     * @return
     */
    public Date getLastReview() {
    	return (Date)map.get(LAST_REVIEW_KEY);
    }

    /**
     *
     * @param lastReview
     */
    public void setLastReview(Date lastReview) {
    	map.put(LAST_REVIEW_KEY, lastReview);
    }

    /**
     *
     * @return
     */
    public Date getNextReview() {
    	return (Date)map.get(NEXT_REVIEW_KEY);
    }

    /**
     *
     * @param nextReview
     */
    public void setNextReview(Date nextReview) {
    	map.put(NEXT_REVIEW_KEY, nextReview);
    }

    /**
     *
     * @return
     */
    public String getReviewInterval() {
    	return (String)map.get(REVIEW_INTERNAL_KEY);
    }

    /**
     *
     * @param reviewInterval
     */
    public void setReviewInterval(String reviewInterval) {
    	map.put(REVIEW_INTERNAL_KEY, reviewInterval);
    }

    /**
     *
     */
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     *
     * @param contentlet
     * @return
     * @throws DotRuntimeException
     */
    public boolean equals(Contentlet contentlet)throws DotRuntimeException {
    	try{
    	    ContentletAPI conAPI = APILocator.getContentletAPI();
    		return conAPI.isContentEqual(this, contentlet, APILocator.getUserAPI().getSystemUser(), true);
    	}catch (DotSecurityException e) {
			throw new DotRuntimeException("Security Exception happened");
		}catch (DotDataException e) {
			 throw new DotRuntimeException("Data Exception happened");
		}
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Contentlet that = (Contentlet) o;

		return getInode().equals(that.getInode());
	}

	@Override
	public int hashCode() {
		return getInode().hashCode();
	}

    /**
     *
     * @return
     */
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     *
     * @return
     */
	public List<String> getDisabledWysiwyg() {
		return (List<String>)map.get(DISABLED_WYSIWYG_KEY);
	}

	/**
	 *
	 * @param disabledFields
	 */
	public void setDisabledWysiwyg(List<String> disabledFields) {
		map.put(DISABLED_WYSIWYG_KEY, disabledFields);
	}

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public String getStringProperty(String fieldVarName) throws DotRuntimeException {
		try{
			Object value = get(fieldVarName);
			if(value instanceof Long || value instanceof Date ){
				return value.toString();
			}
			return (String)value;
		}catch (Exception e) {
			 throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param fieldVarName
	 * @param stringValue
	 * @throws DotRuntimeException
	 */
	public void setStringProperty(String fieldVarName,String stringValue) throws DotRuntimeException {
		map.put(fieldVarName, stringValue);
	}

    /**
     * @param stringValue
     * @throws DotRuntimeException
     */
    public void setStringProperty(com.dotcms.contenttype.model.field.Field field,String stringValue) throws DotRuntimeException {
        map.put(field.variable(), stringValue);
    }
	/**
	 *
	 * @param fieldVarName
	 * @param longValue
	 * @throws DotRuntimeException
	 */
	public void setLongProperty(String fieldVarName, long longValue) throws DotRuntimeException {
		map.put(fieldVarName, longValue);
	}
    public void setLongProperty(com.dotcms.contenttype.model.field.Field field,long stringValue) throws DotRuntimeException {
        map.put(field.variable(), stringValue);
    }
	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public long getLongProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Long)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}

	/**
	 *
	 * @param fieldVarName
	 * @param boolValue
	 * @throws DotRuntimeException
	 */
	public void setBoolProperty(String fieldVarName, boolean boolValue) throws DotRuntimeException {
		map.put(fieldVarName, boolValue);
	}
    /**
     * @param boolValue
     * @throws DotRuntimeException
     */
    public void setBoolProperty(com.dotcms.contenttype.model.field.Field field, boolean boolValue) throws DotRuntimeException {
        map.put(field.variable(), boolValue);
    }

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public boolean getBoolProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return map.get(fieldVarName)!=null?(Boolean)map.get(fieldVarName):false;
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}

	/**
	 *
	 * @param fieldVarName
	 * @param dateValue
	 * @throws DotRuntimeException
	 */
	public void setDateProperty(String fieldVarName, Date dateValue) throws DotRuntimeException {
		map.put(fieldVarName, dateValue);
	}

    /**
     *
     * @param field
     * @param dateValue
     * @throws DotRuntimeException
     */
    public void setDateProperty(com.dotcms.contenttype.model.field.Field field, Date dateValue) throws DotRuntimeException {
        map.put(field.variable(), dateValue);
    }

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public Date getDateProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Date)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}

	/**
	 *
	 * @param fieldVarName
	 * @param floatValue
	 * @throws DotRuntimeException
	 */
	public void setFloatProperty(String fieldVarName, float floatValue) throws DotRuntimeException {
		map.put(fieldVarName, floatValue);
	}

    /**
     * @param floatValue
     * @throws DotRuntimeException
     */
    public void setFloatProperty(com.dotcms.contenttype.model.field.Field field, float floatValue) throws DotRuntimeException {
        map.put(field.variable(), floatValue);
    }

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public float getFloatProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Float)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}

	/**
	 *
	 * @param fieldVarName
	 * @param objValue
	 * @throws DotRuntimeException
	 */
	public void setProperty( String fieldVarName, Object objValue) throws DotRuntimeException {
	    if (fieldVarName!= null && isRelationshipField(fieldVarName)){
	        setRelated(fieldVarName, (List<Contentlet>) objValue);
        } else{
            map.put(fieldVarName, objValue);
            if (!NULL_PROPERTIES.equals(fieldVarName)) { // No need to keep track of the null property it self.
                if (null == objValue) {
                    addNullProperty(fieldVarName);
                } else {
                    removeNullProperty(fieldVarName);
                }
            }
        }
	}

    /**
     * @param fieldVarName
     * @return
     */
    private boolean isRelationshipField(String fieldVarName) {
        return this.getContentType().fieldMap().containsKey(fieldVarName) && this.getContentType()
                .fieldMap().get(fieldVarName) instanceof RelationshipField;
    }

    /**
	 * Returns a map of the contentlet properties based on the fields of the structure
	 * The keys used in the map will be the velocity variables names
	 */
	public Map<String, Object> getMap() throws DotRuntimeException {
		return map;
	}

	/**
	 * Returns the deleted.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
		return InodeUtils.isSet(this.getIdentifier())?APILocator.getVersionableAPI().isDeleted(this):false;
	}

	/**
	 * Returns the live.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getVersionableAPI().isLive(this);
	}

	/**
	 * Returns the locked.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getVersionableAPI().isLocked(this);
	}

	/**
	 * Returns the modDate.
	 * @return java.util.Date
	 */
	public Date getModDate() {
		return (Date)map.get(MOD_DATE_KEY);
	}

	/**
	 * Returns the modUser.
	 * @return String
	 */
	public String getModUser() {
		return (String)map.get(MOD_USER_KEY);
	}

	/**
	 * Returns the working.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
		return InodeUtils.isSet(this.getIdentifier())?APILocator.getVersionableAPI().isWorking(this):false;
	}

	/**
	 * Sets the modDate.
	 * @param modDate The modDate to set
	 */
	public void setModDate(Date modDate) {
		map.put(MOD_DATE_KEY, modDate);
	}

	/**
	 * Sets the modUser.
	 * @param modUser The modUser to set
	 */
	public void setModUser(String modUser) {
		map.put(MOD_USER_KEY, modUser);
	}

	/**
	 * Sets the owner.
	 *
	 * @param owner
	 *            The owner to set
	 */
	public void setOwner(String owner) {
		map.put(OWNER_KEY, owner);
	}

	/**
	 * Returns the owner.
	 *
	 * @return String owner
	 */
	public String getOwner() {
		return (String)map.get(OWNER_KEY);
	}

	/**
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		return (String) map.get(IDENTIFIER_KEY);
	}

	/**
	 * @param identifier
	 *            The identifier to set.
	 */
	public void setIdentifier(String identifier) {
		map.put(IDENTIFIER_KEY, identifier);
	}

	/**
	 * Sets the sort_order.
	 * @param sortOrder The sort_order to set
	 */
	public void setSortOrder(long sortOrder) {
		map.put(SORT_ORDER_KEY, sortOrder);
	}

	/**
	 *
	 * @return
	 */
	public long getSortOrder(){
		return (Long)map.get(SORT_ORDER_KEY);
	}

	/**
	 *
	 */
	public String getPermissionId() {
		return getIdentifier();
	}

	/**
	 *
	 * @return
	 */
	public String getHost() {
		return (String) map.get(HOST_KEY);
	}

	private final static String TITLE_IMAGE_NOT_FOUND = "TITLE_IMAGE_NOT_FOUND";


    public Optional<com.dotcms.contenttype.model.field.Field> getTitleImage() {
        final ContentType type = getContentType();
        if(type==null || type.fieldMap()==null || TITLE_IMAGE_NOT_FOUND.equals(map.get(TITLE_IMAGE_KEY))) {
            return Optional.empty();
        }

        if(map.get(TITLE_IMAGE_KEY) == null) {
            String returnVal = TITLE_IMAGE_NOT_FOUND;
            for(final com.dotcms.contenttype.model.field.Field f : type.fields()) {
                try {
                    if(f instanceof BinaryField && UtilMethods.isImage(this.getBinary(f.variable()).toString())){
                        returnVal=f.variable();
                        break;
                    }
                    else if( f instanceof ImageField && UtilMethods.isSet(get(f.variable()))) {
                        returnVal=f.variable();
                        break;
                    }
                } catch (Exception e) {
                    Logger.debug(this.getClass(), e.getMessage(), e);
                }
            }
            map.put(TITLE_IMAGE_KEY, returnVal);
        }
        return Optional.ofNullable(type.fieldMap().get(String.valueOf(map.get(TITLE_IMAGE_KEY))));
    }



	/**
	 *
	 * @param host
	 */
	public void setHost(String host) {
		map.put(HOST_KEY, host);
	}

	/**
	 * Returns the inode of the folder where this contentlet lives under, if persisted, or if not persisted, where
	 * it will live under when saved/updated
	 *
	 * @return the inode of the folder
	 */
	public String getFolder() {
		return (String) map.get(FOLDER_KEY);
	}

	/**
	 * Sets the inode of the folder where this contentlet will live under when saved/updated
	 *
	 * @param folderInode the inode of the folder
	 */
	public void setFolder(String folderInode) {
		map.put(FOLDER_KEY, folderInode);
	}

	/**
	 * List of permissions it accepts
	 */
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	/**
	 *
	 */
	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	/**
	 *
	 */
	public Permissionable getParentPermissionable() throws DotDataException {

		try {

			User systemUser = APILocator.getUserAPI().getSystemUser();
			FolderAPI fAPI = APILocator.getFolderAPI();
			HostAPI hostAPI = APILocator.getHostAPI();
			Host systemHost = hostAPI.findSystemHost(systemUser, false);
			Structure st = getStructure();



			if(st != null && st.getVelocityVarName() != null && st.getVelocityVarName().equals("Host")) {
				Host hProxy = new Host(this);
				return hProxy.getParentPermissionable();
			}


			// if this contentlet is being saved in a folder, inherit from the folder
			if(InodeUtils.isSet(this.getFolder()) && ! "SYSTEM_FOLDER".equals(this.getFolder())) {
				return fAPI.find(this.getFolder(), APILocator.getUserAPI().getSystemUser(), false);
			}

			// if this contentlet is being saved in a host, inherit from the host
			if(InodeUtils.isSet(this.getHost()) && ! this.getHost().equals(systemHost.getIdentifier())) {
				return hostAPI.find(this.getHost(), systemUser, false);
			}

			// if this contentlet has a structure, inherit from that
			if(st != null && InodeUtils.isSet(st.getInode())){
				return st;
			}
			return null;

		} catch (DotSecurityException e) {
			Logger.error(Contentlet.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

	}

	/**
	 *
	 */
	public String getPermissionType() {
		return Contentlet.class.getCanonicalName();
	}

	/**
	 *
	 * @param velocityVarName
	 * @param newFile
	 * @throws IOException
	 */
	public void setBinary(String velocityVarName, File newFile)throws IOException{
		map.put(velocityVarName, newFile);
	}

    /**
     *
     * @param field
     * @param newFile
     * @throws IOException
     */
    public void setBinary(com.dotcms.contenttype.model.field.Field field, File newFile)throws IOException{
        map.put(field.variable(), newFile);
    }

	/**
	 *
	 * @param velocityVarName
	 * @return
	 * @throws IOException
	 */
	public java.io.File getBinary(String velocityVarName)throws IOException {
		File f = (File) map.get(velocityVarName);
		if((f==null || !f.exists()) ){
			f=null;
			map.remove(velocityVarName);
            if ( map.get( INODE_KEY ) != null && InodeUtils.isSet( (String) map.get( INODE_KEY ) ) ) {
                String inode = (String) map.get(INODE_KEY);
	        	try{

	        		java.io.File binaryFilefolder = new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath()
	                    + java.io.File.separator
	                    + inode.charAt(0)
	                    + java.io.File.separator
	                    + inode.charAt(1)
	                    + java.io.File.separator
	                    + inode
	                    + java.io.File.separator
	                    + velocityVarName);
	                    if(binaryFilefolder.exists()){
	                    	java.io.File[] files = binaryFilefolder.listFiles(new BinaryFileFilter());
		                    if(files.length > 0){
		                    	f = files[0];
		                    	map.put(velocityVarName, f);
		                    }
		                }
	            }catch(Exception e){
	                Logger.error(this,"Error occured while retrieving binary file name : getBinaryFileName(). ContentletInode : "+inode+"  velocityVaribleName : "+velocityVarName );
	                throw new IOException("File System error.");
	            }
			}
		}

		return f;

	}

	/**
	 *
	 * @param velocityVarName
	 * @return
	 * @throws IOException
	 */
	public InputStream getBinaryStream(String velocityVarName) throws IOException{
		InputStream fis = Files.newInputStream(getBinary(velocityVarName).toPath());
		return fis;
	}

	/**
	 *
	 * @param velocityVarName
	 * @return
	 */
	public Map<String, Object> getKeyValueProperty(String velocityVarName) {
		return com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap((String) get(velocityVarName));
	}

	/**
	 *
	 */
	public boolean isParentPermissionable() {
		Structure hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		if(this.getStructureInode().equals(hostStructure.getInode()))
			return true;
		else
			return false;
	}

	/**
	 *
	 * @param inode
	 * @param structureInode
	 * @return
	 */
    public static Object lazyMetadataLoad ( String inode, String structureInode ) {

        String cachedMetadata = CacheLocator.getContentletCache().getMetadata( inode );
        if ( cachedMetadata == null ) {
            // lazy load from db
            try {
                Structure st = CacheLocator.getContentTypeCache().getStructureByInode( structureInode );
                Object fieldVal = APILocator.getContentletAPI().loadField( inode, st.getFieldVar( FileAssetAPI.META_DATA_FIELD ) );
                if ( fieldVal != null && UtilMethods.isSet( fieldVal.toString() ) ) {
                    String loadedMetadata = fieldVal.toString();
                    CacheLocator.getContentletCache().addMetadata( inode, loadedMetadata );
                    return loadedMetadata;
                } else
                    return "";
            } catch ( DotDataException e ) {
                Logger.error( Contentlet.class, "error lazy loading metadata field", e );
                return "";
            }
        } else if ( cachedMetadata.equals( ContentletCache.EMPTY_METADATA ) ) {
            return "";
        } else {
            // normal metadata from cache
            return cachedMetadata;
        }
    }

    /**
     *
     * @param structureInode
     * @param fieldVelVarName
     * @param value
     * @return
     */
    public static boolean isMetadataFieldCached ( String structureInode, String fieldVelVarName, Object value ) {

        if ( fieldVelVarName instanceof String && fieldVelVarName.equals( FileAssetAPI.META_DATA_FIELD ) ) {
            Structure st = CacheLocator.getContentTypeCache().getStructureByInode( structureInode );
            Field f = st.getFieldVar( FileAssetAPI.META_DATA_FIELD );
            return st.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET && UtilMethods.isSet( f.getInode() )
                    && value != null && value.equals( ContentletCache.CACHED_METADATA );
        }
        return false;
    }

    /**
	 * Returns an object from the underlying contentlet Map
	 * @param key
	 * @return
	 */
	public Object get(String key){
		if(map ==null || key ==null){
			return null;
		}
		Object value=map.get(key);

		if(isMetadataFieldCached(getStructureInode(), key, value))
		    return lazyMetadataLoad(getInode(),getStructureInode());

		return value;

	}

	/**
	 * @param lowIndexPriority the lowIndexPriority to set
	 */
	public void setLowIndexPriority(boolean lowIndexPriority) {
		this.lowIndexPriority = lowIndexPriority;
	}

	/**
	 * @return the lowIndexPriority
	 */
	public boolean isLowIndexPriority() {
		return lowIndexPriority;
	}

	/**
	 *
	 */
	public String getType(){

		return "contentlet";
	}

	/**
	 *
	 * @return
	 */
    public Boolean isHTMLPage() {
        return getStructure().getStructureType() == BaseContentType.HTMLPAGE.getType();
    }

    /**
     *
     * @return
     */
	public boolean isFileAsset() {
		return getStructure().getStructureType() == BaseContentType.FILEASSET.getType();
	}

	/**
	 *
	 * @return
	 */
    public boolean isHost() {
        Structure hostStructure =
                CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");

        return getStructure().getInode().equals(hostStructure.getInode());
    }

	/**
	 * If the inode is set, means it has at least one version
	 * @return boolean true if has a version
	 */
	public boolean hasVersion () {

		return InodeUtils.isSet(this.getInode());
	}

	/**
	 * If does not has a version, means is new.
	 * @return boolean true if it is new
	 */
	public boolean isNew () {

		return !this.hasVersion();
	}

    /**
     *
     * @return
     */
    public boolean isSystemHost() {
        Boolean isSystemHost = (Boolean) getMap().get(Host.SYSTEM_HOST_KEY);

        return isSystemHost == null ? false : isSystemHost;
    }

	/**
	 * Sets the workflow action id the Contentlet is going to execute
	 */
    public void setActionId(String actionId) {
        this.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, actionId);
    }

    /**
     * Sets to null the workflow action id in the Contentlet
     */
    public void resetActionId() {
        this.setActionId(null);
    }

	/**
	 * Returns the workflow action id the Contentlet is going to execute
	 */
	public String getActionId() {
		return this.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY);
	}

    /**
     *
     * @throws DotDataException
     */
	public void setTags() throws DotDataException {
		HashMap<String, StringBuilder> contentletTags = new HashMap<>();
		List<TagInode> foundTagInodes = APILocator.getTagAPI().getTagInodesByInode(this.getInode());
		if ( foundTagInodes != null && !foundTagInodes.isEmpty() ) {

			for ( TagInode foundTagInode : foundTagInodes ) {

				StringBuilder contentletTagsBuilder = new StringBuilder();
				String fieldVarName = foundTagInode.getFieldVarName();

				if ( UtilMethods.isSet(fieldVarName) ) {
					//Getting the related tag object
					Tag relatedTag = APILocator.getTagAPI().getTagByTagId(foundTagInode.getTagId());

					if ( contentletTags.containsKey(fieldVarName) ) {
						contentletTagsBuilder = contentletTags.get(fieldVarName);
					}
					if ( contentletTagsBuilder.length() > 0 ) {
						contentletTagsBuilder.append(",");
					}
					if ( relatedTag.isPersona() ) {
						contentletTagsBuilder.append(relatedTag.getTagName() + ":persona");
					} else {
						contentletTagsBuilder.append(relatedTag.getTagName());
					}

					contentletTags.put(fieldVarName, contentletTagsBuilder);
				} else {
					Logger.error(this, "Found Tag with id [" + foundTagInode.getTagId() + "] related with Contentlet " +
							"[" + foundTagInode.getInode() + "] without an associated Field var name.");
				}
			}
		}

		/*
			Now we need to populate the contentlet tag fields with the related tags info for the edit mode,
			this is done only for display purposes.
			 */
		if ( !contentletTags.isEmpty() ) {
			for ( Map.Entry<String, StringBuilder> tagsList : contentletTags.entrySet() ) {
				//We should not store the tags inside the field, the relation must only exist on the tag_inode table
				this.setStringProperty(tagsList.getKey(), tagsList.getValue().toString());
			}
		}
	}

	/**
	 * This method is used to keep track of the null values added to the internal map
	 * @param property
	 */
	@SuppressWarnings("unchecked")
	private void addNullProperty(final String property){
		getWritableNullProperties().add(property);
	}

	/**
	 * .
	 * @param property
	 */
	@SuppressWarnings("unchecked")
	private void removeNullProperty(final String property){
		getWritableNullProperties().remove(property);
	}

	/**
	 * Convenience method to get access to the null values set that is kept within the map
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Set<String> getWritableNullProperties(){
		return (Set<String>)map.computeIfAbsent(NULL_PROPERTIES, s -> {
			return ConcurrentHashMap.newKeySet();
		});
	}

	/**
	 * This method returns an immutable copy of the null properties set to the properties map
	 * @return
	 */
	@com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
	@com.fasterxml.jackson.annotation.JsonIgnore
	@SuppressWarnings("unchecked")
	public Set<String> getNullProperties(){
		final Set<String> set = (Set<String>)this.map.get(NULL_PROPERTIES);
		if(null == set){
		   return ImmutableSet.of();
		}
		return ImmutableSet.copyOf(set);
	}

	/**
	 * Since the Contentlet is kept in cache it makes sense removing certain values from the map
	 */
	public void cleanup(){
	    getMap().remove(IS_COPY_CONTENTLET);
	    getMap().remove(SOURCE_CONTENTLET_ASSET_NAME);
		getWritableNullProperties().clear();
	}


	private class ContentletHashMap extends ConcurrentHashMap<String, Object> {
		 /**
		 *
		 */
		private static final long serialVersionUID = 4108013044908549504L;

		public ContentletHashMap() {
			super();
		}

		public Object put(final String key, final Object value) {

			Contentlet.this.markAsDirty();

			 if(value==null) {
				 Object oldValue = this.get(key);
				 this.remove(key);
				 return oldValue;
			 }
			 return super.put(key, value);
		 }
	}

	/**
	 *
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public boolean hasLiveVersion() throws DotStateException, DotDataException {
		return APILocator.getVersionableAPI().hasLiveVersion(this);
	}

	/**
	 * Get the contentlet Content Type
	 *
	 * @return the contentlet Content Type
	 */
	public ContentType getContentType() {

		try {
			final ContentType foundContentType =
					APILocator.getContentTypeAPI(APILocator.systemUser())
							.find(getContentTypeId());

			if (null != foundContentType) {
				this.contentType = foundContentType;
			}
		} catch (DotDataException | DotSecurityException e) {
			if (!ExceptionUtil.causedBy(e, NotFoundInDbException.class)) {
				throw new DotStateException(e);
			} else {
				Logger.warn(this,
						() -> String.format(
								"Unable to find Content Type for Contentlet [%s], Content Type deleted? - [%s]",
								this.getIdentifier(),
								e.getMessage()));
			}
		}

		return this.contentType;
	}

	/**
	 * Get if the contentlet is a Vanity URL
	 * @return true if the contentlet Content Type is a Vanity URL
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public boolean isVanityUrl() throws DotDataException, DotSecurityException {
		return getContentType().baseType() == BaseContentType.VANITY_URL;
	}

    /**
     * Determines whether this object belongs to a Key/Value Content Type or not.
     *
     * @return If the object is an instance of Key/Value, returns {@code true}. Otherwise, returns
     *         {@code false}.
     * @throws DotDataException An error occurred when retrieving information from the data source.
     * @throws DotSecurityException
     */
	public boolean isKeyValue() throws DotDataException, DotSecurityException {
        return getContentType().baseType() == BaseContentType.KEY_VALUE;
    }

	private ContentletAPI getContentletAPI() {
		if(contentletAPI==null) {
			contentletAPI = APILocator.getContentletAPI();
		}

		return contentletAPI;
	}

	@VisibleForTesting
	protected void setContentletAPI(ContentletAPI contentletAPI) {
		this.contentletAPI = contentletAPI;
	}

	private UserAPI getUserAPI() {
		if(userAPI==null) {
			userAPI = APILocator.getUserAPI();
		}
		return userAPI;
	}

	@VisibleForTesting
	protected void setUserAPI(UserAPI userAPI) {
		this.userAPI = userAPI;
	}

	public boolean validateMe() {
		return !UtilMethods.isSet(map.get(Contentlet.DONT_VALIDATE_ME));
	}

	/**
	 * Returns a list of all contentlets related to this instance given a RelationshipField variable
	 * @param variableName
	 * @param user
	 * @return
	 */
	public List<Contentlet> getRelated(final String variableName, final User user){
		return getRelated(variableName, user, true);
	}

	/**
	 * Returns a list of all contentlets related to this instance given a RelationshipField variable
	 * @param variableName
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public List<Contentlet> getRelated(final String variableName, final User user,
			final boolean respectFrontendRoles) {

		if (!UtilMethods.isSet(this.relatedIds)){
			relatedIds = Maps.newConcurrentMap();
		}

		try {
			User currentUser;

			if (user != null){
				currentUser = user;
			} else{
				currentUser = APILocator.getUserAPI().getAnonymousUser();
			}

			final List<Contentlet> relatedContentlet;

			if (this.relatedIds.containsKey(variableName)) {
				relatedContentlet = getCachedRelatedContentlets(variableName);
			} else {

				relatedContentlet = getNonCachedRelatedContentlets(variableName);
			}

			//Restricts contentlet according to user permissions
			return relatedContentlet.stream().filter(contentlet -> {
				try {
					return APILocator.getPermissionAPI()
							.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
									currentUser,
									currentUser.equals(APILocator.getUserAPI().getAnonymousUser())
											? true : respectFrontendRoles);
				} catch (DotDataException e) {
					return false;
				}
			}).collect(Collectors.toList());

		} catch (DotDataException | DotSecurityException e) {
			Logger.warn(this, "Error getting related content for field " + variableName, e);
			throw new DotStateException(e);
		}
	}

	/**
	 *
	 * @param variableName
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Nullable
	private List<Contentlet> getNonCachedRelatedContentlets(final String variableName)
			throws DotDataException, DotSecurityException {

		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final com.dotcms.contenttype.model.field.Field field = APILocator.getContentTypeFieldAPI()
				.byContentTypeIdAndVar(getContentTypeId(), variableName);

		final List<Contentlet> relatedList = APILocator.getContentletAPI()
				.getRelatedContent(this, APILocator.getRelationshipAPI()
								.getRelationshipFromField(field, systemUser),
						systemUser, false);

		if (UtilMethods.isSet(relatedList)) {
			this.relatedIds.put(variableName,
					relatedList.stream().map(contentlet -> contentlet.getIdentifier())
							.collect(
									CollectionsUtils.toImmutableList()));
		}else{
			this.relatedIds.put(variableName, Collections.emptyList());
		}

		return relatedList;
	}

	/**
	 *
	 * @param variableName
	 * @return
	 */
	@NotNull
	private List<Contentlet> getCachedRelatedContentlets(final String variableName) {
		final List<Contentlet> relatedList = this.relatedIds.get(variableName).stream()
				.map(identifier -> {
					try {
						return APILocator.getContentletAPI()
								.findContentletByIdentifierAnyLanguage(identifier);
					} catch (DotDataException | DotSecurityException e) {
						Logger.warn(this, "No content found with id " + identifier,
								e);
						throw new DotStateException(e);
					}
				}).collect(Collectors.toList());

		return relatedList;
	}

    /**
     * Set related content for a content given a relationship field
     * @param field Relationship {@link com.dotcms.contenttype.model.field.Field}
     * @param contentlets {@link List} of contentlets to be related
     */
    public void setRelated(final com.dotcms.contenttype.model.field.Field field,
            final List<Contentlet> contentlets) {
        setRelated(field.variable(), contentlets);
    }

    /**
     * Set related content for a content given a relationship field variable
     * @param fieldVarName Relationship field variable
     * @param contentlets {@link List} of contentlets to be related
     */
    public void setRelated(final String fieldVarName, final List<Contentlet> contentlets) {
        map.put(fieldVarName, contentlets);

        if (UtilMethods.isSet(this.relatedIds)) {
            //clean up cache
            relatedIds.remove(fieldVarName);
        }
    }

    /**
     * Set related content for a content given their IDs and a relationship field
     * @param field Relationship {@link com.dotcms.contenttype.model.field.Field}
     * @param ids {@link List} of contentlets identifiers to be related
     * @param user User to execute search (respect permissions)
     * @param respectFrontendRoles
     */
    public void setRelatedById(final com.dotcms.contenttype.model.field.Field field,
            final List<String> ids, final User user, final boolean respectFrontendRoles) {
        setRelatedById(field.variable(), ids, user, respectFrontendRoles);
    }

    /**
     * Set related content for a content given their IDs and a relationship field variable
     * @param fieldVarName Relationship field variable
     * @param ids {@link List} of contentlets identifiers to be related
     * @param user User to execute search (respect permissions)
     * @param respectFrontendRoles
     */
    public void setRelatedById(String fieldVarName, List<String> ids, final User user,
            final boolean respectFrontendRoles) {

        setRelatedByQuery(fieldVarName, ids != null ? String.join(",", ids) : null, null, user,
                respectFrontendRoles);
    }

    /**
     * Set related content for a content given a relationship field filtering by lucene query
     * @param field Relationship {@link com.dotcms.contenttype.model.field.Field}
     * @param luceneQuery Query to filter related content
     * @param sortBy Field to sort by query results
     * @param user User to execute search (respect permissions)
     * @param respectFrontendRoles
     */
    public void setRelatedByQuery(final com.dotcms.contenttype.model.field.Field field,
            final String luceneQuery, final String sortBy, final User user,
            final boolean respectFrontendRoles) {

        setRelatedByQuery(field.variable(), luceneQuery, sortBy, user, respectFrontendRoles);
    }

    /**
     * Set related content for a content given a relationship field variable filtering by lucene query
     * @param fieldVarName Relationship field variable
     * @param luceneQuery Query to filter related content
     * @param sortBy Field to sort by query results
     * @param user User to execute search (respect permissions)
     * @param respectFrontendRoles
     */
    public void setRelatedByQuery(final String fieldVarName, final String luceneQuery,
            final String sortBy, final User user, final boolean respectFrontendRoles) {
        try {
            setRelated(fieldVarName, luceneQuery != null ? RelationshipUtil
                    .filterContentlet(this.getLanguageId(), luceneQuery, sortBy, user,
                            respectFrontendRoles, false): null);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error setting related content for field " + fieldVarName
                    + ". Content identifier: " + this.getIdentifier(), e);
            throw new DotStateException(e);
        }
    }
}
