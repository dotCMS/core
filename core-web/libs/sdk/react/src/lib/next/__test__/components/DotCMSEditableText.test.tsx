import { expect } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react';
import * as tinymceReact from '@tinymce/tinymce-react';

import { DotCMSBasicContentlet, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import * as dotcmsUVE from '@dotcms/uve';
import { sendMessageToUVE, getUVEState } from '@dotcms/uve';

import { DotCMSEditableText } from '../../components/DotCMSEditableText/DotCMSEditableText';
import { MOCK_CONTENTLET } from '../mock';

// Define mockEditor before using it in jest.mock
const TINYMCE_EDITOR_MOCK = {
    focus: () => {
        /* empty */
    },
    getContent: (_data: string) => '',
    isDirty: () => false,
    hasFocus: () => false,
    setContent: () => {
        /* empty */
    }
};

const MOCK_UVE_STATE = {
    mode: 'EDIT_MODE',
    dotCMSHost: 'http://localhost:8080',
    languageId: null,
    persona: null,
    variantName: null,
    experimentId: null,
    publishDate: null
};

jest.mock('@tinymce/tinymce-react', () => ({
    Editor: jest.fn(({ onInit, onMouseDown, onFocusOut }) => {
        onInit({}, TINYMCE_EDITOR_MOCK);

        return <div data-testid="tinymce-editor" onMouseDown={onMouseDown} onBlur={onFocusOut} />;
    })
}));

// Mock @dotcms/uve module
jest.mock('@dotcms/uve', () => ({
    ...jest.requireActual('@dotcms/uve'),
    sendMessageToUVE: jest.fn(),
    getUVEState: jest.fn().mockImplementation(() => MOCK_UVE_STATE)
}));

const { Editor } = tinymceReact as jest.Mocked<typeof tinymceReact>;
const mockedGetUVEState = getUVEState as jest.MockedFunction<typeof getUVEState>;

describe('DotCMSEditableText', () => {
    let getUVEStateSpy: jest.SpyInstance;
    let consoleErrorSpy: jest.SpyInstance;
    let consoleWarnSpy: jest.SpyInstance;

    beforeEach(() => {
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
        consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty */
        });
        getUVEStateSpy = jest.spyOn(dotcmsUVE, 'getUVEState');
    });

    afterEach(() => {
        jest.clearAllMocks();
        consoleErrorSpy.mockRestore();
        consoleWarnSpy.mockRestore();
        getUVEStateSpy.mockRestore();
    });

    describe('Console logs', () => {
        it('should log error when contentlet is missing', () => {
            mockedGetUVEState.mockReturnValueOnce({
                mode: UVE_MODE.EDIT,
                dotCMSHost: 'http://localhost:8080',
                languageId: null,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null
            });
            // @ts-expect-error - contentlet is required but we're testing error case
            render(<DotCMSEditableText fieldName="title" />);

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                '[DotCMSEditableText]: contentlet or fieldName is missing',
                'Ensure that all needed props are passed to view and edit the content'
            );
        });

        it('should log error when fieldName is missing', () => {
            mockedGetUVEState.mockReturnValueOnce({
                mode: UVE_MODE.EDIT,
                dotCMSHost: 'http://localhost:8080',
                languageId: null,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null
            });
            // @ts-expect-error - fieldName is required but we're testing error case
            render(<DotCMSEditableText contentlet={MOCK_CONTENTLET} />);
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                '[DotCMSEditableText]: contentlet or fieldName is missing',
                'Ensure that all needed props are passed to view and edit the content'
            );
        });

        it('should log warning when not in EDIT mode', () => {
            mockedGetUVEState.mockReturnValue({
                mode: UVE_MODE.PREVIEW,
                languageId: null,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null,
                dotCMSHost: null
            });
            render(<DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />);

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                '[DotCMSEditableText]: TinyMCE is not available in the current mode'
            );
        });

        it('should log warning when dotCMSHost is not defined', () => {
            mockedGetUVEState.mockReturnValue({
                mode: UVE_MODE.EDIT,
                languageId: null,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null,
                dotCMSHost: null
            });
            render(<DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />);

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                '[DotCMSEditableText]: The `dotCMSHost` parameter is not defined. Check that the UVE is sending the correct parameters.'
            );
        });
    });

    describe('Outside editor', () => {
        beforeEach(() => {
            mockedGetUVEState.mockReturnValue(undefined);
            render(<DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />);
        });

        it('should render the content', () => {
            const editor = screen.queryByTestId('tinymce-editor');
            expect(editor).toBeNull();
            expect(screen.getByText(MOCK_CONTENTLET['title'])).not.toBeNull();
        });
    });

    describe('Inside editor', () => {
        let rerenderFn: (ui: React.ReactNode) => void;

        beforeEach(() => {
            mockedGetUVEState.mockReturnValue({
                mode: UVE_MODE.EDIT,
                dotCMSHost: 'http://localhost:8080',
                languageId: null,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null
            });
            const { rerender } = render(
                <DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />
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
                        licenseKey: 'gpl',
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
                    initialValue: MOCK_CONTENTLET.title,
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

                it("should focus on the editor when the message is 'UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            data: {
                                name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                                payload: {
                                    oldInode: MOCK_CONTENTLET['inode'],
                                    inode: '456'
                                }
                            }
                        })
                    );
                    expect(focusSpy).toHaveBeenCalled();
                });

                it("should not focus on the editor when the message is not 'UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
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
                const mutiplePagesContentlet: DotCMSBasicContentlet = {
                    ...MOCK_CONTENTLET,
                    onNumberOfPages: '2'
                };

                it('should postMessage the UVE if the content is in multiple pages', () => {
                    rerenderFn(
                        <DotCMSEditableText contentlet={mutiplePagesContentlet} fieldName="title" />
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
                    expect(sendMessageToUVE).toHaveBeenCalledWith({
                        action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
                        payload
                    });
                });

                it('should not postMessage the UVE if the content is in a single page', () => {
                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);
                    expect(sendMessageToUVE).not.toHaveBeenCalled();
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
                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);
                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });

                    expect(sendMessageToUVE).not.toHaveBeenCalled();
                });

                it('should not postMessage the UVE if the content did not change', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue(MOCK_CONTENTLET.title);

                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(sendMessageToUVE).not.toHaveBeenCalled();
                });

                it('should postMessage the UVE if the content changed', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue('New content');

                    const editorElem = screen.getByTestId('tinymce-editor');
                    fireEvent(editorElem, event);

                    const postMessageData = {
                        action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                        payload: {
                            content: 'New content',
                            dataset: {
                                inode: MOCK_CONTENTLET.inode,
                                langId: MOCK_CONTENTLET.languageId,
                                fieldName: 'title'
                            }
                        }
                    };

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(sendMessageToUVE).toHaveBeenCalledWith(postMessageData);
                });
            });
        });
    });

    describe('Contentlet and fieldName changes', () => {
        beforeEach(() => {
            getUVEStateSpy.mockImplementation(() => null);
        });

        it('should update the HTML when contentlet changes', () => {
            const { rerender } = render(
                <DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />
            );

            expect(screen.getByText(MOCK_CONTENTLET['title'])).not.toBeNull();

            const newContentlet = { ...MOCK_CONTENTLET, title: 'New Title' };
            rerender(<DotCMSEditableText contentlet={newContentlet} fieldName="title" />);

            expect(screen.getByText('New Title')).not.toBeNull();
        });

        it('should update the HTML when fieldName changes', () => {
            const { rerender } = render(
                <DotCMSEditableText contentlet={MOCK_CONTENTLET} fieldName="title" />
            );

            expect(screen.getByText(MOCK_CONTENTLET['title'])).not.toBeNull();

            const newFieldName = 'description';
            const newContentlet = { ...MOCK_CONTENTLET, description: 'New Description' };
            rerender(<DotCMSEditableText contentlet={newContentlet} fieldName={newFieldName} />);

            expect(screen.getByText('New Description')).not.toBeNull();
        });
    });
});
