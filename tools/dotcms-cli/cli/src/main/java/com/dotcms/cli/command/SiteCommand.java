package com.dotcms.cli.command;

import com.dotcms.model.site.GetSitesResponse;
import com.dotcms.model.site.Site;
import com.dotcms.api.SiteAPI;
import java.util.List;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "site", description = "Retrieves Sites info.")
public class SiteCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(SiteCommand.class);

    @CommandLine.Option(names = {"-n", "--name"}, description = "Filter by site name.", interactive = true)
    String name;

    @CommandLine.Option(names = {"-a", "--archived"}, description = "Show archived sites.", defaultValue = "false")
    Boolean archived;

    @CommandLine.Option(names = {"-l", "--live"}, description = "Show live sites.", defaultValue = "true")
    Boolean live;

    @CommandLine.Option(names = {"-p", "--page"}, description = "Page Number.", defaultValue = "1")
    Integer page;

    @CommandLine.Option(names = {"-ps", "--pageSize"}, description = "Items per page.", defaultValue = "25")
    Integer pageSize;

    @Inject
    @RestClient
    SiteAPI siteAPI;

    @Override
    public void run() {

        final GetSitesResponse response = siteAPI.getSites(name, archived, live, true, page, pageSize);
        final List<Site> sites = response.entity();
        if (sites.isEmpty()) {
            logger.info("I couldn't find any sites with this search criteria.");
        } else {
            for (final Site site : sites) {
                logger.info(site);
            }
        }

    }

}
