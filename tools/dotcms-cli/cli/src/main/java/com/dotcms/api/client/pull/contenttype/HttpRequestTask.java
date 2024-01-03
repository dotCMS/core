package com.dotcms.api.client.pull.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Represents a task that performs HTTP requests concurrently using the Fork/Join framework. It
 * extends the RecursiveTask class and returns a list of ContentType objects.
 */
public class HttpRequestTask extends RecursiveTask<List<ContentType>> {

    private final ContentTypeFetcher contentTypeFetcher;

    private final transient List<ContentType> contentTypes;

    private static final int THRESHOLD = 10;

    public HttpRequestTask(final List<ContentType> contentTypes,
            final ContentTypeFetcher contentTypeFetcher) {
        this.contentTypes = contentTypes;
        this.contentTypeFetcher = contentTypeFetcher;
    }

    @Override
    protected List<ContentType> compute() {

        if (contentTypes.size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            List<ContentType> contentTypeViews = new ArrayList<>();
            for (ContentType contentType : contentTypes) {
                contentTypeViews.add(
                        contentTypeFetcher.fetchByKey(contentType.variable(), null)
                );
            }

            return contentTypeViews;

        } else {

            // If the list is large, split it into two smaller tasks
            int mid = contentTypes.size() / 2;
            HttpRequestTask task1 = new HttpRequestTask(
                    contentTypes.subList(0, mid),
                    contentTypeFetcher
            );
            HttpRequestTask task2 = new HttpRequestTask(
                    contentTypes.subList(mid, contentTypes.size()),
                    contentTypeFetcher
            );

            // Start the first subtask in a new thread
            task1.fork();

            // Start the second subtask and wait for it to finish
            List<ContentType> task2Result = task2.compute();

            // Wait for the first subtask to finish and combine the results
            List<ContentType> task1Result = task1.join();
            task1Result.addAll(task2Result);

            return task1Result;
        }
    }

}
