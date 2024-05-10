package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIModel;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.knuddels.jtokkit.api.Encoding;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmbeddingsTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig app;

    /**
     * $ai.embeddings
     * @param initData
     */
    EmbeddingsTool(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.app = ConfigService.INSTANCE.config(this.host);
    }


    @Override
    public void init(Object initData) {
        /* unneeded because of constructor */
    }

    public List<Float> generateEmbeddings(String prompt) {
        int tokens = countTokens(prompt);
        int maxTokens = OpenAIModel.resolveModel(ConfigService.INSTANCE.config(host).getConfig(AppKeys.EMBEDDINGS_MODEL)).maxTokens;
        if (tokens > maxTokens) {
            Logger.warn(EmbeddingsTool.class, "Prompt is too long.  Maximum prompt size is " + maxTokens + " tokens (roughly ~" + maxTokens * .75 + " words).  Your prompt was " + tokens + " tokens ");
        }
        return EmbeddingsAPI.impl().pullOrGenerateEmbeddings(prompt)._2;
    }

    public int countTokens(String prompt) {
        Optional<Encoding> optionalEncoding = EncodingUtil.registry.getEncodingForModel(app.getModel());
        if (optionalEncoding.isPresent()) {
            return optionalEncoding.get().countTokens(prompt);
        }
        return -1;
    }

    public Map<String, Map<String, Object>> getIndexCount() {
        return EmbeddingsAPI.impl().countEmbeddingsByIndex();
    }


}
