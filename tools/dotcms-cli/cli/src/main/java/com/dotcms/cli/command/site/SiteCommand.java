package com.dotcms.cli.command.site;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteCommand.NAME, description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan -n|@ to filter by name. @|bold,cyan -a|@ Shows archived sites. @|bold,cyan -l|@ Shows live Sites. @|bold,cyan -p|@ (Page) @|bold,cyan -ps|@ (PageSize) Can be used combined for pagination.")
public class SiteCommand implements Callable<Integer> {

    static final String NAME = "site";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;
/*
    static class ListOptions {

        @CommandLine.Option(names = { "-ls", "--list" },
                order = 31,
                description = {"Quick way to visualize all available sites. for more detailed view see options pull and filter"},
                defaultValue = "false",
                required = true)
        boolean list;

        @CommandLine.Option(names = { "-i", "--interactive" },
                order = 32,
                description = {"Allows to load Sites in batches of 10"},
                defaultValue = "true")
        boolean interactive = true;

    }

    static class FilterOptions {
        @CommandLine.Option(names = {"-n", "--name"}, description = "Filter by site name.", interactive = true)
        String name;

        @CommandLine.Option(names = {"-a", "--archived"}, description = "Show archived sites.", defaultValue = "false")
        Boolean archived;

        @CommandLine.Option(names = {"-l", "--live"}, description = "Show live sites.", defaultValue = "true")
        Boolean live;

        @CommandLine.Option(names = {"-p", "--page"}, description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-s", "--pageSize"}, description = "Items per page.", defaultValue = "25")
        Integer pageSize;

    }

    static class PullOptions {

        @CommandLine.Option(names = {"-pl","--pull"}, order = 2, description = "Pull Content-type by id or var-name", required = true)
        String siteNameOrId;

        @CommandLine.Option(names = {"-l", "--live"}, order = 4, description = "live content if omitted then working will be used.", defaultValue = "true")
        Boolean live;

        @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save to.")
        File saveAs;

    }

    static class PushOptions {
        @CommandLine.Option(names = {"-ps", "--push"}, order = 21, required = true, description = " The json formatted Site descriptor file to be pushed. ")
        File siteFile;

        @CommandLine.Option(names = {"-c", "--create"}, order = 21, required = true, description = " Quick way to create a site. Simply pass a site name. ")
        String siteName;

    }

    static class MutuallyExclusiveOptions {

        @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\nList Sites\n")
        ListOptions list;

   //     @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\nSearch Sites\n")
        //FilterOptions filter;
        @CommandLine.ArgGroup(exclusive = false, order = 40, heading = "\nFilter/Search available Content-Types\n")
        FilterOptionsMixin filterOptionsMixin;

        @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\nPull Site\n")
        PullOptions pull;

        @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\nPush Site\n")
        PushOptions push;

    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    MutuallyExclusiveOptions options;
*/
    @Override
    public Integer call() {
/*
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        if (isListOptionOn(options.list)) {
            return executeList(siteAPI, options.list);
        }

        if(isFilterOptionOn(options.filter)){
           return executeFilter(siteAPI,options.filter);
        }

        if(isPullOptionOn(options.pull)){
           return executePull(siteAPI, options.pull);
        }

        if (isPushOptionOn(options.push)) {
           return executePush(siteAPI, options.push);
        }
*/
        return CommandLine.ExitCode.USAGE;
    }
/*
    private int executeList(final SiteAPI siteAPI, final ListOptions list) {
        final int pageSize = 10;
        int page = 0;
        while (true) {
            final ResponseEntityView<List<Site>> response = siteAPI.getSites(null, false, true, false, page, pageSize);

            final List<Site> sites = response.entity();
            for (final Site site : sites) {
                output.info(shortFormat(site));
            }
            if(sites.size() < pageSize){
                break;
            }
            page++;

            if(list.interactive && !BooleanUtils.toBoolean(System.console().readLine("Load next page? y/n:"))){
                break;
            }
        }
        return CommandLine.ExitCode.OK;
    }

    private int executeFilter(final SiteAPI siteAPI, final FilterOptions options) {
            final ResponseEntityView<List<Site>> response = siteAPI.getSites(options.name, options.archived, options.live, false, options.page, options.pageSize);
            final List<Site> sites = response.entity();
            for (final Site site : sites) {
                output.info(shortFormat(site));
            }

        return CommandLine.ExitCode.OK;
    }

    private int executePull(final SiteAPI siteAPI, final PullOptions options) {
        if (null != options.siteNameOrId) {
            try {
                if (options.siteNameOrId.replace("-", "").matches("[a-fA-F0-9]{32}")) {
                    final ResponseEntityView<SiteView> byId = siteAPI.findById(options.siteNameOrId);
                    final SiteView siteView = byId.entity();
                    output.info(shortFormat(siteView));
                    return CommandLine.ExitCode.OK;
                }

                final ResponseEntityView<SiteView> byId = siteAPI.findByName(GetSiteByNameRequest.builder().siteName(options.siteNameOrId).build());
                final SiteView siteView = byId.entity();
                output.info(shortFormat(siteView));
                return CommandLine.ExitCode.OK;
            }catch (NotFoundException e){
                output.error(String.format(
                        "Error occurred while pulling Site: [%s] with message: [%s].",
                        options.siteNameOrId, e.getMessage()));
            }
        }
        return CommandLine.ExitCode.USAGE;
    }

    private int executePush(final SiteAPI siteAPI, final PushOptions options) {

        if (null != options.siteFile) {
            final File file = options.siteFile;
            if (!file.exists() || !file.canRead()) {
                output.error(String.format(
                        "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                        options.siteFile.getAbsolutePath()));
                return CommandLine.ExitCode.SOFTWARE;
            }

            try {
                final ObjectMapper objectMapper = output.objectMapper();
                final CreateUpdateSiteRequest createUpdateSiteRequest = objectMapper.readValue(file, CreateUpdateSiteRequest.class);
                final String siteName = createUpdateSiteRequest.siteName();
                 if(update(siteAPI, createUpdateSiteRequest, siteName)){
                    return CommandLine.ExitCode.OK;
                 }
                output.info(String.format(" No site named [%s] was found. Will attempt to create it. ",siteName));
                final ResponseEntityView<SiteView> response = siteAPI.create(createUpdateSiteRequest);
                final SiteView siteView = response.entity();
                output.info(String.format("Site @|bold,green [%s]|@ successfully created.",siteName));
                output.info(shortFormat(siteView));
                return CommandLine.ExitCode.OK;
            } catch (IOException e) {
                output.error(String.format(
                        "Error occurred while pushing Site from file: [%s] with message: [%s].",
                        file.getAbsolutePath(), e.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        }

        if (!options.siteName.isEmpty()) {
            final ResponseEntityView<SiteView> response = siteAPI.create(CreateUpdateSiteRequest.builder().siteName(options.siteName).build());
            final SiteView siteView = response.entity();
            output.info(" Site [%s] successfully created.");
            output.info(String.format("Site @|bold,green [%s]|@ successfully created.",options.siteName));
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

    private String shortFormat(final Site site) {
        return String.format(
                "name: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] inode: [@|bold,underline,green %s|@] live:[@|bold,yellow %s|@] default: [@|bold,yellow %s|@] archived: [@|bold,yellow %s|@]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

    private String shortFormat(final SiteView site) {
        return String.format(
                "name: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] inode: [@|bold,underline,green %s|@] live:[@|bold,yellow %s|@] default: [@|bold,yellow %s|@] archived: [@|bold,yellow %s|@]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

    private boolean isListOptionOn(final ListOptions options) {
        return null != options && options.list;
    }


    private boolean isFilterOptionOn(final FilterOptions options) {
        return null != options && !options.name.isEmpty();
    }

    private boolean isPushOptionOn(final PushOptions options) {
        return null != options && (null != options.siteFile || (!options.siteName.isEmpty()));
    }

    private boolean isPullOptionOn(final PullOptions options) {
        return null != options && (null != options.siteNameOrId && !options.siteNameOrId.isEmpty());
    }
*/
}
