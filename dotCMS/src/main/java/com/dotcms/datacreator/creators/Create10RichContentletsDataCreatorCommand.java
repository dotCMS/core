package com.dotcms.datacreator.creators;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datacreator.DataCreatorCommand;
import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.business.APILocator;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

public class Create10RichContentletsDataCreatorCommand implements DataCreatorCommand {

    @Override
    public void execute(final Map<String, Object> contextMap) {

        final Map<String, Object> identifierMap = new HashMap<>();
        final ContentType richTextType = Try.of(()->
                APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent")).getOrNull();
        if (null != richTextType) {
            for (int i = 0; i < 10; ++i) {

                identifierMap.put("contentlet" + i, new ContentletDataGen(richTextType)
                        .setProperty("title", "title"+i).setProperty("body", "body"+i).nextPersisted().getIdentifier());
            }
        }

        contextMap.put(this.getName(), identifierMap);
    }
}
