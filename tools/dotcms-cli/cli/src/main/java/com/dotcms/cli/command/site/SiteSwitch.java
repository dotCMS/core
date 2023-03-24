package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteSwitch.NAME,
     description = "@|bold,green Switch Site |@ Option params @|bold,cyan --idOrName|@ site name or site id."
)
public class SiteSwitch extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "switch";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = { "-in", "--idOrName" },
            order = 2, arity = "1", description = "Site by id or name", required = true)
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
