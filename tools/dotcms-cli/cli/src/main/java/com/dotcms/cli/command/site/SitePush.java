package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SitePush.NAME,
     description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan -n|@"
)
public class SitePush extends SiteCommand implements Callable<Integer>{

    static final String NAME = "site-push";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = {"-f", "--file"}, order = 10, arity = "1", description = " The json/yaml formatted Site descriptor file to be pushed. ")
    File siteFile;

    @CommandLine.Option(names = {"-c", "--create"}, order = 20, arity = "1", description = " Quick way to create a site. Simply pass a site name. ")
    String siteName;

    @Override
    public Integer call() {
        return push();
    }

    private int push() {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        if (null != siteFile) {
            if (!siteFile.exists() || !siteFile.canRead()) {
                output.error(String.format(
                        "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                        siteFile.getAbsolutePath()));
                return CommandLine.ExitCode.SOFTWARE;
            }

            try {
                final ObjectMapper objectMapper = output.objectMapper();
                final CreateUpdateSiteRequest createUpdateSiteRequest = objectMapper.readValue(siteFile, CreateUpdateSiteRequest.class);
                final String returnedSiteName = createUpdateSiteRequest.siteName();
                if(update(siteAPI, createUpdateSiteRequest, returnedSiteName)){
                    return CommandLine.ExitCode.OK;
                }
                output.info(String.format(" No site named [%s] was found. Will attempt to create it. ",returnedSiteName));
                final ResponseEntityView<SiteView> response = siteAPI.create(createUpdateSiteRequest);
                final SiteView siteView = response.entity();
                output.info(String.format("Site @|bold,green [%s]|@ successfully created.",returnedSiteName));
                output.info(shortFormat(siteView));
                return CommandLine.ExitCode.OK;
            } catch (IOException e) {
                output.error(String.format(
                        "Error occurred while pushing Site from file: [%s] with message: [%s].",
                        siteFile.getAbsolutePath(), e.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        }

        if ( null != siteName && !siteName.isEmpty()) {
            final ResponseEntityView<SiteView> response = siteAPI.create(CreateUpdateSiteRequest.builder().siteName(siteName).build());
            final SiteView siteView = response.entity();
            output.info(" Site [%s] successfully created.");
            output.info(String.format("Site @|bold,green [%s]|@ successfully created.",siteName));
            output.info(shortFormat(siteView));
            return CommandLine.ExitCode.OK;
        }

        return CommandLine.ExitCode.USAGE;
    }

    private boolean update(SiteAPI siteAPI, CreateUpdateSiteRequest createUpdateSiteRequest, String siteName) {
        try {
            output.info(String.format(" Looking up site by name [%s]", siteName));
            final ResponseEntityView<SiteView> byName = siteAPI.findByName(GetSiteByNameRequest.builder().siteName(siteName).build());
            //Up on read failure we could try to load a yml and pass the respect
            output.info(String.format(" A site named [%s] was found. An update will be attempted. ", siteName));
            final ResponseEntityView<SiteView> update = siteAPI.update(byName.entity().identifier(), createUpdateSiteRequest);
            output.info(shortFormat(update.entity()));
            return true;
        } catch (NotFoundException e) {
            //Not relevant
        }
        return false;
    }

}
