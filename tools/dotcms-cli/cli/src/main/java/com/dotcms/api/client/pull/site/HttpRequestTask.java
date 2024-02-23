package com.dotcms.api.client.pull.site;

import com.dotcms.api.client.task.TaskProcessor;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
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
public class HttpRequestTask extends TaskProcessor<List<Site>,List<SiteView>> {

    private final SiteFetcher siteFetcher;

    private List<Site> sites;

    private final ManagedExecutor executor;
    
    public HttpRequestTask(final SiteFetcher siteFetcher,
            final ManagedExecutor executor) {
        this.siteFetcher = siteFetcher;
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
     * @param sites List of Site objects to process.
     */
    public void setTaskParams(final List<Site> sites) {
        this.sites = sites;
    }

    /**
     * Processes a list of Site objects, either sequentially or in parallel, depending on the list
     * size. If the size of the list is under a predefined threshold, items are processed
     * individually in order. For larger lists, the work is divided into separate concurrent tasks,
     * which are processed in parallel.
     * <p>
     * Each Site object in the list is processed by making a request to fetch its full version.
     *
     * @return A List of fully fetched SiteView objects.
     */
    public List<SiteView> compute() {

        CompletionService<List<SiteView>> completionService =
                new ExecutorCompletionService<>(executor);

        if (sites.size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            List<SiteView> siteViews = new ArrayList<>();
            for (Site site : sites) {
                siteViews.add(siteFetcher.fetchByKey(site.hostName(), false,null));
            }

            return siteViews;

        } else {

            // If the list is large, split it into smaller tasks
            int toProcessCount = splitTasks(sites, completionService);

            // Wait for all tasks to complete and gather the results
            final var foundSites = new ArrayList<SiteView>();
            Function<List<SiteView>, Void> processFunction = taskResult -> {
                foundSites.addAll(taskResult);
                return null;
            };
            processTasks(toProcessCount, completionService, processFunction);
            return foundSites;
        }
    }

    /**
     * Splits a list of Site objects into separate tasks.
     *
     * @param sites             List of Site objects to process.
     * @param completionService The CompletionService to submit tasks to.
     * @return The number of tasks to process.
     */
    private int splitTasks(List<Site> sites,
            CompletionService<List<SiteView>> completionService) {

        int mid = sites.size() / 2;
        var subList1 = sites.subList(0, mid);
        var subList2 = sites.subList(mid, sites.size());

        var task1 = new HttpRequestTask(siteFetcher, executor);
        task1.setTaskParams(subList1);

        var task2 = new HttpRequestTask(siteFetcher, executor);
        task2.setTaskParams(subList2);

        completionService.submit(task1::compute);
        completionService.submit(task2::compute);

        return 2;
    }

}
