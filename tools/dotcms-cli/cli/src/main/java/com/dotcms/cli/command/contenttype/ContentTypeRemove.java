package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.model.ResponseEntityView;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypeRemove.NAME,
        header = "@|bold,blue Use this command to remove Content-types.|@",
        description = {
                " Remove a Content-type from a given CT name or Id.",
                "" // Empty line left on purpose to make some room
        },
        sortOptions = false
)
public class ContentTypeRemove extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "remove";

    @CommandLine.Parameters(index = "0", arity = "1", description = "Name Or Id.")
    String idOrVar;

    /**
     *
     * @return
     */
    @Override
    public Integer call() {
        if (output.isCliTest() || isDeleteConfirmed(idOrVar)) {
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

    private boolean isDeleteConfirmed(final String idOrVar) {
        final String confirmation = String.format("%nPlease confirm that you want to remove content-type identified by [%s] ? [y/n]: ", idOrVar);
        return BooleanUtils.toBoolean(
                System.console().readLine(confirmation));
    }

}
