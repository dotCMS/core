package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(
        sortOptions = false,
        name = ContentTypeFind.NAME,header = "@|bold,blue Use this command to find Content-types.|@",
        description = {
                "Search or Get a List with all available Content-types.",
                "Use @|yellow --name|@ in conjunction with @|bold,blue Filter/Search|@ Options."
        }
)
public class ContentTypeFind extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "find";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @Inject
    RestClientFactory clientFactory;

    /**
     * Here we encapsulate Filter endpoint options
     * This maps directly to POST /api/v1/contenttype/_filter
     */
    static class FilterOptions {

        @CommandLine.Option(names = {"-n","--name"},
                description = "Specify (comma separated) name to search by. ")
        String typeName;

        @CommandLine.Option(names = {"-s", "--site"},
                description = "Filter by site")
        String site;

        @CommandLine.Option(names = {"-o", "--order"},
                description = "Set an order by param. (variable is default) ", defaultValue = "variable")
        String orderBy;

        @CommandLine.Option(names = {"-p", "--page"},
                description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-ps", "--pageSize"},
                description = "Items per page.", defaultValue = "25")
        Integer pageSize;

    }

    /**
     * Here we tell PicoCli that we want each individual Option to act as a separate functionality
     */

   @CommandLine.ArgGroup(exclusive = false,  heading = "\n@|bold,blue Filter/Search Options. |@\n")
   FilterOptions filter;

    @Override
    public Integer call() throws Exception {
        if(null != filter){
            return list(filter);
        }

        return list();
    }

    private int list() {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final int pageSize = 10;
        int page = 0;
        while (true) {
            final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.getContentTypes(
                    null, page, null, "variable", null, null, null);
            final List<ContentType> types = responseEntityView.entity();
            for (final ContentType contentType : types) {
                output.info(shortFormat(contentType));
            }
            if(types.size() < pageSize){
                break;
            }
            page++;

            if(interactiveOption.isInteractive() && !BooleanUtils.toBoolean(System.console().readLine("Load next page? y/n:"))){
                break;
            }

        }
        return CommandLine.ExitCode.OK;
    }


    private int list(final FilterOptions filter) {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.getContentTypes(
                filter.typeName, filter.page, filter.pageSize,
                filter.orderBy, null, null, filter.site);

        final List<ContentType> types = responseEntityView.entity();
        if (types.isEmpty()) {
            output.info("No results matched your search criteria.");
        } else {
            for (final ContentType contentType : types) {
                output.info(shortFormat(contentType));
            }
        }
        return CommandLine.ExitCode.OK;
    }

}
