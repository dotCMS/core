import { listenBlockEditorInlineEvent } from './traditional-vtl.utils';

import * as client from '../sdk-editor';

describe('Traditional VTL utils', () => {
    describe('listenBlockEditorInlineEvent', () => {
        beforeEach(() => {
            document.body.innerHTML = '';
            jest.resetAllMocks();
        });

        it('should add event listeners to nodes with data-block-editor-content attribute', () => {
            document.body.innerHTML = `
                <div data-inode="123" data-language="1" data-content-type="blog" data-field-name="Content" data-block-editor-content="true"></div>
                <div data-inode="321" data-language="1" data-content-type="blog" data-field-name="Content" data-block-editor-content="true"></div>
            `;

            const spy = jest.spyOn(client, 'initInlineEditing');

            listenBlockEditorInlineEvent();

            const nodes: NodeListOf<HTMLElement> = document.querySelectorAll(
                '[data-block-editor-content]'
            );
            nodes.forEach((node) => {
                expect(node.classList.contains('dotcms__inline-edit-field')).toBe(true);
                node.dispatchEvent(new Event('click'));
            });

            expect(spy).toHaveBeenCalledTimes(nodes.length);
            nodes.forEach(({ dataset }) => {
                const { inode, language, contentType, fieldName, blockEditorContent } = dataset;
                const content = JSON.parse(blockEditorContent || '');

                expect(spy).toHaveBeenCalledWith('BLOCK_EDITOR', {
                    inode,
                    content,
                    language: parseInt(language as string),
                    fieldName,
                    contentType
                });
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
