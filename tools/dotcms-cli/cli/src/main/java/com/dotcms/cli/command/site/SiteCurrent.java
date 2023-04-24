package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteCurrent.NAME,
        header = "@|bold,blue Use this command to get the current Site.|@",
        description = {
           " As dotCMS is a multi-tenant application, you can have multiple Sites.",
           " This command will tell you what the current Site is.",
           " The current Site is the one used as target when you run other commands.",
           " The current Site can be changed by using the @|bold,cyan site:switch|@ command.",
           " No additional options are required for this command.",
           "" // empty line left here on purpose to make room at the end
        }
)
public class SiteCurrent extends AbstractSiteCommand implements Callable<Integer> {
    static final String NAME = "current";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOptionMixin;

    @Inject
    RestClientFactory clientFactory;

    @Override
    public Integer call() {
        return current();
    }

    private int current() {
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        try {
            final ResponseEntityView<Site> current = siteAPI.current();
            output.info(String.format("Current Site is [%s].", current.entity().hostName()));
        }catch (Exception e){
            output.error(String.format("Unable to determine what the current Site is with error:[%s]. ", e.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }

}
