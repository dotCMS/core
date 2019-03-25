package com.dotcms.datagen;

import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

public class ContentTypeDataGen extends AbstractDataGen<ContentType> {

    private final long currentTime = System.currentTimeMillis();
    private BaseContentType baseContentType = BaseContentType.CONTENT;
    private String descriptionField = "test-structure-desc-" + currentTime;
    private boolean fixedField;
    private String name = "test-structure-name-" + currentTime;
    private Date iDateField = new Date();
    private String detailPageField = "";
    private boolean systemField;
    private Inode.Type type = Inode.Type.STRUCTURE;
    private String velocityVarNameField = "test-structure-varname-" + currentTime;
    private List<Field> fields=ImmutableList.of();
    @SuppressWarnings("unused")
    public ContentTypeDataGen baseContentType(final BaseContentType baseContentType) {
        this.baseContentType = baseContentType;
        return this;
    }
    @SuppressWarnings("unused")
    public ContentTypeDataGen fields(List<Field> fields) {
        this.fields = fields;
        return this;
    }
    @SuppressWarnings("unused")
    public ContentTypeDataGen description(final String description) {
        this.descriptionField = description;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen fixed(final boolean fixed) {
        this.fixedField = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen iDate(final Date iDate) {
        this.iDateField = iDate;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen detailPage(final String detailPage) {
        this.detailPageField = detailPage;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen system(final boolean system) {
        this.systemField = system;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen type(final Inode.Type type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public ContentTypeDataGen velocityVarName(final String velocityVarName) {
        this.velocityVarNameField = velocityVarName;
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
        s.setDescription(descriptionField);
        s.setFixed(fixedField);
        s.setName(name);
        s.setOwner(user.getUserId());
        s.setDetailPage(detailPageField);
        s.setSystem(systemField);
        s.setType(type.getValue());
        s.setVelocityVarName(velocityVarNameField);
        s.setFolder(folder.getInode());
        s.setHost(host.getIdentifier());
        s.setIDate(iDateField);
        return new StructureTransformer(s).from();
    }

    @Override
    public ContentType persist(final ContentType contentType) {
        try {
            return APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType, fields);
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
