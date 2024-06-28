package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
     name = SiteRemove.NAME,
     aliases = SiteRemove.ALIAS,
     header = "@|bold,blue Use this command to remove a site.|@",
     description = {
        " This operation is irreversible.",
        " So be mindful of what you are doing.",
        " Just like everything else in dotCMS. Sites must follow certain rules.",
        " Before they can be deleted",
        " The Site Not be the default site.",
        " The Site must be stopped first. See @|bold,cyan site:stop|@ command.",
        " The site must be archived first. See @|bold,cyan site:archive|@ command. ",
        "" // empty line left here on purpose to make room at the end
    }
)
public class SiteRemove extends AbstractSiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "remove";

    static final String ALIAS = "rm";

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    Prompt prompt;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        return delete();
    }

    private int delete() {

        final SiteView site = super.findSite(siteNameOrId);

        if(output.isCliTest() || isDeleteConfirmed(site.hostName())){
            return deleteSite(site);
        } else {
            output.info("Delete cancelled");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    private int deleteSite(SiteView site) {
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        ResponseEntityView<Boolean> delete = siteAPI.delete(site.identifier());
        if(Boolean.TRUE.equals(delete.entity())){
            output.info(String.format("Site [%s] removed successfully.",site.hostName()));
        } else {
            output.info(String.format("Site [%s] archived successfully.",site.hostName()));
        }
        return CommandLine.ExitCode.OK;
    }

    private boolean isDeleteConfirmed(final String siteName) {
        if (interactiveOption.isInteractive()) {
            final String confirmation = String.format(
                    "%nPlease confirm that you want to remove the site [%s] ",
                    siteName);
            return prompt.yesOrNo(false, confirmation);
        }
        return true;
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
