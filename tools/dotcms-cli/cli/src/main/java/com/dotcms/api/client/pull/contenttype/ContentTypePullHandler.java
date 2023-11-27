package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.contenttype.model.type.ContentType;
import java.text.SimpleDateFormat;
import javax.enterprise.context.Dependent;
import org.apache.commons.lang3.StringUtils;

@Dependent
public class ContentTypePullHandler implements PullHandler<ContentType> {

    @Override
    public String title() {
        return "Content Types";
    }

    @Override
    public String displayName(final ContentType contentType) {
        return contentType.name();
    }

    @Override
    public String fileName(final ContentType contentType) {
        return contentType.variable();
    }

    @Override
    public String shortFormat(final ContentType contentType) {

        final var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return String.format(
                "varName: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] host: [@|bold,underline,green %s|@] modDate:[@|bold,yellow %s|@] desc: [@|bold,yellow %s|@]",
                contentType.variable(),
                contentType.id(),
                contentType.host(),
                contentType.modDate() != null ?
                        simpleDateFormat.format(contentType.modDate()) : "n/a",
                StringUtils.isNotEmpty(contentType.description()) ?
                        StringUtils.abbreviate(contentType.description(), 50) : "n/a"
        );
    }

}
