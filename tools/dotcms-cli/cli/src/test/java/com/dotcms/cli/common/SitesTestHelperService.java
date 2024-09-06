package com.dotcms.cli.common;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@ApplicationScoped
public class SitesTestHelperService {

    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    @Inject
    RestClientFactory clientFactory;

    /**
     * Creates a new site.
     *
     * @return The name of the newly created test site.
     */
    public SiteCreationResult createSiteOnServer() {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Creating a new test site
        final String newSiteName = String.format("site-%s", UUID.randomUUID());
        CreateUpdateSiteRequest newSiteRequest = CreateUpdateSiteRequest.builder()
                .siteName(newSiteName).build();
        ResponseEntityView<SiteView> createSiteResponse = siteAPI.create(newSiteRequest);
        Assertions.assertNotNull(createSiteResponse);
        // Publish the new site
        siteAPI.publish(createSiteResponse.entity().identifier());
        Assertions.assertTrue(siteExist(newSiteName),
                String.format("Site %s was not created", newSiteName));

        return new SiteCreationResult(newSiteName, createSiteResponse.entity().identifier());
    }

    /**
     * Checks if a site with the given name exists.
     *
     * @param siteName the name of the site to check
     * @return true if the site exists, false otherwise
     */
    public Boolean siteExist(final String siteName) {

        try {

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findSiteByName(siteName);
                            return (response != null && response.entity() != null) &&
                                    ((response.entity().isLive() != null &&
                                            response.entity().isLive()) &&
                                            (response.entity().isWorking() != null &&
                                                    response.entity().isWorking()));
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            return true;
        } catch (ConditionTimeoutException ex) {
            return false;
        }
    }

    /**
     * Retrieves a site by its name.
     *
     * @param siteName The name of the site.
     * @return The ResponseEntityView containing the SiteView object representing the site.
     */
    @ActivateRequestContext
    public ResponseEntityView<SiteView> findSiteByName(final String siteName) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Execute the REST call to retrieve folder contents
        return siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteName).build()
        );
    }

    /**
     * Creates a new site JSON file in the given workspace.
     *
     * @param workspace The workspace where the site file will be created.
     * @param isDefault Whether the site should be created as the default site.
     * @return The name of the created site and the path to the created site file.
     * @throws IOException If an I/O error occurs while creating the site file.
     */
    public SiteDescriptorCreationResult createSiteDescriptor(final Workspace workspace,
            final boolean isDefault) throws IOException {
        return createSiteDescriptor(workspace, isDefault, true);
    }

    /**
     * Creates a new site JSON file in the given workspace.
     *
     * @param workspace The workspace where the site file will be created.
     * @param isDefault Whether the site should be created as the default site.
     * @return The name of the created site and the path to the created site file.
     * @throws IOException If an I/O error occurs while creating the site file.
     */
    public SiteDescriptorCreationResult createSiteDescriptor(final Workspace workspace,
            final boolean isDefault, final boolean withVariables) throws IOException {

        final String siteVariables = withVariables ? ",\n"
                + "  \"variables\" : [ {\n"
                + "    \"name\" : \"var1Name\",\n"
                + "    \"key\" : \"var1Key\",\n"
                + "    \"value\" : \"var1Value\"\n"
                + "  }, {\n"
                + "    \"name\" : \"var2Name\",\n"
                + "    \"key\" : \"var2Key\",\n"
                + "    \"value\" : \"var2Value\"\n"
                + "  }, {\n"
                + "    \"name\" : \"var3Name\",\n"
                + "    \"key\" : \"var3Key\",\n"
                + "    \"value\" : \"var3Value\"\n"
                + "  }, {\n"
                + "    \"name\" : \"var4Name\",\n"
                + "    \"key\" : \"var4Key\",\n"
                + "    \"value\" : \"var4Value\"\n"
                + "  }, {\n"
                + "    \"name\" : \"var5Name\",\n"
                + "    \"key\" : \"var5Key\",\n"
                + "    \"value\" : \"var5Value\"\n"
                + "  } ]" : "";

        final String newSiteName = String.format(
                "new.dotcms.site.%s",
                UUID.randomUUID()
        );
        String siteDescriptor = String.format("{\n"
                + "  \"siteName\" : \"%s\",\n"
                + "  \"languageId\" : 1,\n"
                + "  \"modDate\" : \"2023-05-05T00:13:25.242+00:00\",\n"
                + "  \"modUser\" : \"dotcms.org.1\",\n"
                + "  \"live\" : true,\n"
                + "  \"working\" : true,\n"
                + "  \"default\" : %b\n"
                + "%s\n"
                + "}", newSiteName, isDefault, siteVariables);

        final var path = Path.of(
                workspace.sites().toString(),
                String.format("%s.json", newSiteName)
        );
        Files.write(path, siteDescriptor.getBytes());

        return new SiteDescriptorCreationResult(newSiteName, path);
    }

    /**
     * Represents the result of a site descriptor creation.
     */
    public static class SiteDescriptorCreationResult {

        private final String siteName;
        private final Path path;

        public SiteDescriptorCreationResult(final String siteName, final Path path) {
            this.siteName = siteName;
            this.path = path;
        }

        public String siteName() {
            return siteName;
        }

        public Path path() {
            return path;
        }
    }

    /**
     * Represents the result of a site creation.
     */
    public static class SiteCreationResult {

        private final String siteName;
        private final String identifier;

        public SiteCreationResult(final String siteName, final String identifier) {
            this.siteName = siteName;
            this.identifier = identifier;
        }

        public String siteName() {
            return siteName;
        }

        public String identifier() {
            return identifier;
        }
    }

}
