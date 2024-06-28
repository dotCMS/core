package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CopySiteRequest;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteCopy.NAME,
        aliases = SiteCopy.ALIAS,
        header = "@|bold,blue Use this command to copy an existing site.|@",
        description = {
           " The command provides the ability to copy individually site elements such as: ",
           " pages, folders, links, template containers, and site variables. ",
           " Or everything at once through the use of the @|yellow --all|@ param. ",
           " The new site will be created with the name specified in the param @|yellow --copyName|@ ",
           "" // empty line left here on purpose to make room at the end
        }
)
public class SiteCopy extends AbstractSiteCommand implements Callable<Integer>, DotCommand {
    static final String NAME = "copy";

    static final String ALIAS = "cp";

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

        @CommandLine.Option(names = {"-var",
                "--variable"}, paramLabel = "Variables", description = "if specified site variables will be copied.", defaultValue = "false")
        boolean copySiteVariables;
    }

    @CommandLine.ArgGroup(exclusive = false,  heading = "\n@|bold,blue Individual Copy Options. |@\n")
    CopyOptions copyOptions;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        return copy();
    }

    private int copy() {

        final SiteView site = findSite(siteNameOrId);

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        ResponseEntityView<SiteView> copy = siteAPI.copy(fromSite(site, copySiteName, copyAll));

        output.info(String.format(
                "New Copy Site is [%s]. Please note that the full replication of all site elements "
                        + "is executed as a background job. To confirm the success of the copy "
                        + "operation, please check the dotCMS server.",
                copy.entity().hostName()
        ));

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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
