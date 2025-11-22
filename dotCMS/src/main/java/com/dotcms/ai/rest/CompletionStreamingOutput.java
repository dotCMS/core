package com.dotcms.ai.rest;

import com.dotcms.ai.api.SummarizeRequest;
import com.dotcms.ai.util.LineReadingOutputStream;
import com.dotmarketing.business.APILocator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

public class CompletionStreamingOutput implements StreamingOutput {

    private final SummarizeRequest summarizeRequest;

    public CompletionStreamingOutput(final SummarizeRequest summarizeRequest) {
        this.summarizeRequest = summarizeRequest;
    }

    @Override
    public void write(final OutputStream output) throws IOException, WebApplicationException {

        APILocator.getDotAIAPI().getCompletionsAPI().summarize(summarizeRequest, output);//new LineReadingOutputStream(output));
    }
}
