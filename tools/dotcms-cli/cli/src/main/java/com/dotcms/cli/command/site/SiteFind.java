package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import java.util.List;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteFind.NAME,
     header = "@|bold,blue Use this command to Search / Find Sites.|@",
     description = {
            "Search or Get a List with all available Sites.",
            "Use @|yellow --name|@ in conjunction with @|bold,blue Filter/Search|@ Options.",
             "" // This is needed to add a new line after the description.
    }
)
public class SiteFind extends AbstractSiteCommand implements Callable<Integer>, DotCommand {
    static final String NAME = "find";

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

    @CommandLine.ArgGroup(exclusive = false, order = 1, heading = "\n@|bold,blue Filter/Search Options. |@\n")
    FilterOptions filter;

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @ConfigProperty(name = "site.pageSize", defaultValue = "25")
    Integer pageSize;

    @Inject
    Prompt prompt;

    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        if(null != filter){
           return filter(filter);
        }

        return list();

    }

    private int list() {
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        int page = 1;

        while (true) {

            final ResponseEntityView<List<Site>> response = siteAPI.getSites(null, null, false, false, page, pageSize);

            final List<Site> sites = response.entity();
            if (sites.isEmpty()) {
                output.info("@|yellow No sites were returned, Check you have access permissions.|@");
                break;
            }

            for (final Site site : sites) {
                output.info(shortFormat(site));
            }

            //First we show live sites
            if(sites.size() < pageSize){
                break;
            }
            page++;
            if(interactiveOption.isInteractive() && !prompt.yesOrNo(true,"Load next page? y/n: ")){
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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }


}
