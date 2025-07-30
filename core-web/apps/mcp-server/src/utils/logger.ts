/**
 * Logger class for MCP server
 * Logs to stderr to avoid interfering with MCP protocol communication
 */
export class Logger {
    private context: string;
    private verbose: boolean;

    constructor(context: string) {
        this.context = context;
        this.verbose = process.env.VERBOSE === 'true';
    }

    /**
     * Log a message with optional data
     * Data is only logged when verbose mode is enabled
     */
    log(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        let logMessage = `[${timestamp}] [${this.context}] ${message}`;

        // Only include data if verbose mode is enabled
        if (data && this.verbose) {
            logMessage += '\n' + JSON.stringify(data, null, 2);
        }

        logMessage += '\n';
        process.stderr.write(logMessage);
    }

    /**
     * Log an error with additional context
     * Error data is always logged regardless of verbose mode
     */
    error(message: string, error: unknown): void {
        const timestamp = new Date().toISOString();
        const errorDetails =
            error instanceof Error ? { message: error.message, stack: error.stack } : error;
        const logMessage = `[${timestamp}] [${this.context}] ERROR: ${message}\n${JSON.stringify(errorDetails, null, 2)}\n`;
        process.stderr.write(logMessage);
    }

    /**
     * Log a warning message
     * Data is only logged when verbose mode is enabled
     */
    warn(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        let logMessage = `[${timestamp}] [${this.context}] WARN: ${message}`;

        // Only include data if verbose mode is enabled
        if (data && this.verbose) {
            logMessage += '\n' + JSON.stringify(data, null, 2);
        }

        logMessage += '\n';
        process.stderr.write(logMessage);
    }

    /**
     * Log debug information
     * Debug data is only logged when verbose mode is enabled
     */
    debug(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        let logMessage = `[${timestamp}] [${this.context}] DEBUG: ${message}`;

        // Only include data if verbose mode is enabled
        if (data && this.verbose) {
            logMessage += '\n' + JSON.stringify(data, null, 2);
        }

        logMessage += '\n';
        process.stderr.write(logMessage);
    }

    /**
     * Log a message with data regardless of verbose mode
     * Use this for important data that should always be logged
     */
    logWithData(message: string, data: unknown): void {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${this.context}] ${message}\n${JSON.stringify(data, null, 2)}\n`;
        process.stderr.write(logMessage);
    }
}
