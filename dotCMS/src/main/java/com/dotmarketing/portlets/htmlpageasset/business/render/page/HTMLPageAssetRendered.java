package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import java.util.Collection;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

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
                                 final int personalizationNumber,
                                 final User user) {

        super(site, template, containers, page, layout, canCreateTemplate, canEditTemplate, viewAs, personalizationNumber, user);
        this.html = html;


    }

    public String getHtml() {
        return html;
    }
}
