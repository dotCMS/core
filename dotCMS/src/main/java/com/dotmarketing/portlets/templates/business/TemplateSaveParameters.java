package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.List;

/**
 * Parameters for Saving or Updating a Template and its Layout
 * The parameters you can set are:
 *
 * - newTemplate: This parameter allows you to set basic details for the new template, such as the title.
 *
 * - oldTemplateLayout: This represents the layout currently saved in the database. It is the layout that needs to be changed.
 *
 * - newLayout: This is the new layout. It is used in conjunction with oldTemplateLayout to calculate the changes between the two.
 * After this, the {@link com.dotmarketing.beans.MultiTree} is updated according to the two layouts.
 *
 * - pageIds: These are the IDs of the pages that need to be updated. This parameter helps filter
 * the {@link com.dotmarketing.beans.MultiTree} that will be updated.
 *
 * - site: This refers to the site associated with the template.
 */
public class TemplateSaveParameters {

    private final Template newTemplate;
    private final TemplateLayout oldTemplateLayout;
    private final List<String> pageIds;
    private final TemplateLayout newLayout;
    private final Host site;

    private TemplateSaveParameters(final Builder builder){
        this.newTemplate = builder.newTemplate;
        this.oldTemplateLayout = builder.oldTemplateLayout;
        this.pageIds = builder.pageIds;
        this.newLayout = builder.newLayout;
        this.site = builder.site;
    }

    public Template getNewTemplate() {
        return newTemplate;
    }

    public TemplateLayout getOldTemplateLayout() {
        return oldTemplateLayout;
    }

    public List<String> getPageIds() {
        return pageIds;
    }

    public TemplateLayout getNewLayout() {
        return newLayout;
    }

    public Host getSite() {
        return site;
    }

    public static class Builder {
        private Template newTemplate;
        private TemplateLayout oldTemplateLayout;
        private List<String> pageIds;
        private TemplateLayout newLayout;
        private Host site;

        public Builder setNewTemplate(Template newTemplate) {
            this.newTemplate = newTemplate;
            return this;
        }

        public Builder setOldTemplateLayout(TemplateLayout oldTemplateLayout) {
            this.oldTemplateLayout = oldTemplateLayout;
            return this;
        }

        public Builder setPageIds(List<String> pageIds) {
            this.pageIds = pageIds;
            return this;
        }

        public Builder setNewLayout(TemplateLayout newLayout) {
            this.newLayout = newLayout;
            return this;
        }

        public Builder setSite(Host site) {
            this.site = site;
            return this;
        }

        public TemplateSaveParameters build (){
            return new TemplateSaveParameters(this);
        }
    }
}
