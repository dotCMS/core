package com.dotcms.jitsu.validators;

import com.dotcms.analytics.metrics.EventType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class manages validators for analytics events.
 * It stores a collection of validators organized by JSON paths for a specific event type.
 * Each JSON path can have multiple validators associated with it.
 */
public class Validators {

    /**
     * Map of validators organized by JSON path.
     * The key is the JSON path and the value is a collection of validators for that path.
     */
    final Map<String, List<AnalyticsValidator>> validatorsByJsonPath;

    /**
     * Constructs a new Validators instance for the specified event type.
     */
    public Validators(final List<Validators.JSONPathValidators> jsonPathValidators) {
        this.validatorsByJsonPath = jsonPathValidators.stream()
                .collect(Collectors.toMap(
                        JSONPathValidators::getJsonPath,
                        JSONPathValidators::getAnalyticsValidators
                ));
    }

    /**
     * Gets the list of validators for a specific JSON path.
     *
     * @param jsonPath The JSON path for which to retrieve validators
     * @return The list of validators associated with the specified JSON path
     */
    public List<AnalyticsValidator> getValidators(final String jsonPath){
        return validatorsByJsonPath.get(jsonPath);
    }

    /**
     * Gets all JSON paths that have validators associated with them.
     *
     * @return An array of all JSON paths
     */
    public Collection<String> getPaths() {
        return validatorsByJsonPath.keySet();
    }

    public boolean isRequired(final String path) {
        return validatorsByJsonPath.get(path).stream()
                .anyMatch(validator -> validator instanceof RequiredFieldValidator);
    }

    /**
     * Inner class that represents a collection of validators for a specific JSON path.
     * Each instance contains a JSON path and a list of validators that should be applied to that path.
     */
    public static class JSONPathValidators {
        /**
         * The JSON path to which the validators apply.
         */
        private final String jsonPath;

        /**
         * The list of validators to apply to the JSON path.
         */
        private final List<AnalyticsValidator> analyticsValidators;

        /**
         * Constructs a new JSONPathValidators instance.
         *
         * @param jsonPath The JSON path to which the validators apply
         * @param analyticsValidators The list of validators to apply to the JSON path
         */
        public JSONPathValidators(final String jsonPath,
                                  final List<AnalyticsValidator> analyticsValidators) {
            this.jsonPath = jsonPath;
            this.analyticsValidators = analyticsValidators;
        }

        /**
         * Gets the JSON path to which the validators apply.
         *
         * @return The JSON path
         */
        public String getJsonPath() {
            return jsonPath;
        }

        /**
         * Gets the list of validators to apply to the JSON path.
         *
         * @return The list of validators
         */
        public List<AnalyticsValidator> getAnalyticsValidators() {
            return analyticsValidators;
        }
    }

}
