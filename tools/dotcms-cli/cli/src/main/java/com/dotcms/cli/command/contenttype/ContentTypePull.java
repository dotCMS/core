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

import static com.dotcms.cli.common.Utils.nextFileName;

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

    @Parameters(index = "0", arity = "1", description = "CT Identifier or varName.")
    String idOrVar;

    @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save Pulled CT to a file.")
    File saveAs;

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
                    final String asString = objectMapper.writeValueAsString(contentType);
                    output.info(asString);

                    //By default, We'll always save pulled CT as file using CT's var name
                    final Path path;
                    if (null != saveAs) {
                       path = saveAs.toPath();
                    } else {
                        //But this behavior can be modified if we explicitly add a file name
                        final String fileName = String.format("%s.%s",contentType.variable(),output.getInputOutputFormat().getExtension());
                        final Path next = Path.of(".", fileName);
                        path = nextFileName(next);
                    }
                    Files.writeString(path, asString);
                    output.info(String.format("Output has been written to file [%s].",path));
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
