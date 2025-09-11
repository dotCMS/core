package com.dotcms.ai.v2.api.embeddings.extractor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Extracts text and metadata from dotCMS contentlets.
 * @author jsanca
 */
public interface ContentExtractor {

    /** Returns null if not found or not eligible. */
    ExtractedContent extract(String identifier, long languageId) throws Exception;

    /**
     * Provides an iterator over extracted content, hiding pagination logic.
     * @param host site/host filter (optional)
     * @param contentType content type filter (optional)
     * @param languageId language filter (optional)
     * @param pageSize batch size per DB/API call (e.g. 50 or 100)
     */
    default Iterator<ExtractedContent> iterator(final String host,
                                                final String contentType,
                                                final Long languageId,
                                                final int pageSize) {
        return new Iterator<>() {

            private int offset = 0;
            private List<ExtractedContent> buffer = Collections.emptyList();
            private int index = 0;
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {

                if (index < buffer.size()) {
                    return true;
                }

                if (exhausted) {
                    return false;
                }

                try {
                    buffer = list(host, contentType, languageId, pageSize, offset);
                    if (buffer.isEmpty()) {
                        exhausted = true;
                        return false;
                    }
                    offset += buffer.size();
                    index = 0;
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to fetch content batch", e);
                }
            }

            @Override
            public ExtractedContent next() {
                if (!hasNext()) throw new NoSuchElementException();
                return buffer.get(index++);
            }
        };
    }

    /** Legacy pagination (still useful internally). */
    List<ExtractedContent> list(String host,
                                String contentType,
                                Long languageId,
                                int limit,
                                int offset) throws Exception;
}

