/**
 * Mock implementation of ResizeObserver for testing environments
 */
export class MockResizeObserver {
    observe() {
        // do nothing
    }
    unobserve() {
        // do nothing
    }
    disconnect() {
        // do nothing
    }
}

/**
 * Sets up the ResizeObserver mock globally for testing
 */
export function setupResizeObserverMock(): void {
    window.ResizeObserver = MockResizeObserver;
}
