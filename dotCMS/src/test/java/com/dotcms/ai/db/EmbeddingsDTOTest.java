package com.dotcms.ai.db;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that building an {@link EmbeddingsDTO} with an explicit threshold never reads the
 * dotAI app configuration. Every row returned by a similarity search is built this way, and
 * resolving the configured default per row rebuilt the whole app configuration (decrypting the
 * app secrets and parsing the provider config) once per row, only to discard it.
 *
 * <p>This runs without a dotCMS instance, so any read of the app configuration fails the test.</p>
 *
 * @author hassandotcms
 */
public class EmbeddingsDTOTest {

    /**
     * A builder given an explicit threshold builds without reading the app configuration and keeps
     * the threshold it was given.
     */
    @Test
    public void test_build_withExplicitThreshold_doesNotReadAppConfig() {
        final EmbeddingsDTO dto = new EmbeddingsDTO.Builder()
                .withInode("inode-1")
                .withIndexName("default")
                .withThreshold(0.12f)
                .build();

        assertEquals(0.12f, dto.threshold, 0f);
    }

}
