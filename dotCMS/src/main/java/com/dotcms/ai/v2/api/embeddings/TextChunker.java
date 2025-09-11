package com.dotcms.ai.v2.api.embeddings;

import java.util.List;

/** Segments long text into chunks suitable for embedding.
 * @author jsanca
 **/
public interface TextChunker {

    List<String> chunk(String text);

    /** Segments long text into chunks suitable for embedding. */

}
