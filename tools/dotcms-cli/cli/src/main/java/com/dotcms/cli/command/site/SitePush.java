package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SitePush.NAME,
     header = "@|bold,blue Push a Site from a given file |@",
     description = {
             " This command will push a site to the current active",
             " remote instance of dotCMS from a given file.",
             " When pulling a site from a remote dotCMS instance",
             " the site is saved to a file.",
             " The file name will be the site's name.",
             " To make changes to the a Site",
             " modify the file and push it back to the remote instance.",
             " The file can also be used as a base to create a brand new Site.",
             " The format can be changed using the @|yellow --format|@ option.",
             "" // empty line left here on purpose to make room at the end
     }
)
public class SitePush extends AbstractSiteCommand implements Callable<Integer>{

    static final String NAME = "push";

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Option(names = { "-f", "--force" }, paramLabel = "force execution" ,description = "Force must me set to true to update a site name.")
    public boolean forceExecution;

    @CommandLine.Parameters(index = "0", arity = "1", description = " The json/yaml formatted Site descriptor file to be pushed. ")
    File siteFile;

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
                final ObjectMapper objectMapper = formatOption.objectMapper();
                final SiteView in = objectMapper.readValue(siteFile, SiteView.class);
                final String returnedSiteName = in.siteName();
                final CreateUpdateSiteRequest createUpdateSiteRequest = toRequest(in);
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

        return CommandLine.ExitCode.USAGE;
    }

    CreateUpdateSiteRequest toRequest(final SiteView siteView) {
        return CreateUpdateSiteRequest.builder()
                .siteName(siteView.siteName())
                .keywords(siteView.keywords())
                .googleMap(siteView.googleMap())
                .addThis(siteView.addThis())
                .aliases(siteView.aliases())
                .identifier(siteView.identifier())
                .inode(siteView.inode())
                .proxyUrlForEditMode(siteView.proxyUrlForEditMode())
                .googleAnalytics(siteView.googleAnalytics())
                .description(siteView.description())
                .tagStorage(siteView.tagStorage())
                .siteThumbnail(siteView.siteThumbnail())
                .embeddedDashboard(siteView.embeddedDashboard())
                .forceExecution(forceExecution)
                .build();
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
            output.error(String.format(" No site named [%s] was found. ", siteName));
        }
        return false;
    }

}
