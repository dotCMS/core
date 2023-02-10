package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

public class ContentTypePull implements Callable<Integer> {

    static final String NAME = "content-type-pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Option(names = {"-pl","--pull"}, order = 2, description = "Pull Content-type by id or var-name", required = true)
    String idOrVar;

    @CommandLine.Option(names = {"-ln", "--lang"}, order = 3, description = "Content-type Language.", defaultValue = "1")
    Long lang;

    @CommandLine.Option(names = {"-l", "--live"}, order = 4, description = "live content if omitted then working will be used.", defaultValue = "true")
    Boolean live;

    @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save to.")
    File saveAs;

    @Override
    public Integer call() {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
            try {
                final ResponseEntityView<ContentType> responseEntityView = contentTypeAPI.getContentType(idOrVar, lang, live);
                final ContentType contentType = responseEntityView.entity();
                final ObjectMapper objectMapper = output.objectMapper();
                final String asString = objectMapper.writeValueAsString(contentType);
                output.info(asString);
                if(null != saveAs){
                    Files.write( saveAs.toPath(), asString.getBytes());
                }

            } catch (IOException | NotFoundException e) {
                output.error(String.format(
                        "Error occurred while pulling ContentType: [%s] with message: [%s].",
                        idOrVar, e.getMessage()));
                return CommandLine.ExitCode.SOFTWARE;
            }


        return CommandLine.ExitCode.USAGE;
    }

}
