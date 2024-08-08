package com.dotcms.ai.api;

/**
 * This class is in charge of providing the EmbeddingsAPI.
 * @author jsanca
 */
public interface EmbeddingsAPIProvider {

    EmbeddingsAPI getEmbeddingsAPI(Object... initArguments);
}
