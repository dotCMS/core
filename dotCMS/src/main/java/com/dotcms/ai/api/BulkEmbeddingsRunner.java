package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The BulkEmbeddingsRunner class is responsible for generating embeddings for a list of contentlets in a bulk operation.
 * It implements the Runnable interface, allowing it to be used in a multithreaded context.
 * The class takes a list of inodes, representing the contentlets to be processed, and an EmbeddingsForm, which contains the necessary information for the embeddings generation.
 * The embeddings generation is performed in the run() method, which is executed in a separate thread when the class is used in a Thread object.
 */
public class BulkEmbeddingsRunner implements Runnable {

    private final User user;
    private final EmbeddingsForm embeddingsForm;
    private final List<String> inodes;

    public BulkEmbeddingsRunner(List<String> inodes, EmbeddingsForm embeddingsForm) {
        user = Try.of(() -> APILocator
                        .getUserAPI()
                        .loadUserById(embeddingsForm.userId))
                .getOrElse(APILocator.systemUser());
        this.embeddingsForm = embeddingsForm;
        this.inodes = inodes;
    }

    @Override
    public void run() {
        for (final String inode : inodes) {
            try {
                final Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, false);
                if (UtilMethods.isEmpty(contentlet::getContentType) || !contentlet.isLive()) {
                    continue;
                }

                final List<Field> fields = contentlet.getContentType()
                        .fields()
                        .stream()
                        .filter(f -> embeddingsForm.fieldsAsList().contains(f.variable().toLowerCase()))
                        .collect(Collectors.toList());
                // if a velocity template is passed in, use it.  Otherwise, try the fields
                if (!APILocator.getDotAIAPI().getEmbeddingsAPI().generateEmbeddingsForContent(
                        contentlet,
                        embeddingsForm.velocityTemplate,
                        embeddingsForm.indexName)) {
                    APILocator.getDotAIAPI().getEmbeddingsAPI().generateEmbeddingsForContent(contentlet, fields, embeddingsForm.indexName);
                }
            } catch (Exception e) {
                Logger.warn(this.getClass(), "unable to embed content:" + inode + " error:" + e.getMessage(), e);
            }
        }
    }

}
