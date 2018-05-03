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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * It is a {@link PageView} rendered
 */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered extends PageView {
    private String html;
    private boolean canCreateTemplate;
    private boolean canEditTemplate;
    private ViewAsPageStatus viewAs;

    public HTMLPageAssetRendered(Host site,
                                 Template template,
                                 List<ContainerRendered> containers,
                                 HTMLPageAssetInfo page,
                                 TemplateLayout layout,
                                 String html,
                                 boolean canCreateTemplate,
                                 boolean canEditTemplate,
                                 ViewAsPageStatus viewAs) {

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
