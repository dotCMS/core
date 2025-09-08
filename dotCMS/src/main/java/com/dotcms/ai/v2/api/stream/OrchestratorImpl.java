package com.dotcms.ai.v2.api.stream;

import com.dotcms.ai.v2.api.CompletionRequest;
import com.dotcms.ai.v2.api.embeddings.RetrievedChunk;
import com.dotcms.ai.v2.api.embeddings.Retriever;
import com.dotcms.ai.v2.api.provider.ModelProviderFactory;
import com.dotcms.ai.v2.api.util.PromptComposer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

import javax.enterprise.context.ApplicationScoped;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Default orchestrator implementation for Completions.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Normalize request defaults</li>
 *   <li>Resolve model (sync/stream) from ModelProviderFactory</li>
 *   <li>Optionally retrieve context (RAG) via Retriever</li>
 *   <li>Compose system/context/user prompt blocks</li>
 *   <li>Call LangChain4j model and return/stream results</li>
 * </ul>
 * @author jsanca
 */
@ApplicationScoped
public class OrchestratorImpl implements Orchestrator {

    private final ModelProviderFactory modelFactory;
    private final Retriever retriever;

    @Inject
    public OrchestratorImpl(final ModelProviderFactory modelFactory, final Retriever retriever) {
        this.modelFactory = modelFactory;
        this.retriever = retriever;
    }

    @Override
    public String complete(final CompletionRequest completionRequest) {


        final String providerKey = completionRequest.getModelProviderKey();
        // 1) Retrieval (optional)
        final List<RetrievedChunk> ctx = retrieveIfRequested(completionRequest);

        // 2) Compose prompt
        // todo: see if we can do this on langchain
        final String system = PromptComposer.systemForLanguage(completionRequest.getLanguage(), null);
        final String context = PromptComposer.contextBlock(ctx);
        final String user = PromptComposer.userBlock(completionRequest.getPrompt(), completionRequest.getResponseFormat());
        final String finalPrompt = PromptComposer.combine(system, context, user);

        // 3) Resolve model (sync) and generate
        final ChatModel model = this.modelFactory.get(
                providerKey, completionRequest.getModelConfig() // this has tempeture and max tokens

        );
        /*ChatModel model = modelFactory.chatModel(
                completionRequest.model,
                ModelParams.builder()
                        .temperature(completionRequest.temperature)
                        .maxOutputTokens(completionRequest.maxTokens)
                        .build()
        );*/

        // todo: I have to see what the composer does, but I think the ChatMessage is the key, need to check the examples
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatResponse chatResponse = model.chat(messages); //generate(finalPrompt);
        return chatResponse.aiMessage().text(); // todo: return here the whole
    }

    @Override
    public void completeStream(CompletionRequest request, OutputStream out) {

        List<RetrievedChunk> ctx = retrieveIfRequested(request);

        String system = PromptComposer.systemForLanguage(request.getLanguage(), null);
        String context = PromptComposer.contextBlock(ctx);
        String user = PromptComposer.userBlock(request.getPrompt(), request.getResponseFormat());
        String finalPrompt = PromptComposer.combine(system, context, user);

        final StreamingChatModel chatModel = this.modelFactory.getStreaming(
                request.getStreamingModelProviderKey(), request.getModelConfig() // this has tempeture and max tokens

        );

        /*StreamingChatLanguageModel streaming = modelFactory.streamingChatModel(
                req.model,
                ModelParams.builder()
                        .temperature(req.temperature)
                        .maxOutputTokens(req.maxTokens)
                        .build()
        );*/

        StreamingResponseHandler<String> handler = new StreamingResponseHandler<String>() {
            @Override public void onNext(String token) {
                writeSilently(out, token);
            }
            @Override public void onError(Throwable error) {
                writeSilently(out, "\n[STREAM ERROR] " + error.getMessage());
            }
            @Override public void onComplete(Response<String> response) {
                // Optionally emit a terminator/newline
                writeSilently(out, "");
            }
        };
        streaming.generate(finalPrompt, handler);
    }

    @Override
    public String summarize(CompletionRequest request, String style, Integer maxChars) {
        CompletionRequest req = CompletionNormalizer.normalize(request);

        List<RetrievedChunk> ctx = retrieveIfRequested(req);

        String system = PromptComposer.systemForLanguage(req.language, style);
        String context = PromptComposer.contextBlock(ctx);
        String user = PromptComposer.summarizeUserBlock(req.prompt, style, maxChars, req.responseFormat);
        String finalPrompt = PromptComposer.combine(system, context, user);

        ChatLanguageModel model = modelFactory.chatModel(
                req.model,
                ModelParams.builder()
                        .temperature(req.temperature)
                        .maxOutputTokens(req.maxTokens)
                        .build()
        );

        Response<String> resp = model.generate(finalPrompt);
        String text = resp.content();
        if (maxChars != null && maxChars > 0 && text != null && text.length() > maxChars) {
            text = text.substring(0, Math.max(0, maxChars - 3)) + "...";
        }
        return text;
    }

    // todo: this may be the embeddings abstraction
    private List<RetrievedChunk> retrieveIfRequested(CompletionRequest req) {
        boolean useRag = (req.searchLimit != null && req.searchLimit > 0)
                || (req.contentType != null && req.contentType.length > 0)
                || (req.fieldVar != null && !req.fieldVar.trim().isEmpty())
                || (req.site != null && !req.site.trim().isEmpty());

        if (!useRag) return Collections.emptyList();

        RetrievalQuery q = new RetrievalQuery();
        q.site = req.site;
        q.contentTypes = req.contentType;
        q.languageId = req.language == null ? null : String.valueOf(req.language);
        q.fieldVar = req.fieldVar;
        q.prompt = req.prompt;
        q.limit = req.searchLimit;
        q.offset = req.searchOffset;
        q.threshold = req.threshold == null ? 0.75 : req.threshold;
        q.operator = mapOperator(req.operator);
        // q.userId = ... // add if you enforce permission filtering

        return retriever.search(q);
    }

    private static SimilarityOperator mapOperator(String op) {
        String s = (op == null) ? "cosine" : op.trim().toLowerCase();
        switch (s) {
            case "innerproduct": return SimilarityOperator.INNER_PRODUCT;
            case "distance":     return SimilarityOperator.EUCLIDEAN;
            default:             return SimilarityOperator.COSINE;
        }
    }

    private static void writeSilently(OutputStream out, String chunk) {
        try {
            if (chunk != null && !chunk.isEmpty()) {
                out.write(chunk.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException ignored) { }
    }
}
