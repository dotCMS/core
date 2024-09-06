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
@CommandLine.Command(name = SiteUnarchive.NAME,
        header = "@|bold,blue Use this command to unarchive a site.|@",
        description = {
                " Once a site is unarchived it is available for use. ",
                " See @|bold,cyan site:archive|@ command. ",
                "" // This is needed to add a new line after the description.
        }
)
public class SiteUnarchive extends AbstractSiteCommand implements Callable<Integer>, DotCommand {
    static final String NAME = "unarchive";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name or Id.")
    String siteNameOrId;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        return unarchive();
    }

    private int unarchive() {

        final SiteView site = findSite(siteNameOrId);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> archive = siteAPI.unarchive(site.identifier());
        output.info(String.format("Site [%s] unarchived successfully.",archive.entity().hostName()));
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
