package com.dotcms.ai.api;

import com.dotcms.ai.api.embeddings.EmbeddingIndexRequest;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiModelConfigFactory;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
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
    private final AiModelConfigFactory modelConfigFactory = CDIUtils.getBeanThrows(AiModelConfigFactory.class);

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

        final Host site = Try.of(()->APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).getOrElse(APILocator.systemHost());
        final Optional<AiModelConfig> modelConfigOpt = this.modelConfigFactory.getAiModelConfigOrDefaultChat(site, embeddingsForm.model);

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

                if(modelConfigOpt.isPresent()) {

                    EmbeddingsForm finalForm = embeddingsForm;
                    if (StringUtils.isNotSet(embeddingsForm.model)) {
                        // probably get the default one
                        finalForm = EmbeddingsForm.copy(embeddingsForm).model(modelConfigOpt.get().getName()).build();
                    }

                    Logger.debug(this, "Using new AI api for the embeddings with the form: " + finalForm);
                    final EmbeddingIndexRequest embeddingIndexRequest = toEmbeddingIndexRequest(finalForm,
                            modelConfigOpt.get(), contentlet);
                    APILocator.getDotAIAPI()
                            .getEmbeddingsAPI().indexOne(embeddingIndexRequest);
                } else {

                    // if a velocity template is passed in, use it.  Otherwise, try the fields
                    if (!APILocator.getDotAIAPI().getEmbeddingsAPI().generateEmbeddingsForContent(
                            contentlet,
                            embeddingsForm.velocityTemplate,
                            embeddingsForm.indexName)) {
                        APILocator.getDotAIAPI().getEmbeddingsAPI().generateEmbeddingsForContent(contentlet, fields, embeddingsForm.indexName);
                    }
                }
            } catch (Exception e) {
                Logger.warn(this.getClass(), "unable to embed content:" + inode + " error:" + e.getMessage(), e);
            }
        }
    }

    private EmbeddingIndexRequest toEmbeddingIndexRequest(final EmbeddingsForm finalForm,
                                                          final AiModelConfig aiModelConfig,
                                                          final Contentlet contentlet) {
        return EmbeddingIndexRequest.builder()
                .withVelocityTemplate(finalForm.velocityTemplate)
                .withUserId(finalForm.userId)
                .withIdentifier(contentlet.getIdentifier())
                .withLanguageId(contentlet.getLanguageId())
                .withVendorModelPath(finalForm.model)
                .withIndexName(finalForm.indexName)
                .withModelConfig(aiModelConfig)
                .build();
    }

}
