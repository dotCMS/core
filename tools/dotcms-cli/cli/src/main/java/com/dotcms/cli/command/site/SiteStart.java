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
@CommandLine.Command(name = SiteStart.NAME,
        header = "@|bold,blue Use this command to start a site.|@",
     description = {
                " Before a site can be used it must be started first.",
                " You can think og this as a way to publish a site.",
                " Once a site is started it is available for use. ",
                " See @|bold,cyan site:stop|@ command. ",
             }
)
public class SiteStart extends AbstractSiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "start";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        return executeArchive();
    }

    private int executeArchive() {

        final SiteView site = findSite(siteNameOrId);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> publish = siteAPI.publish(site.identifier());
        output.info(String.format("Site [%s] published successfully.",publish.entity().hostName()));
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
