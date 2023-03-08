package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteCreate.NAME,
        description = "@|bold,green Quickly create Sites.|@"
)
public class SiteCreate extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "create";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Parameters(index = "0", arity = "1", description = " Quick way to create a site. Simply pass a site name. ")
    String siteName;

    @Override
    public Integer call() throws Exception {
        if ( null != siteName && !siteName.isEmpty()) {

            final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

            final ResponseEntityView<SiteView> response = siteAPI.create(CreateUpdateSiteRequest.builder().siteName(siteName).build());
            final SiteView siteView = response.entity();
            output.info(" Site [%s] successfully created.");
            output.info(String.format("Site @|bold,green [%s]|@ successfully created.",siteName));
            output.info(shortFormat(siteView));
            return CommandLine.ExitCode.OK;
        }

        return CommandLine.ExitCode.USAGE;
    }
}
