package com.dotcms.cli.command;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import java.util.List;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteCommand.NAME, description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan -n|@ to filter by name. @|bold,cyan -a|@ Shows archived sites. @|bold,cyan -l|@ Shows live Sites. @|bold,cyan -p|@ (Page) @|bold,cyan -ps|@ (PageSize) Can be used combined for pagination.")
public class SiteCommand implements Runnable {

    static final String NAME = "site";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Option(names = {"-n", "--name"}, description = "Filter by site name.", interactive = true)
    String name;

    @CommandLine.Option(names = {"-a", "--archived"}, description = "Show archived sites.", defaultValue = "false")
    Boolean archived;

    @CommandLine.Option(names = {"-l", "--live"}, description = "Show live sites.", defaultValue = "true")
    Boolean live;

    @CommandLine.Option(names = {"-p", "--page"}, description = "Page Number.", defaultValue = "1")
    Integer page;

    @CommandLine.Option(names = {"-s", "--pageSize"}, description = "Items per page.", defaultValue = "25")
    Integer pageSize;

    @Inject
    RestClientFactory clientFactory;

    @Override
    public void run() {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<List<Site>> response = siteAPI.getSites(name, archived, live, true, page, pageSize);
        final List<Site> sites = response.entity();
        if (sites.isEmpty()) {
            output.error("I couldn't find any sites with this search criteria.");
        } else {
            for (final Site site : sites) {
                output.info(site.toString());
            }
        }

    }

}
