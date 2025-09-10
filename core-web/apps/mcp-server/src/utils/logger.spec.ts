jest.unmock('./logger');
import { Logger } from './logger';

describe('Logger', () => {
    let logger: Logger;
    let stderrSpy: jest.SpyInstance;
    let originalEnv: string | undefined;

    beforeEach(() => {
        // Save original environment
        originalEnv = process.env.VERBOSE;

        // Create spy for stderr.write
        stderrSpy = jest.spyOn(process.stderr, 'write').mockImplementation(() => true);

        // Create logger instance
        logger = new Logger('TEST_CONTEXT');
    });

    afterEach(() => {
        // Restore original environment
        if (originalEnv === undefined) {
            delete process.env.VERBOSE;
        } else {
            process.env.VERBOSE = originalEnv;
        }

        // Restore stderr.write
        stderrSpy.mockRestore();
    });

    describe('constructor', () => {
        it('should create logger with context and non-verbose mode by default', () => {
            expect(logger).toBeDefined();
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((logger as any).context).toBe('TEST_CONTEXT');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((logger as any).verbose).toBe(false);
        });

        it('should enable verbose mode when VERBOSE environment variable is true', () => {
            process.env.VERBOSE = 'true';
            const verboseLogger = new Logger('VERBOSE_TEST');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((verboseLogger as any).verbose).toBe(true);
        });

        it('should not enable verbose mode for non-true VERBOSE values', () => {
            process.env.VERBOSE = 'false';
            const nonVerboseLogger = new Logger('NON_VERBOSE_TEST');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((nonVerboseLogger as any).verbose).toBe(false);
        });
    });

    describe('log', () => {
        it('should log message without data', () => {
            logger.log('Test message');

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('[TEST_CONTEXT]');
            expect(logCall).toContain('Test message');
            expect(logCall).toMatch(/^\[\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z\]/);
        });

        it('should log message with data in verbose mode', () => {
            process.env.VERBOSE = 'true';
            const verboseLogger = new Logger('VERBOSE_TEST');
            const testData = { key: 'value', number: 42 };

            verboseLogger.log('Test message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('Test message');
            expect(logCall).toContain(JSON.stringify(testData, null, 2));
        });

        it('should not log data in non-verbose mode', () => {
            const testData = { key: 'value', number: 42 };
            logger.log('Test message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('Test message');
            expect(logCall).not.toContain(JSON.stringify(testData, null, 2));
        });
    });

    describe('error', () => {
        it('should log error with Error object', () => {
            const testError = new Error('Test error message');
            testError.stack = 'Error stack trace';

            logger.error('Operation failed', testError);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('ERROR: Operation failed');
            expect(logCall).toContain('Test error message');
            expect(logCall).toContain('Error stack trace');
        });

        it('should log error with string error', () => {
            logger.error('Operation failed', 'String error');

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('ERROR: Operation failed');
            expect(logCall).toContain('String error');
        });

        it('should log error with unknown error type', () => {
            const unknownError = { custom: 'error', code: 500 };
            logger.error('Operation failed', unknownError);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('ERROR: Operation failed');
            expect(logCall).toContain(JSON.stringify(unknownError, null, 2));
        });

        it('should always log error data regardless of verbose mode', () => {
            const testError = new Error('Test error');
            logger.error('Operation failed', testError);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('Test error');
        });
    });

    describe('warn', () => {
        it('should log warning message without data', () => {
            logger.warn('Warning message');

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('WARN: Warning message');
        });

        it('should log warning with data in verbose mode', () => {
            process.env.VERBOSE = 'true';
            const verboseLogger = new Logger('VERBOSE_TEST');
            const testData = { warning: 'details' };

            verboseLogger.warn('Warning message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('WARN: Warning message');
            expect(logCall).toContain(JSON.stringify(testData, null, 2));
        });

        it('should not log data in non-verbose mode', () => {
            const testData = { warning: 'details' };
            logger.warn('Warning message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('WARN: Warning message');
            expect(logCall).not.toContain(JSON.stringify(testData, null, 2));
        });
    });

    describe('debug', () => {
        it('should log debug message without data', () => {
            logger.debug('Debug message');

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('DEBUG: Debug message');
        });

        it('should log debug with data in verbose mode', () => {
            process.env.VERBOSE = 'true';
            const verboseLogger = new Logger('VERBOSE_TEST');
            const testData = { debug: 'info' };

            verboseLogger.debug('Debug message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('DEBUG: Debug message');
            expect(logCall).toContain(JSON.stringify(testData, null, 2));
        });

        it('should not log data in non-verbose mode', () => {
            const testData = { debug: 'info' };
            logger.debug('Debug message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('DEBUG: Debug message');
            expect(logCall).not.toContain(JSON.stringify(testData, null, 2));
        });
    });

    describe('logWithData', () => {
        it('should always log data regardless of verbose mode', () => {
            const testData = { important: 'data', value: 123 };
            logger.logWithData('Important message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('Important message');
            expect(logCall).toContain(JSON.stringify(testData, null, 2));
        });

        it('should format data properly', () => {
            const testData = { nested: { object: 'value' }, array: [1, 2, 3] };
            logger.logWithData('Data message', testData);

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            expect(logCall).toContain('"nested"');
            expect(logCall).toContain('"object": "value"');
            expect(logCall).toContain('"array"');
        });
    });

    describe('timestamp formatting', () => {
        it('should use ISO format for timestamps', () => {
            logger.log('Test message');

            expect(stderrSpy).toHaveBeenCalledTimes(1);
            const logCall = stderrSpy.mock.calls[0][0];
            const timestampRegex = /^\[\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z\]/;
            expect(logCall).toMatch(timestampRegex);
        });
    });

    describe('context formatting', () => {
        it('should include context in all log messages', () => {
            logger.log('Test message');
            logger.error('Test error', new Error('test'));
            logger.warn('Test warning');
            logger.debug('Test debug');
            logger.logWithData('Test data', { key: 'value' });

            expect(stderrSpy).toHaveBeenCalledTimes(5);
            stderrSpy.mock.calls.forEach((call) => {
                expect(call[0]).toContain('[TEST_CONTEXT]');
            });
        });
    });
});
