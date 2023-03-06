package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteStart.NAME,
     description = "@|bold,green Start Site |@ Option params @|bold,cyan --idOrName|@ site name or site id."
)
public class SiteStart extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "start";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Site name Or Id.")
    String siteNameOrId;

    @Override
    public Integer call() {
        return executeArchive();
    }

    private int executeArchive() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        final SiteView siteView = site.get();
        final ResponseEntityView<SiteView> publish = siteAPI.publish(siteView.identifier());
        output.info(String.format("Site [%s] published successfully.",publish.entity().hostName()));
        return CommandLine.ExitCode.OK;
    }

}
