package com.dotcms.ai.service;

import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotmarketing.util.json.JSONObject;

public interface OpenAIImageService {

    JSONObject sendTextPrompt(String prompt);

    JSONObject sendRawRequest(String prompt);

    JSONObject sendRequest(JSONObject jsonObject);

    JSONObject sendRequest(AIImageRequestDTO dto);
}
