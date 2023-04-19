package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import java.util.List;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SiteFind.NAME,
     description = "@|bold,green Retrieves Sites info.|@  @|bold,cyan --all|@ Brings them all.  Use in conjunction params @|bold,cyan -n|@ to filter by name. @|bold,cyan -a|@ Shows archived sites. @|bold,cyan -l|@ Shows live Sites. @|bold,cyan -p|@ (Page) @|bold,cyan -ps|@ (PageSize) Can be used combined for pagination."
)
public class SiteFind extends AbstractSiteCommand implements Callable<Integer> {
    static final String NAME = "find";

    @CommandLine.Mixin(name = "output")
     OutputOptionMixin output;

    static class FilterOptions {
        @CommandLine.Option(names = {"-n", "--name"}, arity = "1" ,description = "Filter by site name.")
        String name;

        @CommandLine.Option(names = {"-ar", "--archived"}, description = "Show archived sites.", defaultValue = "false")
        Boolean archived;

        @CommandLine.Option(names = {"-l", "--live"}, description = "Show live sites.", defaultValue = "true")
        Boolean live;

        @CommandLine.Option(names = {"-p", "--page"}, arity = "1" ,description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-s", "--pageSize"}, arity = "1", description = "Items per page.", defaultValue = "25")
        Integer pageSize;

    }

    @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\nSearch Sites\n")
    FilterOptions filter;

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @Override
    public Integer call() {

        if(null != filter){
           return filter(filter);
        }

        return list();

    }

    private int list() {
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        final int pageSize = 10;
        int page = 0;

        boolean live = true;

        while (true) {

            final ResponseEntityView<List<Site>> response = siteAPI.getSites(null, null, live, false, page, pageSize);

            final List<Site> sites = response.entity();

            for (final Site site : sites) {
                output.info(shortFormat(site));
            }

            //First we show live sites
            if(live) {
                //When we're showing live sites, and we run out of `live` sites
                if (sites.size() < pageSize) {
                    live = false; //We need to switch to `working` sites
                    page = 0;  //Page needs to be reset
                } else {
                    // otherwise business as usual get me next page
                    page++;
                }
              //At some point we run out of live sites time to show 'working' sites
            } else {
                if (sites.size() < pageSize) {
                    break;
                }
                page++;
            }
            if(interactiveOption.isInteractive() && !BooleanUtils.toBoolean(System.console().readLine("Load next page? y/n:" ))){
                break;
            }
        }
        return CommandLine.ExitCode.OK;
    }

    private int filter(final FilterOptions options) {
            final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
            final ResponseEntityView<List<Site>> response = siteAPI.getSites(options.name, options.archived, options.live, false, options.page, options.pageSize);
            final List<Site> sites = response.entity();
            for (final Site site : sites) {
                output.info(shortFormat(site));
            }

        return CommandLine.ExitCode.OK;
    }
}
