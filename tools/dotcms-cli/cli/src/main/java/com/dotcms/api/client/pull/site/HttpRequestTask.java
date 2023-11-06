package com.dotcms.api.client.pull.site;

import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Represents a task that performs HTTP requests concurrently using the Fork/Join framework. It
 * extends the RecursiveTask class and returns a list of SiteView objects.
 */
public class HttpRequestTask extends RecursiveTask<List<SiteView>> {

    private SiteFetcher siteFetcher;

    private List<Site> sites;

    private static final int THRESHOLD = 10;

    public HttpRequestTask(final List<Site> sites, final SiteFetcher siteFetcher) {
        this.sites = sites;
        this.siteFetcher = siteFetcher;
    }

    @Override
    protected List<SiteView> compute() {

        if (sites.size() <= THRESHOLD) {

            // If the list is small enough, process sequentially
            List<SiteView> siteViews = new ArrayList<>();
            for (Site site : sites) {
                siteViews.add(siteFetcher.fetchByKey(site.hostName(), null));
            }

            return siteViews;

        } else {

            // If the list is large, split it into two smaller tasks
            int mid = sites.size() / 2;
            HttpRequestTask task1 = new HttpRequestTask(sites.subList(0, mid), siteFetcher);
            HttpRequestTask task2 = new HttpRequestTask(sites.subList(mid, sites.size()),
                    siteFetcher);

            // Start the first subtask in a new thread
            task1.fork();

            // Start the second subtask and wait for it to finish
            List<SiteView> task2Result = task2.compute();

            // Wait for the first subtask to finish and combine the results
            List<SiteView> task1Result = task1.join();
            task1Result.addAll(task2Result);

            return task1Result;
        }
    }

}
