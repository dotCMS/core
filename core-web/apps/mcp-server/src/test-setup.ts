// Shared test setup for all MCP server tests
// This file contains common mocks that are used across multiple test files

// Mock the AgnosticClient
export const mockFetch = jest.fn();

jest.mock('./services/client', () => {
    return {
        AgnosticClient: class MockAgnosticClient {
            dotcmsUrl = 'http://localhost';
            fetch = mockFetch;
        }
    };
});

// Mock Logger
jest.mock('./utils/logger', () => {
    return {
        Logger: jest.fn().mockImplementation(() => ({
            log: jest.fn(),
            error: jest.fn()
        }))
    };
});

// Reset mocks before each test
beforeEach(() => {
    mockFetch.mockClear();
});
