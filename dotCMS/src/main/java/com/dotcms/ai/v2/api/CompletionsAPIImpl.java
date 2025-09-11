package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.aiservices.RAGCompletionsService;
import com.dotcms.ai.v2.api.embeddings.DotPgVectorEmbeddingStore;
import com.dotcms.ai.v2.api.embeddings.retrieval.EmbeddingStoreRetriever;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievalQuery;
import com.dotcms.ai.v2.api.embeddings.retrieval.RetrievedChunk;
import com.dotcms.ai.v2.api.embeddings.retrieval.Retriever;
import com.dotcms.ai.v2.api.provider.ModelProviderFactory;
import com.dotcms.ai.v2.api.util.PromptComposer;
import com.dotmarketing.util.Logger;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service façade implementing the CompletionsAPI by delegating to an orchestrator.
 * Plug your ModelProviderFactory and Retriever here.
 * @author jsanca
 */
@ApplicationScoped
public class CompletionsAPIImpl implements CompletionsAPI {

    private final ModelProviderFactory modelProviderFactory;

    @Inject
    public CompletionsAPIImpl(final ModelProviderFactory modelProviderFactory) {
        this.modelProviderFactory = modelProviderFactory;
    }

    @Override
    public CompletionResponse complete(final CompletionRequest request) {

        Logger.debug(this, ()-> "Doing a complete on completions api, request: " + request);
        final CompletionRequest normalizedCompletionRequest = CompletionNormalizer.normalize(request);

        final String providerKey = normalizedCompletionRequest.getModelProviderKey(); // todo implement this
        // 1) Retrieval (optional)
        final List<RetrievedChunk> retrievedChunks = retrieveIfRequested(normalizedCompletionRequest);

        // 2) Compose prompt
        // todo: see if we can do this on langchain
        final String system = PromptComposer.systemForLanguage(request.getLanguage(), null); // todo: retrieve this from the config
        final String context = PromptComposer.contextBlock(retrievedChunks); // todo: we need a default format (the one here is good) and based on the content type see if there is a velocity to transform it
        final String user = PromptComposer.userBlock(request.getPrompt(), request.getResponseFormat()); // this one is mostly ok but we have to remove the metadata format

        // 3) Resolve model (sync) and generate
        final ChatModel model = this.modelProviderFactory.get(
                providerKey, request.getModelConfig() // this has tempeture and max tokens

        );

        /*ChatModel model = modelFactory.chatModel(
                completionRequest.model,
                ModelParams.builder()
                        .temperature(completionRequest.temperature)
                        .maxOutputTokens(completionRequest.maxTokens)
                        .build()
        );*/

        final RAGCompletionsService ragService = AiServices.builder(RAGCompletionsService.class)
                .chatModel(model)
                .build();

        //final String AiMessage message =  ragService.complete(system, context, user);

        // todo: I have to see what the composer does, but I think the ChatMessage is the key, need to check the examples
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatResponse chatResponse = model.chat(messages); //generate(finalPrompt);
        final String text = chatResponse.aiMessage().text(); // todo: return here the whole

        return CompletionResponse.of(text);
    }

    private static SimilarityOperator mapOperator(final String op) {
        final String s = (op == null) ? "cosine" : op.trim().toLowerCase();
        switch (s) {
            case "innerproduct": return SimilarityOperator.INNER_PRODUCT;
            case "distance":     return SimilarityOperator.EUCLIDEAN;
            default:             return SimilarityOperator.COSINE;
        }
    }


    private List<RetrievedChunk> retrieveIfRequested(final CompletionRequest req) {

        final EmbeddingModel embeddingModel = this.modelProviderFactory.getEmbedding(req.getEmbeddinModelProviderKey(), req.getModelConfig());
        final EmbeddingStore<TextSegment> store = DotPgVectorEmbeddingStore.builder()
                .indexName(req.getIndexName()!=null?req.getIndexName():"default")
                .dimension(embeddingModel.dimension())
                .operator(mapOperator(req.getOperator()))  // o INNER_PRODUCT/EUCLIDEAN
                .build();

        final Retriever retriever = EmbeddingStoreRetriever.builder()
                .store(store)
                .embeddingModel(embeddingModel)
                .defaultLimit(8) // check all these values
                .maxLimit(64)
                .overfetchFactor(3)
                .defaultThreshold(0.75)
                .build();


        RetrievalQuery.Builder retrievalQueryBuilder = RetrievalQuery.builder();
        retrievalQueryBuilder.prompt(req.getPrompt());//  "Latest pet accessories for dogs";
        retrievalQueryBuilder.site(req.getSite()); // "www.mysite.com";
        retrievalQueryBuilder.contentTypes(req.getContentType()); // = new String[]{"Product", "Blog"};
        retrievalQueryBuilder.languageId(req.getLanguage().toString()); // = "1";
        //retrievalQueryBuilder.identifier() = null;
        retrievalQueryBuilder.limit(req.getSearchLimit());// = 5;
        retrievalQueryBuilder.offset(req.getSearchOffset()); // = 0;
        final boolean useRag = (req.getSearchOffset() != null && req.getSearchOffset() > 0)
                || (req.getContentType() != null && req.getContentType().size() > 0)
                //|| (req.fieldVar != null && !req.fieldVar.trim().isEmpty())
                || (req.getSite() != null && !req.getSite().trim().isEmpty());

        if (!useRag) {
            return Collections.emptyList();
        }

        final List<RetrievedChunk> chunks = retriever.search(retrievalQueryBuilder.build());

        return chunks;
    }

    @Override
    public CompletionResponse summarize(final SummarizeRequest request) {

        Logger.debug(this, ()-> "Doing a summarize on completions api, request: " + request);
        final SummarizeRequest normalizedCompletionRequest = CompletionNormalizer.normalize(request);
        final String text = null; //orchestrator.summarize(normalizedCompletionRequest, request.getStyle(), request.getMaxChars());
        return CompletionResponse.of(text);
    }

    @Override
    public void completeStream(final CompletionRequest request, final OutputStream output) {

        Logger.debug(this, ()-> "Doing a completeStream on completions api, request: " + request);
        final CompletionRequest normalizedCompletionRequest = CompletionNormalizer.normalize(request);
        //this.orchestrator.completeStream(normalizedCompletionRequest, output);
    }
}
