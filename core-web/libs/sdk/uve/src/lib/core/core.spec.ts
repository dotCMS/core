import { MockInstance, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UVE_MODE, UVEEventType } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import { createUVESubscription, getUVEState, isRequestFromUVE } from './core.utils';

/**
 * Vitest's jsdom environment installs `window` on globalThis as a NON-configurable
 * getter, so `vi.spyOn(global, 'window', 'get')` throws "Cannot redefine property:
 * window" and took this whole file down. Jest's jsdom left it configurable, which is
 * why swapping the entire window worked there.
 *
 * These tests only ever vary two things, and both are reachable on the real window:
 * `parent` is a configurable accessor, and the URL is settable through
 * `history.replaceState`. `location` itself is NOT configurable and must not be
 * stubbed. `replaceState` is same-origin only, so the path stays relative — every
 * assertion reads query parameters, never the origin.
 *
 * The `window === undefined` branch cannot be expressed in a DOM environment at all;
 * it lives in core.no-window.spec.ts, which runs under the node environment.
 */
const enterUVE = (search = ''): void => {
    vi.spyOn(window, 'parent', 'get').mockReturnValue({} as Window);
    window.history.replaceState({}, '', `/hello${search}`);
};

afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, '', '/');
});

describe('getUVEStatus', () => {
    it('should return undefined when not in editor', () => {
        // Left as jsdom found it: a top-level page is its own `parent`, which is
        // exactly the "not inside UVE" condition.

        expect(getUVEState()).toBe(undefined);
    });

    it('should return edit mode when in editor with edit parameter', () => {
        enterUVE('?mode=EDIT_MODE');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should return preview mode when in editor with preview parameter', () => {
        enterUVE('?mode=PREVIEW_MODE');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.PREVIEW,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should return live mode when in editor with live parameter', () => {
        enterUVE('?mode=LIVE');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.LIVE,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should return mode as edit when the mode parameter is missing', () => {
        enterUVE('');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should set the mode to edit when the mode parameter is invalid', () => {
        enterUVE('?mode=IM_TRYING_TO_BREAK_IT');

        getUVEState();

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should parse all URL parameters correctly', () => {
        enterUVE(
            '?mode=EDIT_MODE&personaId=mobile&variantName=test-variant&experimentId=exp-123&publishDate=2024-03-20&language_id=en-US&dotCMSHost=https://test.com'
        );

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.EDIT,
            persona: 'mobile',
            variantName: 'test-variant',
            experimentId: 'exp-123',
            publishDate: '2024-03-20',
            languageId: 'en-US',
            dotCMSHost: 'https://test.com'
        });
    });

    it('should handle partial URL parameters', () => {
        enterUVE('?mode=PREVIEW_MODE&personaId=desktop&dotCMSHost=https://test.com');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.PREVIEW,
            persona: 'desktop',
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: 'https://test.com'
        });
    });

    it('should handle URL encoded parameters with variant and experiment', () => {
        enterUVE(
            '?mode=LIVE&variantName=test%20variant&experimentId=exp%2D123&language_id=en%2DUS&dotCMSHost=https://test.com'
        );

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.LIVE,
            persona: null,
            variantName: 'test variant',
            experimentId: 'exp-123',
            publishDate: null,
            languageId: 'en-US',
            dotCMSHost: 'https://test.com'
        });
    });

    it('should handle variantName and experimentId being provided together', () => {
        enterUVE('?mode=LIVE&variantName=test-variant&experimentId=exp-123');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.LIVE,
            persona: null,
            variantName: 'test-variant',
            experimentId: 'exp-123',
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });
    });

    it('should handle dotCMSHost parameter', () => {
        enterUVE('?mode=LIVE&dotCMSHost=https://test.com');

        expect(getUVEState()).toEqual({
            mode: UVE_MODE.LIVE,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: 'https://test.com'
        });
    });
});

describe('createUVESubscription', () => {
    let consoleWarnSpy: MockInstance;
    let consoleErrorSpy: MockInstance;

    beforeEach(() => {
        consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation();
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation();
    });

    afterEach(() => {
        consoleWarnSpy.mockRestore();
        consoleErrorSpy.mockRestore();
    });

    const noop = () => {
        /* do nothing */
    };

    it('should log warning when not running inside UVE', () => {
        // Left as jsdom found it: a top-level page is its own `parent`, which is
        // exactly the "not inside UVE" condition.

        const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, noop);

        expect(consoleWarnSpy).toHaveBeenCalledWith('UVE Subscription: Not running inside UVE');
        expect(subscription).toEqual({
            unsubscribe: expect.any(Function),
            event: UVEEventType.CONTENT_CHANGES
        });
    });

    it('should log error when event is not found', () => {
        enterUVE('?mode=EDIT_MODE');

        const subscription = createUVESubscription('non-existent-event' as UVEEventType, noop);

        expect(consoleErrorSpy).toHaveBeenCalledWith(
            'UVE Subscription: Event non-existent-event not found'
        );
        expect(subscription).toEqual({
            unsubscribe: expect.any(Function),
            event: 'non-existent-event'
        });
    });

    it('should create a valid subscription for changes event', () => {
        enterUVE('?mode=EDIT_MODE');
        const addEventListener = vi.spyOn(window, 'addEventListener');

        const callback = vi.fn();
        const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, callback);

        expect(subscription).toBeDefined();
        expect(subscription.event).toBe('changes');
        expect(subscription.unsubscribe).toBeDefined();
        expect(addEventListener).toHaveBeenCalledWith(
            'message',
            expect.any(Function as unknown as (event: MessageEvent) => void)
        );
    });

    it('should handle message events correctly', () => {
        enterUVE('?mode=EDIT_MODE');
        const addEventListener = vi.spyOn(window, 'addEventListener');

        const callback = vi.fn();
        createUVESubscription(UVEEventType.CONTENT_CHANGES, callback);

        // Get the message event listener that was registered
        const messageCallback = addEventListener.mock.calls[0][1];

        // Create and dispatch a message event
        const messageEvent = new MessageEvent('message', {
            data: {
                name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
                payload: { test: 'data' }
            }
        });

        messageCallback(messageEvent);

        expect(callback).toHaveBeenCalledWith({ test: 'data' });
    });

    it('should properly unsubscribe from events', () => {
        enterUVE('?mode=EDIT_MODE');
        const addEventListener = vi.spyOn(window, 'addEventListener');
        const removeEventListener = vi.spyOn(window, 'removeEventListener');

        const callback = vi.fn();
        const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, callback);

        const messageCallback = addEventListener.mock.calls[0][1]; // Get the second argument (1) of the first call (0)
        subscription.unsubscribe?.();
        expect(removeEventListener).toHaveBeenCalledWith('message', messageCallback);
    });
});

describe('isRequestFromUVE', () => {
    it('should return true when dotCMSHost is present', () => {
        expect(isRequestFromUVE({ dotCMSHost: 'https://demo.dotcms.com' })).toBe(true);
    });

    it('should return false when dotCMSHost is absent', () => {
        expect(isRequestFromUVE({})).toBe(false);
    });

    it('should return false when dotCMSHost is an empty string', () => {
        expect(isRequestFromUVE({ dotCMSHost: '' })).toBe(false);
    });
});
