package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import java.util.List;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        sortOptions = false,
        name = ContentTypeFind.NAME,header = "@|bold,blue Use this command to find Content-types.|@",
        description = {
                "Search or Get a List with all available Content-types.",
                "Use @|yellow --name|@ in conjunction with @|bold,blue Filter/Search|@ Options."
        }
)
public class ContentTypeFind extends AbstractContentTypeCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "find";

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @Inject
    Prompt prompt;

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
                description = {
                    "Set an order by param. (variable is used default)",
                    "Expected values that can be used are: ",
                    "variable, name, description, modDate"
                },
                defaultValue = "variable")
        String orderBy;

        @CommandLine.Option(names = {"-d", "--direction"},
                description = "Set order direction. Accepts ASC or DESC (case insensitive)", defaultValue = "ASC")
        String direction;

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

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @ConfigProperty(name = "contentType.pageSize", defaultValue = "25")
    Integer pageSize;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        if(null != filter){
            return list(filter);
        }

        return list();
    }

    private int list() {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        int page = 1;
        while (true) {
            final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.getContentTypes(
                    null, page,  pageSize, "variable", null, null, null);
            final List<ContentType> types = responseEntityView.entity();
            if (types.isEmpty()) {
                output.info("@|yellow No content-types were returned, Check you have access permissions.|@");
                break;
            }
            for (final ContentType contentType : types) {
                output.info(shortFormat(contentType));
            }
            if(types.size() < pageSize){
                break;
            }
            page++;

            if(interactiveOption.isInteractive() && !prompt.yesOrNo(true,"Load next page? y/n:")){
                break;
            }

        }
        return CommandLine.ExitCode.OK;
    }


    private int list(final FilterOptions filter) {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.getContentTypes(
                filter.typeName, filter.page, filter.pageSize,
                filter.orderBy, filter.direction.toUpperCase(), null, filter.site);

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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
