package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.ShortOutputOptionMixin;
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
        header = "@|bold,blue Retrieves a Content-type descriptor from a given name or Id.|@",
        description = {
                " This gets you a Content-type from a given Id or Name.",
                " The content-type descriptor will be retried and saved to a file.",
                " The file name will be the content-type's variable name.",
                " if a file is pulled more than once",
                " the file gets override.",
                " By default files are saved to the current directory. in json format.",
                " The format can be changed using the @|yellow --format|@ option.",
                " format can be either @|yellow JSON|@ or @|yellow YAML|@.",
                " File location can be changed using the @|yellow --saveTo|@ option.",
                " Use @|yellow --idOrName|@ to pass the CT identifier or name.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class ContentTypePull extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;
    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @Inject
    RestClientFactory clientFactory;

    @Parameters( paramLabel = "idOrName", index = "0", arity = "1", description = "Identifier or Name.")
    String idOrVar;

    @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save Pulled CT to a file.")
    File saveAs;

    @Override
    public Integer call() {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
            try {
                final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.getContentType(idOrVar, null, null);
                final ContentType contentType = responseEntityView.entity();
                final ObjectMapper objectMapper = formatOption.objectMapper();

                if(shortOutputOption.isShortOutput()) {
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
                        final String fileName = String.format("%s.%s",contentType.variable(), formatOption.getInputOutputFormat().getExtension());
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
