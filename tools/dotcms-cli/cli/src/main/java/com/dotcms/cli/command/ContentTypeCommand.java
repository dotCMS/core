package com.dotcms.cli.command;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.contenttype.FilterContentTypesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypeCommand.NAME,
        header = "@|bold,green Provides Content-type CRUD support operations.|@",
        description = "",
        sortOptions = false
)
public class ContentTypeCommand implements Callable<Integer> {

    static final String NAME = "content-type";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @ArgGroup(exclusive = true, multiplicity = "1")
    MutuallyExclusiveOptions options;

    /**
     * Here we tell PicoCli that we want each individual Option to act as a separate functionality
     */
    static class MutuallyExclusiveOptions {

        @ArgGroup(exclusive = false, order = 1, heading = "\nPull Content-type\n")
        PullOptions pull;

        @ArgGroup(exclusive = false, order = 20, heading = "\nPush Content-Type\n")
        PushOptions push;

        @ArgGroup(exclusive = false, order = 30, heading = "\nList available Content-Types\n")
        ListOptions list;

        @ArgGroup(exclusive = false, order = 40, heading = "\nFilter/Search available Content-Types\n")
        FilterOptions filter;

        @ArgGroup(exclusive = false, order = 50, heading = "\nRemove Unwanted Content-types\n")
        RemoveOptions remove;

    }

    /**
     * Here we encapsulate Pull Content-Type options
     * This maps directly to GET: /api/v1/contenttype/id/{Content-Type}
     */
    static class PullOptions {

        @CommandLine.Option(names = {"-pl","--pull"}, order = 2, description = "Pull Content-type by id or var-name", required = true)
        String idOrVar;

        @CommandLine.Option(names = {"-ln", "--lang"}, order = 3, description = "Content-type Language.", defaultValue = "1")
        Long lang;

        @CommandLine.Option(names = {"-l", "--live"}, order = 4, description = "live content if omitted then working will be used.", defaultValue = "true")
        Boolean live;

        @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save to.")
        File saveAs;

    }

    /**
     * Here we encapsulate Push Content-Type options
     */
    static class PushOptions {
        @CommandLine.Option(names = { "-ps", "--push" }, order = 21,required = true, description = " The json formatted content-type descriptor file to be pushed. ")
        File contentTypeFile;
    }

    /**
     * Here we encapsulate List Options
     * This maps directly to GET: /api/v1/contenttype
     */
    static class ListOptions {

        @CommandLine.Option(names = { "-ls", "--list" },
                order = 31,
                description = {"Quick way to visualize all available content-types. for more detailed view see options pull and filter"},
                defaultValue = "false",
                required = true)
        boolean list;

        @CommandLine.Option(names = { "-i", "--interactive" },
                order = 32,
                description = {"Allows to load Content-Types in batches of 10"},
                defaultValue = "true")
        boolean interactive = true;
    }

    /**
     * Here we encapsulate Filter endpoint options
     * This maps directly to POST /api/v1/contenttype/_filter
     */
    static class FilterOptions {

        @CommandLine.Option(names = {"-f","--filter"},
                order = 41,
                description = "Specify (comma separated) var-name to search by. ")
        String typeName;

        @CommandLine.Option(names = {"-h", "--host"},
                order = 42,
                description = "Filter by host")
        String host;

        @CommandLine.Option(names = {"-o", "--order"},
                order=43,
                description = "Set an order by param. (variable is default) ", defaultValue = "variable")
        String orderBy;

        @CommandLine.Option(names = {"-p", "--page"},
                order = 44,
                description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-s", "--pageSize"},
                order = 45,
                description = "Items per page.", defaultValue = "25")
        Integer pageSize;

    }

    static class RemoveOptions {
        @CommandLine.Option(names = {"-rm","--remove"}, order = 51, description = "Remove Content-type by id or var-name", required = true)
        String idOrVar;
    }

    /**
     * This command has 4 main options
     * Push
     * Pull
     * List
     * Filter
     * @return
     */
    @Override
    public Integer call() {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);

        if (isListOptionOn(options.list)) {
            return executeList(contentTypeAPI, options.list);
        }

        if (isPullOptionOn(options.pull)) {
            return executePull(contentTypeAPI, objectMapper, options.pull);
        }

        if (isPushOptionOn(options.push)) {
            return executePush(contentTypeAPI, objectMapper, options.push);
        }

        if (isFilterOptionOn(options.filter)) {
            return executeFilter(contentTypeAPI, options.filter);
        }

        if(isRemoveOptionOn(options.remove)){
            return executeRemove(contentTypeAPI, options.remove);
        }

