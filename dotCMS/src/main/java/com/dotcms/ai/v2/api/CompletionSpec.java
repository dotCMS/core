package com.dotcms.ai.v2.api;

import java.util.List;
import java.util.Map;

/**
 * Just a common interface for the completion request classes
 * @author jsanca
 */
public interface CompletionSpec {

    String getPrompt();

    String getSite();
    List<String> getContentType();
    String getFieldVar();
    Integer getSearchLimit();
    Integer getSearchOffset();
    String getOperator();
    Double getThreshold();

    String getModel();
    Float getTemperature();
    Integer getMaxTokens();
    Boolean getStream();

    Long getLanguage();
    Map<String, Object> getResponseFormat();

    String getEmbeddinModelProviderKey();
}
