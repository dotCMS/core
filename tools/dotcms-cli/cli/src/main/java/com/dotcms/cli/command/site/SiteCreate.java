package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = SiteCreate.NAME,
        header = "@|bold,blue Quick way to create a Site |@",
        description = {
            " This command is the quickest way to create a site. Simply pass a site name.",
            " The resulting site is created empty with no content.",
            " For a more elaborate way to create a site, use the @|bold,cyan site:pull|@ command.",
            " which takes a detailed descriptor file as input.",
            " This might come handy on certain situations",
            " e.g. you can use the @|bold,cyan site:create|@ command to create an empty site",
            " Then pull it and start working on it.",
            "" // empty line left here on purpose to make room at the end
        }
)
public class SiteCreate extends AbstractSiteCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "create";

    @CommandLine.Parameters(index = "0", arity = "1", description = " Site name. ")
    String siteName;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        if ( null != siteName && !siteName.isEmpty()) {

            final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

            final ResponseEntityView<SiteView> response = siteAPI.create(CreateUpdateSiteRequest.builder().siteName(siteName).build());
            final SiteView siteView = response.entity();
            output.info(String.format("Site @|bold,green [%s]|@ successfully created.",siteName));
            output.info(shortFormat(siteView));
            return CommandLine.ExitCode.OK;
        }

        return CommandLine.ExitCode.USAGE;
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
