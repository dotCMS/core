package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.ShortOutputOptionMixin;
import com.dotcms.cli.common.WorkspaceMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.views.CommonViews;
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
                "" // empty line left here on purpose to make room at the end
        }
)
public class ContentTypePull extends AbstractContentTypeCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "pull";
    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @Parameters( paramLabel = "idOrName", index = "0", arity = "1", description = "Identifier or Name.")
    String idOrVar;

    @Override
    public Integer call() throws IOException {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

                final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.getContentType(idOrVar, null, null);
                final ContentType contentType = responseEntityView.entity();
                final ObjectMapper objectMapper = formatOption.objectMapper();

        if(shortOutputOption.isShortOutput()) {
            final String asString = shortFormat(contentType);
            output.info(asString);
        } else {
                    final String asString = objectMapper.writerWithView(CommonViews.InternalView.class).writeValueAsString(contentType);
                    if(output.isVerbose()) {
                        output.info(asString);
                    }
                    final Workspace workspace = workspaceManager.getOrCreate(workspaceMixin.workspace());
                    final String fileName = String.format("%s.%s",contentType.variable(), formatOption.getInputOutputFormat().getExtension());
                    final Path path = Path.of(workspace.contentTypes().toString(), fileName);

                    Files.writeString(path, asString);
                    output.info(String.format("Output has been written to file [%s].",path));
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
