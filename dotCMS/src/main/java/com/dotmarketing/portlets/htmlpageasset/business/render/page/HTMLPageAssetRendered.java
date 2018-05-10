package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.List;

/**
 * It is a {@link PageView} rendered
 */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered extends PageView {
    private final String html;
    private final boolean canCreateTemplate;
    private final boolean canEditTemplate;
    private final ViewAsPageStatus viewAs;

    public HTMLPageAssetRendered(final Host site,
                                 final Template template,
                                 final List<ContainerRendered> containers,
                                 final HTMLPageAssetInfo page,
                                 final TemplateLayout layout,
                                 final String html,
                                 final boolean canCreateTemplate,
                                 final boolean canEditTemplate,
                                 final ViewAsPageStatus viewAs) {

        super(site, template, containers, page, layout);
        this.html = html;
        this.canCreateTemplate = canCreateTemplate;
        this.canEditTemplate = canEditTemplate;
        this.viewAs = viewAs;
    }

    public String getHtml() {
        return html;
    }

    public boolean isCanCreateTemplate() {
        return canCreateTemplate;
    }


    public boolean isCanEditTemplate() {
        return canEditTemplate;
    }

    public ViewAsPageStatus getViewAs() {
        return viewAs;
    }
}
