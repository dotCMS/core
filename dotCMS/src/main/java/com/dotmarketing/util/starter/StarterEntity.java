package com.dotmarketing.util.starter;


import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutsRoles;
import com.dotmarketing.business.PortletsLayouts;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UsersRoles;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.LinkVersionInfo;
import com.dotmarketing.portlets.rules.util.RulesImportExportObject;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Image;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that models an entity to be used when a starter is initialized.
 * Entity examples: {@link com.dotmarketing.portlets.folders.model.Folder}, {@link com.dotmarketing.portlets.templates.model.Template}
 * @author nollymarlonga
 */
class StarterEntity {

    private final String fileName;
    private final Object type;

    private StarterEntity(final Builder builder){
        this.fileName = builder.fileName;
        this.type = builder.type;
    }

    public String fileName(){
        return fileName;
    }

    public Object type(){
        return type;
    }

    static class Builder {

        private String fileName;
        private Object type;

        public static Builder newInstance()
        {
            return new Builder();
        }

        private Builder() {}

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setType(Object type) {
            this.type = type;
            return this;
        }

        public StarterEntity build(){
            return new StarterEntity(this);
        }
    }

    public static List<StarterEntity> entitiesToImport = ImmutableList.of(
            StarterEntity.Builder.newInstance().setFileName("Company.json")
                    .setType(new TypeReference<List<Company>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Role.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Role>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(User.class.getCanonicalName() + ".json")
                    .setType(new TypeReference<List<User>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(UsersRoles.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<UsersRoles>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Layout.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Layout>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(PortletsLayouts.class.getCanonicalName())
                    .setType(new TypeReference<List<PortletsLayouts>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(LayoutsRoles.class.getCanonicalName())
                    .setType(new TypeReference<List<LayoutsRoles>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Language.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Language>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Identifier.class.getCanonicalName())
                    .setType(new TypeReference<List<Identifier>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Folder.class.getCanonicalName())
                    .setType(new TypeReference<List<Folder>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(ContentTypeImportExportUtil.CONTENT_TYPE_FILE_EXTENSION)
                    .setType(ContentType.class).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(Relationship.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Relationship>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(Contentlet.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<com.dotcms.content.model.Contentlet>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Template.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Template>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Link.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Link>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(Container.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Container>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(ContainerStructure.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<ContainerStructure>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Category.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Category>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Tree.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Tree>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName("WorkflowSchemeImportExportObject.json")
                    .setType(WorkflowSchemeImportExportObject.class).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(Permission.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Permission>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(ContentletVersionInfo.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<ContentletVersionInfo>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(LinkVersionInfo.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<LinkVersionInfo>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(TemplateVersionInfo.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<TemplateVersionInfo>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(ContainerVersionInfo.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<ContainerVersionInfo>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName("RuleImportExportObject.json")
                    .setType(RulesImportExportObject.class).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(WorkflowTask.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<WorkflowTask>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(WorkflowHistory.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<WorkflowHistory>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(WorkflowComment.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<WorkflowComment>>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(WorkFlowTaskFiles.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<WorkFlowTaskFiles>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(Tag.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<Tag>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName("Counter.json")
                    .setType(new TypeReference<List>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName("Image.json")
                    .setType(new TypeReference<List<Image>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName("Portlet.json")
                    .setType(new TypeReference<List>() {
                    }).build(),
            StarterEntity.Builder.newInstance()
                    .setFileName(MultiTree.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<MultiTree>>() {
                    }).build(),
            StarterEntity.Builder.newInstance().setFileName(TagInode.class.getCanonicalName() + "_")
                    .setType(new TypeReference<List<TagInode>>() {
                    }).build()
    );
}
