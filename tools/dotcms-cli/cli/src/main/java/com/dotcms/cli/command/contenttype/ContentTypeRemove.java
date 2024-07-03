package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.ResponseEntityView;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypeRemove.NAME,
        aliases = ContentTypeRemove.ALIAS,
        header = "@|bold,blue Use this command to remove Content-types.|@",
        description = {
                " Remove a Content-type from a given CT name or Id.",
                "" // Empty line left on purpose to make some room
        },
        sortOptions = false
)
public class ContentTypeRemove extends AbstractContentTypeCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "remove";
    static final String ALIAS = "rm";

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Name Or Id.")
    String idOrVar;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    Prompt prompt;

    /**
     *
     * @return
     */
    @Override
    public Integer call() {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        if (output.isCliTest() || isDeleteConfirmed()) {
            final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
            final ResponseEntityView<String> responseEntityView = contentTypeAPI.delete(idOrVar);
            final String entity = responseEntityView.entity();
            output.info(entity);
            return ExitCode.OK;
        } else {
            output.info("Delete cancelled");
            return ExitCode.SOFTWARE;
        }
    }

    private boolean isDeleteConfirmed() {
       if(interactiveOption.isInteractive()) {
           return prompt.yesOrNo(false, "Are you sure you want to continue ");
       }
       return true;
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
