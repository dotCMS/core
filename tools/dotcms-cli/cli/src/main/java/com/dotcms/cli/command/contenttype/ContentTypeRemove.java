package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.util.concurrent.Callable;

@ActivateRequestContext
@CommandLine.Command(
        name = ContentTypeRemove.NAME,
        header = "@|bold,green Use this command to remove Content-types.|@",
        description = "@|bold,green Remove a Content-type from a given CT name or Id.|@.",
        sortOptions = false
)
public class ContentTypeRemove extends AbstractContentTypeCommand implements Callable<Integer> {

    static final String NAME = "remove";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Name Or Id.")
    String idOrVar;

    /**
     *
     * @return
     */
    @Override
    public Integer call() {
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final ResponseEntityView<String> responseEntityView = contentTypeAPI.delete(idOrVar);
        final String entity = responseEntityView.entity();
        output.info(entity);
        return ExitCode.OK;
    }
}
