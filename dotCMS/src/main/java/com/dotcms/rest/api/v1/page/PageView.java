package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class PageView implements Serializable {

    private final Host site;
    private final Template template;
    private final Map<String, ContainerView> containers;
    private final HTMLPageAsset page;
    private final TemplateLayout layout;

    public PageView(Host site, Template template, Map<String, ContainerView> containers,
                    HTMLPageAsset page, TemplateLayout layout) {
        this.site = site;
        this.template = template;
        this.containers = containers;
        this.page = page;
        this.layout = layout;
    }

    public TemplateLayout getLayout() {
        return this.layout;
    }

    public Host getSite() {
        return this.site;
    }

    public Template getTemplate() {
        return this.template;
    }

    public Map<String, ContainerView> getContainers() {
        return this.containers;
    }

    public HTMLPageAsset getPage() {
        return this.page;
    }

}
