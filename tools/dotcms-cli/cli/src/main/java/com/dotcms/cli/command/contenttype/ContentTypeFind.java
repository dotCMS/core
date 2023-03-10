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
public class ContentTypeFind extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "find";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

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

   @CommandLine.ArgGroup(exclusive = false, order = 10, heading = "\nFilter/Search available Content-Types\n")
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

            if(output.isInteractive() && !BooleanUtils.toBoolean(System.console().readLine("Load next page? y/n:"))){
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
