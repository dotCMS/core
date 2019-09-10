package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.util.Collection;

public class PageViewBuilder {
    private Host site;
    private Template template;
    private Collection<? extends ContainerRaw> containers;
    private HTMLPageAssetInfo page;
    private TemplateLayout layout;
    private boolean canCreateTemplate;
    private boolean canEditTemplate;
    private ViewAsPageStatus viewAs;
    private int personalizationNumber;
    private User user;
    private String pageHTML;

    public static PageViewBuilder get(){
        return new PageViewBuilder();
    }

    public PageViewBuilder setSite(final Host site) {
        this.site = site;
        return this;
    }

    public PageViewBuilder setTemplate(final Template template) {
        this.template = template;
        return this;
    }

    public PageViewBuilder setContainers(final Collection<? extends ContainerRaw> containers) {
        this.containers = containers;
        return this;
    }

    public PageViewBuilder setPage(final HTMLPageAssetInfo page) {
        this.page = page;
        return this;
    }

    public PageViewBuilder setLayout(final TemplateLayout layout) {
        this.layout = layout;
        return this;
    }

    public PageViewBuilder setCanCreateTemplate(final boolean canCreateTemplate) {
        this.canCreateTemplate = canCreateTemplate;
        return this;
    }

    public PageViewBuilder setCanEditTemplate(final boolean canEditTemplate) {
        this.canEditTemplate = canEditTemplate;
        return this;
    }

    public PageViewBuilder setViewAs(final ViewAsPageStatus viewAs) {
        this.viewAs = viewAs;
        return this;
    }

    public PageViewBuilder setPersonalizationNumber(final int personalizationNumber) {
        this.personalizationNumber = personalizationNumber;
        return this;
    }

    public PageViewBuilder setUser(final User user) {
        this.user = user;
        return this;
    }

    public PageViewBuilder setPageHTML(final String pageHTML) {
        this.pageHTML = pageHTML;
        return this;
    }

    public PageView build(){
        return pageHTML == null ? createPageView() : createHTMLPageAssetRendered();
    }

    private PageView createPageView(){
        return new PageView(site, template, containers, page, layout, canCreateTemplate,
                canEditTemplate, viewAs, personalizationNumber, user);
    }

    private HTMLPageAssetRendered createHTMLPageAssetRendered(){

        return new HTMLPageAssetRendered(site, template, containers, page, layout, pageHTML,
                canCreateTemplate, canEditTemplate, viewAs,
                personalizationNumber, user
        );
    }
}
