package com.dotcms.cli.common;

import com.dotcms.api.WorkflowAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.content.Contentlet;
import com.dotcms.model.content.CreateContentRequest;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContentsTestHelperService {

    @Inject
    RestClientFactory clientFactory;

    /**
     * Creates a page on the server.
     *
     * @param siteId The ID of the site in which the page should be created.
     * @return The result of the page creation, containing the identifier, inode, and URL of the
     * created page.
     */
    public PageCreationResult createPageOnServer(final String siteId) {

        final WorkflowAPI workflowAPI = clientFactory.getClient(WorkflowAPI.class);

        final String pageName = String.format("Page %s", UUID.randomUUID());
        final String pageURL = String.format("page-%s", UUID.randomUUID());

        var page = Contentlet.builder()
                .stName("htmlpageasset")
                .title(pageName)
                .url(pageURL)
                .friendlyName(pageName)
                .template("SYSTEM_TEMPLATE")
                .sortOrder("0")
                .cachettl("100")
                .hostFolder(siteId)
                .build();

        final var response = workflowAPI.create(
                "PUBLISH", null, null, "WAIT_FOR",
                CreateContentRequest.builder().contentlet(page).build()
        );

        final var entityResponse = response.entity();
        return new PageCreationResult(
                entityResponse.get("identifier").toString(),
                entityResponse.get("inode").toString(),
                entityResponse.get("url").toString()
        );
    }

    /**
     * Represents the result of a page creation.
     */
    public static class PageCreationResult {

        private final String identifier;
        private final String inode;
        private final String url;

        public PageCreationResult(final String identifier, final String inode, final String url) {
            this.identifier = identifier;
            this.inode = inode;
            this.url = url;
        }

        public String identifier() {
            return identifier;
        }

        public String inode() {
            return inode;
        }

        public String url() {
            return url;
        }

    }

}
