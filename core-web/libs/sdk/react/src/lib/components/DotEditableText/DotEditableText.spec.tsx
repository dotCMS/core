import { expect } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react';
import * as tinymceReact from '@tinymce/tinymce-react';

import * as dotcmsClient from '@dotcms/client';

import { DotEditableText } from './DotEditableText';

import { dotcmsContentletMock } from '../../mocks/mockPageContext';

const { CUSTOMER_ACTIONS, postMessageToEditor } = dotcmsClient;

// Define mockEditor before using it in jest.mock
const TINYMCE_EDITOR_MOCK = {
    focus: () => {
        /* */
    },
    getContent: (_data: string) => '',
    isDirty: () => false,
    hasFocus: () => false,
    setContent: () => {
        /* */
    }
};

jest.mock('@tinymce/tinymce-react', () => ({
    Editor: jest.fn(({ onInit, onMouseDown, onFocusOut }) => {
        onInit({}, TINYMCE_EDITOR_MOCK);

        return <div data-testid="tinymce-editor" onMouseDown={onMouseDown} onBlur={onFocusOut} />;
    })
}));

// Mock @dotcms/client module
jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: jest.fn().mockImplementation(() => true),
    postMessageToEditor: jest.fn(),
    DotCmsClient: {
        dotcmsUrl: 'http://localhost:8080'
    }
}));

const mockedDotcmsClient = dotcmsClient as jest.Mocked<typeof dotcmsClient>;
const { Editor } = tinymceReact as jest.Mocked<typeof tinymceReact>;

describe('DotEditableText', () => {
    describe('Outside editor', () => {
        beforeEach(() => {
            mockedDotcmsClient.isInsideEditor.mockReturnValue(false);
            render(<DotEditableText contentlet={dotcmsContentletMock} fieldName="title" />);
        });

        it('should render the content', () => {
            const editor = screen.queryByTestId('tinymce-editor');
            expect(editor).toBeNull();
            expect(screen.getByText(dotcmsContentletMock['title'])).not.toBeNull();
        });
    });

    describe('Inside editor', () => {
        let rerenderFn: (ui: React.ReactNode) => void;

        beforeEach(() => {
            mockedDotcmsClient.isInsideEditor.mockReturnValue(true);
            const { rerender } = render(
                <DotEditableText contentlet={dotcmsContentletMock} fieldName="title" />
            );
            rerenderFn = rerender;
        });

        it('should pass the correct props to the Editor component', () => {
            const editor = screen.getByTestId('tinymce-editor');
            expect(editor).not.toBeNull();

            expect(Editor).toHaveBeenCalledWith(
                {
                    tinymceScriptSrc: 'http://localhost:8080/ext/tinymcev7/tinymce.min.js',
                    inline: true,
                    init: {
                        inline: true,
                        menubar: false,
                        plugins: '',
                        powerpaste_html_import: 'clean',
                        powerpaste_word_import: 'clean',
                        suffix: '.min',
                        toolbar: '',
                        valid_styles: {
                            '*': 'font-size,font-family,color,text-decoration,text-align'
                        }
                    },
                    initialValue: dotcmsContentletMock.title,
                    onMouseDown: expect.any(Function),
                    onFocusOut: expect.any(Function),
                    onInit: expect.any(Function)
                },
                {}
            );
        });

        describe('DotEditableText events', () => {
            let focusSpy: jest.SpyInstance;

            describe('Window Message', () => {
                beforeEach(() => {
                    focusSpy = jest.spyOn(TINYMCE_EDITOR_MOCK, 'focus');
                });

                it("should focus on the editor when the message is 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            data: {
                                name: 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS',
                                payload: {
                                    oldInode: dotcmsContentletMock['inode'],
                                    inode: '456'
                                }
                            }
                        })
                    );
                    expect(focusSpy).toHaveBeenCalled();
                });

                it("should not focus on the editor when the message is not 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            data: { name: 'ANOTHER_EVENT' }
                        })
                    );
                    expect(focusSpy).not.toHaveBeenCalled();
                });
            });

            describe('mousedown', () => {
                const event = new MouseEvent('mousedown', {
                    bubbles: true
                });
                const mutiplePagesContentlet = {
                    ...dotcmsContentletMock,
                    onNumberOfPages: 2
                };

                it('should postMessage the UVE if the content is in multiple pages', () => {
                    rerenderFn(
                        <DotEditableText contentlet={mutiplePagesContentlet} fieldName="title" />
                    );
                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);

                    const payload = {
                        dataset: {
                            fieldName: 'title',
                            inode: mutiplePagesContentlet.inode,
                            language: mutiplePagesContentlet.languageId
                        }
                    };
                    expect(postMessageToEditor).toHaveBeenCalledWith({
                        action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
                        payload
                    });
                });

                it('should not postMessage the UVE if the content is in a single page', () => {
                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                });
            });

            describe('onFocusOut', () => {
                let isDirtySpy: jest.SpyInstance;
                let getContentSpy: jest.SpyInstance;

                const event = new FocusEvent('focusout', {
                    bubbles: true
                });

                beforeEach(() => {
                    isDirtySpy = jest.spyOn(TINYMCE_EDITOR_MOCK, 'isDirty');
                    getContentSpy = jest.spyOn(TINYMCE_EDITOR_MOCK, 'getContent');
                });

                it('should not postMessage the UVE if the editor is not dirty', () => {
                    mockedDotcmsClient.isInsideEditor.mockReturnValue(false);
                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);
                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                });

                it('should not postMessage the UVE if the content did not change', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue(dotcmsContentletMock.title);

                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                });

                it('should postMessage the UVE if the content changed', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue('New content');

                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);

                    const postMessageData = {
                        action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                        payload: {
                            content: 'New content',
                            dataset: {
                                inode: dotcmsContentletMock.inode,
                                langId: dotcmsContentletMock.languageId,
                                fieldName: 'title'
                            }
                        }
                    };

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).toHaveBeenCalledWith(postMessageData);
                });
            });
        });
    });

    afterEach(() => jest.clearAllMocks()); // Clear all mocks to avoid side effects from other tests
});
