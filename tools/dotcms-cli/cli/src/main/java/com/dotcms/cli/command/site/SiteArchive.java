package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteArchive.NAME,
        header = "@|bold,blue Use this command to archive a site.|@",
        description = {
                " Before a site can be delete it must be archived first.",
                " Archiving a site means it is no longer available for use. ",
                " It is not visible in the UI and it is not available via API.",
                " but this process can be undone. See  @|bold,cyan site:unarchive|@ command. ",
                "" // empty line left here on purpose to make room at the end
        }
)
public class SiteArchive extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "archive";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOptionMixin;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @Override
    public Integer call() {

        return archive();
    }

    private int archive() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        final SiteView siteView = site.get();
        final ResponseEntityView<SiteView> archive = siteAPI.archive(siteView.identifier());
        output.info(String.format("Site [%s] archived successfully.",archive.entity().hostName()));
        return CommandLine.ExitCode.OK;
    }

}
