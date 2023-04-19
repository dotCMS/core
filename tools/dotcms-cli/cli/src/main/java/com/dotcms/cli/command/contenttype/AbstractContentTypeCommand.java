package com.dotcms.cli.command.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.NotFoundException;
import java.text.SimpleDateFormat;
import java.util.Optional;

public abstract class AbstractContentTypeCommand {

    Optional<ContentType> findExistingContentType(final ContentTypeAPI contentTypeAPI, final String varNameOrId ){
        try {
            final ResponseEntityView<ContentType> found = contentTypeAPI.getContentType(varNameOrId, null, false);
            return Optional.of(found.entity());
        }catch ( NotFoundException e){
            //Not relevant
        }
        return Optional.empty();
    }

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String shortFormat(final ContentType contentType) {
        return String.format(
                "varName: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] host: [@|bold,underline,green %s|@] modDate:[@|bold,yellow %s|@] desc: [@|bold,yellow %s|@]",
                contentType.variable(),
                contentType.id(),
                contentType.host(),
                contentType.modDate() != null ? simpleDateFormat.format(contentType.modDate()): "n/a" ,
                StringUtils.isNotEmpty(contentType.description()) ? StringUtils.abbreviate(
                        contentType.description(), 50) : "n/a"
        );
    }

}
