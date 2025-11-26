package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteArchive.NAME,
        header = "@|bold,blue Use this command to archive a site.|@",
        description = {
                " Before a site can be delete it must be archived first.",
                " Archiving a site means it is no longer available for use. ",
                " It is not visible in the UI and it is not available via API.",
                " but this process can be undone. See @|bold,cyan site:unarchive|@ command. ",
                "" // empty line left here on purpose to make room at the end
        }
)
public class SiteArchive extends AbstractSiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "archive";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        return archive();
    }

    private int archive() {
        final SiteView site = findSite(siteNameOrId);
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> archive = siteAPI.archive(site.identifier());
        output.info(String.format("Site [%s] archived successfully.",archive.entity().hostName()));
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
