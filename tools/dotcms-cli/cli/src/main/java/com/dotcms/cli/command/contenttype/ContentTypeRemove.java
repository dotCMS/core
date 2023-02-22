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
        header = "@|bold,green Use his command to remove Content-types.|@",
        description = "@|bold,green Remove a Content-type from a given file definition.|@ @|bold,cyan --file|@ to send the descriptor. ",
        sortOptions = false
)
public class ContentTypeRemove extends ContentTypeCommand implements Callable<Integer> {


    static final String NAME = "content-type-remove";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = {"-iv","--idOrVar"}, order = 1, description = "Pull Content-type by id or var-name", required = true)
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

        //We're not supposed to get this far unless our params are messed up
        return ExitCode.OK;
    }
}
