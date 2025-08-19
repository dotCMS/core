package com.dotcms.metrics;

/**
 * HTTP status code ranges for metrics categorization.
 * 
 * This enum provides type-safe categorization of HTTP status codes into
 * standard ranges (2xx, 3xx, 4xx, 5xx) with additional metadata about
 * whether each range represents an error condition.
 * 
 * @author dotCMS Team
 */
public enum StatusCodeRange {
    
    SUCCESS_2XX("2xx", 200, 299, false),
    REDIRECT_3XX("3xx", 300, 399, false),
    CLIENT_ERROR_4XX("4xx", 400, 499, true),
    SERVER_ERROR_5XX("5xx", 500, 599, true),
    OTHER("other", -1, -1, false);
    
    private final String label;
    private final int min;
    private final int max;
    private final boolean isError;
    
    /**
     * Private constructor for enum values.
     * 
     * @param label the string label for this range (e.g., "2xx")
     * @param min the minimum status code in this range (inclusive)
     * @param max the maximum status code in this range (inclusive)
     * @param isError whether this range represents error conditions
     */
    StatusCodeRange(String label, int min, int max, boolean isError) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.isError = isError;
    }
    
    /**
     * Get the string label for this status code range.
     * 
     * @return the label (e.g., "2xx", "4xx")
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Check if this status code range represents an error condition.
     * 
     * @return true if this range represents errors (4xx, 5xx), false otherwise
     */
    public boolean isError() {
        return isError;
    }
    
    /**
     * Get the minimum status code in this range.
     * 
     * @return minimum status code, or -1 for OTHER
     */
    public int getMin() {
        return min;
    }
    
    /**
     * Get the maximum status code in this range.
     * 
     * @return maximum status code, or -1 for OTHER
     */
    public int getMax() {
        return max;
    }
    
    /**
     * Determine the status code range for a given HTTP status code.
     * 
     * @param statusCode the HTTP status code
     * @return the corresponding StatusCodeRange enum value
     */
    public static StatusCodeRange fromStatusCode(int statusCode) {
        for (StatusCodeRange range : values()) {
            if (range != OTHER && statusCode >= range.min && statusCode <= range.max) {
                return range;
            }
        }
        return OTHER;
    }
    
    /**
     * Find a status code range by its string label.
     * 
     * @param label the string label to search for (e.g., "2xx")
     * @return the corresponding StatusCodeRange enum value, or OTHER if not found
     */
    public static StatusCodeRange fromLabel(String label) {
        if (label == null) {
            return OTHER;
        }
        
        for (StatusCodeRange range : values()) {
            if (range.label.equals(label)) {
                return range;
            }
        }
        return OTHER;
    }
    
    /**
     * Get all error ranges (4xx and 5xx).
     * 
     * @return array of error status code ranges
     */
    public static StatusCodeRange[] getErrorRanges() {
        return new StatusCodeRange[]{CLIENT_ERROR_4XX, SERVER_ERROR_5XX};
    }
    
    /**
     * Get all success ranges (2xx and 3xx).
     * 
     * @return array of success status code ranges
     */
    public static StatusCodeRange[] getSuccessRanges() {
        return new StatusCodeRange[]{SUCCESS_2XX, REDIRECT_3XX};
    }
}