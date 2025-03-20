import { describe, it, expect, beforeAll, beforeEach, afterEach } from '@jest/globals';

import { UVE_MODE } from './types';
import { getUVEState, createUVESubscription } from './utils';

import { __NOTIFY_CLIENT__ } from '../internal';

describe('getUVEStatus', () => {
    beforeAll(() => {
        jest.spyOn(global, 'window', 'get').mockReset();
    });

    it('should return undefined when not in editor', () => {
        const mockWindow = {
            ...window,
            parent: window
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        expect(getUVEState()).toBe(undefined);
    });

    it('should return undefined when window is undefined', () => {
        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(undefined as unknown as Window & typeof globalThis);

        expect(getUVEState()).toBe(undefined);
    });

    it('should return edit mode when in editor with edit parameter', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=EDIT_MODE'
            }
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null
        });
    });

    it('should return preview mode when in editor with preview parameter', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=PREVIEW_MODE'
            }
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.PREVIEW,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null
        });
    });

    it('should return live mode when in editor with live parameter', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=LIVE'
            }
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.LIVE,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null
        });
    });

    it('should return mode as edit when the mode parameter is missing', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello'
            }
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null
        });
    });
});

it('should set the mode to edit when the mode parameter is invalid', () => {
    const mockWindow = {
        ...window,
        parent: {
            ...window
        },
        location: {
            href: 'https://test.com/hello?mode=IM_TRYING_TO_BREAK_IT'
        }
    };

    const spy = jest.spyOn(global, 'window', 'get');
    spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

    getUVEState();

    expect(getUVEState()).toEqual({
        mode: UVE_MODE.EDIT,
        persona: null,
        variantName: null,
        experimentId: null,
        publishDate: null,
        languageId: null
    });
});

it('should parse all URL parameters correctly', () => {
    const mockWindow = {
        ...window,
        parent: {
            ...window
        },
        location: {
            href: 'https://test.com/hello?mode=EDIT_MODE&personaId=mobile&variantName=test-variant&experimentId=exp-123&publishDate=2024-03-20&language_id=en-US'
        }
    };

    const spy = jest.spyOn(global, 'window', 'get');
    spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

    expect(getUVEState()).toEqual({
        mode: UVE_MODE.EDIT,
        persona: 'mobile',
        variantName: 'test-variant',
        experimentId: 'exp-123',
        publishDate: '2024-03-20',
        languageId: 'en-US'
    });
});

it('should handle partial URL parameters', () => {
    const mockWindow = {
        ...window,
        parent: {
            ...window
        },
        location: {
            href: 'https://test.com/hello?mode=PREVIEW_MODE&personaId=desktop'
        }
    };

    const spy = jest.spyOn(global, 'window', 'get');
    spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

    expect(getUVEState()).toEqual({
        mode: UVE_MODE.PREVIEW,
        persona: 'desktop',
        variantName: null,
        experimentId: null,
        publishDate: null,
        languageId: null
    });
});

it('should handle URL encoded parameters with variant and experiment', () => {
    const mockWindow = {
        ...window,
        parent: {
            ...window
        },
        location: {
            href: 'https://test.com/hello?mode=LIVE&variantName=test%20variant&experimentId=exp%2D123&language_id=en%2DUS'
        }
    };

    const spy = jest.spyOn(global, 'window', 'get');
    spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

    expect(getUVEState()).toEqual({
        mode: UVE_MODE.LIVE,
        persona: null,
        variantName: 'test variant',
        experimentId: 'exp-123',
        publishDate: null,
        languageId: 'en-US'
    });
});

it('should handle variantName and experimentId being provided together', () => {
    const mockWindow = {
        ...window,
        parent: {
            ...window
        },
        location: {
            href: 'https://test.com/hello?mode=LIVE&variantName=test-variant&experimentId=exp-123'
        }
    };

    const spy = jest.spyOn(global, 'window', 'get');
    spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

    expect(getUVEState()).toEqual({
        mode: UVE_MODE.LIVE,
        persona: null,
        variantName: 'test-variant',
        experimentId: 'exp-123',
        publishDate: null,
        languageId: null
    });
});

describe('createUVESubscription', () => {
    let mockWindow: unknown;
    let consoleWarnSpy: jest.SpyInstance;
    let consoleErrorSpy: jest.SpyInstance;

    beforeAll(() => {
        jest.spyOn(global, 'window', 'get').mockReset();
    });

    beforeEach(() => {
        consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    });

    afterEach(() => {
        consoleWarnSpy.mockRestore();
        consoleErrorSpy.mockRestore();
    });

    const noop = () => {
        /* do nothing */
    };

    it('should log warning when not running inside UVE', () => {
        const mockWindow = {
            ...window,
            parent: window
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        const subscription = createUVESubscription('test-event', noop);

        expect(consoleWarnSpy).toHaveBeenCalledWith('UVE Subscription: Not running inside UVE');
        expect(subscription).toEqual({
            unsubscribe: expect.any(Function),
            event: 'test-event'
        });
    });

    it('should log error when event is not found', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=EDIT_MODE'
            }
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

        const subscription = createUVESubscription('non-existent-event', noop);

        expect(consoleErrorSpy).toHaveBeenCalledWith(
            'UVE Subscription: Event non-existent-event not found'
        );
        expect(subscription).toEqual({
            unsubscribe: expect.any(Function),
            event: 'non-existent-event'
        });
    });

    it('should create a valid subscription for changes event', () => {
        mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=EDIT_MODE'
            },
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            postMessage: jest.fn()
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as Window & typeof globalThis);

        const callback = jest.fn();
        const subscription = createUVESubscription('changes', callback);

        expect(subscription).toBeDefined();
        expect(subscription.event).toBe('changes');
        expect(subscription.unsubscribe).toBeDefined();
        expect((mockWindow as Window).addEventListener).toHaveBeenCalledWith(
            'message',
            expect.any(Function as unknown as (event: MessageEvent) => void)
        );
    });

    it('should handle message events correctly', () => {
        mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=EDIT_MODE'
            },
            addEventListener: jest.fn(),
            removeEventListener: jest.fn()
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as Window & typeof globalThis);

        const callback = jest.fn();
        createUVESubscription('changes', callback);

        // Get the message event listener that was registered
        const messageCallback = ((mockWindow as Window).addEventListener as jest.Mock).mock
            .calls[0][1];

        // Create and dispatch a message event
        const messageEvent = new MessageEvent('message', {
            data: {
                name: __NOTIFY_CLIENT__.UVE_SET_PAGE_DATA,
                payload: { test: 'data' }
            }
        });

        messageCallback(messageEvent);

        expect(callback).toHaveBeenCalledWith({ test: 'data' });
    });

    it('should properly unsubscribe from events', () => {
        mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=EDIT_MODE'
            },
            addEventListener: jest.fn(),
            removeEventListener: jest.fn()
        };

        const spy = jest.spyOn(global, 'window', 'get');
        spy.mockReturnValue(mockWindow as Window & typeof globalThis);

        const callback = jest.fn();
        const subscription = createUVESubscription('changes', callback);

        const messageCallback = ((mockWindow as Window).addEventListener as jest.Mock).mock
            .calls[0][1]; // Get the second argument (1) of the first call (0)
        subscription.unsubscribe();
        expect((mockWindow as Window).removeEventListener).toHaveBeenCalledWith(
            'message',
            messageCallback
        );
    });
});
