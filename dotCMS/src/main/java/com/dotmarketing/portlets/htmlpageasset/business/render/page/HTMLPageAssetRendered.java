package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.List;

/**
 * It is a {@link HTMLPageAssetRendered} rendered
 */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered {
    private String html;
    private HTMLPageAssetInfo pageInfo;
    private List<ContainerRendered> containers;
    private boolean canCreateTemplate;
    private TemplateLayout layout;
    private Template template;
    private boolean canEditTemplate;
    private ViewAsPageStatus viewAs;

    HTMLPageAssetRendered(){}

    public String getHtml() {
        return html;
    }

    HTMLPageAssetRendered setHtml(String html) {
        this.html = html;
        return this;
    }

    public HTMLPageAssetInfo getPageInfo() {
        return pageInfo;
    }

    HTMLPageAssetRendered setPageInfo(HTMLPageAssetInfo pageInfo) {
        this.pageInfo = pageInfo;
        return this;
    }

    public List<ContainerRendered> getContainers() {
        return containers;
    }

    HTMLPageAssetRendered setContainers(List<ContainerRendered> containers) {
        this.containers = containers;
        return this;
    }

    public boolean isCanCreateTemplate() {
        return canCreateTemplate;
    }

    HTMLPageAssetRendered setCanCreateTemplate(boolean canCreateTemplate) {
        this.canCreateTemplate = canCreateTemplate;
        return this;
    }

    public TemplateLayout getLayout() {
        return layout;
    }

    HTMLPageAssetRendered setLayout(TemplateLayout layout) {
        this.layout = layout;
        return this;
    }

    public Template getTemplate() {
        return template;
    }

    HTMLPageAssetRendered setTemplate(Template template) {
        this.template = template;
        return this;
    }

    public boolean isCanEditTemplate() {
        return canEditTemplate;
    }

    HTMLPageAssetRendered setCanEditTemplate(boolean canEditTemplate) {
        this.canEditTemplate = canEditTemplate;
        return this;
    }

    public ViewAsPageStatus getViewAs() {
        return viewAs;
    }

    HTMLPageAssetRendered setViewAs(ViewAsPageStatus viewAs) {
        this.viewAs = viewAs;
        return this;
    }
}
