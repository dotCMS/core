import {
    formatResponse,
    createSuccessResponse,
    createErrorResponse,
    executeWithErrorHandling,
    type McpSuccessResponse,
    type McpErrorResponse,
    type McpResponse
} from './response';

describe('Response Utils', () => {
    describe('formatResponse', () => {
        it('should return message only when no data provided', () => {
            const result = formatResponse('Simple message');
            expect(result).toBe('Simple message');
        });

        it('should return message with undefined data', () => {
            const result = formatResponse('Simple message', undefined);
            expect(result).toBe('Simple message');
        });

        it('should format string data correctly', () => {
            const result = formatResponse('Message', 'Additional info');
            expect(result).toBe('Message\n\nAdditional info');
        });

        it('should format object data correctly', () => {
            const data = { key: 'value', number: 42 };
            const result = formatResponse('Message', data);

            expect(result).toContain('Message');
            expect(result).toContain('Details:');
            expect(result).toContain('"key": "value"');
            expect(result).toContain('"number": 42');
        });

        it('should truncate long object data', () => {
            const largeData = {
                longString: 'a'.repeat(1000),
                moreData: 'b'.repeat(1000)
            };
            const result = formatResponse('Message', largeData, { maxDataLength: 100 });

            expect(result).toContain('Message');
            expect(result).toContain('[truncated]');
            expect(result.length).toBeLessThan(200); // Should be truncated
        });

        it('should not truncate short object data', () => {
            const shortData = { key: 'value' };
            const result = formatResponse('Message', shortData, { maxDataLength: 1000 });

            expect(result).toContain('Message');
            expect(result).not.toContain('[truncated]');
            expect(result).toContain('"key": "value"');
        });

        it('should handle non-object, non-string data types', () => {
            const result = formatResponse('Message', 42);
            expect(result).toBe('Message\n\nData: 42');
        });

        it('should handle boolean data', () => {
            const result = formatResponse('Message', true);
            expect(result).toBe('Message\n\nData: true');
        });

        it('should handle null data', () => {
            const result = formatResponse('Message', null);
            expect(result).toBe('Message');
        });

        it('should use default options when not provided', () => {
            const largeData = { data: 'x'.repeat(2000) };
            const result = formatResponse('Message', largeData);

            expect(result).toContain('[truncated]');
        });

        it('should respect includeRawData option', () => {
            const data = { key: 'value' };
            const result = formatResponse('Message', data, { includeRawData: true });

            expect(result).toContain('Message');
            expect(result).toContain('Details:');
            expect(result).toContain('"key": "value"');
        });
    });

    describe('createSuccessResponse', () => {
        it('should create success response without data', () => {
            const result = createSuccessResponse('Success message');

            expect(result).toEqual({
                content: [
                    {
                        type: 'text',
                        text: 'Success message'
                    }
                ]
            });
        });

        it('should create success response with data', () => {
            const data = { id: 1, name: 'Test' };
            const result = createSuccessResponse('Success message', data);

            expect(result.content).toHaveLength(1);
            expect(result.content[0].type).toBe('text');
            expect(result.content[0].text).toContain('Success message');
            expect(result.content[0].text).toContain('"id": 1');
            expect(result.content[0].text).toContain('"name": "Test"');
        });

        it('should create success response with string data', () => {
            const result = createSuccessResponse('Success message', 'Additional info');

            expect(result.content[0].text).toBe('Success message\n\nAdditional info');
        });

        it('should not have isError property', () => {
            const result = createSuccessResponse('Success message');

            expect(result).not.toHaveProperty('isError');
        });

        it('should work with typed data', () => {
            interface TestData {
                id: number;
                name: string;
            }
            const data: TestData = { id: 1, name: 'Test' };
            const result = createSuccessResponse<TestData>('Success message', data);

            expect(result.content[0].text).toContain('"id": 1');
        });
    });

    describe('createErrorResponse', () => {
        it('should create error response from Error object', () => {
            const error = new Error('Test error message');
            const result = createErrorResponse('Operation failed', error);

            expect(result).toEqual({
                isError: true,
                content: [
                    {
                        type: 'text',
                        text: 'Operation failed: Test error message'
                    }
                ]
            });
        });

        it('should create error response from string error', () => {
            const result = createErrorResponse('Operation failed', 'String error');

            expect(result).toEqual({
                isError: true,
                content: [
                    {
                        type: 'text',
                        text: 'Operation failed: String error'
                    }
                ]
            });
        });

        it('should create error response from object with message property', () => {
            const error = { message: 'Custom error message', code: 500 };
            const result = createErrorResponse('Operation failed', error);

            expect(result.content[0].text).toBe('Operation failed: Custom error message');
        });

        it('should handle unknown error types', () => {
            const error = { custom: 'error', code: 500 };
            const result = createErrorResponse('Operation failed', error);

            expect(result.content[0].text).toBe('Operation failed: Unknown error occurred');
        });

        it('should handle null error', () => {
            const result = createErrorResponse('Operation failed', null);

            expect(result.content[0].text).toBe('Operation failed: Unknown error occurred');
        });

        it('should handle undefined error', () => {
            const result = createErrorResponse('Operation failed', undefined);

            expect(result.content[0].text).toBe('Operation failed: Unknown error occurred');
        });

        it('should always have isError property set to true', () => {
            const result = createErrorResponse('Operation failed', 'error');

            expect(result.isError).toBe(true);
        });
    });

    describe('executeWithErrorHandling', () => {
        it('should return success response when operation succeeds', async () => {
            const successResponse: McpSuccessResponse = {
                content: [{ type: 'text', text: 'Operation succeeded' }]
            };

            const operation = jest.fn().mockResolvedValue(successResponse);
            const result = await executeWithErrorHandling(operation, 'Test operation failed');

            expect(result).toEqual(successResponse);
            expect(operation).toHaveBeenCalledTimes(1);
        });

        it('should return error response when operation throws Error', async () => {
            const error = new Error('Test error');
            const operation = jest.fn().mockRejectedValue(error);

            const result = await executeWithErrorHandling(operation, 'Test operation failed');

            expect(result).toEqual({
                isError: true,
                content: [
                    {
                        type: 'text',
                        text: 'Test operation failed: Test error'
                    }
                ]
            });
            expect(operation).toHaveBeenCalledTimes(1);
        });

        it('should return error response when operation throws string', async () => {
            const operation = jest.fn().mockRejectedValue('String error');

            const result = await executeWithErrorHandling(operation, 'Test operation failed');

            expect(result).toEqual({
                isError: true,
                content: [
                    {
                        type: 'text',
                        text: 'Test operation failed: String error'
                    }
                ]
            });
        });

        it('should return error response when operation throws unknown error', async () => {
            const operation = jest.fn().mockRejectedValue({ custom: 'error' });

            const result = await executeWithErrorHandling(operation, 'Test operation failed');

            expect(result).toEqual({
                isError: true,
                content: [
                    {
                        type: 'text',
                        text: 'Test operation failed: Unknown error occurred'
                    }
                ]
            });
        });

        it('should handle async operations correctly', async () => {
            const operation = jest.fn().mockImplementation(async () => {
                await new Promise((resolve) => setTimeout(resolve, 10));

                return createSuccessResponse('Async operation complete');
            });

            const result = await executeWithErrorHandling(operation, 'Async operation failed');

            expect(result.content[0].text).toBe('Async operation complete');
            expect(operation).toHaveBeenCalledTimes(1);
        });

        it('should preserve error response format', async () => {
            const errorResponse: McpErrorResponse = {
                isError: true,
                content: [{ type: 'text', text: 'Custom error response' }]
            };

            const operation = jest.fn().mockResolvedValue(errorResponse);
            const result = await executeWithErrorHandling(operation, 'Test operation failed');

            expect(result).toEqual(errorResponse);
        });
    });

    describe('Type definitions', () => {
        it('should properly type McpSuccessResponse', () => {
            const successResponse: McpSuccessResponse = {
                content: [{ type: 'text', text: 'Success' }],
                customProperty: 'allowed'
            };

            expect(successResponse.content).toHaveLength(1);
            expect(successResponse.content[0].type).toBe('text');
            expect(successResponse.customProperty).toBe('allowed');
        });

        it('should properly type McpErrorResponse', () => {
            const errorResponse: McpErrorResponse = {
                isError: true,
                content: [{ type: 'text', text: 'Error' }],
                customProperty: 'allowed'
            };

            expect(errorResponse.isError).toBe(true);
            expect(errorResponse.content).toHaveLength(1);
            expect(errorResponse.customProperty).toBe('allowed');
        });

        it('should allow union type for McpResponse', () => {
            const responses: McpResponse[] = [
                createSuccessResponse('Success'),
                createErrorResponse('Error', 'test')
            ];

            expect(responses).toHaveLength(2);
            expect('isError' in responses[1]).toBe(true);
            expect('isError' in responses[0]).toBe(false);
        });
    });
});
