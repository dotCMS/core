package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.google.common.collect.ImmutableMap;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
                                 final Collection<ContainerRendered> containers,
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
