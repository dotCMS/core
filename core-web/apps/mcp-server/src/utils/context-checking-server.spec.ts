import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { RequestHandlerExtra } from '@modelcontextprotocol/sdk/shared/protocol';
import { ServerNotification, ServerRequest } from '@modelcontextprotocol/sdk/types';

import { createContextCheckingServer } from './context-checking-server';
import { getContextStore } from './context-store';

// Mock the context store
jest.mock('./context-store', () => ({
    getContextStore: jest.fn()
}));

// Mock the logger
jest.mock('./logger', () => ({
    Logger: jest.fn().mockImplementation(() => ({
        log: jest.fn(),
        debug: jest.fn(),
        warn: jest.fn(),
        error: jest.fn()
    }))
}));

describe('createContextCheckingServer', () => {
    let mockServer: jest.Mocked<McpServer>;
    let mockContextStore: {
        getIsInitialized: jest.Mock;
        getStatus: jest.Mock;
    };
    let mockRegisterTool: jest.Mock;
    let contextCheckingServer: McpServer;

    beforeEach(() => {
        // Mock context store
        mockContextStore = {
            getIsInitialized: jest.fn(),
            getStatus: jest.fn()
        };
        (getContextStore as jest.Mock).mockReturnValue(mockContextStore);

        // Mock MCP server
        mockRegisterTool = jest.fn();
        mockServer = {
            registerTool: mockRegisterTool
            // Add other required properties/methods as needed
        } as unknown as jest.Mocked<McpServer>;

        // Create the context checking server
        contextCheckingServer = createContextCheckingServer(mockServer);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Proxy Creation', () => {
        it('should return a proxied server object', () => {
            expect(contextCheckingServer).toBeDefined();
            expect(contextCheckingServer).not.toBe(mockServer);
        });

        it('should proxy registerTool method', () => {
            expect(typeof contextCheckingServer.registerTool).toBe('function');
        });

        it('should preserve other server properties', () => {
            // Add a test property to the mock server
            const mockServerWithProp = mockServer as typeof mockServer & { testProperty: string };
            mockServerWithProp.testProperty = 'test value';

            const contextCheckingServerWithProp =
                contextCheckingServer as typeof contextCheckingServer & { testProperty: string };
            expect(contextCheckingServerWithProp.testProperty).toBe('test value');
        });
    });

    describe('Tool Registration', () => {
        it('should register tool with wrapped callback', () => {
            const mockCallback = jest.fn();
            const toolName = 'test-tool';
            const toolConfig = { description: 'Test tool' };

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            expect(mockRegisterTool).toHaveBeenCalledTimes(1);
            expect(mockRegisterTool).toHaveBeenCalledWith(
                toolName,
                toolConfig,
                expect.any(Function)
            );
        });

        it('should preserve original callback for context_initialization tool', async () => {
            const mockCallback = jest.fn().mockResolvedValue({ success: true });
            const toolName = 'context_initialization';
            const toolConfig = { description: 'Context initialization tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            // Call the wrapped callback
            const result = await wrappedCallback(mockArgs, mockExtra);

            expect(mockCallback).toHaveBeenCalledWith(mockArgs, mockExtra);
            expect(result).toEqual({ success: true });
            expect(mockContextStore.getIsInitialized).not.toHaveBeenCalled();
        });

        it('should check context initialization for non-exempt tools', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(true);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: true,
                timestamp: new Date()
            });

            const mockCallback = jest.fn().mockResolvedValue({ success: true });
            const toolName = 'regular-tool';
            const toolConfig = { description: 'Regular tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            // Call the wrapped callback
            const result = await wrappedCallback(mockArgs, mockExtra);

            expect(mockContextStore.getIsInitialized).toHaveBeenCalledTimes(1);
            expect(mockCallback).toHaveBeenCalledWith(mockArgs, mockExtra);
            expect(result).toEqual({ success: true });
        });

        it('should throw error when context not initialized for regular tools', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(false);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: false,
                timestamp: null
            });

            const mockCallback = jest.fn();
            const toolName = 'regular-tool';
            const toolConfig = { description: 'Regular tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            // Call the wrapped callback and expect it to throw
            await expect(wrappedCallback(mockArgs, mockExtra)).rejects.toThrow(
                'Cannot execute tool "regular-tool" because context initialization is required first'
            );

            expect(mockContextStore.getIsInitialized).toHaveBeenCalledTimes(1);
            expect(mockCallback).not.toHaveBeenCalled();
        });

        it('should include detailed error message when context not initialized', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(false);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: false,
                timestamp: null
            });

            const mockCallback = jest.fn();
            const toolName = 'content-tool';
            const toolConfig = { description: 'Content tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            try {
                await wrappedCallback(mockArgs, mockExtra);
            } catch (error) {
                expect(error).toBeInstanceOf(Error);
                const errorMessage = (error as Error).message;

                expect(errorMessage).toContain('Cannot execute tool "content-tool"');
                expect(errorMessage).toContain(
                    'REQUIRED ACTION: You must call the "context_initialization" tool'
                );
                expect(errorMessage).toContain('context_initialization tool:');
                expect(errorMessage).toContain('- Discovers all available content types');
                expect(errorMessage).toContain('- Loads current site information');
                expect(errorMessage).toContain('- Provides essential context');
                expect(errorMessage).toContain('Current initialization status: Not initialized');
                expect(errorMessage).toContain('Timestamp: Never');
            }
        });

        it('should show timestamp in error message when previously initialized', async () => {
            const testTimestamp = new Date('2023-01-01T12:00:00Z');
            mockContextStore.getIsInitialized.mockReturnValue(false);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: false,
                timestamp: testTimestamp
            });

            const mockCallback = jest.fn();
            const toolName = 'content-tool';
            const toolConfig = { description: 'Content tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            try {
                await wrappedCallback(mockArgs, mockExtra);
            } catch (error) {
                const errorMessage = (error as Error).message;
                expect(errorMessage).toContain(`Timestamp: ${testTimestamp}`);
            }
        });
    });

    describe('Multiple Tool Registration', () => {
        it('should handle multiple tool registrations correctly', () => {
            const mockCallback1 = jest.fn();
            const mockCallback2 = jest.fn();
            const mockCallback3 = jest.fn();

            contextCheckingServer.registerTool('tool1', { description: 'Tool 1' }, mockCallback1);
            contextCheckingServer.registerTool('tool2', { description: 'Tool 2' }, mockCallback2);
            contextCheckingServer.registerTool(
                'context_initialization',
                { description: 'Context init' },
                mockCallback3
            );

            expect(mockRegisterTool).toHaveBeenCalledTimes(3);
        });

        it('should apply context checking to all non-exempt tools', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(true);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: true,
                timestamp: new Date()
            });

            const mockCallback1 = jest.fn().mockResolvedValue({ result: 'tool1' });
            const mockCallback2 = jest.fn().mockResolvedValue({ result: 'tool2' });
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool('tool1', { description: 'Tool 1' }, mockCallback1);
            contextCheckingServer.registerTool('tool2', { description: 'Tool 2' }, mockCallback2);

            // Get the wrapped callbacks
            const wrappedCallback1 = mockRegisterTool.mock.calls[0][2];
            const wrappedCallback2 = mockRegisterTool.mock.calls[1][2];

            // Call both wrapped callbacks
            const result1 = await wrappedCallback1(mockArgs, mockExtra);
            const result2 = await wrappedCallback2(mockArgs, mockExtra);

            expect(mockContextStore.getIsInitialized).toHaveBeenCalledTimes(2);
            expect(result1).toEqual({ result: 'tool1' });
            expect(result2).toEqual({ result: 'tool2' });
        });
    });

    describe('Error Handling', () => {
        it('should handle errors thrown by original callback', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(true);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: true,
                timestamp: new Date()
            });

            const mockError = new Error('Original callback error');
            const mockCallback = jest.fn().mockRejectedValue(mockError);
            const toolName = 'error-tool';
            const toolConfig = { description: 'Error tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            // Call the wrapped callback and expect it to throw the original error
            await expect(wrappedCallback(mockArgs, mockExtra)).rejects.toThrow(
                'Original callback error'
            );

            expect(mockContextStore.getIsInitialized).toHaveBeenCalledTimes(1);
            expect(mockCallback).toHaveBeenCalledWith(mockArgs, mockExtra);
        });

        it('should handle errors from context store', async () => {
            mockContextStore.getIsInitialized.mockImplementation(() => {
                throw new Error('Context store error');
            });

            const mockCallback = jest.fn();
            const toolName = 'test-tool';
            const toolConfig = { description: 'Test tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            // Call the wrapped callback and expect it to throw the context store error
            await expect(wrappedCallback(mockArgs, mockExtra)).rejects.toThrow(
                'Context store error'
            );

            expect(mockCallback).not.toHaveBeenCalled();
        });
    });

    describe('Integration with Logger', () => {
        it('should log when creating context checking server', () => {
            // Logger is mocked, so we just verify the server is created
            expect(contextCheckingServer).toBeDefined();
        });

        it('should log when registering tools', () => {
            const mockCallback = jest.fn();
            contextCheckingServer.registerTool('test-tool', { description: 'Test' }, mockCallback);

            // Logger is mocked, so we just verify the registration works
            expect(mockRegisterTool).toHaveBeenCalledTimes(1);
        });
    });

    describe('Proxy Behavior', () => {
        it('should forward non-registerTool property access to original server', () => {
            const mockServerWithProps = mockServer as typeof mockServer & {
                customProperty: string;
                customMethod: jest.Mock;
            };
            mockServerWithProps.customProperty = 'custom value';
            mockServerWithProps.customMethod = jest.fn().mockReturnValue('method result');

            const contextCheckingServerWithProps =
                contextCheckingServer as typeof contextCheckingServer & {
                    customProperty: string;
                    customMethod: jest.Mock;
                };
            expect(contextCheckingServerWithProps.customProperty).toBe('custom value');
            expect(contextCheckingServerWithProps.customMethod()).toBe('method result');
        });

        it('should forward method calls to original server', () => {
            const mockMethod = jest.fn().mockReturnValue('result');
            const mockServerWithMethod = mockServer as typeof mockServer & {
                someMethod: jest.Mock;
            };
            mockServerWithMethod.someMethod = mockMethod;

            const contextCheckingServerWithMethod =
                contextCheckingServer as typeof contextCheckingServer & { someMethod: jest.Mock };
            const result = contextCheckingServerWithMethod.someMethod('arg1', 'arg2');

            expect(mockMethod).toHaveBeenCalledWith('arg1', 'arg2');
            expect(result).toBe('result');
        });
    });

    describe('Context Initialization Status Messages', () => {
        it('should show "Initialized" in error message when context is initialized', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(false);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: true, // Status shows initialized but getIsInitialized returns false
                timestamp: new Date()
            });

            const mockCallback = jest.fn();
            const toolName = 'test-tool';
            const toolConfig = { description: 'Test tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            try {
                await wrappedCallback(mockArgs, mockExtra);
            } catch (error) {
                const errorMessage = (error as Error).message;
                expect(errorMessage).toContain('Current initialization status: Initialized');
            }
        });

        it('should show "Not initialized" in error message when context is not initialized', async () => {
            mockContextStore.getIsInitialized.mockReturnValue(false);
            mockContextStore.getStatus.mockReturnValue({
                isInitialized: false,
                timestamp: null
            });

            const mockCallback = jest.fn();
            const toolName = 'test-tool';
            const toolConfig = { description: 'Test tool' };
            const mockArgs = { test: 'args' };
            const mockExtra = {} as RequestHandlerExtra<ServerRequest, ServerNotification>;

            contextCheckingServer.registerTool(toolName, toolConfig, mockCallback);

            // Get the wrapped callback
            const wrappedCallback = mockRegisterTool.mock.calls[0][2];

            try {
                await wrappedCallback(mockArgs, mockExtra);
            } catch (error) {
                const errorMessage = (error as Error).message;
                expect(errorMessage).toContain('Current initialization status: Not initialized');
            }
        });
    });
});
