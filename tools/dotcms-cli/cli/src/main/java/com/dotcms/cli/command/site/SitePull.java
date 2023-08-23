package com.dotcms.cli.command.site;

import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.ShortOutputOptionMixin;
import com.dotcms.cli.common.WorkspaceMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SitePull.NAME,
        header = "@|bold,blue Retrieves a site descriptor from a name or Id.|@",
        description = {
           "  This retrieves Sites info.",
           "  The Site info will be retrieved and saved to a file.",
           "  The file name will be the Site's host name.",
           "  if a file is pulled more than once",
           "  the file gets override.",
           "  By default files are saved to the current directory. in json format.",
           "  The format can be changed using the @|yellow --format|@ option.",
           "  format can be either @|yellow JSON|@ or @|yellow YAML|@.",
           "" // empty line left here on purpose to make room at the end
        }
)
public class SitePull extends AbstractSiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "pull";

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Mixin(name = "workspace")
    WorkspaceMixin workspaceMixin;

    @Inject
    WorkspaceManager workspaceManager;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "idOrName", description = "Site name or Id.")
    String siteNameOrId;

    @Override
    public Integer call() throws IOException {
        return pull();
    }

    private int pull() throws IOException {

        final SiteView siteView = findSite(siteNameOrId);
        if (shortOutputOption.isShortOutput()) {
            final String shortFormat = shortFormat(siteView);
            output.info(shortFormat);
        } else {
            ObjectMapper objectMapper = formatOption.objectMapper();
            final String asString = objectMapper.writeValueAsString(siteView);
            if(output.isVerbose()) {
                output.info(asString);
            }

            final Workspace workspace = workspaceManager.getOrCreate(workspaceMixin.workspace());
            final String fileName = String.format("%s.%s", siteView.hostName(),
                    formatOption.getInputOutputFormat().getExtension());
            final Path path = Path.of(workspace.sites().toString(), fileName);
            Files.writeString(path, asString);
            output.info(String.format("Output has been written to file [%s].", path));
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
