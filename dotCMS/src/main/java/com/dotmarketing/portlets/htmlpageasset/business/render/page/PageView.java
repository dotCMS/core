package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the different parts that make up the structure of an HTML Page in the system and its
 * associated data structures. A dotCMS page is basically composed of:
 * <ul>
 * <li>The HTML Page.</li>
 * <li>Its template.</li>
 * <li>The site it's located in.</li>
 * <li>The list of containers.</li>
 * <li>Its template layout.</li>
 * </ul>
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
@JsonSerialize(using = PageViewSerializer.class)
public class PageView implements Serializable {

    private static final long serialVersionUID = 1642131505258302751L;

    private final Host site;
    private final Template template;
    private final Collection<? extends ContainerRaw> containers;
    private final HTMLPageAsset htmlPageAsset;
    private final TemplateLayout layout;
    private final ViewAsPageStatus viewAs;
    private final boolean canCreateTemplate;
    private final boolean canEditTemplate;
    // content associated to the url content map
    private final Contentlet urlContent;
    private int numberContents = 0;
    final String pageUrlMapper;
    final boolean live;

    /**
     * Creates an instance of this class based on a builder.
     */
    PageView(final Builder builder) {

        this.site = builder.site;
        this.template =  builder.template;
        this.containers =  builder.containers;
        this.htmlPageAsset =  builder.page;
        this.layout =  builder.layout;
        this.viewAs =  builder.viewAs;
        this.canCreateTemplate =  builder.canCreateTemplate;
        this.canEditTemplate =  builder.canEditTemplate;
        this.pageUrlMapper = builder.pageUrlMapper;
        this.numberContents = getContentsNumber();
        this.live = builder.live;
        this.urlContent = builder.urlContent;
    }

    public boolean isLive() {
        return live;
    }

    private int getContentsNumber() {
        final Optional<Integer> contentsNumber = this.getContainersMap().values()
                .stream()
                .flatMap(containerRaw -> containerRaw.getContentlets().values().stream())
                .map(contents -> contents.size())
                .reduce((currentValue, accumulator) -> currentValue + accumulator);

        return contentsNumber.isPresent() ? contentsNumber.get() : 0;
    }

    /**
     * Returns the {@link TemplateLayout} of the page.
     *
     * @return The {@link TemplateLayout}.
     */
    public TemplateLayout getLayout() {
        return this.layout;
    }

    /**
     * Returns the {@link Host} where the HTML Page lives in.
     *
     * @return The {@link Host}.
     */
    public Host getSite() {
        return this.site;
    }

    /**
     * Returns the {@link Template} associated to the HTML Page.
     *
     * @return The {@link Template}.
     */
    public Template getTemplate() {
        return this.template;
    }

    /**
     * Returns The map of Containers and their respective relationships with Content Types.
     *
     * @return The map of Containers.
     */
    public Collection<? extends ContainerRaw>  getContainers() {
        return this.containers;
    }

    public Map<String, ContainerRaw> getContainersMap() {
        final Map<String, ContainerRaw> containerRawMap = new HashMap<>();

        containers.stream().forEach(containerRaw -> {

            final Container container = containerRaw.getContainer();

            if (container instanceof FileAssetContainer) {

                final String path = FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) container);
                containerRawMap.put(path, containerRaw);
            } else {

                final String identifier = container.getIdentifier();
                containerRawMap.put(identifier, containerRaw);
            }
        });

        return containerRawMap;
    }

    /**
     * Returns The {@link HTMLPageAsset} object.
     *
     * @return The {@link HTMLPageAsset}.
     */
    public HTMLPageAsset getPage() {
        return this.htmlPageAsset;
    }

    @Override
    public String toString() {
        return "PageView{" + "site=" + site + ", template=" + template + ", containers=" +
                containers + ", page=" + htmlPageAsset + ", layout=" + layout + '}';
    }
    /**
     * returns ViewAs Information
     * @return
     */
    public ViewAsPageStatus getViewAs() {
        return viewAs;
    }
    
    public boolean canCreateTemplate() {
        return canCreateTemplate;
    }

    public boolean canEditTemplate() {
        return canEditTemplate;
    }

    public int getNumberContents() {
        return numberContents;
    }

    public String getPageUrlMapper() {
        return pageUrlMapper;
    }

    public Contentlet getUrlContent() {
        return urlContent;
    }

    public static class Builder {

        //  The {@link Host} where the HTML Page lives in.
        private  Host site;
        // The {@link Template} associated to the HTML Page.
        private  Template template;
        // The map of Containers and their respective relationships with Content Types
        private  Collection<? extends ContainerRaw> containers;
        private  HTMLPageAsset page;
        // The {@link TemplateLayout} that specifies the design of the template
        private  TemplateLayout layout;
        private  ViewAsPageStatus viewAs;
        private  boolean canCreateTemplate;
        private  boolean canEditTemplate;
        // content associated to the url content map
        private  Contentlet urlContent;
        private String pageUrlMapper;
        private boolean live;

        public Builder site(final Host site) {
            this.site = site;
            return this;
        }

        public Builder template(final Template template) {
            this.template = template;
            return this;
        }

        public Builder containers(final Collection<? extends ContainerRaw> containers) {
            this.containers = containers;
            return this;
        }

        public Builder page(final HTMLPageAsset page) {
            this.page = page;
            return this;
        }

        public Builder layout(final TemplateLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder viewAs(final ViewAsPageStatus viewAs) {
            this.viewAs = viewAs;
            return this;
        }

        public Builder canCreateTemplate(final boolean canCreateTemplate) {
            this.canCreateTemplate = canCreateTemplate;
            return this;
        }

        public Builder canEditTemplate(final boolean canEditTemplate) {
            this.canEditTemplate = canEditTemplate;
            return this;
        }

        public Builder urlContent(final Contentlet urlContent) {
            this.urlContent = urlContent;
            return this;
        }

        public Builder pageUrlMapper(final String pageUrlMapper) {
            this.pageUrlMapper = pageUrlMapper;
            return this;
        }

        public Builder live(final boolean live) {
            this.live = live;
            return this;
        }

        public PageView build() {
            return new PageView(this);
        }
    }

}
