package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import java.util.Collection;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

/**
 * It is a {@link PageView} rendered
 */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered extends PageView {
    private final String html;

    public HTMLPageAssetRendered(final Host site,
                                 final Template template,
                                 final Collection<? extends ContainerRaw> containers,
                                 final HTMLPageAssetInfo page,
                                 final TemplateLayout layout,
                                 final String html,
                                 final boolean canCreateTemplate,
                                 final boolean canEditTemplate,
                                 final ViewAsPageStatus viewAs,
                                 final String pageUrlMapper,
                                 final boolean live) {

        super(site, template, containers, page, layout, canCreateTemplate, canEditTemplate, viewAs, pageUrlMapper, live);
        this.html = html;


    }

    public String getHtml() {
        return html;
    }




}
