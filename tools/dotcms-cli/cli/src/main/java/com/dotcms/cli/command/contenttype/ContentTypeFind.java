package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
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
        name = ContentTypeFind.NAME,
        description = "@|bold,green Search or Get a List with all available Content-types  |@  Use @|bold,cyan --all|@ to get a complete list. @|bold,cyan --name|@ To specify a search criteria."
)
public class ContentTypeFind extends ContentTypeCommand implements Callable<Integer> {

    static final String NAME = "content-type-find";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    /**
     * Here we encapsulate List Options
     * This maps directly to GET: /api/v1/contenttype
     */
    static class ListOptions {

        @CommandLine.Option(names = { "-a", "--all" },
                order = 10,
                description = {"Quick way to visualize all available content-types. for more detailed view see options pull and filter"},
                defaultValue = "false",
                required = true)
        boolean all;

        @CommandLine.Option(names = { "-i", "--interactive" },
                order = 20,
                description = {"Allows loading items in batches of 10"},
                defaultValue = "true")
        boolean interactive = true;
    }

    /**
     * Here we encapsulate Filter endpoint options
     * This maps directly to POST /api/v1/contenttype/_filter
     */
    static class FilterOptions {

        @CommandLine.Option(names = {"-n","--name"},
                order = 30,
                description = "Specify (comma separated) var-name to search by. ")
        String typeName;

        @CommandLine.Option(names = {"-h", "--host"},
                order = 40,
                description = "Filter by host")
        String host;

        @CommandLine.Option(names = {"-o", "--order"},
                order=50,
                description = "Set an order by param. (variable is default) ", defaultValue = "variable")
        String orderBy;

        @CommandLine.Option(names = {"-p", "--page"},
                order = 60,
                description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-ps", "--pageSize"},
                order = 70,
                description = "Items per page.", defaultValue = "25")
        Integer pageSize;

    }

    /**
     * Here we tell PicoCli that we want each individual Option to act as a separate functionality
     */
    static class MutuallyExclusiveOptions {

       @CommandLine.ArgGroup(exclusive = false, order = 80, heading = "\nList-All available Content-Types\n")
       ListOptions list;

       @CommandLine.ArgGroup(exclusive = false, order = 90, heading = "\nFilter/Search available Content-Types\n")
       FilterOptions filter;

    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    MutuallyExclusiveOptions options;

    @Override
    public Integer call() throws Exception {

        if(null != options.list){
           return list(options.list);
        }

        if(null != options.filter){
           return list(options.filter);
        }

        return CommandLine.ExitCode.USAGE;
    }

    private int list(final ListOptions list) {
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

            if(list.interactive && !BooleanUtils.toBoolean(System.console().readLine("Load next page? y/n:"))){
                break;
            }

        }
        return CommandLine.ExitCode.OK;
    }


    private int list(final FilterOptions filter) {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.getContentTypes(
                filter.typeName, filter.page, filter.pageSize,
                filter.orderBy, null, null, filter.host);

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
