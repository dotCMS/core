package com.dotcms.cli.command;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import java.util.List;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(name = "site", description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan -n|@ to filter by name. @|bold,cyan -a|@ Shows archived sites. @|bold,cyan -l|@ Shows live Sites. @|bold,cyan -p|@ (Page) @|bold,cyan -ps|@ (PageSize) Can be used combined for pagination.")
public class SiteCommand implements Callable<Integer> {


    static final String NAME = "site";

    /*
    @CommandLine.Option(names = {"-n", "--name"}, description = "Filter by site name.")
    String name;

    @CommandLine.Option(names = {"-a", "--archived"}, description = "Show archived sites.", defaultValue = "false")
    Boolean archived;

    @CommandLine.Option(names = {"-li", "--live"}, description = "Show live sites.", defaultValue = "true")
    Boolean live;

    @CommandLine.Option(names = {"-p", "--page"}, description = "Page Number.", defaultValue = "1")
    Integer page;

    @CommandLine.Option(names = {"-ps", "--pageSize"}, description = "Items per page.", defaultValue = "25")
    Integer pageSize;
*/
    static class ListAllOptionGroup {

        @CommandLine.Option(names = {"-l", "--list"}, description = "List all sites.", required = true)
        Boolean list;
    }

    static class SearchOptionGroup {

        @CommandLine.Option(names = {"-f", "--find"}, description = "Find site by name", required = true)
        Boolean find;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Filter by site name.", required = true)
        String name;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    ListAllOptionGroup listOption;


    @ArgGroup(exclusive = true, multiplicity = "0..1")
    SearchOptionGroup searchOption;

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    ServiceManager serviceManager;

    @Inject
    RestClientFactory clientFactory;



    @Override
    public Integer call() {

        System.out.println("@|bold,green Testing.|@");

        if(null != listOption) {
            System.out.println(listOption.list);
        }

        if(null != searchOption) {
            System.out.println(searchOption.name);
        }
        /*
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<List<Site>> response = siteAPI.getSites(name, archived, live, false, page, pageSize);
        final List<Site> sites = response.entity();
        if (sites.isEmpty()) {

        } else {
            for (final Site site : sites) {

            }
        }*/

        return ExitCode.OK;

    }

}
