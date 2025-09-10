package com.dotcms.graphql.datafetcher.page;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * GraphQL data fetcher that retrieves the identifier of a running experiment associated with a page.
 * <p>
 * This fetcher is designed to be used within the Page GraphQL schema to provide information
 * about any active experiment running on the page. It extracts the page contentlet from the
 * environment's source, then uses the ExperimentsAPI to fetch any running experiment for
 * that page.
 * <p>
 * If a running experiment is found for the page, its identifier is returned.
 * If no experiment is running on the page, null is returned.
 */
public class RunningExperimentFetcher implements DataFetcher<String> {

    /**
     * Fetches the identifier of a running experiment for the current page.
     *
     * @param environment The GraphQL data fetching environment containing the page contentlet as
     *                    its source
     * @return The identifier of the running experiment if one exists, or null if no experiment is
     * running on the page
     * @throws Exception If an error occurs while retrieving the experiment information
     */
    @Override
    public String get(final DataFetchingEnvironment environment) throws Exception {

        try {

            final Contentlet page = environment.getSource();

            // Getting the experiment associated to this page
            final var experiment = APILocator.getExperimentsAPI()
                    .getRunningExperimentPerPage(page.getIdentifier());

            return experiment.map(Experiment::getIdentifier).orElse(null);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

}
