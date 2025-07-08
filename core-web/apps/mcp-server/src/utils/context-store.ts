import { Logger } from './logger';

/**
 * Context Store - Singleton class to manage initialization state
 *
 * This store tracks whether the context_initialization tool has been called
 * and provides methods to check and update the initialization flag.
 */
export class ContextStore {
    private static instance: ContextStore | null = null;
    private isInitialized = false;
    private initializationTimestamp: Date | null = null;
    private readonly logger: Logger;

    /**
     * Private constructor to enforce singleton pattern
     */
    private constructor() {
        this.logger = new Logger('CONTEXT_STORE');
        this.logger.debug('Context Store instance created');
    }

    /**
     * Get the singleton instance of ContextStore
     */
    static getInstance(): ContextStore {
        if (!ContextStore.instance) {
            ContextStore.instance = new ContextStore();
        }

        return ContextStore.instance;
    }

    /**
     * Set the initialization flag to true
     * This should be called when context_initialization tool is executed
     */
    setInitialized(): void {
        this.isInitialized = true;
        this.initializationTimestamp = new Date();
        this.logger.log('Context initialization flag set to true', {
            timestamp: this.initializationTimestamp.toISOString()
        });
    }

    /**
     * Check if context has been initialized
     * @returns {boolean} true if context_initialization has been called
     */
    getIsInitialized(): boolean {
        return this.isInitialized;
    }

    /**
     * Get the timestamp when initialization was completed
     * @returns {Date | null} timestamp of initialization or null if not initialized
     */
    getInitializationTimestamp(): Date | null {
        return this.initializationTimestamp;
    }

    /**
     * Reset the initialization state
     * Useful for testing or if context needs to be re-initialized
     */
    reset(): void {
        this.isInitialized = false;
        this.initializationTimestamp = null;
        this.logger.log('Context initialization state reset');
    }

    /**
     * Get initialization status information
     * @returns {object} Object containing initialization status and timestamp
     */
    getStatus(): { isInitialized: boolean; timestamp: Date | null; age?: string } {
        const status = {
            isInitialized: this.isInitialized,
            timestamp: this.initializationTimestamp
        };

        // Add age calculation if initialized
        if (this.isInitialized && this.initializationTimestamp) {
            const ageMs = Date.now() - this.initializationTimestamp.getTime();
            const ageMinutes = Math.floor(ageMs / (1000 * 60));
            const ageSeconds = Math.floor((ageMs % (1000 * 60)) / 1000);

            return {
                ...status,
                age: ageMinutes > 0 ? `${ageMinutes}m ${ageSeconds}s` : `${ageSeconds}s`
            };
        }

        return status;
    }

    /**
     * Log current status for debugging purposes
     */
    logStatus(): void {
        this.logger.log('Current context store status', this.getStatus());
    }
}

/**
 * Convenience function to get the singleton instance
 * This provides a cleaner import for other modules
 */
export const getContextStore = (): ContextStore => ContextStore.getInstance();
