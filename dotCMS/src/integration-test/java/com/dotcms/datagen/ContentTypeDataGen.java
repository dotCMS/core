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

    private final long currentTime = System.currentTimeMillis();
    private BaseContentType baseContentType = BaseContentType.CONTENT;
    private String description = "test-structure-desc-" + currentTime;
    private boolean fixed;
    private String name = "test-structure-name-" + currentTime;
    private Date iDate = new Date();
    private String detailPage = "";
    private boolean system;
    private Inode.Type type = Inode.Type.STRUCTURE;
    private String velocityVarName = "test-structure-varname-" + currentTime;

    @SuppressWarnings("unused")
    public ContentTypeDataGen baseContentType(final BaseContentType baseContentType) {
        this.baseContentType = baseContentType;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen description(final String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen fixed(final boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen iDate(final Date iDate) {
        this.iDate = iDate;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen detailPage(final String detailPage) {
        this.detailPage = detailPage;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen system(final boolean system) {
        this.system = system;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen type(final Inode.Type type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen velocityVarName(final String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen folder(final Folder folder) {
        this.folder = folder;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen host(final Host host) {
        this.host = host;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen user(final User user) {
        this.user = user;
        return this;
    }

    @Override
    public ContentType next() {
        final Structure s = new Structure();
        s.setStructureType(baseContentType.getType());
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
    public ContentType persist(final ContentType contentType) {
        try {
            return APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist ContentType.", e);
        }


    }

    public static void remove(final ContentType contentType) {
        try {
            APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
        } catch (Exception e) {
            throw new RuntimeException("Unable to remove ContentType.", e);
        }
    }
}
