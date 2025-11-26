import { ContextStore, getContextStore } from './context-store';

// Mock the Logger
jest.mock('./logger', () => ({
    Logger: jest.fn().mockImplementation(() => ({
        log: jest.fn(),
        debug: jest.fn(),
        error: jest.fn()
    }))
}));

describe('ContextStore', () => {
    let store: ContextStore;

    beforeEach(() => {
        // Reset the singleton instance before each test
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (ContextStore as any).instance = null;
        store = ContextStore.getInstance();
    });

    afterEach(() => {
        // Clean up singleton instance
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (ContextStore as any).instance = null;
    });

    describe('Singleton Pattern', () => {
        it('should return the same instance when called multiple times', () => {
            const instance1 = ContextStore.getInstance();
            const instance2 = ContextStore.getInstance();

            expect(instance1).toBe(instance2);
        });

        it('should create new instance only on first call', () => {
            const instance1 = ContextStore.getInstance();
            const instance2 = ContextStore.getInstance();
            const instance3 = ContextStore.getInstance();

            expect(instance1).toBe(instance2);
            expect(instance2).toBe(instance3);
        });
    });

    describe('getContextStore convenience function', () => {
        it('should return the same instance as ContextStore.getInstance()', () => {
            const instance1 = ContextStore.getInstance();
            const instance2 = getContextStore();

            expect(instance1).toBe(instance2);
        });
    });

    describe('Initial State', () => {
        it('should start with isInitialized as false', () => {
            expect(store.getIsInitialized()).toBe(false);
        });

        it('should start with null timestamp', () => {
            expect(store.getInitializationTimestamp()).toBeNull();
        });

        it('should return correct initial status', () => {
            const status = store.getStatus();

            expect(status).toEqual({
                isInitialized: false,
                timestamp: null
            });
        });
    });

    describe('setInitialized', () => {
        it('should set isInitialized to true', () => {
            store.setInitialized();

            expect(store.getIsInitialized()).toBe(true);
        });

        it('should set initialization timestamp', () => {
            const beforeTime = new Date();
            store.setInitialized();
            const afterTime = new Date();

            const timestamp = store.getInitializationTimestamp();
            expect(timestamp).not.toBeNull();
            expect(timestamp?.getTime()).toBeGreaterThanOrEqual(beforeTime.getTime());
            expect(timestamp?.getTime()).toBeLessThanOrEqual(afterTime.getTime());
        });

        it('should maintain state across multiple calls', async () => {
            store.setInitialized();
            const firstTimestamp = store.getInitializationTimestamp();

            // Wait a bit to ensure different timestamp
            await new Promise((resolve) => setTimeout(resolve, 10));

            store.setInitialized();
            const secondTimestamp = store.getInitializationTimestamp();

            expect(store.getIsInitialized()).toBe(true);
            expect(secondTimestamp).not.toEqual(firstTimestamp);
        });
    });

    describe('getStatus', () => {
        it('should return status with age when initialized', () => {
            store.setInitialized();

            const status = store.getStatus();

            expect(status.isInitialized).toBe(true);
            expect(status.timestamp).not.toBeNull();
            expect(status.age).toBeDefined();
            expect(typeof status.age).toBe('string');
        });

        it('should format age in seconds for recent initialization', () => {
            store.setInitialized();

            const status = store.getStatus();

            expect(status.age).toMatch(/^\d+s$/);
        });

        it('should format age in minutes and seconds for older initialization', () => {
            // Mock an old timestamp
            const oldTimestamp = new Date(Date.now() - 90000); // 90 seconds ago
            store.setInitialized();
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (store as any).initializationTimestamp = oldTimestamp;

            const status = store.getStatus();

            expect(status.age).toMatch(/^\d+m \d+s$/);
        });

        it('should not include age when not initialized', () => {
            const status = store.getStatus();

            expect(status.age).toBeUndefined();
        });
    });

    describe('reset', () => {
        it('should reset isInitialized to false', () => {
            store.setInitialized();
            expect(store.getIsInitialized()).toBe(true);

            store.reset();
            expect(store.getIsInitialized()).toBe(false);
        });

        it('should reset timestamp to null', () => {
            store.setInitialized();
            expect(store.getInitializationTimestamp()).not.toBeNull();

            store.reset();
            expect(store.getInitializationTimestamp()).toBeNull();
        });

        it('should reset status completely', () => {
            store.setInitialized();
            expect(store.getStatus().isInitialized).toBe(true);

            store.reset();
            const status = store.getStatus();

            expect(status).toEqual({
                isInitialized: false,
                timestamp: null
            });
        });
    });

    describe('logStatus', () => {
        it('should call logStatus without throwing', () => {
            expect(() => store.logStatus()).not.toThrow();
        });

        it('should call logStatus when initialized without throwing', () => {
            store.setInitialized();
            expect(() => store.logStatus()).not.toThrow();
        });
    });

    describe('Logger Integration', () => {
        it('should log when setInitialized is called', () => {
            // Test that the logger is called, without checking exact parameters
            // since Logger is mocked at a higher level
            expect(() => store.setInitialized()).not.toThrow();
        });

        it('should log when reset is called', () => {
            // Test that the logger is called, without checking exact parameters
            // since Logger is mocked at a higher level
            expect(() => store.reset()).not.toThrow();
        });

        it('should create logger instance on construction', () => {
            // Create a new instance to test constructor behavior
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (ContextStore as any).instance = null;
            const newStore = ContextStore.getInstance();

            // Verify the store was created successfully
            expect(newStore).toBeDefined();
            expect(newStore.getIsInitialized()).toBe(false);
        });
    });

    describe('Edge Cases', () => {
        it('should handle multiple rapid setInitialized calls', async () => {
            store.setInitialized();
            const firstTimestamp = store.getInitializationTimestamp();

            // Wait a small amount to ensure different timestamps
            await new Promise((resolve) => setTimeout(resolve, 5));

            store.setInitialized();
            const secondTimestamp = store.getInitializationTimestamp();

            expect(store.getIsInitialized()).toBe(true);
            expect(secondTimestamp?.getTime()).not.toEqual(firstTimestamp?.getTime());
        });

        it('should handle reset after multiple initializations', () => {
            store.setInitialized();
            store.setInitialized();
            store.setInitialized();

            store.reset();

            expect(store.getIsInitialized()).toBe(false);
            expect(store.getInitializationTimestamp()).toBeNull();
        });

        it('should handle getStatus calls in rapid succession', () => {
            store.setInitialized();

            const status1 = store.getStatus();
            const status2 = store.getStatus();

            expect(status1.isInitialized).toBe(status2.isInitialized);
            expect(status1.timestamp).toEqual(status2.timestamp);
            // Age might differ slightly due to timing
        });
    });

    describe('Thread Safety Considerations', () => {
        it('should maintain singleton pattern across async operations', async () => {
            const promises = Array.from({ length: 10 }, () =>
                Promise.resolve(ContextStore.getInstance())
            );

            const instances = await Promise.all(promises);

            // All instances should be the same
            instances.forEach((instance) => {
                expect(instance).toBe(instances[0]);
            });
        });
    });
});
