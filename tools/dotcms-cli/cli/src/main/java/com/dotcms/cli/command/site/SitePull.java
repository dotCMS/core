package com.dotcms.cli.command.site;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Utils;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.dotcms.cli.common.Utils.nextFileName;

@ActivateRequestContext
@CommandLine.Command(name = SitePull.NAME,
        description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan idOrName|@ to get a site by name or id. @|bold,cyan -l|@ Shows live Sites. "
)
public class SitePull extends AbstractSiteCommand implements Callable<Integer> {

    static final String NAME = "pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Site name Or Id.")
    String siteNameOrId;

    @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save to.")
    File saveAs;

    @Override
    public Integer call() {
        return pull();
    }

    private int pull() {

        final Optional<SiteView> site = super.findSite(siteNameOrId);

        if (site.isEmpty()) {
            output.error(String.format(
                    "Error occurred while pulling Site Info: [%s].", siteNameOrId));
            return CommandLine.ExitCode.SOFTWARE;
        }

        final SiteView siteView = site.get();
        try {
            if (output.isShortenOutput()) {
                final String shortFormat = shortFormat(siteView);
                output.info(shortFormat);
                if (null != saveAs) {
                    Files.writeString(saveAs.toPath(), shortFormat);
                }
            } else {
                ObjectMapper objectMapper = output.objectMapper();
                final String asString = objectMapper.writeValueAsString(siteView);
                output.info(asString);
                Path path;
                if (null != saveAs) {
                    path = saveAs.toPath();
                } else {
                    final String fileName = String.format("%s.%s",siteView.hostName(),output.getInputOutputFormat().getExtension());
                    final Path next = Path.of(".", fileName);
                    path = nextFileName(next);
                }
                Files.writeString(path, asString);
                output.info(String.format("Output has been written to file [%s].",path));
            }
        } catch (IOException e) {
            output.error("Error occurred transforming the response: ", e.getMessage());
        }

        return CommandLine.ExitCode.OK;
    }

}
