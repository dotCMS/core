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

 * - pageIds: These are the IDs of the pages that need to be updated. This parameter helps filter
 * the {@link com.dotmarketing.beans.MultiTree} that will be updated.
 *
 * - site: This refers to the site associated with the template.
 *
 * - useHistory: This boolean attribute plays a significant role in determining how changes are calculated.
 * Let's explain the two scenarios in more detail:
 *
 * Suppose we have the following TemplateLayout:
 * <code>
 * Row 1: Container A, UUID 1, history ["1"]
 * Row 2: Container A, UUID 2, history ["2"]
 * </code>
 *
 * Now, let's see how the behavior changes based on the value of useHistory.
 *
 * useHistory = FALSE: In this case, changes are calculated using the UUIDs in the newLayoutTemplate.
 * For example, if we want to move the second row to the top, we would send a newTemplateLayout
 * as follows with useHistory set to false:
 *<code>
 * Row 1: Container A, UUID 2
 * Row 2: Container A, UUID 1
 * </code>
 *
 * UUIDs are assigned from left to right and top to bottom. Since useHistory is false, the unordered UUIDs indicate
 * the changes. The code will iterate through all containers in the layout and recognize that the instance with UUID 2
 * has moved to the top.
 *
 * useHistory = TRUE: Now, let's consider the same example with useHistory set to true, and the newTemplateLayout
 * is as follows:
 * <code>
 * Row 1: Container A, UUID 1, history ["2", "1"]
 * Row 2: Container A, UUID 2, history ["1", "2"]
 * </code>
 *
 * In this case, the history is used to determine the changes. The first container in the old layout has a history of ["1"].
 * The code will search for a container with the same history in the new template. The match is:
 *
 * <code>
 * Row 2: Container A, UUID 2, history ["1", "2"]
 * </code>
 *
 * This match is based on the first position in the history list ("1"), which indicates that the container initially
 * associated with UUID 1 has moved to UUID 2.
 *
 */
public class TemplateSaveParameters {

    private final Template newTemplate;
    private final TemplateLayout oldTemplateLayout;
    private final List<String> pageIds;
    private final TemplateLayout newLayout;
    private final Host site;
    private final boolean useHistory;

    private TemplateSaveParameters(final Builder builder){
        this.newTemplate = builder.newTemplate;
        this.oldTemplateLayout = builder.oldTemplateLayout;
        this.pageIds = builder.pageIds;
        this.newLayout = builder.newLayout;
        this.site = builder.site;
        this.useHistory = builder.useHistory;
    }

    public boolean isUseHistory() {
        return useHistory;
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
        private boolean useHistory;

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

        public Builder setUseHistory(boolean useHistory) {
            this.useHistory = useHistory;
            return this;
        }
    }
}
