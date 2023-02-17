package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypePull.NAME,
        description = "@|bold,green Get a Content-type from a given  idOrVar |@ Use @|bold,cyan --idOrVar|@ to pass the CT identifier or var name."
)
public class ContentTypePull extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @Parameters(index = "0", arity = "1", description = "")
    String idOrVar;


    @Override
    public Integer call() {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
            try {
                final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.getContentType(idOrVar, null, null);
                final ContentType contentType = responseEntityView.entity();
                final ObjectMapper objectMapper = output.objectMapper();

                if(output.isShortenOutput()) {
                    final String asString = shortFormat(contentType);
                    output.info(asString);
                } else {
                    final String fileName = String.format("%s.%s",contentType.variable(),output.getInputOutputFormat().getExtension());
                    final Path saveAs = output.nextFileName(fileName);
                    final String asString = objectMapper.writeValueAsString(contentType);
                    output.info(asString);
                    Files.writeString(saveAs, asString);
                }
            } catch (IOException | NotFoundException e) {
                output.error(String.format(
                        "Error occurred while pulling ContentType: [%s] with message: [%s].",
                        idOrVar, e.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        return CommandLine.ExitCode.OK;
    }

}
