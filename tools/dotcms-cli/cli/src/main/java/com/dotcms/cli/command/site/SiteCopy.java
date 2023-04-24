package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteCopy.NAME,
        header = "@|bold,blue Use this command to copy an existing site.|@",
        description = {
           " The command provides the ability to copy individually site elements such as: ",
           " pages, folders, links, template containers, and site variables. ",
           " Or everything at once through the use of the @|yellow --all|@ param. ",
           " The new site will be created with the name specified in the param @|yellow --copyName|@ ",
           "" // empty line left here on purpose to make room at the end
        }
)
public class SiteCopy extends AbstractSiteCommand implements Callable<Integer> {
    static final String NAME = "copy";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = {"-cn", "--copyName"},  paramLabel = "copyName", description = "New Site name.")
    String copySiteName;

    @CommandLine.Option(names = {"-in",
            "--idOrName"}, paramLabel = "idOrName", description = "Site name or Id.", required = true)
    String siteNameOrId;
    @CommandLine.Option(names = {"-a",
            "--all"}, paramLabel = "All", description = "if specified everything will be copied.", defaultValue = "false")
    boolean copyAll;


    static class CopyOptions {

        @CommandLine.Option(names = {"-p",
                "--page"}, paramLabel = "pages", description = "if specified content on pages will be copied.", defaultValue = "false")
        boolean copyContentOnPages;

        @CommandLine.Option(names = {"-c",
                "--content"}, paramLabel = "Content", description = "if specified content on site will be copied.", defaultValue = "false")
        boolean copyContentOnSite;

        @CommandLine.Option(names = {"-f",
                "--folder"}, paramLabel = "Folders", description = "if specified folders will be copied.", defaultValue = "false")
        boolean copyFolders;

        @CommandLine.Option(names = {"-l",
                "--link"}, paramLabel = "Links", description = "if specified links will be copied.", defaultValue = "false")
        boolean copyLinks;

        @CommandLine.Option(names = {"-t",
                "--template"}, paramLabel = "Templates", description = "if specified templates will be copied.", defaultValue = "false")
        boolean copyTemplateContainers;

        @CommandLine.Option(names = {"-v",
                "--var"}, paramLabel = "Variables", description = "if specified site variables will be copied.", defaultValue = "false")
        boolean copySiteVariables;
    }

    @CommandLine.ArgGroup(exclusive = false,  heading = "\n@|bold,blue Individual Copy Options. |@\n")
    CopyOptions copyOptions;

    @CommandLine.Mixin
    HelpOptionMixin helpOptionMixin;

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
            ResponseEntityView<SiteView> copy = siteAPI.copy(fromSite(siteView, copySiteName,
                    copyAll));
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

        boolean copyContentOnPages = false;
        boolean copyContentOnSite = false;
        boolean copyFolders = false;
        boolean copyLinks = false;
        boolean copyTemplateContainers = false;
        boolean copySiteVariables = false;

        if(null != copyOptions){
            copyContentOnPages = copyOptions.copyContentOnPages;
            copyContentOnSite = copyOptions.copyContentOnSite;
            copyFolders = copyOptions.copyFolders;
            copyLinks = copyOptions.copyLinks;
            copyTemplateContainers = copyOptions.copyTemplateContainers;
            copySiteVariables = copyOptions.copySiteVariables;
        }


        return CopySiteRequest.builder()
               .copyFromSiteId(fromSite.identifier())
               .copyAll(deepCopy)
               .copyContentOnPages(copyContentOnPages)
               .copyContentOnSite(copyContentOnSite)
               .copyFolders(copyFolders)
               .copyLinks(copyLinks)
               .copyTemplatesContainers(copyTemplateContainers)
               .copySiteVariables(copySiteVariables)
               .site(siteRequest)
               .build();
    }

}
