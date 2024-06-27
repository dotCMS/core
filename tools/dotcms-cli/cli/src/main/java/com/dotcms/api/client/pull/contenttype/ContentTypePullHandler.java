package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.client.pull.GeneralPullHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.pull.PullOptions;
import java.text.SimpleDateFormat;
import java.util.List;
import jakarta.enterprise.context.Dependent;
import org.apache.commons.lang3.StringUtils;

@Dependent
public class ContentTypePullHandler extends GeneralPullHandler<ContentType> {

    @Override
    public String title() {
        return "Content Types";
    }

    @Override
    public String startPullingHeader(final List<ContentType> contents) {

        return String.format("\r@|bold,green [%d]|@ %s to pull",
                contents.size(),
                title()
        );
    }

    @Override
    public String displayName(final ContentType contentType) {
        return contentType.name();
    }

    @Override
    public String fileName(final ContentType contentType) {
        return NamingUtils.contentTypeFileName(contentType);
    }

    @Override
    public String shortFormat(final ContentType contentType, final PullOptions pullOptions) {

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
