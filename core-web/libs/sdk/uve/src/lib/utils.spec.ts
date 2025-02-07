import { UVE_MODE } from './types';
import { getUVEState } from './utils';

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
                href: 'https://test.com/hello?mode=edit'
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
                href: 'https://test.com/hello?mode=preview'
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
                href: 'https://test.com/hello?mode=live'
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

    it('should return mode as unknown when the mode parameter is missing', () => {
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
            mode: UVE_MODE.UNKNOWN,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null
        });
    });

    it('should warn the user when the mode is unknown', () => {
        const consoleSpy = jest.spyOn(console, 'warn');
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

        getUVEState();

        expect(consoleSpy).toHaveBeenCalledWith(
            "Couldn't identify the current mode of UVE, please contact customer support. Mode: unknown"
        );
    });

    it('should warn the user when the mode is unknown', () => {
        const consoleSpy = jest.spyOn(console, 'warn');
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

        expect(consoleSpy).toHaveBeenCalledWith(
            "Couldn't identify the current mode of UVE, please contact customer support. Mode: IM_TRYING_TO_BREAK_IT"
        );
    });

    it('should parse all URL parameters correctly', () => {
        const mockWindow = {
            ...window,
            parent: {
                ...window
            },
            location: {
                href: 'https://test.com/hello?mode=edit&personaId=mobile&variantName=test-variant&experimentId=exp-123&publishDate=2024-03-20&language_id=en-US'
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
                href: 'https://test.com/hello?mode=preview&personaId=desktop'
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
                href: 'https://test.com/hello?mode=live&variantName=test%20variant&experimentId=exp%2D123&language_id=en%2DUS'
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
                href: 'https://test.com/hello?mode=live&variantName=test-variant&experimentId=exp-123'
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
});
