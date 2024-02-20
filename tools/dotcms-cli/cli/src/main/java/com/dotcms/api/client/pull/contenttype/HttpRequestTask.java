package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.contenttype.model.type.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.function.Function;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.context.ManagedExecutor;

/**
 * Represents a task that performs HTTP requests concurrently.
 */
@Dependent
public class HttpRequestTask extends TaskProcessor {

    private final ContentTypeFetcher contentTypeFetcher;

    private List<ContentType> contentTypes;

    private final ManagedExecutor executor;

    public HttpRequestTask(final ContentTypeFetcher contentTypeFetcher,
            final ManagedExecutor executor) {
        this.contentTypeFetcher = contentTypeFetcher;
        this.executor = executor;
    }

    /**
     * Sets the parameters for the HttpRequestTask. This method provides a way to inject necessary
     * configuration after the instance of HttpRequestTask has been created by the container, which
     * is a common pattern when working with frameworks like Quarkus that manage object creation and
     * dependency injection in a specific manner.
     * <p>
     * This method is used as an alternative to constructor injection, which is not feasible due to
     * the limitations or constraints of the framework's dependency injection mechanism. It allows
     * for the explicit setting of traversal parameters after the object's instantiation, ensuring
     * that the executor is properly configured before use.
     *
     * @param contentTypes List of ContentType objects to process.
     */
    public void setTaskParams(final List<ContentType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    /**
     * Processes a list of ContentType objects, either sequantially or in parallel, depending on the
     * list size. If the size of the list is under a predefined threshold, items are processed
     * individually in order. For larger lists, the work is divided into separate concurrent tasks,
     * which are processed in parallel.
     * <p>
     * Each ContentType object in the list is processed by making a request to fetch its full
     * version.
     *
     * @return A List of fully fetched ContentType objects.
     */
    public List<ContentType> compute() {

        CompletionService<List<ContentType>> completionService =
                new ExecutorCompletionService<>(executor);

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

            // If the list is large, split it into smaller tasks
            int toProcessCount = splitTasks(contentTypes, completionService);

            // Wait for all tasks to complete and gather the results
            final var foundContentTypes = new ArrayList<ContentType>();
            Function<List<ContentType>, Void> processFunction = taskResult -> {
                foundContentTypes.addAll(taskResult);
                return null;
            };
            processTasks(toProcessCount, completionService, processFunction);
            return foundContentTypes;
        }
    }

    /**
     * Splits a list of ContentType objects into separate tasks.
     *
     * @param contentTypes      List of ContentType objects to process.
     * @param completionService The CompletionService to submit tasks to.
     * @return The number of tasks to process.
     */
    private int splitTasks(final List<ContentType> contentTypes,
            final CompletionService<List<ContentType>> completionService) {

        int mid = contentTypes.size() / 2;
        var subList1 = contentTypes.subList(0, mid);
        var subList2 = contentTypes.subList(mid, contentTypes.size());

        var task1 = new HttpRequestTask(contentTypeFetcher, executor);
        task1.setTaskParams(subList1);

        var task2 = new HttpRequestTask(contentTypeFetcher, executor);
        task2.setTaskParams(subList2);

        completionService.submit(task1::compute);
        completionService.submit(task2::compute);

        return 2;
    }

}
