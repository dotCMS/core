import { getUVEState } from '@dotcms/uve';

import {
    listenEditorMessages,
    listenHoveredContentlet,
    fetchPageDataFromInsideUVE,
    scrollHandler
} from './listeners/listeners';
import { postMessageToEditor, CLIENT_ACTIONS } from './models/client.model';
import {
    addClassToEmptyContentlets,
    initEditor,
    initInlineEditing,
    updateNavigation
} from './sdk-editor';

jest.mock('./models/client.model', () => {
    return {
        postMessageToEditor: jest.fn(),
        CLIENT_ACTIONS: {
            NAVIGATION_UPDATE: 'set-url',
            SET_BOUNDS: 'set-bounds',
            SET_CONTENTLET: 'set-contentlet',
            EDIT_CONTENTLET: 'edit-contentlet',
            IFRAME_SCROLL: 'scroll',
            PING_EDITOR: 'ping-editor',
            CONTENT_CHANGE: 'content-change',
            NOOP: 'noop'
        },
        INITIAL_DOT_UVE: {
            editContentlet: jest.fn(),
            reorderMenu: jest.fn(),
            lastScrollYPosition: 0
        }
    };
});

jest.mock('./listeners/listeners', () => ({
    pingEditor: jest.fn(),
    listenEditorMessages: jest.fn(),
    listenHoveredContentlet: jest.fn(),
    scrollHandler: jest.fn(),
    listenContentChange: jest.fn(),
    fetchPageDataFromInsideUVE: jest.fn()
}));

describe('DotCMSPageEditor', () => {
    describe('is NOT inside editor', () => {
        describe('same window parent', () => {
            beforeEach(() => {
                const mockWindow = {
                    ...window,
                    parent: window
                };

                const spy = jest.spyOn(global, 'window', 'get');
                spy.mockReturnValueOnce(mockWindow as unknown as Window & typeof globalThis);
            });

            afterEach(() => {
                jest.clearAllMocks();
            });

            it('should initialize without any listener', () => {
                const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

                expect(addEventListenerSpy).not.toHaveBeenCalled();
            });

            it('should initialize UVEState as undefined', () => {
                expect(getUVEState()).toBe(undefined);
            });
        });

        describe('No window', () => {
            beforeEach(() => {
                const mockWindow = undefined;

                const spy = jest.spyOn(global, 'window', 'get');
                spy.mockReturnValueOnce(mockWindow as unknown as Window & typeof globalThis);
            });

            afterEach(() => {
                jest.clearAllMocks();
            });

            it('should initialize UVEState as undefined', () => {
                expect(getUVEState()).toBe(undefined);
            });
        });

        describe('No dotUVE', () => {
            beforeEach(() => {
                const mockWindow = {
                    ...window,
                    parent: {
                        ...window //Another reference
                    }
                };
                const spy = jest.spyOn(global, 'window', 'get');
                spy.mockReturnValueOnce(mockWindow as unknown as Window & typeof globalThis);
            });

            afterEach(() => {
                jest.clearAllMocks();
            });

            it('should initialize without any listener', () => {
                const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

                expect(addEventListenerSpy).not.toHaveBeenCalled();
            });
            it('should initialize UVEState as undefined', () => {
                expect(getUVEState()).toBe(undefined);
            });
        });
    });

    describe('is inside editor', () => {
        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: {
                    ...window
                },
                location: {
                    href: 'https://test.com/hello?editorMode=edit'
                },
                dotUVE: {
                    lastScrollPosition: 0
                }
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize properly', () => {
            expect(getUVEState()).toEqual({
                mode: 'edit'
            });
        });

        it('should update navigation', () => {
            updateNavigation('/');
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: 'index'
                }
            });
        });

        it('should init editor calling listeners', () => {
            initEditor({ pathname: 'some-url' });
            expect(fetchPageDataFromInsideUVE).toHaveBeenCalledWith('some-url');
            expect(listenEditorMessages).toHaveBeenCalled();
            expect(listenHoveredContentlet).toHaveBeenCalled();
            expect(scrollHandler).toHaveBeenCalled();
            expect(window.dotUVE).toEqual({
                editContentlet: expect.any(Function),
                reorderMenu: expect.any(Function),
                lastScrollYPosition: 0
            });
        });
    });

    describe('Add Class to Empty Contentets', () => {
        it('should add class to empty contentlets', () => {
            const contentlet = document.createElement('div');
            contentlet.setAttribute('data-dot-object', 'contentlet');
            Object.defineProperty(contentlet, 'clientHeight', { value: 100 }); // Emulate a contentlet with height in the DOM
            document.body.appendChild(contentlet);

            const emptyContentlet = document.createElement('div');
            emptyContentlet.setAttribute('data-dot-object', 'contentlet');
            document.body.appendChild(emptyContentlet);

            addClassToEmptyContentlets();

            expect(emptyContentlet.classList.contains('empty-contentlet')).toBe(true);
            expect(contentlet.classList.contains('empty-contentlet')).toBe(false);
        });
    });

    describe('initInlineEditing', () => {
        it('should send the correct message to the editor to edit `block-editor`', () => {
            const type = 'BLOCK_EDITOR';
            const data = {
                inode: '123',
                language: 1,
                contentType: 'text',
                fieldName: 'body',
                content: {}
            };

            initInlineEditing(type, data);

            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.INIT_INLINE_EDITING,
                payload: {
                    type,
                    data
                }
            });
        });

        it('should send the correct message to the editor to edit `WYSIWYG`', () => {
            const type = 'WYSIWYG';

            initInlineEditing(type);

            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.INIT_INLINE_EDITING,
                payload: {
                    type,
                    data: undefined
                }
            });
        });
    });

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
                    href: 'https://test.com/hello?editorMode=edit'
                }
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

            expect(getUVEState()).toEqual({
                mode: 'edit'
            });
        });

        it('should return preview mode when in editor with preview parameter', () => {
            const mockWindow = {
                ...window,
                parent: {
                    ...window
                },
                location: {
                    href: 'https://test.com/hello?editorMode=preview'
                }
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

            expect(getUVEState()).toEqual({
                mode: 'preview'
            });
        });

        it('should return live mode when in editor with live parameter', () => {
            const mockWindow = {
                ...window,
                parent: {
                    ...window
                },
                location: {
                    href: 'https://test.com/hello?editorMode=live'
                }
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);

            expect(getUVEState()).toEqual({
                mode: 'live'
            });
        });

        it('should return mode as unknown when the editorMode parameter is missing', () => {
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
                mode: 'unknown'
            });
        });

        it('should warn the user when the editorMode is unknown', () => {
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
                "Couldn't identify the current mode of UVE, please contact customer support."
            );
        });
    });
});
