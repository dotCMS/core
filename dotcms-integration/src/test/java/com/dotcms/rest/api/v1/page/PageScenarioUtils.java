package com.dotcms.rest.api.v1.page;

import static org.junit.Assert.assertEquals;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

/**
 * Utility class for common page scenario operations
 * Provides methods to validate content, extract PageView from response, and manage content date ranges
 */
class PageScenarioUtils {

    /**
     * Configuration class for content date ranges
     * Unified class that can handle both expired and valid content configurations
     */
    static class ContentConfig {
        final String title;
        final int daysBeforeCurrentForPublish;    // Days before current date for publishDate
        final Integer daysBeforeCurrentForExpire; // Days before current date for expireDate (null if not used)
        final Integer daysAfterCurrentForExpire;  // Days after current date for expireDate (null if not used)

        /**
         * Private constructor - use static factory methods instead
         */
        private ContentConfig(String title, int daysBeforePublish, Integer daysBeforeExpire, Integer daysAfterExpire) {
            this.title = title;
            this.daysBeforeCurrentForPublish = daysBeforePublish;
            this.daysBeforeCurrentForExpire = daysBeforeExpire;
            this.daysAfterCurrentForExpire = daysAfterExpire;
        }

        /**
         * Static factory method for content that never expires
         * @param title Content title
         * @param daysBeforePublish Days before reference date for publishDate
         * @return ContentConfig for content that never expires
         */
        static ContentConfig neverExpires(String title, int daysBeforePublish) {
            return new ContentConfig(title, daysBeforePublish, null, null);
        }

        /**
         * Static factory method for expired content
         * @param title Content title
         * @param daysBeforePublish Days before reference date for publishDate
         * @param daysBeforeExpire Days before reference date for expireDate
         * @return ContentConfig for expired content
         */
        static ContentConfig expired(String title, int daysBeforePublish, int daysBeforeExpire) {
            return new ContentConfig(title, daysBeforePublish, daysBeforeExpire, null);
        }

        /**
         * Static factory method for valid content with future expiration
         * @param title Content title
         * @param daysBeforePublish Days before reference date for publishDate
         * @param daysAfterExpire Days after reference date for expireDate
         * @return ContentConfig for valid content
         */
        static ContentConfig validWithExpiration(String title, int daysBeforePublish, int daysAfterExpire) {
            return new ContentConfig(title, daysBeforePublish, null, daysAfterExpire);
        }

        /**
         * Determines if this content will be expired relative to the reference date
         * @return true if content will be expired, false otherwise
         */
        boolean isExpiredContent() {
            return daysBeforeCurrentForExpire != null;
        }

        /**
         * Determines if this content has an expiration date
         * @return true if content has expiration date, false if never expires
         */
        boolean hasExpirationDate() {
            return daysBeforeCurrentForExpire != null || daysAfterCurrentForExpire != null;
        }
    }


    /**
     * Extracts PageView from Response
     *
     * @param response The response containing the PageView
     * @return PageView extracted from response
     * @throws IllegalArgumentException if response format is invalid
     */
     static PageView extractPageViewFromResponse(final Response response) {
        try {
            return (PageView) ((ResponseEntityView<?>) response.getEntity()).getEntity();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract PageView from response: " + e.getMessage(), e);
        }
    }

    /**
     * Minimal result class for validation summary
     */
    static class ValidationSummary {
        final int totalFound;
        final int matched;
        final int unmatched;
        final boolean allMatch;

        ValidationSummary(int totalFound, int matched, int unmatched, boolean allMatch) {
            this.totalFound = totalFound;
            this.matched = matched;
            this.unmatched = unmatched;
            this.allMatch = allMatch;
        }

        @Override
        public String toString() {
            return String.format("ValidationSummary{total=%d, matched=%d, unmatched=%d, allMatch=%s}",
                    totalFound, matched, unmatched, allMatch);
        }
    }

    /**
     * Enhanced internal validation method that returns detailed results
     *
     * @param pageView The page view to validate
     * @param containsText Text that should be contained in all titles (ignored if expectNone is true)
     * @return ValidationSummary with detailed results
     */
    static ValidationSummary validateContentletTitlesContainingInternal(final PageView pageView,
            final String containsText){
        return validateContentletTitlesContainingInternal(pageView, containsText, false);
    }

