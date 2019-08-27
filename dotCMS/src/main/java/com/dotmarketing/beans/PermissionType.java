package com.dotmarketing.beans;

import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

/**
 * Created by freddyrodriguez on 27/4/16.
 */
public enum PermissionType {

    TEMPLATE(Template.class.getCanonicalName(), ApplyTo.ONLY_HOST),
    TEMPLATE_LAYOUT(TemplateLayout.class.getCanonicalName(), ApplyTo.ONLY_HOST),
    CONTAINER(Container.class.getCanonicalName(), ApplyTo.ONLY_HOST),
    FOLDER(Folder.class.getCanonicalName()),
    IHTMLPAGE(IHTMLPage.class.getCanonicalName()),
    LINK(Link.class.getCanonicalName()),
    CONTENTLET(Contentlet.class.getCanonicalName()),
    STRUCTURE(Structure.class.getCanonicalName()),
    RULE(Rule.class.getCanonicalName()),
    CATEGORY(Category.class.getCanonicalName());

    private final ApplyTo applyTo;
    private String key;

    PermissionType(String key){
        this( key, ApplyTo.HOST_AND_FOLDER);
    }

    PermissionType(String key, ApplyTo applyTo){
        this.key = key;
        this.applyTo = applyTo;
    }

    @Override
    public java.lang.String toString() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public ApplyTo getApplyTo() {
        return applyTo;
    }

    public enum ApplyTo{
        ONLY_HOST, HOST_AND_FOLDER
    }

}
