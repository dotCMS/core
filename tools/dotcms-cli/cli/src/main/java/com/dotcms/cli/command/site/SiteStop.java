package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
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
public class SiteStop extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "stop";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name Or Id.")
    String siteNameOrId;

    @Override
    public Integer call() {
       try{
          return unpublish();
       } catch (Exception e){
           return output.handleCommandException(e,"Error while stopping site. ");
       }
    }

    private int unpublish() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        final SiteView siteView = site.get();
        final ResponseEntityView<SiteView> unpublish = siteAPI.unpublish(siteView.identifier());
        output.info(String.format("Site [%s] un-published successfully.",unpublish.entity().hostName()));
        return CommandLine.ExitCode.OK;
    }

}
