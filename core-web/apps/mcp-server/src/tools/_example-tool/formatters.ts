/**
 * Response formatters for example tool
 *
 * CRITICAL: Transform Responses (CONTEXT Framework)
 *
 * LLMs work best with natural language, not raw JSON.
 * Your formatters should:
 * - Use prose, not data dumps
 * - Include actionable next steps
 * - Provide context about what happened
 * - Include relevant URLs (if DOTCMS_URL is set)
 * - Be scannable (use line breaks, bullets when appropriate)
 */

/**
 * Formats the example tool response
 *
 * TODO: Replace this with your actual formatting logic
 *
 * @param data - The raw data from services
 * @returns Natural language response for the LLM
 */
export function formatExampleResponse(_data: unknown): string {
    // TODO: Transform your service responses into natural language

    // Example pattern:
    // return `Successfully ${params.action}ed ${data.length} content items!
    //
    // Content Type: ${params.contentType}
    // Items affected:
    // ${data.map(item => `- ${item.title} (${item.identifier})`).join('\n')}
    //
    // Next steps:
    // - View content in dotCMS: ${getDotCMSUrl()}/dotAdmin/#/c/content/${data[0].inode}
    // - Search for more content using content_search tool`;

    return 'TODO: Format your response here';
}

/**
 * TODO: Add more formatters if needed
 *
 * Examples:
 * - formatErrorDetails() - for tool-specific error formatting
 * - formatSummary() - for bulk operation summaries
 * - formatProgress() - for multi-step operation updates
 */
