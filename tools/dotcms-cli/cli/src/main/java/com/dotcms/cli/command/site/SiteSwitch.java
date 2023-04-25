package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteSwitch.NAME,
       header = "@|bold,blue Use this command to move between sites.|@",
       description = {
               " As @|bold,underline,blue dotCMS|@ is a multi-tenant application, you can switch between sites. ",
               "" // This is needed to add a new line after the description.
       }
)
public class SiteSwitch extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "switch";

    @CommandLine.Option(names = { "-in", "--idOrName" }, arity = "1", paramLabel = "idOrName", description = "Site name or Id.", required = true)
    String siteNameOrId;

    @Override
    public Integer call() {

        return doSwitch();
    }

    private int doSwitch() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final SiteView siteView = site.get();
        ResponseEntityView<Boolean> switchSite = siteAPI.switchSite(siteView.identifier());
        if(Boolean.TRUE.equals(switchSite.entity())) {
            output.info(String.format("Successfully switched to site: [%s]. ",siteView.hostName()));
            return CommandLine.ExitCode.OK;
        } else {
            output.info(String.format("switched to site: [%s] failed. ",siteView.hostName()));
            return CommandLine.ExitCode.SOFTWARE;
        }

    }

}