        //We're not supposed to get this far here unless our params are messed up
        return ExitCode.USAGE;
    }

    /**
     * Executes Filter option-subcommand
     * @param contentTypeAPI
     * @param filter
     * @return
     */
    private int executeFilter(final ContentTypeAPI contentTypeAPI, final FilterOptions filter) {

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
        return ExitCode.OK;
    }

    /**
     * Executes Push option-subcommand
     * @param contentTypeAPI
     * @param objectMapper
     * @param pushOptions
     * @return
     */
    private int executePush(final ContentTypeAPI contentTypeAPI, final ObjectMapper objectMapper, final PushOptions pushOptions) {
        final File file = pushOptions.contentTypeFile;
        if (!file.exists() || !file.canRead()) {
            output.error(String.format(
                    "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                    pushOptions.contentTypeFile.getAbsolutePath()));
            return ExitCode.SOFTWARE;
        }

        try {

            final ContentType contentType = objectMapper
                    .readValue(file, ContentType.class);
            final String varNameOrId =
                    StringUtils.isNotEmpty(contentType.variable()) ? contentType.variable()
                            : contentType.id();

            if (StringUtils.isNotEmpty(varNameOrId)) {
                output.info(String.format("The identifier @|bold,green [%s]|@ provided in the content-type file will be used for look-up.", varNameOrId));
            } else  {
                output.info("The content-type file @|bold does not|@ provided an identifier. ");
            }

            if (StringUtils.isNotEmpty(varNameOrId) && findExistingContentType(contentTypeAPI,
                    varNameOrId).isPresent()) {
                output.info(String.format(
                        "ContentType identified by @|bold,green [%s]|@ already exists. An @|bold update |@ will be attempted.",
                        contentType.variable()));
                final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.updateContentTypes(
                        contentType.variable(), contentType);

                output.info(String.format("Content-Type @|bold,green [%s]|@ successfully updated.",varNameOrId));

                if(output.isVerbose()){
                    final ContentType entity = responseEntityView.entity();
                    output.info(objectMapper.writeValueAsString(entity));
                }

            } else {
                output.info(String.format(
                        "ContentType identified by @|bold,green [%s]|@ does not exist. Attempting to create it. ",
                        contentType.variable()));
                final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.createContentTypes(
                        ImmutableList.of(contentType));

                output.info(String.format("Content-Type @|bold,green [%s]|@ successfully created.",varNameOrId));

                if(output.isVerbose()){
                    final List<ContentType> contentTypes = responseEntityView.entity();
                    if(!contentTypes.isEmpty()){
                        output.info(objectMapper.writeValueAsString(contentTypes.get(0)));
                    } else {
                        output.error("Response was empty.");
                    }
                }
            }
        } catch (IOException e) {
            output.error(String.format(
                    "Error occurred while pushing ContentType from file: [%s] with message: [%s].",
                    file.getAbsolutePath(), e.getMessage()));
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

    /**
     * Executes Pull option-subcommand
     * @param contentTypeAPI
     * @param objectMapper
     * @param pull
     * @return
     */
    private int executePull(final ContentTypeAPI contentTypeAPI, final ObjectMapper objectMapper, final PullOptions pull) {
        try {
            final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.getContentType(pull.idOrVar, pull.lang, pull.live);
            final ContentType contentType = responseEntityView.entity();

            final String asString = objectMapper.writeValueAsString(contentType);
            output.info(asString);
            final File saveAs = pull.saveAs;
            if(null != saveAs){
                try {
                    Files.write( saveAs.toPath(), asString.getBytes());
                } catch (IOException e) {
                    output.error(String.format("Error occurred saving the output to the specified file [%s]",saveAs));
                }
            }

        } catch (NotFoundException | JsonProcessingException e) {
            output.error(String.format(
                    "Error occurred while pulling ContentType: [%s] with message: [%s].",
                    pull.idOrVar, e.getMessage()));
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

    /**
     * Executes List option-subcommand
     * @param contentTypeAPI
     * @return
     */
    private int executeList(final ContentTypeAPI contentTypeAPI, final ListOptions list) {
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
        return ExitCode.OK;
    }

    /**
     * Executes Remove option-subcommand
     * @param contentTypeAPI
     * @param remove
     * @return
     */
    private int executeRemove(final ContentTypeAPI contentTypeAPI, final RemoveOptions remove) {
        final ResponseEntityView<String> responseEntityView = contentTypeAPI.delete(remove.idOrVar);
        final String entity = responseEntityView.entity();
        output.info(entity);
        return ExitCode.OK;
    }

    public Optional<ContentType> findExistingContentType(final ContentTypeAPI contentTypeAPI, final String varNameOrId ){
        try {
            final ResponseEntityView<ContentType> found = contentTypeAPI.getContentType(varNameOrId, null, false);
            return Optional.of(found.entity());
        }catch (NotFoundException e){
             //Not relevant
        }
        return Optional.empty();
    }

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String shortFormat(final ContentType contentType) {
        return String.format(
                "varName: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] host: [@|bold,underline,green %s|@] modDate:[@|bold,yellow %s|@] desc: [@|bold,yellow %s|@]",
                contentType.variable(),
                contentType.id(),
                contentType.host(),
                contentType.modDate() != null ? format.format(contentType.modDate()): "n/a" ,
                StringUtils.isNotEmpty(contentType.description()) ? StringUtils.abbreviate(
                        contentType.description(), 50) : "n/a"
        );
    }

    private boolean isListOptionOn(final ListOptions list){
        return list != null && list.list;
    }

    private boolean isPullOptionOn(final PullOptions options) {
        return options != null && StringUtils.isNotEmpty(options.idOrVar);
    }

    private boolean isPushOptionOn(final PushOptions options) {
        return null != options && options.contentTypeFile != null;
    }

    private boolean isFilterOptionOn(final FilterOptions options){
       return options != null && null != options.typeName;
    }

    private boolean isRemoveOptionOn(final RemoveOptions options){
        return null != options && StringUtils.isNotEmpty(options.idOrVar);
    }
}
