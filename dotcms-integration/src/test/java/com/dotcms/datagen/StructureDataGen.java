package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.util.Date;

public class StructureDataGen extends AbstractDataGen<Structure> {

    private long currentTime = System.currentTimeMillis();
    private BaseContentType structureType = BaseContentType.CONTENT;
    private String description = "test-structure-desc-" + currentTime;
    private boolean fixed;
    private String name = "test-structure-name-" + currentTime;
    private Date iDate = new Date();
    private String detailPage = "";
    private boolean system;
    private Inode.Type type = Inode.Type.STRUCTURE;
    private String velocityVarName = "test_structure_varname_" + currentTime;

    @SuppressWarnings("unused")
    public StructureDataGen structureType(BaseContentType structureType) {
        this.structureType = structureType;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen description(String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen fixed(boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen iDate(Date iDate) {
        this.iDate = iDate;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen detailPage(String detailPage) {
        this.detailPage = detailPage;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen system(boolean system) {
        this.system = system;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen type(Inode.Type type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen velocityVarName(String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen folder(Folder folder) {
        this.folder = folder;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen host(Host host) {
        this.host = host;
        return this;
    }

    @SuppressWarnings("unused")
    public StructureDataGen user(User user) {
        this.user = user;
        return this;
    }

    @Override
    public Structure next() {
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
        return s;
    }

    @WrapInTransaction
    @Override
    public Structure persist(Structure object) {
        try {
            StructureFactory.saveStructure(object);
        } catch (DotHibernateException e) {
            throw new RuntimeException("Unable to persist structure.", e);
        }

        return object;
    }

    @WrapInTransaction
    public static void remove(Structure object) {
        try {
            StructureFactory.deleteStructure(object);
        } catch (DotDataException e) {
            throw new RuntimeException("Unable to remove structure.", e);
        }
    }
}
