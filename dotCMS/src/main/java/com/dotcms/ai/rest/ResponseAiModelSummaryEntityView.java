package com.dotcms.ai.rest;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

public class ResponseAiModelSummaryEntityView extends ResponseEntityView<List<AiModelSummaryView>> {
    public ResponseAiModelSummaryEntityView(List<AiModelSummaryView> entity) {
        super(entity);
    }
}
