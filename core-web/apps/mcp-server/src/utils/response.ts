import { Logger } from './logger.js';

/**
 * Success response structure for MCP tools
 */
export interface McpSuccessResponse {
    [x: string]: unknown;
    content: Array<{
        type: 'text';
        text: string;
    }>;
}

/**
 * Error response structure for MCP tools
 */
export interface McpErrorResponse {
    [x: string]: unknown;
    isError: true;
    content: Array<{
        type: 'text';
        text: string;
    }>;
}

/**
 * Standard MCP response union type
 */
export type McpResponse = McpSuccessResponse | McpErrorResponse;

/**
 * Formats response data for LLM consumption
 */
export function formatResponse(
    message: string,
    data?: unknown,
    options?: {
        includeRawData?: boolean;
        maxDataLength?: number;
    }
): string {
    if (!data) {
        return message;
    }

    // Check for environment variable to control truncation
    const envMaxLength = process.env.MCP_RESPONSE_MAX_LENGTH;
    const defaultMaxLength = envMaxLength ? parseInt(envMaxLength, 10) : Number.MAX_SAFE_INTEGER;
    
    const opts = { includeRawData: false, maxDataLength: defaultMaxLength, ...options };

    // Handle different data types appropriately
    if (typeof data === 'string') {
        return `${message}\n\n${data}`;
    }

    if (typeof data === 'object') {
        // For objects, provide structured information
        const jsonStr = JSON.stringify(data, null, 2);
        
        const shouldTruncate = jsonStr.length > opts.maxDataLength;
        if (shouldTruncate) {
            const logger = new Logger('formatResponse');
            logger.log(`Response truncated from ${jsonStr.length} to ${opts.maxDataLength} characters`);
        }
        
        const truncated = shouldTruncate
            ? jsonStr.substring(0, opts.maxDataLength) + '...[truncated]'
            : jsonStr;

        return `${message}\n\nDetails:\n${truncated}`;
    }

    return `${message}\n\nData: ${String(data)}`;
}

/**
 * Creates a standardized success response
 */
export function createSuccessResponse<T = unknown>(message: string, data?: T): McpSuccessResponse {
    const text = formatResponse(message, data);

    return {
        content: [
            {
                type: 'text',
                text
            }
        ]
    };
}

/**
 * Creates a standardized error response with improved error handling
 */
export function createErrorResponse(errorPrefix: string, error: unknown): McpErrorResponse {
    let errorMessage: string;

    if (error instanceof Error) {
        errorMessage = error.message;
    } else if (typeof error === 'string') {
        errorMessage = error;
    } else if (error && typeof error === 'object' && 'message' in error) {
        errorMessage = String(error.message);
    } else {
        errorMessage = 'Unknown error occurred';
    }

    return {
        isError: true,
        content: [
            {
                type: 'text',
                text: `${errorPrefix}: ${errorMessage}`
            }
        ]
    };
}

/**
 * Creates standardized try-catch wrapper for error handling
 * Use this in your tool implementations instead of raw try-catch
 */
export async function executeWithErrorHandling(
    operation: () => Promise<McpResponse>,
    errorPrefix: string
): Promise<McpResponse> {
    try {
        return await operation();
    } catch (error: unknown) {
        return createErrorResponse(errorPrefix, error);
    }
}
