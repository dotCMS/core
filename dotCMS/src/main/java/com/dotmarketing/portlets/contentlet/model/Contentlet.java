package com.dotmarketing.portlets.contentlet.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.org.apache.commons.lang.builder.HashCodeBuilder;
import com.dotcms.repackage.org.apache.commons.lang.builder.ToStringBuilder;
import com.dotcms.sync.Exportable;
import com.dotcms.sync.Importable;
import com.dotcms.sync.exception.DotDependencyException;
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
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * @author David Tores
 */
public class Contentlet implements Serializable, Permissionable, Categorizable, Versionable, Exportable, Importable, Treeable, Ruleable {

    private static final long serialVersionUID = 1L;
    public static final String INODE_KEY = "inode";
    public static final String LANGUAGEID_KEY = "languageId";
    public static final String STRUCTURE_INODE_KEY = "stInode";
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
    public static final String FOLDER_KEY = "folder";
    public static final String WORKFLOW_ACTION_KEY = "wfActionId";
    public static final String WORKFLOW_ASSIGN_KEY = "wfActionAssign";
    public static final String WORKFLOW_COMMENTS_KEY = "wfActionComments";

    public static final String DONT_VALIDATE_ME = "_dont_validate_me";
    public static final String DISABLE_WORKFLOW = "__disable_workflow__";

    public static final String WORKFLOW_PUBLISH_DATE = "wfPublishDate";
    public static final String WORKFLOW_PUBLISH_TIME = "wfPublishTime";
    public static final String WORKFLOW_EXPIRE_DATE = "wfExpireDate";
    public static final String WORKFLOW_EXPIRE_TIME = "wfExpireTime";
    public static final String WORKFLOW_NEVER_EXPIRE = "wfNeverExpire";
    public static final String TEMP_BINARY_IMAGE_INODES_LIST = "tempBinaryImageInodesList";

    protected Map<String, Object> map = new ContentletHashMap();
    private boolean lowIndexPriority = false;


    public String getCategoryId() {
        return getInode();
    }

