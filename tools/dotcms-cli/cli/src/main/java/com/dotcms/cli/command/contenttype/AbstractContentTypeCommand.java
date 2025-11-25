package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import jakarta.ws.rs.NotFoundException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import picocli.CommandLine;

public abstract class AbstractContentTypeCommand {

    @Inject
    protected RestClientFactory clientFactory;

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected AuthenticationMixin authenticationMixin;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOption;

    Optional<ContentType> findExistingContentType(final ContentTypeAPI contentTypeAPI, final String varNameOrId ){
        try {
            final ResponseEntityView<ContentType> found = contentTypeAPI.getContentType(varNameOrId, null, false);
            return Optional.of(found.entity());
        }catch ( NotFoundException e){
           output.warn("Content-type not found: " + varNameOrId);
        }
        return Optional.empty();
    }

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String shortFormat(final ContentType contentType) {
        return String.format(
                "variable: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] host: [@|bold,underline,green %s|@] modDate:[@|bold,yellow %s|@] description: [@|bold,yellow %s|@]",
                contentType.variable(),
                contentType.id(),
                contentType.host(),
                contentType.modDate() != null ? simpleDateFormat.format(contentType.modDate()): "n/a" ,
                StringUtils.isNotEmpty(contentType.description()) ? StringUtils.abbreviate(
                        contentType.description(), 50) : "n/a"
        );
    }

}
