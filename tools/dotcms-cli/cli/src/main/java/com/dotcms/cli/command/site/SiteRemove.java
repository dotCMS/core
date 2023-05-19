package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteRemove.NAME,
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
public class SiteRemove extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "remove";

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @Override
    public Integer call() {

        return delete();
    }

    private int delete() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        if(output.isCliTest() || isDeleteConfirmed(site.get().hostName())){
            return deleteSite(site.get());
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
                    "%nPlease confirm that you want to remove the site [%s] ? [y/n]: ",
                    siteName);
            return BooleanUtils.toBoolean(
                    System.console().readLine(confirmation));
        }
        return true;
    }

}
