package com.dotcms.cli.command;

import com.dotcms.api.contenttype.ContentTypeService;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypeCommand.NAME,
        header = "@|bold,green Prints a list of all content types existing in a given site.|@ ",
        description = {

        }
)
public class ContentTypeCommand implements Callable<Integer> {

    private static final Logger logger = Logger.getLogger(ContentTypeCommand.class);

    static final String NAME = "contentTypes";

    @Inject
    ContentTypeService contentTypeService;

    //setting exclusive=false on the group makes the options mutually dependent
    //setting multiplicity = "1" on the group makes the group mandatory
    //both options are non-required in the group (this is the default, you can also explicitly say required = false)
    @CommandLine.ArgGroup(multiplicity = "1", exclusive = false)
    RequireOneOrBothNotNoneGroup options;

    static class RequireOneOrBothNotNoneGroup {

        @CommandLine.Option(names = {"-l", "--list"}, description = "Prints out a list of available params.")
        Boolean listContentTypes;
    }

    @Override
    public Integer call() {

        if (Boolean.TRUE.equals(options.listContentTypes)) {
            logger.info(contentTypeService.getContentTypes());
        }
        return ExitCode.OK;
    }
}
