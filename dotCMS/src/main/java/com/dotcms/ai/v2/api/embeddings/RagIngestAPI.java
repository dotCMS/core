package com.dotcms.ai.v2.api.embeddings;

public interface RagIngestAPI {
    int indexOne(SingleRagIndexRequest ragIndexRequest) throws Exception;

    int indexContentType(ContentTypeRagIndexRequest contentTypeRagIndexRequest);
}
