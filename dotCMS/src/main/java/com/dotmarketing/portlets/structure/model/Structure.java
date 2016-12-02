package com.dotmarketing.portlets.structure.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;



public class Structure extends Inode implements Permissionable, Treeable,ContentTypeIf  {

    public static final String STRUCTURE_TYPE_ALL       = "_all";


    /**
     * @deprecated As of 2016-05-16, replaced by {@link Type#CONTENT}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_CONTENT      = 1;

    /**
     * @deprecated As of 2016-05-16, replaced by {@link Type#WIDGET}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_WIDGET       = 2;

    /**
     * @deprecated As of 2016-05-16, replaced by {@link Type#FORM}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_FORM         = 3;

    /**
     * @deprecated As of 2016-05-16, replaced by {@link Type#FILEASSET}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_FILEASSET    = 4;

    /**
     * @deprecated As of 2016-05-16, replaced by {@link Type#HTMLPAGE}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_HTMLPAGE     = 5;

    /**
     * @deprecated As of 2016-05-16, replaced by  {@link Type#PERSONA}
     */
    @Deprecated
    public static final int STRUCTURE_TYPE_PERSONA      = 6;

            
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private boolean defaultStructure;
    private String reviewInterval;
    private String reviewerRole;
    private String pagedetail;
    private int structureType = BaseContentType.CONTENT.getType();
    private boolean fixed;
    private boolean system;
    private String velocityVarName;
    private String urlMapPattern;
    private String host="SYSTEM_HOST";
    private String folder="SYSTEM_FOLDER";
    private String publishDateVar;
    private String expireDateVar;
    private Date modDate;



    public String getDetailPage() {
        return pagedetail;
    }

    public void setDetailPage(String pagedetail) {
        this.pagedetail = pagedetail;
    }

    public String getPagedetail() {
        return pagedetail;
    }

    public void setPagedetail(String pagedetail) {
        this.pagedetail = pagedetail;
    }

    @Override
    public String id(){
      return this.getInode();
    }
    
    public Structure () {
        
       
        
        super.setType("structure");
        modDate = new Date();

    }

    public boolean isDefaultStructure() {
        return defaultStructure;
    }
    public void setDefaultStructure(boolean defaultStructure) {
        this.defaultStructure = defaultStructure;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void delete() throws DotHibernateException, DotDataException
    {
        boolean recursive = true;
        delete(recursive);
    }

    public void delete(boolean recursive) throws DotHibernateException, DotDataException
    {
        if(recursive)
        {
            List<Field> list = FieldFactory.getFieldsByStructure(inode);
            for(int i = 0;i < list.size();i++)
            {
                Field field = (Field) list.get(i);
                field.delete();
            }
        }
        StructureFactory.deleteStructure(this);
    }
    /**
     * @deprecated  As of version dotCMS 1.2, this will be have private access, replaced by
     *              {FieldsCache.getFields(inode)}
     */
    public List<Field> getFields()
    {
        return FieldFactory.getFieldsByStructure(inode);
    }
    public List<Field> getFieldsBySortOrder()
    {
        return FieldFactory.getFieldsByStructureSortedBySortOrder(inode);
    }



    public String getReviewerRole() {
        return reviewerRole;
    }

    public void setReviewerRole(String reviewerRole) {
        this.reviewerRole = reviewerRole;
    }

    public String getReviewInterval() {
        return reviewInterval;
    }

    public void setReviewInterval(String reviewInterval) {
        this.reviewInterval = reviewInterval;
    }

    /**
     * Retrieves a structure field based on the field label name
     * This is a not recommended way to obtain a field since the
     * field label name can be changed by the user
     * @param fieldName
     * @return
     * @deprecated This is a not recommended way to obtain a field since the
     * field label name can be changed by the user
     */
    public Field getField(String fieldName)
    {
        List<Field> fields = FieldsCache.getFieldsByStructureInode(inode);
        for(Field field : fields)
        {
            if(field.getFieldName().equals(fieldName))
            {
                return field;
            }
        }
        return new Field();
    }


    /**
     * Retrieves a field by the velocity variable name,
     * This should be the preferred method to obtain a field
     * since the velocity variable name of the field never changes
     * after it gets created.
     * @param velocityVarName
     * @return The field or null if the field doesn't exist
     */
    public Field getFieldVar(String velocityVarName)
    {
        List<Field> fields = FieldsCache.getFieldsByStructureInode(inode);
        for(Field field : fields)
        {
            if(field.getVelocityVarName().equals(velocityVarName))
            {
                return field;
            }
        }
        return null;
    }

    public boolean isContent() {
        return structureType == STRUCTURE_TYPE_CONTENT;
    }

    public boolean isWidget() {
        return structureType == STRUCTURE_TYPE_WIDGET;
    }

    public boolean isForm() {
        return structureType == STRUCTURE_TYPE_FORM;
    }
    public boolean isFileAsset() {
        return structureType == STRUCTURE_TYPE_FILEASSET;

    }
    public boolean isHTMLPageAsset() {
        return structureType==STRUCTURE_TYPE_HTMLPAGE;
    }
    public boolean isPersona(){
        return structureType==STRUCTURE_TYPE_PERSONA;
    }
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public int getStructureType() {
        return structureType;
    }

    public void setStructureType(int structureType) {
        this.structureType = structureType;
    }

    public Map<String, Object> getMap(){
        return UtilMethods.toMap(this);
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

    @JsonIgnore
    public Permissionable getParentPermissionable() throws DotDataException {
        try {

            if(UtilMethods.isSet(getFolder()) && !getFolder().equals("SYSTEM_FOLDER")){

                return APILocator.getFolderAPI().find(getFolder(), APILocator.getUserAPI().getSystemUser(), false);

            }else if(UtilMethods.isSet(getHost()) && !getHost().equals("SYSTEM_HOST")){

                try {
                    return APILocator.getHostAPI().find(getHost(), APILocator.getUserAPI().getSystemUser(), false);
                } catch (DotSecurityException e) {
                    Logger.debug(Structure.class, e.getMessage(), e);
                }
            }
            return APILocator.getHostAPI().findSystemHost();
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }


    @Override
    public boolean isParentPermissionable() {
        return true;
    }

    public void setVelocityVarName(String velocityVarName) {
        this.velocityVarName = velocityVarName;
    }

    public String getVelocityVarName() {
        return velocityVarName;
    }

    public void setUrlMapPattern(String urlMapPattern) {
        this.urlMapPattern = urlMapPattern;
    }

    public String getUrlMapPattern() {
        return urlMapPattern;
    }

    /**
     * Returns the inode of the folder where this structure lives under, if persisted, or if not persisted, where
     * it will live under when saved/updated
     *
     * @return the inode of the folder
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the inode of the folder where this structure will live under when saved/udpdated
     *
     * @param folderInode the inode of the folder
     */
    public void setFolder(String folderInode) {
        this.folder = folderInode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String toString() {

        return ToStringBuilder.reflectionToString(this);
    }


    public String getPublishDateVar() {
        return publishDateVar;
    }


    public void setPublishDateVar(String publishDateVar) {
        this.publishDateVar = publishDateVar;
    }


    public String getExpireDateVar() {
        return expireDateVar;
    }


    public void setExpireDateVar(String expireDateVar) {
        this.expireDateVar = expireDateVar;
    }


    public Date getModDate() {
        return modDate;
    }


    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

}
