package com.dotcms.api.client.pull.contenttype;

import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.contenttype.model.type.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.enterprise.context.Dependent;
import org.eclipse.microprofile.context.ManagedExecutor;

/**
 * Represents a task that performs HTTP requests concurrently.
 */
@Dependent
public class HttpRequestTask extends
        TaskProcessor<List<ContentType>, CompletableFuture<List<ContentType>>> {

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
    @Override
    public void setTaskParams(final List<ContentType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    /**
     * Processes a list of ContentType objects, either sequentially or in parallel, depending on the
     * list size. If the size of the list is under a predefined threshold, items are processed
     * individually in order. For larger lists, the work is divided into separate concurrent tasks,
     * which are processed in parallel.
     * <p>
     * Each ContentType object in the list is processed by making a request to fetch its full
     * version.
     *
     * @return A List of fully fetched ContentType objects.
     */
    @Override
    public CompletableFuture<List<ContentType>> compute() {

        if (contentTypes.size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            return CompletableFuture.supplyAsync(() -> contentTypes.stream()
                    .map(contentType ->
                            contentTypeFetcher.fetchByKey(contentType.variable(), false, null)
                    ).collect(Collectors.toList()), executor);

        }

        // If the list is large, split it into smaller tasks
        return splitTasks(contentTypes);
    }

    /**
     * Splits a list of ContentType objects into separate tasks.
     *
     * @param contentTypes      List of ContentType objects to process.
     * @return A CompletableFuture representing the combined results of the separate tasks.
     */
    private CompletableFuture<List<ContentType>> splitTasks(final List<ContentType> contentTypes) {

        int mid = contentTypes.size() / 2;
        var subList1 = contentTypes.subList(0, mid);
        var subList2 = contentTypes.subList(mid, contentTypes.size());

        var task1 = new HttpRequestTask(contentTypeFetcher, executor);
        task1.setTaskParams(subList1);
        var futureTask1 = task1.compute();

        var task2 = new HttpRequestTask(contentTypeFetcher, executor);
        task2.setTaskParams(subList2);
        var futureTask2 = task2.compute();

        return futureTask1.thenCombine(futureTask2, (list1, list2) -> {
            var combinedList = new ArrayList<>(list1);
            combinedList.addAll(list2);
            return combinedList;
        });
    }

}
