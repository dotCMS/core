/**
 * Logger class for MCP server
 * Logs to stderr to avoid interfering with MCP protocol communication
 */
export class Logger {
    private context: string;

    constructor(context: string) {
        this.context = context;
    }

    /**
     * Log a message with optional data
     */
    log(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${this.context}] ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
        process.stderr.write(logMessage);
    }

    /**
     * Log an error with additional context
     */
    error(message: string, error: unknown): void {
        const timestamp = new Date().toISOString();
        const errorDetails = error instanceof Error
            ? { message: error.message, stack: error.stack }
            : error;
        const logMessage = `[${timestamp}] [${this.context}] ERROR: ${message}\n${JSON.stringify(errorDetails, null, 2)}\n`;
        process.stderr.write(logMessage);
    }

    /**
     * Log a warning message
     */
    warn(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${this.context}] WARN: ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
        process.stderr.write(logMessage);
    }

    /**
     * Log debug information
     */
    debug(message: string, data?: unknown): void {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] [${this.context}] DEBUG: ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
        process.stderr.write(logMessage);
    }
}
