package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteCopy.NAME,
        description = "@|bold,green Copy Site |@ No params are expected. "
)
public class SiteCopy extends AbstractSiteCommand implements Callable<Integer> {
    static final String NAME = "copy";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = {"-in", "--idOrName"},
            order = 10, description = "Pull Site by id or name", required = true)
    String siteNameOrId;

    @CommandLine.Option(names = {"-d", "--deep"},
            order = 20, description = "Pull Site by id or name", defaultValue = "false")
    boolean deepCopy;

    @CommandLine.Option(names = {"-cn", "--copyName"},
            order = 10, description = "New Site name")
    String copySiteName;


    @Override
    public Integer call() {
        return copy();
    }

    private int copy() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final SiteView siteView = site.get();

        try {
            ResponseEntityView<SiteView> copy = siteAPI.copy(fromSite(siteView, copySiteName, deepCopy));
            output.info(String.format("New Copy Site is [%s].", copy.entity().hostName()));
        }catch (Exception e){
            output.error(String.format("An Error occurred copying site [%s] with error:[%s]. ",siteView.hostName(), e.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }

    CopySiteRequest fromSite( final SiteView fromSite, String copySiteName, final boolean deepCopy){
        String copyName = copySiteName;
        if(null == copyName){
            copyName =  String.format("%s.%d.copy",fromSite.hostName(),System.currentTimeMillis());
        }

        CreateUpdateSiteRequest siteRequest = CreateUpdateSiteRequest.builder().siteName(
                copyName
        ).build();
        return CopySiteRequest.builder()
               .copyFromSiteId(fromSite.identifier())
               .copyAll(deepCopy)
               .copyContentOnPages(deepCopy)
               .copyContentOnSite(deepCopy)
               .copyFolders(deepCopy)
               .copyLinks(deepCopy)
               .copyTemplatesContainers(deepCopy)
               .copySiteVariables(deepCopy)
               .site(siteRequest)
               .build();
    }

}
