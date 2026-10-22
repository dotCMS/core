package com.dotcms.rest.api.v1.container;

import com.dotmarketing.portlets.containers.model.Container;
import java.util.List;

/**
 * Describes a {@link Container} together with the Templates that still use it. The values are
 * returned as plain data so that the caller decides how to present them to the user, for instance
 * when explaining why a Container cannot be deleted yet.
 */
public class ContainerDependenciesView {

    private final String identifier;
    private final String inode;
    private final String title;
    private final List<DependentTemplateView> templates;

    public ContainerDependenciesView(final Container container,
            final List<DependentTemplateView> templates) {
        this.identifier = container.getIdentifier();
        this.inode = container.getInode();
        this.title = container.getTitle();
        this.templates = templates;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public String getTitle() {
        return title;
    }

    public List<DependentTemplateView> getTemplates() {
        return templates;
    }
}
