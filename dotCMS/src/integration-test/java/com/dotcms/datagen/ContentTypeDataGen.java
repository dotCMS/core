package com.dotcms.datagen;

import java.util.Date;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public class ContentTypeDataGen extends AbstractDataGen<ContentType> {

    private long currentTime = System.currentTimeMillis();
    private BaseContentType structureType = BaseContentType.CONTENT;
    private String description = "test-structure-desc-" + currentTime;
    private boolean fixed;
    private String name = "test-structure-name-" + currentTime;
    private Date iDate = new Date();
    private String detailPage = "";
    private boolean system;
    private Inode.Type type = Inode.Type.STRUCTURE;
    private String velocityVarName = "test-structure-varname-" + currentTime;

    @SuppressWarnings("unused")
    public ContentTypeDataGen structureType(BaseContentType structureType) {
        this.structureType = structureType;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen description(String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen fixed(boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen iDate(Date iDate) {
        this.iDate = iDate;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen detailPage(String detailPage) {
        this.detailPage = detailPage;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen system(boolean system) {
        this.system = system;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen type(Inode.Type type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen velocityVarName(String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen folder(Folder folder) {
        this.folder = folder;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen host(Host host) {
        this.host = host;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen user(User user) {
        this.user = user;
        return this;
    }

    @Override
    public ContentType next() {
        Structure s = new Structure();
        s.setStructureType(structureType.getType());
        s.setDescription(description);
        s.setFixed(fixed);
        s.setName(name);
        s.setOwner(user.getUserId());
        s.setDetailPage(detailPage);
        s.setSystem(system);
        s.setType(type.getValue());
        s.setVelocityVarName(velocityVarName);
        s.setFolder(folder.getInode());
        s.setHost(host.getIdentifier());
        s.setIDate(iDate);
        return new StructureTransformer(s).from();
    }

    @Override
    public ContentType persist(ContentType object) {
        try {
            object = APILocator.getContentTypeAPI(APILocator.systemUser()).save(object);
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist ContentType.", e);
        }

        return object;
    }

    public static void remove(ContentType object) {
        try {
            APILocator.getContentTypeAPI(APILocator.systemUser()).delete(object);
        } catch (Exception e) {
            throw new RuntimeException("Unable to remove ContentType.", e);
        }
    }
}
