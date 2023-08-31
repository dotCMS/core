package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.WorkspaceMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypePush.NAME,
        header = "@|bold,blue Use this command to push a Content-type from a file.|@",
        description = {
                " This command will push a content-type to the current active",
                " remote instance of dotCMS from a given file.",
                " When pulling a content-type from a remote dotCMS instance",
                " the content-type is saved to a file.",
                " The file name will be the content-type's variable name.",
                " To make changes to the a Content-type",
                " modify the file and push it back to the remote instance.",
                " The file can also be used as a base to create a brand new content-type.",
                " The format can be changed using the @|yellow --format|@ option.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class ContentTypePush extends AbstractContentTypeCommand implements Callable<Integer>,
        DotCommand {

    static final String NAME = "push";

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOptionMixin;

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @CommandLine.Parameters(index = "0", arity = "1", description = "The json/yml formatted content-type descriptor file to be pushed. ")
    File file;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        File inputFile = this.file;
        if (null == inputFile) {
            output.error("The input file is required.");
            return ExitCode.USAGE;
        } else {
            final Optional<Workspace> workspace = workspaceManager.findWorkspace(
                    workspaceMixin.workspace());
            if (workspace.isPresent() && (!inputFile.isAbsolute())) {
                inputFile = Path.of(workspace.get().contentTypes().toString(), inputFile.getName())
                        .toFile();
            }
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new IOException(String.format(
                        "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                        inputFile));
            }
        }

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ObjectMapper objectMapper = formatOptionMixin.objectMapper(inputFile);

        final ContentType contentType = objectMapper
                .readValue(inputFile, ContentType.class);
        final String varNameOrId =
                StringUtils.isNotEmpty(contentType.variable()) ? contentType.variable()
                        : contentType.id();

        if (StringUtils.isNotEmpty(varNameOrId)) {
            output.info(String.format(
                    "The identifier @|bold,green [%s]|@ provided in the content-type file will be used for look-up.",
                    varNameOrId));
        } else {
            output.info("The content-type file @|bold does not|@ provided an identifier. ");
        }

        if (StringUtils.isNotEmpty(varNameOrId) && findExistingContentType(contentTypeAPI,
                varNameOrId).isPresent()) {
            output.info(String.format(
                    "ContentType identified by @|bold,green [%s]|@ already exists. An @|bold update |@ will be attempted.",
                    contentType.variable()));
            final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.updateContentTypes(
                    contentType.variable(), contentType);

            output.info(String.format("Content-Type @|bold,green [%s]|@ successfully updated.",
                    varNameOrId));

            if (output.isVerbose()) {
                final ContentType entity = responseEntityView.entity();
                output.info(objectMapper.writeValueAsString(entity));
            }

        } else {
            output.info(String.format(
                    "ContentType identified by @|bold,green [%s]|@ does not exist. Attempting to create it. ",
                    contentType.variable()));
            final ResponseEntityView<List<ContentType>> responseEntityView = contentTypeAPI.createContentTypes(
                    List.of(contentType));

            output.info(String.format("Content-Type @|bold,green [%s]|@ successfully created.",
                    varNameOrId));

            if (output.isVerbose()) {
                final List<ContentType> contentTypes = responseEntityView.entity();
                if (!contentTypes.isEmpty()) {
                    output.info(objectMapper.writeValueAsString(contentTypes.get(0)));
                } else {
                    throw new IOException("Response was empty.");
                }
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
