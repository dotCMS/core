package com.dotcms.ai.v2.api.embeddings.extractor;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class DotCMSContentExtractor implements ContentExtractor {
    @Override
    public ExtractedContent extract(String identifier, long languageId) throws Exception {
        return null;
    }

    @Override
    public List<ExtractedContent> list(String host, String contentType, Long languageId, int limit, int offset) throws Exception {
        return List.of();
    }
}
