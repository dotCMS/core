package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypePush.NAME,
        description = "@|bold,green Push / Create a Content-type from a given file definition.|@ @|bold,cyan --file|@ to send the descriptor. "
)
public class ContentTypePush extends ContentTypeCommand implements Callable<Integer> {

    static final String NAME = "content-type-push";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = { "-f", "--file" }, order = 1,required = true, description = " The json/yml formatted content-type descriptor file to be pushed. ")
    File file;

    @Override
    public Integer call() throws Exception {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);


        if (!file.exists() || !file.canRead()) {
            output.error(String.format(
                    "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                    file.getAbsolutePath()));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final ObjectMapper objectMapper = output.objectMapper();

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
                        List.of(contentType));

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
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;

    }
}
