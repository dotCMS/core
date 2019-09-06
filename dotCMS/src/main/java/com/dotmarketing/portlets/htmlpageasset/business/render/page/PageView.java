package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import java.io.Serializable;
import java.util.*;

import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

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
    private final HTMLPageAssetInfo htmlPageAssetInfo;
    private final TemplateLayout layout;
    private final ViewAsPageStatus viewAs;
    private final boolean canCreateTemplate;
    private final boolean canEditTemplate;
    private int numberContents = 0;
    private int personalizationNumber = 0;

    /**
     * Creates an instance of this class.
     *
     * @param site       The {@link Host} where the HTML Page lives in.
     * @param template   The {@link Template} associated to the HTML Page.
     * @param containers The map of Containers and their respective relationships with Content
     *                   Types.
     * @param page       The {@link HTMLPageAsset} object.
     * @param layout     The {@link TemplateLayout} that specifies the design of the template.
     */
    PageView(final Host site,
             final Template template,
             final Collection<? extends ContainerRaw> containers,
             final HTMLPageAssetInfo page,
             final TemplateLayout layout,
             final boolean canCreateTemplate,
             final boolean canEditTemplate,
             final ViewAsPageStatus viewAs,
             final int personalizationNumber) {

        this.site = site;
        this.template = template;
        this.containers = containers;
        this.htmlPageAssetInfo = page;
        this.layout = layout;
        this.viewAs = viewAs;
        this.canCreateTemplate = canCreateTemplate;
        this.canEditTemplate = canEditTemplate;

        final Map<String, ContainerRaw> containersMap = this.getContainersMap();

        if (this.layout != null) {
            this.numberContents = getContentsNumber(containersMap);
        }

        this.personalizationNumber = personalizationNumber;
    }

    private final int getContentsNumber(final Map<String, ContainerRaw> containersMap) {
        final Optional<Integer> optionalResult = this.layout.getBody().getRows()
            .stream()
            .flatMap(row -> row.getColumns().stream())
            .flatMap(column -> column.getContainers().stream())
            .map(containerUUID -> {
                final ContainerRaw containerRaw = containersMap.get(containerUUID.getIdentifier());

                if (containerRaw != null) {
                    final List<Map<String, Object>> contents = containerRaw.getContentlets().get(PageRenderUtil.CONTAINER_UUID_PREFIX + containerUUID.getUUID());
                    return contents != null ? contents.size() : 0;
                } else {
                    return 0;
                }
            })
            .reduce((value, accumulator) -> value + accumulator);

        return optionalResult.isPresent() ? optionalResult.get() : 0;
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

            if (containerRaw.getContainer() instanceof FileAssetContainer) {

                final String path = FileAssetContainer.class.cast(containerRaw.getContainer()).getPath();
                containerRawMap.put(path, containerRaw);
            } else {

                final String identifier = containerRaw.getContainer().getIdentifier();
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
    public HTMLPageAssetInfo getPageInfo() {
        return this.htmlPageAssetInfo;
    }

    @Override
    public String toString() {
        return "PageView{" + "site=" + site + ", template=" + template + ", containers=" +
                containers + ", page=" + htmlPageAssetInfo.getPage() + ", layout=" + layout + '}';
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

    public int getPersonalizationNumber() {
        return personalizationNumber;
    }
}
