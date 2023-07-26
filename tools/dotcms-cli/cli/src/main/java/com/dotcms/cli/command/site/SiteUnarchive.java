package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
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
public class SiteUnarchive extends AbstractSiteCommand implements Callable<Integer> {
    static final String NAME = "unarchive";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name or Id.")
    String siteNameOrId;

    @Override
    public Integer call() {
      try{
            return unarchive();
        }catch (Exception e){
            return output.handleCommandException(e,"Error while restoring archived site. ");
        }
    }

    private int unarchive() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        final SiteView siteView = site.get();
        final ResponseEntityView<SiteView> archive = siteAPI.unarchive(siteView.identifier());
        output.info(String.format("Site [%s] unarchived successfully.",archive.entity().hostName()));
        return CommandLine.ExitCode.OK;
    }

}
