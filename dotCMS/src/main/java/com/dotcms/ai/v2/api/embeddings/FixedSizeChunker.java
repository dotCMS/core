package com.dotcms.ai.v2.api.embeddings;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixedSizeChunker implements TextChunker {
    private final int maxChars;     // p.ej. 1200
    private final int overlap;      // p.ej. 200

    public FixedSizeChunker(final int maxChars, final int overlap) {

        this.maxChars = Math.max(300, maxChars);
        this.overlap = Math.max(0, Math.min(this.maxChars/3, overlap));
    }

    @Override
    public List<String> chunk(final String text) {

        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> parts = new ArrayList<>();
        int i = 0;
        final int n = text.length();

        while (i < n) {

            final int end = Math.min(n, i + maxChars);
            parts.add(text.substring(i, end));
            if (end == n) {
                break;
            }
            i = end - overlap;
        }

        return parts;
    }
}
