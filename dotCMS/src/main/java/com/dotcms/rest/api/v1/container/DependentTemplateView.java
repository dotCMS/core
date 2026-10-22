package com.dotcms.rest.api.v1.container;

/**
 * Basic information about a Template that uses a Container. See {@link ContainerDependenciesView}.
 */
public class DependentTemplateView {

    private final String identifier;
    private final String title;
    private final String siteName;

    /**
     * @param identifier The Template identifier.
     * @param title      The Template title.
     * @param siteName   The name of the Site the Template lives in, or {@code null} when the Site
     *                   cannot be resolved or the user is not allowed to read it.
     */
    public DependentTemplateView(final String identifier, final String title,
            final String siteName) {
        this.identifier = identifier;
        this.title = title;
        this.siteName = siteName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public String getSiteName() {
        return siteName;
    }
}
