/**
 * Log level type for the DotLogger
 */
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

/**
 * Log level constants for convenient usage
 */
export const LOG_LEVELS = {
    DEBUG: 'debug',
    INFO: 'info',
    WARN: 'warn',
    ERROR: 'error'
} as const satisfies Record<string, LogLevel>;

/**
 * Priority map for log level comparisons
 * Lower numbers = more verbose, higher numbers = less verbose
 */
const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
    debug: 0,
    info: 1,
    warn: 2,
    error: 3
};

/**
 * Custom logger for DotCMS SDK
 * Provides structured logging with context identification and configurable log level filtering
 *
 * @example
 * ```typescript
 * const logger = new DotLogger('Analytics', 'Impression', 'info');
 * logger.debug('This will not show'); // Below threshold
 * logger.info('Tracker initialized'); // Shows
 * logger.error('Failed to track'); // Shows
 * ```
 */
export class DotLogger {
    private readonly packageName: string;
    private readonly context: string;
    private readonly minLevel: LogLevel;

    constructor(packageName: string, context: string, minLevel: LogLevel = 'warn') {
        this.packageName = packageName;
        this.context = context;
        this.minLevel = minLevel;
    }

    /**
     * Creates the formatted prefix for log messages
     * Format: [DotCMS PackageName | Context] [LEVEL]
     */
    private getPrefix(level: string): string {
        return `[DotCMS ${this.packageName} | ${this.context}] [${level.toUpperCase()}]`;
    }

    /**
     * Checks if a log level should be displayed based on the minimum threshold
     */
    private shouldLog(level: LogLevel): boolean {
        return LOG_LEVEL_PRIORITY[level] >= LOG_LEVEL_PRIORITY[this.minLevel];
    }

    /**
     * Log a DEBUG level message
     * Used for detailed debugging information
     */
    public debug(...args: unknown[]): void {
        if (this.shouldLog('debug')) {
            // eslint-disable-next-line no-console
            console.log(this.getPrefix('debug'), ...args);
        }
    }

    /**
     * Log an INFO level message
     * Used for general informational messages
     */
    public info(...args: unknown[]): void {
        if (this.shouldLog('info')) {
            // eslint-disable-next-line no-console
            console.info(this.getPrefix('info'), ...args);
        }
    }

    /**
     * Log a WARN level message
     * Used for warning messages that don't prevent operation
     */
    public warn(...args: unknown[]): void {
        if (this.shouldLog('warn')) {
            console.warn(this.getPrefix('warn'), ...args);
        }
    }

    /**
     * Log an ERROR level message
     * Always logs regardless of threshold for critical errors
     */
    public error(...args: unknown[]): void {
        console.error(this.getPrefix('error'), ...args);
    }

    /**
     * Create a console group for organizing related log messages
     * Respects the minimum log level threshold
     */
    public group(label: string): void {
        if (this.shouldLog('debug')) {
            // eslint-disable-next-line no-console
            console.group(`${this.getPrefix('debug')} ${label}`);
        }
    }

    /**
     * End the current console group
     * Respects the minimum log level threshold
     */
    public groupEnd(): void {
        if (this.shouldLog('debug')) {
            // eslint-disable-next-line no-console
            console.groupEnd();
        }
    }

    /**
     * Start a timer with the given label
     * Useful for performance measurements
     */
    public time(label: string): void {
        if (this.shouldLog('debug')) {
            // eslint-disable-next-line no-console
            console.time(`${this.getPrefix('debug')} ${label}`);
        }
    }

    /**
     * End a timer and log the elapsed time
     * Useful for performance measurements
     */
    public timeEnd(label: string): void {
        if (this.shouldLog('debug')) {
            // eslint-disable-next-line no-console
            console.timeEnd(`${this.getPrefix('debug')} ${label}`);
        }
    }

    /**
     * Log a message (alias for info)
     * Provided for backward compatibility
     */
    public log(...args: unknown[]): void {
        this.info(...args);
    }
}
