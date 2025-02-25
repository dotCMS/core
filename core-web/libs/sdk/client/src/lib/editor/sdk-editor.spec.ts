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
    isInsideEditor,
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

            expect(isInsideEditor()).toBe(false);
            expect(addEventListenerSpy).not.toHaveBeenCalled();
        });
    });

    describe('is inside editor', () => {
        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: null
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize properly', () => {
            expect(isInsideEditor()).toBe(true);
        });

        it('should update navigation', () => {
            updateNavigation('/');
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: '/'
                }
            });
        });

        it('should update navigation if its empty', () => {
            updateNavigation('');
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: '/'
                }
            });
        });
        it('should update navigation if its undefined', () => {
            updateNavigation(undefined as unknown as string);
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CLIENT_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: '/'
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
});
