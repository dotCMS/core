package com.dotcms.cli.command.site;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.site.SiteView;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import javax.enterprise.context.control.ActivateRequestContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(name = SitePull.NAME,
        description = "@|bold,green Retrieves Sites info.|@ Option params @|bold,cyan idOrName|@ to get a site by name or id. @|bold,cyan -l|@ Shows live Sites. "
)
public class SitePull extends SiteCommand implements Callable<Integer> {

    static final String NAME = "site-pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Option(names = {"-in", "--idOrName"},
            order = 2, description = "Pull Site by id or name", required = true)
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
            if (output.isVerbose()) {
                ObjectMapper objectMapper = output.objectMapper();
                final String valueAsString = objectMapper.writeValueAsString(siteView);
                output.info(valueAsString);
                if (null != saveAs) {
                    Files.writeString(saveAs.toPath(), valueAsString);
                }
            } else {
                final String shortFormat = shortFormat(siteView);
                output.info(shortFormat);
                if (null != saveAs) {
                    Files.writeString(saveAs.toPath(), shortFormat);
                }
            }
        } catch (IOException e) {
            output.error("Error occurred transforming the response: ", e.getMessage());
        }

        return CommandLine.ExitCode.OK;
    }

}
