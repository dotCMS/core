package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.ShortOutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

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
    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @Inject
    WorkspaceManager workspaceManager;

    @Parameters( paramLabel = "idOrName", index = "0", arity = "1", description = "Identifier or Name.")
    String idOrVar;

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

                    final Workspace workspace = workspaceManager.resolve();

                    final String fileName = String.format("%s.%s",contentType.variable(), formatOption.getInputOutputFormat().getExtension());
                    final Path path = Path.of(workspace.contentTypes().toString(), fileName);

                    Files.writeString(path, asString);
                    output.info(String.format("Output has been written to file [%s].",path));
                }
            } catch (IOException | NotFoundException e) {
                output.error(String.format(
                        "Error occurred while pulling ContentType: [%s] with message: [%s:%s].",
                        idOrVar,  e.getClass().getSimpleName(), e.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }
        return CommandLine.ExitCode.OK;
    }

}
