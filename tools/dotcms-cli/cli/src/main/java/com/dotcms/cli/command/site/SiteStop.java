package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteStop.NAME,
     header = "@|bold,blue Use this command to stop a site.|@",
          description = {
                 " Once a site is stopped it is no longer available for use. ",
                 " See @|bold,cyan site:start|@ command. ",
                 "" // This is needed to add a new line after the description.
          }
)
public class SiteStop extends AbstractSiteCommand implements Callable<Integer>,DotCommand {

    static final String NAME = "stop";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

       return unpublish();
    }

    private int unpublish() {

        final SiteView site = super.findSite(siteNameOrId);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> unpublish = siteAPI.unpublish(site.identifier());
        output.info(String.format("Site [%s] un-published successfully.",unpublish.entity().hostName()));
        return CommandLine.ExitCode.OK;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}
