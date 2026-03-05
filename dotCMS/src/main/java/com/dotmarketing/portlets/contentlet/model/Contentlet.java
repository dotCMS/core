package com.dotmarketing.portlets.contentlet.model;

import static com.dotmarketing.portlets.contentlet.business.MetadataCache.EMPTY_METADATA_MAP;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.RelationshipUtil;
import com.dotcms.variant.VariantAPI;
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
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.BooleanUtils;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contentlet implements Serializable, Permissionable, Categorizable, Versionable, Treeable, Ruleable,
		ManifestItem {

	// Reserved fields names
	public static final String CLASS_NAME_KEY = "class";
	public static final String CON_FOLDER_KEY = "conFolder";
	public static final String CON_HOST_KEY = "conHost";
	public static final String DELETED_KEY = "deleted";
	public static final String FILE_KEY = "file";
	public static final String FORM_KEY = "form";
	public static final String IDENTIFIER_KEY = "identifier";
	public static final String INODE_KEY = "inode";
	public static final String OWNER_CAN_PUBLISH_KEY = "ownerCanPublish";
	public static final String OWNER_CAN_READ_KEY = "ownerCanRead";
	public static final String OWNER_CAN_WRITE_KEY = "ownerCanWrite";
	public static final String PERMISSIONS_KEY = "permissions";
	public static final String TYPE_KEY = "type";
	public static final String WEBSITE_KEY = "website";
	public static final String STRING_KEY = "string";
	public static final String NUMBER_KEY = "number";
	public static final String TITTLE_KEY = "title";
	public static final String LOCKED_KEY = "locked";
	public static final String ARCHIVED_KEY = "archived";
	public static final String LIVE_KEY = "live";
	public static final String WORKING_KEY = "working";
	public static final String CREATION_DATE_KEY = "creationDate";
	public static final String MOD_DATE_KEY = "modDate";
	public static final String MOD_USER_KEY = "modUser";
	public static final String MOD_USER_NAME_KEY = "modUserName";
	public static final String OWNER_KEY = "owner";
	public static final String OWNER_USER_NAME_KEY = "ownerUserName";
	public static final String PUBLISH_DATE_KEY = "publishDate";
	public static final String PUBLISH_USER_KEY = "publishUser";
	public static final String PUBLISH_USER_NAME_KEY = "publishUserName";
	public static final String HOST_KEY = "host";
	public static final String FOLDER_KEY = "folder";
	public static final String SORT_ORDER_KEY = "sortOrder";
	public static final String DISABLED_WYSIWYG_KEY = "disabledWYSIWYG";
	public static final String LANGUAGEID_KEY = "languageId";
	//	End of reserved fields names

  private static final long serialVersionUID = 1L;
  public static final String HAS_TITLE_IMAGE_KEY = "hasTitleImage";
  public static final String VARIANT_ID = "variantId";
  public static final String VARIANT = "variant";
  public static final String STRUCTURE_INODE_KEY = "stInode";
  public static final String STRUCTURE_NAME_KEY = "stName";
  public static final String CONTENT_TYPE_KEY = "contentType";
  public static final String BASE_TYPE_KEY = "baseType";
  public static final String HOST_NAME = "hostName";
  public static final String NULL_PROPERTIES = "nullProperties";
  public static final String WORKFLOW_ACTION_KEY = "wfActionId";
  public static final String WORKFLOW_ASSIGN_KEY = "wfActionAssign";
  public static final String WORKFLOW_COMMENTS_KEY = "wfActionComments";
  public static final String WORKFLOW_BULK_KEY = "wfActionBulk";
  public static final String DOT_NAME_KEY = "__DOTNAME__";

  public static final String TITLE_IMAGE_KEY = "titleImage";

  public static final String URL_MAP_FOR_CONTENT_KEY = "URL_MAP_FOR_CONTENT";

  public static final String CONTENTLET_AS_JSON = "contentletAsJson";
  public static final String ON_NUMBER_OF_PAGES = "onNumberOfPages";

  public static final String DONT_VALIDATE_ME = "_dont_validate_me";
  public static final String DISABLE_WORKFLOW = "__disable_workflow__";
  public static final String VALIDATE_EMPTY_FILE = "_validateEmptyFile_";
  public static final String STYLE_PROPERTIES_KEY = "dotStyleProperties";

  // means the contentlet is being used on unit test mode.
  // this is only for unit test. do not use on production.
  @VisibleForTesting
  public static final String IS_TEST_MODE = "_is_test_mode";

  public static final String TO_BE_PUBLISH = "to_be_publish";

  /**
   * Flag to avoid to trigger the workflow again on the checkin when it is already in progress.
   */
  public static final String WORKFLOW_IN_PROGRESS = "__workflow_in_progress__";
  public static final String IS_COPY_CONTENTLET = "_is_copy_contentlet";
  public static final String CONTENTLET_ASSET_NAME_COPY = "_contentlet_asset_name_copy";
  public static final String AUTO_ASSIGN_WORKFLOW = "AUTO_ASSIGN_WORKFLOW";
  public static final String TEMPLATE_MAPPINGS = "TEMPLATE_MAPPINGS";
  public static final String IS_COPY = "_is_being_copied";

  public static final String WORKFLOW_PUBLISH_DATE = "wfPublishDate";
  public static final String WORKFLOW_PUBLISH_TIME = "wfPublishTime";
  public static final String WORKFLOW_EXPIRE_DATE = "wfExpireDate";
  public static final String WORKFLOW_EXPIRE_TIME = "wfExpireTime";
  public static final String WORKFLOW_NEVER_EXPIRE = "wfNeverExpire";
  public static final String WORKFLOW_TIMEZONE_ID = "timezoneId";
  public static final String FILTER_KEY = "filterKey";
  public static final String WHERE_TO_SEND = "whereToSend";
  public static final String I_WANT_TO = "iWantTo";
  public static final String PATH_TO_MOVE = "_path_to_move";
  public static final String TEMP_BINARY_IMAGE_INODES_LIST = "tempBinaryImageInodesList";
  public static final String RELATIONSHIP_KEY = "__##relationships##__";
  public static final String CONTENT_TYPE_ICON = "contentTypeIcon";
  public static final String HAS_LIVE_VERSION = "hasLiveVersion";

  public static final String SKIP_RELATIONSHIPS_VALIDATION = "__skipRelationshipValidation__";

  public static final String EVENT_VAR_NAME = "calendarEvent";

  private transient ContentType contentType;
  protected Map<String, Object> map;

  private boolean lowIndexPriority = false;

  private transient ContentletAPI contentletAPI;
  private transient UserAPI userAPI;
  private transient IndexPolicy indexPolicy = null;
  private transient IndexPolicy indexPolicyDependencies = null;

  private transient boolean needsReindex;

  private transient boolean loadedTags = false;

	private String variantId = VariantAPI.DEFAULT_VARIANT.name();

	/**
	 * Returns true if this contentlet needs reindex
	 * @return true if needs reindex
	 */
	@JsonIgnore
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
	@JsonIgnore
	public IndexPolicy getIndexPolicy() {

		return (null == this.indexPolicy)
		    ? IndexPolicyProvider.getInstance().forSingleContent()
		        :indexPolicy;
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
			this.indexPolicy = indexPolicy;
	}

	public IndexPolicy getIndexPolicyDependencies() {

		return (null == this.indexPolicyDependencies)?
		    IndexPolicyProvider.getInstance().forContentDependencies():indexPolicyDependencies;
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


			this.indexPolicyDependencies = indexPolicy;

	}

	@Override
    public String getCategoryId() {
    	return getInode();
    }

  /**
   * Create a contentlet based on a map (makes a copy of it)
   *
   * @param mapIn
   */
  public Contentlet(final Map<String, Object> mapIn) {
    this();
    mapIn.values().removeIf(Objects::isNull);
    this.map.putAll(mapIn);
  }

	/**
	 * Create a contentlet based on a map (makes a copy of it)
	 * @param contentlet
	 */
	public Contentlet(final Contentlet contentlet) {
		this(contentlet.getMap());
		this.setIndexPolicy(contentlet.getIndexPolicy());

	}

  /**
   * Default class constructor.
   */
  public Contentlet() {
    this.map = new ContentletHashMap();
    setInode("");
    setIdentifier("");
    setLanguageId(0);
    setContentTypeId("");
    setSortOrder(0);
    setDisabledWysiwyg(new ArrayList<>());
    getWritableNullProperties();
    this.needsReindex = false;

  }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public final String getTitle(){
    	try {

    		if (isSet((String) this.map.get(TITTLE_KEY))) {
    			return map.get(TITTLE_KEY).toString();
			}

    		//Verifies if the content type has defined a title field
			final Optional<com.dotcms.contenttype.model.field.Field>
					fieldWithSuspectTitleFound = getFieldWithVarStartingWithTitleWord();

			String title = fieldWithSuspectTitleFound.isPresent() &&  map.get(fieldWithSuspectTitleFound.get().variable())!=null?
				map.get(fieldWithSuspectTitleFound.get().variable()).toString(): null;

			if (!isSet(title)) {
				title = this.buildName();
			}else{
                map.put(TITTLE_KEY, title);
            }

    	    return title;
		} catch (Exception e) {
			Logger.debug(this,"Unable to get title for contentlet, id: " + getIdentifier(), e);
			return  "";
		}
	}

	/**
	 * This method fetch the field and look for a listed properties, it will try to get the first text field as a title.
	 * Otherwise will try to see if there is a binary available.
	 *
	 * In addition if a title is found, it is chuck to 255.
	 *
	 * Finally there is two special cases for FileAssets and DotAssets
	 *
	 * If any match, uses the identifier as a title.
	 * @return String
	 * @throws DotContentletStateException
	 */
	@CloseDBIfOpened
	private String buildName()
			throws DotContentletStateException {

		// if already set previously
		String returnValue = (String) this.map.get(Contentlet.DOT_NAME_KEY);
		if(isSet(returnValue)){
			return returnValue;
		}

		// look for listed, text and binary fields
		final List<Field> fields = FieldsCache.getFieldsByStructureInode(this.getStructureInode());
		String binaryValue       = null;

		for (final Field field : fields) {

			try {

				if(field.isListed()  && this.map.get(field.getVelocityVarName())!=null) {

					if (APILocator.getContentletAPI().isFieldTypeString(field)) {
						returnValue = this.map.get(field.getVelocityVarName()).toString();
						break; // found one
					}

					// if it is a binary
					if (binaryValue == null && Field.FieldType.BINARY.toString().equals(field.getFieldType()) && field.isIndexed()) {
					    final File binaryFile = this.getBinary(field.getVelocityVarName());
					    if (null != binaryFile) {
                            binaryValue = binaryFile.getName();
                        }
					}
				}
			} catch(Exception e){
                Logger.warn(this.getClass(),
                        "unable to get field value " + field.getVelocityVarName()
                                + " . Content inode: " + this.getInode() + ". Reason: " + e, e);
			}
		}

		// if not found text but found binary
		returnValue = !isSet(returnValue) && isSet(binaryValue)? binaryValue:returnValue;

		if(isSet(returnValue)) {

			this.setStringProperty(Contentlet.DOT_NAME_KEY, returnValue.length() > 250 ?
					returnValue.substring(0, 250) : returnValue);
			return this.getStringProperty(Contentlet.DOT_NAME_KEY);
		}

		/// if not found listed, so try to see by type (file asset or dotasset)
		if (isSet(this.getIdentifier())) {

			if (this.isFileAsset()) {
				try {

					final String assetName = APILocator.getIdentifierAPI().find(this.getIdentifier()).getAssetName();
					this.setStringProperty(Contentlet.DOT_NAME_KEY, assetName);
				} catch (Exception e){
					Logger.warn(this.getClass(), "Unable to get assetName for contentlet with identifier: " + this.getIdentifier(), e);
				}
			} else {

				final Optional<BaseContentType> baseContentTypeOpt = this.getBaseType();
				if (baseContentTypeOpt.isPresent() && baseContentTypeOpt.get() == BaseContentType.DOTASSET) {
					try {

						final String transientNameKey = DotAssetContentType.ASSET_FIELD_VAR + "name";
						final String dotAssetName     = this.getStringProperty(transientNameKey);
						String assetName              = dotAssetName;
						if (!isSet(dotAssetName) && null != this.getBinary(DotAssetContentType.ASSET_FIELD_VAR)) {
							assetName = this.getBinary(DotAssetContentType.ASSET_FIELD_VAR).getName();
							this.setStringProperty(transientNameKey, assetName);
						}

						this.setStringProperty(Contentlet.DOT_NAME_KEY, assetName);
					} catch (Exception e) {
						Logger.warn(this.getClass(), "Unable to get binary name for contentlet with identifier: " + this.getIdentifier(), e);
					}
				}
			}
		}

		// nothing, so set identifier.
		this.setStringProperty("__NAME__", this.getIdentifier());
		return this.getIdentifier();
	}

	/**
	 * Looks for a field whose variable starts with "title" and if found returns it
	 * @return the first field found whose variable starts with "title", if any
	 */
	private Optional<com.dotcms.contenttype.model.field.Field> getFieldWithVarStartingWithTitleWord() {
		return this.getContentType().fields().stream()
				.filter(field -> isSet(field.variable())
						&& field.variable().startsWith(TITTLE_KEY)).findAny();
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
      return isSet(map.get(STRUCTURE_INODE_KEY)) ?( String)  map.get(STRUCTURE_INODE_KEY) : null;
    }

    /**
     * @deprecated as of 4.1
     * use instead:
     * {@link #getContentTypeId()}
     */

    public String getStructureInode() {
        return getContentTypeId();
    }

	/**
	 * @deprecated Please use the {@link #setContentTypeId(String)} method.
	 * @param structureInode
	 */
    @Deprecated
    public void setStructureInode(String structureInode) {
      setContentTypeId(structureInode);
    }

	/**
	 * Assigns a specific Content Type to this Contentlet object.
	 *
	 * @param id
	 *            - The Content Type ID.
	 */
    public void setContentTypeId(String id) {
      map.put(STRUCTURE_INODE_KEY,id);

    }

    /**
     *
     * @param type
     */
    public void setContentType(final ContentType type) {
      setContentTypeId(type.id());
    }
	/**
	 * @deprecated As of dotCMS 4.1.0. Please use the following approach:
	 * <pre>
	 *             {@link #getContentType()}
	 *             </pre>
	 */
    @Deprecated
    @JsonIgnore
	public Structure getStructure() {
		final ContentType type = this.getContentType();
		return null != type?
				new StructureTransformer(getContentType()).asStructure():null;
	}

    /**
     *
     * @return
     */
    @JsonIgnore
    public boolean hasAssetNameExtension() {
        boolean hasExtension = false;
        if(null != getContentType()){
           hasExtension = (getContentType().baseType() == BaseContentType.HTMLPAGE || getContentType().baseType() == BaseContentType.FILEASSET );
		}
        return hasExtension;
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
	@JsonIgnore
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     *
     * @return
     */
    @JsonIgnore
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

			return value != null ? value.toString() : null;
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
		addRemoveNullProperty(fieldVarName, stringValue);
	}

    /**
     * @param stringValue
     * @throws DotRuntimeException
     */
    public void setStringProperty(com.dotcms.contenttype.model.field.Field field,String stringValue) throws DotRuntimeException {
        setStringProperty(field.variable(), stringValue);
    }
	/**
	 *
	 * @param fieldVarName
	 * @param longValue
	 * @throws DotRuntimeException
	 */
	public void setLongProperty(String fieldVarName, long longValue) throws DotRuntimeException {
		map.put(fieldVarName, longValue);
		addRemoveNullProperty(fieldVarName, longValue);
	}
    public void setLongProperty(com.dotcms.contenttype.model.field.Field field,long longValue) throws DotRuntimeException {
        setLongProperty(field.variable(), longValue);

    }
	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public long getLongProperty(String fieldVarName) throws DotRuntimeException {
		try{
	    final Object test = map.get(fieldVarName);
	    return test == null
	        ? 0
	            : test instanceof String
	            ? Long.parseLong((String) test)
	                : ((Number) test).longValue();
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
		addRemoveNullProperty(fieldVarName, boolValue);
	}
    /**
     * @param boolValue
     * @throws DotRuntimeException
     */
    public void setBoolProperty(com.dotcms.contenttype.model.field.Field field, boolean boolValue) throws DotRuntimeException {
        setBoolProperty(field.variable(), boolValue);
    }

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public boolean getBoolProperty(String fieldVarName) throws DotRuntimeException {
		try {
			if (map.get(fieldVarName) instanceof String) {
				return BooleanUtils.toBoolean(map.get(fieldVarName).toString());
			}
			if (map.get(fieldVarName) instanceof Boolean) {
				return (Boolean) map.get(fieldVarName);
			}
			return false;
		} catch (Exception e) {
			throw new DotRuntimeException("Unable to retrieve field value", e);
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
		addRemoveNullProperty(fieldVarName, dateValue);
	}

    /**
     *
     * @param field
     * @param dateValue
     * @throws DotRuntimeException
     */
    public void setDateProperty(com.dotcms.contenttype.model.field.Field field, Date dateValue) throws DotRuntimeException {
        map.put(field.variable(), dateValue);
		addRemoveNullProperty(field.variable(), dateValue);
    }

	/**
	 *
	 * @param fieldVarName
	 * @return
	 * @throws DotRuntimeException
	 */
	public Date getDateProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return map.containsKey(fieldVarName) ? (Date) map.get(fieldVarName) : null;
		}catch (Exception e) {
			 throw new DotRuntimeException(String.format("Unable to retrive field(%s) value", fieldVarName), e);
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
		addRemoveNullProperty(fieldVarName, floatValue);
	}

    /**
     * @param floatValue
     * @throws DotRuntimeException
     */
    public void setFloatProperty(com.dotcms.contenttype.model.field.Field field, float floatValue) throws DotRuntimeException {
		setFloatProperty(field.variable(), floatValue);
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
    public void setProperty(String fieldVarName, Object objValue) throws DotRuntimeException {
        if (fieldVarName != null && isRelationshipField(fieldVarName)) {
            if (objValue instanceof List) {
                setRelated(fieldVarName, (List<Contentlet>) objValue);
                return;
            }
            //When invoked from a copyContentlet action the relationship field value is a String representation of the identifier or a query
            if (objValue instanceof String) {
                //here we might have a query or an identifier, but this method can handle both cases
                setRelatedByQuery(fieldVarName, objValue.toString(), null, APILocator.systemUser(),
                        false);
                return;
            }
        }
        map.put(fieldVarName, objValue);
        addRemoveNullProperty(fieldVarName, objValue);
    }

	private void addRemoveNullProperty(String fieldVarName, Object objValue) {
		if (!NULL_PROPERTIES.equals(fieldVarName)) { // No need to keep track of the null property itself.
			if (null == objValue) {
				addNullProperty(fieldVarName);
			} else {
				removeNullProperty(fieldVarName);
			}
		}
	}

	/**
     * @param fieldVarName
     * @return
     */
	private boolean isRelationshipField(String fieldVarName) {
		return this.getContentType().fieldMap() != null && this.getContentType().fieldMap()
				.containsKey(fieldVarName) && this.getContentType()
				.fieldMap().get(fieldVarName) instanceof RelationshipField;
	}

    /**
	 * Returns a map of the contentlet properties based on the fields of the structure
	 * The keys used in the map will be the velocity variables names
	 */
	@JsonIgnore
	public Map<String, Object> getMap() throws DotRuntimeException {

        try {
            setTags();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
		return InodeUtils.isSet(this.getIdentifier()) && APILocator.getVersionableAPI()
				.isWorking(this);
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
	  return getLongProperty(SORT_ORDER_KEY);
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

	public final static String TITLE_IMAGE_NOT_FOUND = "TITLE_IMAGE_NOT_FOUND";


    public Optional<com.dotcms.contenttype.model.field.Field> getTitleImage() {
        final ContentType type = getContentType();
        if(type==null || type.fieldMap()==null || TITLE_IMAGE_NOT_FOUND.equals(map.get(TITLE_IMAGE_KEY))) {
            return Optional.empty();
        }

        if(map.get(TITLE_IMAGE_KEY) == null) {
            String returnVal = TITLE_IMAGE_NOT_FOUND;
            for(final com.dotcms.contenttype.model.field.Field field : type.fields()) {
                try {
                    if(field instanceof BinaryField){
                        final Metadata metadata = getBinaryMetadata(field);
                        if(null != metadata && (metadata.isImage() || metadata.getContentType().contains("pdf"))){
                          returnVal = field.variable();
                          break;
                        }
                    }
                    else if( field instanceof ImageField && isSet(get(field.variable()))) {
                        returnVal=field.variable();
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
	@JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	/**
	 *
	 */
	@JsonIgnore
	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	/**
	 *
	 */
	@JsonIgnore
	public Permissionable getParentPermissionable() throws DotDataException {

		try {

			User systemUser = APILocator.getUserAPI().getSystemUser();
			FolderAPI fAPI = APILocator.getFolderAPI();
			HostAPI hostAPI = APILocator.getHostAPI();
			Host systemHost = hostAPI.findSystemHost(systemUser, false);
			ContentType type = Try.of(()->getContentType()).getOrNull();



			if(type != null && "Host".equalsIgnoreCase(type.variable())) {
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
			if(type != null && InodeUtils.isSet(type.inode())){
				return type;
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
	        		java.io.File binaryFileFolder = new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath()
	                    + java.io.File.separator
	                    + inode.charAt(0)
	                    + java.io.File.separator
	                    + inode.charAt(1)
	                    + java.io.File.separator
	                    + inode
	                    + java.io.File.separator
	                    + velocityVarName);
	                    if(binaryFileFolder.exists()){
	                    	java.io.File[] files = binaryFileFolder.listFiles(new BinaryFileFilter());
		                    if(null != files && files.length > 0){
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
	@SuppressWarnings("unchecked")
	public Map<String, Object> getKeyValueProperty(String velocityVarName) {
		final Object value = get(velocityVarName);
		if(value instanceof Map){
		   return (Map)value;
	    }
        if(value instanceof Metadata){
            return (Map)((Metadata)value).getMap();
        }
		return com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap((String) value);
	}

	/**
	 *
	 */
	@JsonIgnore
	public boolean isParentPermissionable() {
		Structure hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		if(this.getStructureInode().equals(hostStructure.getInode()))
			return true;
		else
			return false;
	}

	/**
	 * Returns the metadata associated to the field, it will expected that the field is actually a binary
	 * @see #getBinaryMetadata(com.dotcms.contenttype.model.field.Field)
	 * @param field {@link Field}
	 * @return Map
	 */
	@Deprecated
	@JsonIgnore
	public Metadata getBinaryMetadata (final Field field) throws DotDataException {

		return this.getBinaryMetadata(field.getVelocityVarName());
	}

	/**
	 * Returns the metadata associated to the field, it will expected that the field is actually a binary
	 * @param field {@link com.dotcms.contenttype.model.field.Field}
	 * @return Map
	 */
	@JsonIgnore
	public Metadata getBinaryMetadata (final com.dotcms.contenttype.model.field.Field field)
			throws DotDataException {

		return this.getBinaryMetadata(field.variable());
	}

	/**
	 *  Returns the metadata associated to the field, it will expected that the field is actually a binary
	 * @param fieldVariableName {@link String}
	 * @return Map
	 */
	@JsonIgnore
	public Metadata getBinaryMetadata (final String fieldVariableName)
			throws DotDataException {

		return APILocator.getFileMetadataAPI().getOrGenerateMetadata(this, fieldVariableName);
	}


    /**
	 * Returns an object from the underlying contentlet Map
	 * @param key
	 * @return
	 */
	public Object get(final String key) {
		if (map == null || key == null) {
			return null;
		}

		Object value = map.get(key);

		if (InodeUtils.isSet(getInode()) && FileAssetAPI.META_DATA_FIELD.equals(key)) {
			final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
			//if the metaData attribute is requested from a fileAsset that's is pretty straight forward
			// we simply return the the MD associated with the field `fileAsset`
			if (isFileAsset()) {
				final Metadata fileAssetMetadata = Try
						.of(() -> //here we only return MD if it has been already generated NOT earlier
						// That is why we're accessing the API method tha does not force it's generation
						// otherwise we would loose control and API behavior would become a bit unpredictable
								fileMetadataAPI.getMetadata(this, FileAssetAPI.BINARY_FIELD)
						).getOrNull();
				if (null != fileAssetMetadata) {
					return fileAssetMetadata.getFieldsMeta();
				}
			} else {
			    //Otherwise this will look the first indexed binary
				final Optional<Metadata> defaultMetadata = fileMetadataAPI
						.getDefaultMetadata(this);
				if (defaultMetadata.isPresent()) {
					return defaultMetadata.get().getFieldsMeta();
				}
			}

			return EMPTY_METADATA_MAP;
		}

		if (value == null) {
			value = Try.of(() -> getConstantValue(key)).getOrNull();
		}

		return value;

	}

	public String getConstantValue(final String key) {
	  try {
      if(this.getContentType()!=null && this.getContentType().fieldMap().containsKey(key)) {
        com.dotcms.contenttype.model.field.Field field = this.getContentType().fieldMap().get(key);
        if(field!=null && field instanceof ConstantField ) {
          // cache it in map for future use
          map.put(key, field.values());
          return field.values();
        }
      }
	  }
	  catch(Throwable t) {
	    Logger.warnAndDebug(this.getClass(), t.getMessage(),t);
	  }
    return null;
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
	 * It'll tell you if you're dealing with content of type htmlPage
	 * @return
	 */
    public Boolean isHTMLPage() {
      return getContentType().baseType() == BaseContentType.HTMLPAGE;
    }

    /**
     * It'll tell you if you're dealing with content of type FileAsset
     * @return
     */
	public boolean isFileAsset() {
		return getContentType().baseType() == BaseContentType.FILEASSET;
	}

	/**
	 * It'll tell you if you're dealing with content of type DotAsset
	 * @return
	 */
	public boolean isDotAsset() {
		return getContentType().baseType() == BaseContentType.DOTASSET;
	}
    /**
     * It'll tell you if you're dealing with content of type Persona
     * @return
     */
    public boolean isPersona() {
        return getContentType().baseType() == BaseContentType.PERSONA;
    }
    
    public boolean isForm() {
        return getContentType().baseType() == BaseContentType.FORM;
    }
    
	/**
	 * It'll tell you if you're dealing with content of type FileAsset that is used as container
	 * see Containers as files
	 * @return
	 */
	@JsonIgnore
	public boolean isFileAssetContainer(){
       return FileAssetContainerUtil.getInstance().isFileAssetContainer(this);
    }

	/**
	 * It'll tell you if you're dealing with content of type Host
	 * @return
	 */
    public boolean isHost() {
      
        ContentType type = getContentType();
        return type!= null && type.variable().equals(Host.HOST_VELOCITY_VAR_NAME);
    }

	/**
	 * It'll tell you if you're dealing with content of type event
	 * @return
	 */
    @JsonIgnore
	public boolean isCalendarEvent() {
		return getStructure().getStructureType() == BaseContentType.CONTENT.getType() &&  EVENT_VAR_NAME.equalsIgnoreCase(getStructure().getVelocityVarName()) ;
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
    @JsonIgnore
	public String getActionId() {
		return this.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY);
	}

    /**
     * Set the tags to the contentlet
     * @throws DotDataException
     */
    @CloseDBIfOpened
	public void setTags() throws DotDataException {

		if (!this.loadedTags && isSet(getContentTypeId())) {

			final boolean hasTagFields = this.getContentType().fields().stream().anyMatch(TagField.class::isInstance);

			if (hasTagFields) {

				final HashMap<String, StringBuilder> contentletTagsMap = new HashMap<>();
				final List<TagInode> foundTagInodes = APILocator.getTagAPI().getTagInodesByInode(this.getInode());
				if (isSet(foundTagInodes)) {

					for (final TagInode foundTagInode : foundTagInodes) {

						final String fieldVarName = foundTagInode.getFieldVarName();

						// if the map does not have already this field on the map so populate it. we do not want to override the eventual user values.
						if (!map.containsKey(fieldVarName)) {
							StringBuilder contentletTagsBuilder = new StringBuilder();

							if (isSet(fieldVarName)) {
								//Getting the related tag object
								Tag relatedTag = APILocator.getTagAPI().getTagByTagId(foundTagInode.getTagId());

								if (contentletTagsMap.containsKey(fieldVarName)) {
									contentletTagsBuilder = contentletTagsMap.get(fieldVarName);
								}
								if (contentletTagsBuilder.length() > 0) {
									contentletTagsBuilder.append(",");
								}

								contentletTagsBuilder.append(relatedTag.getTagName());

								contentletTagsMap.put(fieldVarName, contentletTagsBuilder);
							} else {

								Logger.error(this, "Found Tag with id [" + foundTagInode.getTagId() + "] related with Contentlet " +
										"[" + foundTagInode.getInode() + "] without an associated Field var name.");
							}
						}
					}
				}

				/*
				Now we need to populate the contentlet tag fields with the related tags info for the edit mode,
				this is done only for display purposes.
				 */
				if (!contentletTagsMap.isEmpty()) {
					for (final Map.Entry<String, StringBuilder> tagsList : contentletTagsMap.entrySet()) {
						//We should not store the tags inside the field, the relation must only exist on the tag_inode table
						this.setStringProperty(tagsList.getKey(), tagsList.getValue().toString());
					}
				}
			}

			this.loadedTags = true;
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
	@JsonIgnore
	private Set<String> getWritableNullProperties(){
		return (Set<String>)map.computeIfAbsent(NULL_PROPERTIES, s -> {
			return ConcurrentHashMap.newKeySet();
		});
	}

	/**
	 * This method returns an immutable copy of the null properties set to the properties map
	 * @return
	 */
	@JsonIgnore
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
	    getMap().remove(CONTENTLET_ASSET_NAME_COPY);
	    getMap().remove(TEMPLATE_MAPPINGS);
	    getMap().remove(CONTENTLET_AS_JSON);
		getWritableNullProperties().clear();
	}

	@Override
	public ManifestInfo getManifestInfo() {

		final String type = Host.class.isInstance(this.getClass()) || this.isHost() ?
				PusheableAsset.SITE.getType():PusheableAsset.CONTENTLET.getType();

		return new ManifestInfoBuilder()
			.objectType(type)
			.id(this.getIdentifier())
			.inode(this.getInode())
			.title(this.getTitle().replace("\n", ","))
			.siteId(this.getHost())
			.folderId(this.getFolder())
			.build();

	}

    public void setVariantId(final String variantId) {
		this.variantId = UtilMethods.isSet(variantId) ? variantId : VariantAPI.DEFAULT_VARIANT.name();
    }

	public String getVariantId() {
		if (!UtilMethods.isSet(variantId)) {
			this.variantId = VariantAPI.DEFAULT_VARIANT.name();
		}

		return this.variantId;
	}

	@VisibleForTesting
    public class ContentletHashMap extends ConcurrentHashMap<String, Object> {
		 /**
		 *
		 */
		private static final long serialVersionUID = 4108013044908549504L;

		public ContentletHashMap() {
			super();
		}

		public Object put(final String key, final Object newValue) {

		    final Object oldValue = this.get(key);
		    if(!java.util.Objects.equals(oldValue, newValue)) {
		        Contentlet.this.markAsDirty();
		    }

		    if(newValue==null) {
		        return super.remove(key);
		    }

            return super.put(key, newValue);

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
	 * Get the optional contentlet Base Content Type
	 * 1) first look up on the contentlet properties
	 * 2) otherwise tries to look for based on the content type (if set)
	 *
	 * returns empty if can not determine any content type
	 *
	 * @return the contentlet Base Content Type
	 */
	@JsonIgnore
	public Optional<BaseContentType> getBaseType() {

		if (this.map.containsKey(Contentlet.BASE_TYPE_KEY)) {

			return Optional.ofNullable(BaseContentType.getBaseContentType(
					(String) this.map.get(Contentlet.BASE_TYPE_KEY)));
		}

		final ContentType contentletContentType = this.getContentType();
		if (null != contentletContentType) {

			return Optional.ofNullable(contentletContentType.baseType());
		}

		return Optional.empty();
	}

	/**
	 * Get the contentlet Content Type
	 *
	 * @return the contentlet Content Type
	 */
	@JsonIgnore
	public ContentType getContentType() {
	  if(getContentTypeId()==null) return null;
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
				Logger.debug(this,
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
	public boolean isVanityUrl()  {
		return getContentType()!=null && getContentType().baseType() == BaseContentType.VANITY_URL;
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

	/**
	 * Determines whether this object belongs to a Language Variable Content Type or not.
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public boolean isLanguageVariable() throws DotDataException, DotSecurityException {
		return isKeyValue() && LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME.equals(getContentType().variable());
	}

	@JsonIgnore
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

	@VisibleForTesting
	protected void setUserAPI(UserAPI userAPI) {
		this.userAPI = userAPI;
	}
	@JsonIgnore
	public boolean validateMe() {
		return !isSet(map.get(Contentlet.DONT_VALIDATE_ME));
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
            final boolean respectFrontendRoles){
        return getRelated(variableName, user,respectFrontendRoles, null);
    }

    /**
     * Returns a list of all contentlets related to this instance given a RelationshipField variable
     * @param variableName
     * @param user
     * @param respectFrontendRoles
     * @param pullByParents
     * @return
     */
    public List<Contentlet> getRelated(final String variableName, final User user,
            final boolean respectFrontendRoles, Boolean pullByParents) {
        return APILocator.getContentletAPI()
                .getRelatedContent(this, variableName, user, respectFrontendRoles, pullByParents,
                        -1, 0, null);
    }

    /**
     * Returns a list of all contentlets related to this instance given a RelationshipField variable
     * @param variableName
     * @param user
     * @param respectFrontendRoles
     * @param pullByParents
     * @param language
     * @param live
     * @return
     */
    public List<Contentlet> getRelated(final String variableName, final User user,
            final boolean respectFrontendRoles, Boolean pullByParents, final long language, final Boolean live) {
        return APILocator.getContentletAPI()
                .getRelatedContent(this, variableName, user, respectFrontendRoles, pullByParents,
                        -1, 0, null, language, live);
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

	/**
	 * Determine if the workflow is disable for this contentlet
	 * @return boolean true if is disable
	 */
	@JsonIgnore
	public boolean isDisableWorkflow() {

		return null != this.getMap().get(Contentlet.DISABLE_WORKFLOW) &&
				Boolean.TRUE.equals(this.getMap().get(Contentlet.DISABLE_WORKFLOW));
	}

	/**
	 * Determine if the workflow is in progress for this contentlet
	 * @return
	 */
	@JsonIgnore
	public boolean isWorkflowInProgress () {

		return null != this.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS) &&
				Boolean.TRUE.equals(this.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS));
	}

}