    public Contentlet(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * default constructor
     */
    public Contentlet() {
        setInode("");
        setIdentifier("");
        setLanguageId(0);
        setStructureInode("");
        setSortOrder(0);
        setDisabledWysiwyg(new ArrayList<String>());
        //setHost(HostFactory.SYSTEM_HOST);//http://jira.dotmarketing.net/browse/DOTCMS-3232
        //setFolder(FolderFactory.SYSTEM_FOLDER);
    }


    @JsonIgnore
    public List<Exportable> getDependencies() {
        // TODO Auto-generated method stub
        List<Exportable> ret = new ArrayList<Exportable>();
        ret.add(getStructure());
        for (Field f : getStructure().getFields()) {
            ret.addAll(f.getDependencies());
        }
        ret.add(this);
        return ret;
    }


    public boolean isDependenciesMet() throws DotDependencyException {
        // TODO Auto-generated method stub
        return false;
    }

    public String getTitle() {

        if (map.get("title") != null) {
            return map.get("title").toString();
        }

        try {
            ContentletAPI conAPI = APILocator.getContentletAPI();
            String x = conAPI.getName(this, APILocator.getUserAPI().getSystemUser(), false);
            map.put("title", x);
            return x;
        } catch (Exception e) {
            Logger.error(this, "Unable to get title", e);
            return "";
        }
    }


    public String getVersionId() {
        return getIdentifier();
    }

    public String getVersionType() {
        return new String("content");
    }

    public void setVersionId(String versionId) {
        setIdentifier(versionId);
    }

    public String getInode() {
        if (InodeUtils.isSet((String) map.get(INODE_KEY)))
            return (String) map.get(INODE_KEY);

        return "";
    }

    public void setInode(String inode) {
        map.put(INODE_KEY, inode);
    }

    public long getLanguageId() {
        return (Long) map.get(LANGUAGEID_KEY);
    }

    public void setLanguageId(long languageId) {
        map.put(LANGUAGEID_KEY, languageId);
    }

    public String getStructureInode() {
        return (String) map.get(STRUCTURE_INODE_KEY);
    }

    public void setStructureInode(String structureInode) {
        map.put(STRUCTURE_INODE_KEY, structureInode);
    }

    /**
     */
    public Structure getStructure() {
        Structure structure = null;
        structure = CacheLocator.getContentTypeCache().getStructureByInode(getStructureInode());
        return structure;
    }

    public boolean hasAssetNameExtension() {
        boolean hasExtension = false;
        if (getStructure() != null
                && getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
            hasExtension = true;
        }

        return hasExtension;
    }

    public Date getLastReview() {
        return (Date) map.get(LAST_REVIEW_KEY);
    }

    public void setLastReview(Date lastReview) {
        map.put(LAST_REVIEW_KEY, lastReview);
    }

    public Date getNextReview() {
        return (Date) map.get(NEXT_REVIEW_KEY);
    }

    public void setNextReview(Date nextReview) {
        map.put(NEXT_REVIEW_KEY, nextReview);
    }

    public String getReviewInterval() {
        return (String) map.get(REVIEW_INTERNAL_KEY);
    }

    public void setReviewInterval(String reviewInterval) {
        map.put(REVIEW_INTERNAL_KEY, reviewInterval);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Contentlet contentlet) throws DotRuntimeException {
        try {
            ContentletAPI conAPI = APILocator.getContentletAPI();
            return conAPI.isContentEqual(this, contentlet, APILocator.getUserAPI().getSystemUser(), true);
        } catch (DotSecurityException e) {
            throw new DotRuntimeException("Security Exception happened");
        } catch (DotDataException e) {
            throw new DotRuntimeException("Data Exception happened");
        }
    }

    public boolean equals(Object o) {

        if (!(o instanceof Contentlet))
            return false;
        return o != null && ((Contentlet) o).getInode().equalsIgnoreCase(this.getInode());
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getInode()).toHashCode();
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public List<String> getDisabledWysiwyg() {
        return (List<String>) map.get(DISABLED_WYSIWYG_KEY);
    }

    public void setDisabledWysiwyg(List<String> disabledFields) {
        map.put(DISABLED_WYSIWYG_KEY, disabledFields);
    }

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

    public void setStringProperty(String fieldVarName, String stringValue) throws DotRuntimeException {
        map.put(fieldVarName, stringValue);
    }

    public void setLongProperty(String fieldVarName, long longValue) throws DotRuntimeException {
        map.put(fieldVarName, longValue);
    }

    public long getLongProperty(String fieldVarName) throws DotRuntimeException {
        try {
            return (Long) map.get(fieldVarName);
        } catch (Exception e) {
            throw new DotRuntimeException("Unable to retrive field value", e);
        }
    }

    public void setBoolProperty(String fieldVarName, boolean boolValue) throws DotRuntimeException {
        map.put(fieldVarName, boolValue);
    }

    public boolean getBoolProperty(String fieldVarName) throws DotRuntimeException {
        try {
            return (Boolean) map.get(fieldVarName);
        } catch (Exception e) {
            throw new DotRuntimeException("Unable to retrive field value", e);
        }
    }

    public void setDateProperty(String fieldVarName, Date dateValue) throws DotRuntimeException {
        map.put(fieldVarName, dateValue);
    }

    public Date getDateProperty(String fieldVarName) throws DotRuntimeException {
        try {
            return (Date) map.get(fieldVarName);
        } catch (Exception e) {
            throw new DotRuntimeException("Unable to retrive field value", e);
        }
    }

    public void setFloatProperty(String fieldVarName, float floatValue) throws DotRuntimeException {
        map.put(fieldVarName, floatValue);
    }

    public float getFloatProperty(String fieldVarName) throws DotRuntimeException {
        try {
            return (Float) map.get(fieldVarName);
        } catch (Exception e) {
            throw new DotRuntimeException("Unable to retrive field value", e);
        }
    }

    public void setProperty(String fieldVarName, Object objValue) throws DotRuntimeException {
        map.put(fieldVarName, objValue);
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
     *
     * @return boolean
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
        return InodeUtils.isSet(this.getIdentifier()) ? APILocator.getVersionableAPI().isDeleted(this) : false;
    }

    /**
     * Returns the live.
     *
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
     *
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
     *
     * @return java.util.Date
     */
    public Date getModDate() {
        return (Date) map.get(MOD_DATE_KEY);
    }

    @Override
    public String getName() {
        return getTitle();
    }

    /**
     * Returns the modUser.
     *
     * @return String
     */
    public String getModUser() {
        return (String) map.get(MOD_USER_KEY);
    }

    /**
     * Returns the working.
     *
     * @return boolean
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
        return InodeUtils.isSet(this.getIdentifier()) ? APILocator.getVersionableAPI().isWorking(this) : false;
    }

    /**
     * Sets the modDate.
     *
     * @param modDate The modDate to set
     */
    public void setModDate(Date modDate) {
        map.put(MOD_DATE_KEY, modDate);
    }

    /**
     * Sets the modUser.
     *
     * @param modUser The modUser to set
     */
    public void setModUser(String modUser) {
        map.put(MOD_USER_KEY, modUser);
    }

    /**
     * Sets the owner.
     *
     * @param owner The owner to set
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
        return (String) map.get(OWNER_KEY);
    }

    /**
     * @return Returns the identifier.
     */
    public String getIdentifier() {
        return (String) map.get(IDENTIFIER_KEY);
    }

    /**
     * @param identifier The identifier to set.
     */
    public void setIdentifier(String identifier) {
        map.put(IDENTIFIER_KEY, identifier);
    }

    /**
     * Sets the sort_order.
     *
     * @param sort_order The sort_order to set
     */
    public void setSortOrder(long sortOrder) {
        map.put(SORT_ORDER_KEY, sortOrder);
    }

    public long getSortOrder() {
        return (Long) map.get(SORT_ORDER_KEY);
    }

    public String getPermissionId() {
        return getIdentifier();
    }

    //http://jira.dotmarketing.net/browse/DOTCMS-1073
    /*public void setBinary(String fieldVarName,String value) throws DotRuntimeException {
		map.put(fieldVarName, value);
	}
	public String removeBinary(String fieldVarName) throws DotRuntimeException {
		return (String) map.remove(fieldVarName);
	}*/
    //http://jira.dotmarketing.net/browse/DOTCMS-3232
    public String getHost() {
        return (String) map.get(HOST_KEY);
    }

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

    public List<RelatedPermissionableGroup> permissionDependencies(
            int requiredPermission) {
        return null;
    }

    public Permissionable getParentPermissionable() throws DotDataException {

        try {

            User systemUser = APILocator.getUserAPI().getSystemUser();
            FolderAPI fAPI = APILocator.getFolderAPI();
            HostAPI hostAPI = APILocator.getHostAPI();
            Host systemHost = hostAPI.findSystemHost(systemUser, false);
            Structure st = getStructure();


            if (st != null && st.getVelocityVarName() != null && st.getVelocityVarName().equals("Host")) {
                Host hProxy = new Host(this);
                return hProxy.getParentPermissionable();
            }


            // if this contentlet is being saved in a folder, inherit from the folder
            if (InodeUtils.isSet(this.getFolder()) && !"SYSTEM_FOLDER".equals(this.getFolder())) {
                return fAPI.find(this.getFolder(), APILocator.getUserAPI().getSystemUser(), false);
            }

            // if this contentlet is being saved in a host, inherit from the host
            if (InodeUtils.isSet(this.getHost()) && !this.getHost().equals(systemHost.getIdentifier())) {
                return hostAPI.find(this.getHost(), systemUser, false);
            }

            // if this contentlet has a structure, inherit from that
            if (st != null && InodeUtils.isSet(st.getInode())) {
                return st;
            }
            return null;

        } catch (DotSecurityException e) {
            Logger.error(Contentlet.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }

    }

    public String getPermissionType() {
        return Contentlet.class.getCanonicalName();
    }

    //http://jira.dotmarketing.net/browse/DOTCMS-3463
    public void setBinary(String velocityVarName, File newFile) throws IOException {
        map.put(velocityVarName, newFile);
    }

    public java.io.File getBinary(String velocityVarName) throws IOException {
        File f = (File) map.get(velocityVarName);
        if ((f == null || !f.exists())) {
            f = null;
            map.remove(velocityVarName);
            if (map.get(INODE_KEY) != null && InodeUtils.isSet((String) map.get(INODE_KEY))) {
                String inode = (String) map.get(INODE_KEY);
                try {

                    java.io.File binaryFilefolder = new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath()
                            + java.io.File.separator
                            + inode.charAt(0)
                            + java.io.File.separator
                            + inode.charAt(1)
                            + java.io.File.separator
                            + inode
                            + java.io.File.separator
                            + velocityVarName);
                    if (binaryFilefolder.exists()) {
                        java.io.File[] files = binaryFilefolder.listFiles(new BinaryFileFilter());
                        if (files.length > 0) {
                            f = files[0];
                            map.put(velocityVarName, f);
                        }
                    }
                } catch (Exception e) {
                    Logger.error(this, "Error occured while retrieving binary file name : getBinaryFileName(). ContentletInode : " + inode + "  velocityVaribleName : " + velocityVarName);
                    throw new IOException("File System error.");
                }
            }
        }

        return f;

    }

    public InputStream getBinaryStream(String velocityVarName) throws IOException {
        FileInputStream fis = new FileInputStream(getBinary(velocityVarName));
        return fis;
    }

    public Map<String, Object> getKeyValueProperty(String velocityVarName) {
        return com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap((String) get(velocityVarName));
    }


    public boolean isParentPermissionable() {
        Structure hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
        if (this.getStructureInode().equals(hostStructure.getInode()))
            return true;
        else
            return false;
    }

    public static Object lazyMetadataLoad(String inode, String structureInode) {

        String cachedMetadata = CacheLocator.getContentletCache().getMetadata(inode);
        if (cachedMetadata == null) {
            // lazy load from db
            try {
                Structure st = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
                Object fieldVal = APILocator.getContentletAPI().loadField(inode, st.getFieldVar(FileAssetAPI.META_DATA_FIELD));
                if (fieldVal != null && UtilMethods.isSet(fieldVal.toString())) {
                    String loadedMetadata = fieldVal.toString();
                    CacheLocator.getContentletCache().addMetadata(inode, loadedMetadata);
                    return loadedMetadata;
                } else
                    return "";
            } catch (DotDataException e) {
                Logger.error(Contentlet.class, "error lazy loading metadata field", e);
                return "";
            }
        } else if (cachedMetadata.equals(ContentletCache.EMPTY_METADATA)) {
            return "";
        } else {
            // normal metadata from cache
            return cachedMetadata;
        }
    }

    public static boolean isMetadataFieldCached(String structureInode, String fieldVelVarName, Object value) {

        if (fieldVelVarName instanceof String && fieldVelVarName.equals(FileAssetAPI.META_DATA_FIELD)) {
            Structure st = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
            Field f = st.getFieldVar(FileAssetAPI.META_DATA_FIELD);
            return st.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET && UtilMethods.isSet(f.getInode())
                    && value != null && value.equals(ContentletCache.CACHED_METADATA);
        }
        return false;
    }

    /**
     * Returns an object from the underlying contentlet Map
     *
     * @param key
     * @return
     */
    public Object get(String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);

        // http://jira.dotmarketing.net/browse/DOTCMS-7335
        if (isMetadataFieldCached(getStructureInode(), key, value))
            return lazyMetadataLoad(getInode(), getStructureInode());

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

    public String getType() {

        return "contentlet";
    }

    public Boolean isHTMLPage() {
        return getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE;
    }

    public boolean isFileAsset() {
        return getStructure().getStructureType() == Structure.Type.FILEASSET.getType();
    }

    public boolean isHost() {
        Structure hostStructure =
                CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");

        return getStructure().getInode().equals(hostStructure.getInode());
    }

    public boolean isSystemHost() {
        Boolean isSystemHost = (Boolean) getMap().get(Host.SYSTEM_HOST_KEY);

        return isSystemHost == null ? false : isSystemHost;
    }

    public void setTags() throws DotDataException {
        HashMap<String, StringBuilder> contentletTags = new HashMap<>();
        List<TagInode> foundTagInodes = APILocator.getTagAPI().getTagInodesByInode(this.getInode());
        if (foundTagInodes != null && !foundTagInodes.isEmpty()) {

            for (TagInode foundTagInode : foundTagInodes) {

                StringBuilder contentletTagsBuilder = new StringBuilder();
                String fieldVarName = foundTagInode.getFieldVarName();

                if (UtilMethods.isSet(fieldVarName)) {
                    //Getting the related tag object
                    Tag relatedTag = APILocator.getTagAPI().getTagByTagId(foundTagInode.getTagId());

                    if (contentletTags.containsKey(fieldVarName)) {
                        contentletTagsBuilder = contentletTags.get(fieldVarName);
                    }
                    if (contentletTagsBuilder.length() > 0) {
                        contentletTagsBuilder.append(",");
                    }
                    if (relatedTag.isPersona()) {
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
        if (!contentletTags.isEmpty()) {
            for (Map.Entry<String, StringBuilder> tagsList : contentletTags.entrySet()) {
                //We should not store the tags inside the field, the relation must only exist on the tag_inode table
                this.setStringProperty(tagsList.getKey(), tagsList.getValue().toString());
            }
        }
    }

    public class ContentletHashMap extends ConcurrentHashMap<String, Object> {
        /**
         *
         */
        private static final long serialVersionUID = 4108013044908549504L;

        public ContentletHashMap() {
            super();
        }

        public Object put(String key, Object value) {
            if (value == null) {
                Object oldValue = this.get(key);
                this.remove(key);
                return oldValue;
            }
            return super.put(key, value);
        }
    }

    public boolean hasLiveVersion() throws DotStateException, DotDataException {
        return APILocator.getVersionableAPI().hasLiveVersion(this);
    }
}