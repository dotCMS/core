import { listenBlockEditorInlineEvent } from './traditional-vtl.utils';

import { CLIENT_ACTIONS } from '../models/client.model';

describe('Traditional VTL utils', () => {
    describe('listenBlockEditorInlineEvent', () => {
        beforeEach(() => {
            document.body.innerHTML = '';
            jest.resetAllMocks();
        });

        it('should add event listeners to nodes with data-block-editor-content attribute', () => {
            document.body.innerHTML = `
                <div data-block-editor-content="true"></div>
                <div data-block-editor-content="true"></div>
            `;

            const postMessageMock = jest.fn();
            window.parent.postMessage = postMessageMock;

            listenBlockEditorInlineEvent();

            const nodes = document.querySelectorAll('[data-block-editor-content]');
            nodes.forEach((node) => {
                expect(node.classList.contains('dotcms__inline-edit-field')).toBe(true);
                node.dispatchEvent(new Event('click'));
            });

            expect(postMessageMock).toHaveBeenCalledTimes(nodes.length);
            nodes.forEach((node) => {
                expect(postMessageMock).toHaveBeenCalledWith(
                    {
                        payload: Object.assign({}, (node as HTMLElement).dataset),
                        action: CLIENT_ACTIONS.INIT_BLOCK_EDITOR_INLINE_EDITING
                    },
                    '*'
                );
            });
        });

        it('should call listenBlockEditorClick on window load if document is not ready', () => {
            Object.defineProperty(document, 'readyState', {
                value: 'loading',
                writable: true
            });

            const addEventListenerMock = jest.spyOn(window, 'addEventListener');

            listenBlockEditorInlineEvent();

            expect(addEventListenerMock).toHaveBeenCalledWith('load', expect.any(Function));
        });
    });
});
