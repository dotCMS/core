/**
 * Mock implementation of ResizeObserver for testing environments
 */
export class MockResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}

/**
 * Sets up the ResizeObserver mock globally for testing
 */
export function setupResizeObserverMock(): void {
    window.ResizeObserver = MockResizeObserver;
} 