package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
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

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final SiteView siteView = site.get();
        ResponseEntityView<Boolean> delete = siteAPI.delete(siteView.identifier());
        if(Boolean.TRUE.equals(delete.entity())){
            output.info(String.format("Site [%s] delete successfully.",siteView.hostName()));
        } else {
            output.info(String.format("Site [%s] archived successfully.",siteView.hostName()));
        }
        return CommandLine.ExitCode.OK;
    }

}