    /**
     * Enhanced internal validation method that returns detailed results
     *
     * @param pageView The page view to validate
     * @param containsText Text that should be contained in all titles (ignored if expectNone is true)
     * @param expectNone If true, validates that NO contentlets are present. containsText is ignored.
     * @return ValidationSummary with detailed results
     */
     static ValidationSummary validateContentletTitlesContainingInternal(final PageView pageView,
            final String containsText,
            final boolean expectNone) {

        final List<? extends ContainerRaw> containers = (List<? extends ContainerRaw>)pageView.getContainers();
        assertEquals(1, containers.size());
        final Map<String, List<Contentlet>> contentlets = containers.get(0).getContentlets();

        // If expecting no contentlets
        if (expectNone) {
            boolean isEmpty = contentlets.isEmpty() ||
                    contentlets.values().stream().allMatch(List::isEmpty);

            if (isEmpty) {
                Logger.info(PageResourceTest.class, "VALIDATION PASSED: No contentlets found as expected");
                return new ValidationSummary(0, 0, 0, true);
            } else {
                int totalContentlets = contentlets.values().stream()
                        .mapToInt(List::size)
                        .sum();
                Logger.error(PageResourceTest.class,
                        "VALIDATION FAILED: Expected no contentlets but found " + totalContentlets);
                return new ValidationSummary(totalContentlets, 0, totalContentlets, false);
            }
        }

        // Validate containsText is provided when not expecting none
        if (null == containsText || containsText.trim().isEmpty()) {
            Logger.error(PageResourceTest.class,
                    "VALIDATION FAILED: containsText is required when expectNone is false");
            return new ValidationSummary(0, 0, 0, false);
        }

        // If expecting contentlets but none found
        if (contentlets.isEmpty()) {
            Logger.error(PageResourceTest.class,
                    "VALIDATION FAILED: Expected contentlets containing '" + containsText + "' but found none");
            return new ValidationSummary(0, 0, 0, false);
        }

        final List<String> foundTitles = new ArrayList<>();
        final List<String> matchedTitles = new ArrayList<>();
        final List<String> unmatchedTitles = new ArrayList<>();
        boolean allMatch = true;

        // Extract all titles and check for contains text
        for (Map.Entry<String, List<Contentlet>> entry : contentlets.entrySet()) {
            final String containerKey = entry.getKey();
            final List<Contentlet> contentletList = entry.getValue();

            Logger.info(PageResourceTest.class, "Processing container key: " + containerKey);

            for (Contentlet contentlet : contentletList) {
                try {
                    final String title = contentlet.getStringProperty("title");
                    if (null != title && !title.trim().isEmpty()) {
                        foundTitles.add(title);

                        // Check if title contains the specified text (case-insensitive)
                        if (title.toLowerCase().contains(containsText.toLowerCase())) {
                            matchedTitles.add(title);
                            Logger.info(PageResourceTest.class, "✓ CONTAINS '" + containsText + "': " + title);
                        } else {
                            unmatchedTitles.add(title);
                            allMatch = false;
                            Logger.error(PageResourceTest.class, "✗ DOES NOT CONTAIN '" + containsText + "': " + title);
                        }
                    } else {
                        Logger.warn(PageResourceTest.class, "Found contentlet with empty or null title");
                        allMatch = false;
                    }
                } catch (Exception e) {
                    Logger.error(PageResourceTest.class, "Error getting title from contentlet: " + e.getMessage());
                    allMatch = false;
                }
            }
        }

        // Create result object
        final ValidationSummary result = new ValidationSummary(foundTitles.size(), matchedTitles.size(),
                unmatchedTitles.size(), allMatch && !foundTitles.isEmpty());

        // Log summary
        Logger.info(PageResourceTest.class, "=== VALIDATION SUMMARY ===");
        Logger.info(PageResourceTest.class, "Total contentlets found: " + result.totalFound);
        Logger.info(PageResourceTest.class, "Titles containing '" + containsText + "': " + result.matched);
        Logger.info(PageResourceTest.class, "Titles NOT containing '" + containsText + "': " + result.unmatched);

        if (result.allMatch && result.totalFound > 0) {
            Logger.info(PageResourceTest.class, "✓ VALIDATION PASSED: All " + result.totalFound +
                    " contentlets contain '" + containsText + "'");
        } else if (result.totalFound == 0) {
            Logger.error(PageResourceTest.class, "✗ VALIDATION FAILED: No contentlets found");
        } else {
            Logger.error(PageResourceTest.class, "✗ VALIDATION FAILED: " + result.unmatched +
                    " out of " + result.totalFound + " contentlets do NOT contain '" + containsText + "'");
        }

        return result;
    }

    /**
     * Validates that all contentlets in a page match the specified criteria
     *
     * @param response The response containing the PageView to validate
     * @param containsText Text that should be contained in all titles (ignored if expectNone is true)
     * @param expectNone If true, validates that NO contentlets are present. containsText is ignored.
     * @return true if validation passes, false otherwise
     */
    static boolean validateAllContentletTitlesContaining(final Response response,
            final String containsText,
            final boolean expectNone) {
        final PageView pageView = extractPageViewFromResponse(response);
        return validateContentletTitlesContainingInternal(pageView, containsText, expectNone).allMatch;
    }

    /**
     * Overloaded method for backward compatibility when expectNone is false
     *
     * @param response The response containing the PageView to validate
     * @param containsText Text that should be contained in all titles
     * @return true if all contentlets contain the specified text, false otherwise
     */
    static boolean validateAllContentletTitlesContaining(final Response response,
            final String containsText) {
        return validateAllContentletTitlesContaining(response, containsText, false);
    }

    /**
     * Convenience method to validate that no contentlets are present
     *
     * @param response The response containing the PageView to validate
     * @return true if no contentlets are found, false otherwise
     */
    static boolean validateNoContentlets(final Response response) {
        return validateAllContentletTitlesContaining(response, null, true);
    }

    /**
     * Legacy method that works with PageView directly (for backward compatibility)
     *
     * @param pageView The page view to validate
     * @param containsText Text that should be contained in all titles (ignored if expectNone is true)
     * @param expectNone If true, validates that NO contentlets are present. containsText is ignored.
     * @return true if validation passes, false otherwise
     */
    static boolean validateAllContentletTitlesContaining(final PageView pageView,
            final String containsText,
            final boolean expectNone) {
        return validateContentletTitlesContainingInternal(pageView, containsText, expectNone).allMatch;
    }

    /**
     * Legacy overloaded method for backward compatibility when expectNone is false
     *
     * @param pageView The page view to validate
     * @param containsText Text that should be contained in all titles
     * @return true if all contentlets contain the specified text, false otherwise
     */
    static boolean validateAllContentletTitlesContaining(final PageView pageView,
            final String containsText) {
        return validateContentletTitlesContainingInternal(pageView, containsText, false).allMatch;
    }

    /**
     * Legacy convenience method to validate that no contentlets are present
     *
     * @param pageView The page view to validate
     * @return true if no contentlets are found, false otherwise
     */
    public static boolean validateNoContentlets(final PageView pageView) {
        return validateContentletTitlesContainingInternal(pageView, null, true).allMatch;
    }

}
