import { expect } from '@jest/globals';
import { render, screen } from '@testing-library/react';
import * as tinymceReact from '@tinymce/tinymce-react';

import * as dotcmsClient from '@dotcms/client';

import { DotEditableText } from './DotEditableText';

import { dotcmsContentletMock } from '../../mocks/mockPageContext';

// Define mockEditor before using it in jest.mock

jest.mock('@tinymce/tinymce-react', () => ({
    Editor: jest.fn((_props: tinymceReact.IAllProps) => {
        return <div data-testid="tinymce-editor" />;
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
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('Outside editor', () => {
        beforeEach(() => {
            mockedDotcmsClient.isInsideEditor.mockReturnValue(false);
            render(<DotEditableText contentlet={dotcmsContentletMock} fieldName="title" />);
        });

        it('should render without crashing', () => {
            const editor = screen.queryByTestId('tinymce-editor');
            expect(editor).toBeNull();
        });
    });

    describe('Inside editor', () => {
        beforeEach(() => {
            mockedDotcmsClient.isInsideEditor.mockReturnValue(true);
            render(<DotEditableText contentlet={dotcmsContentletMock} fieldName="title" />);
        });

        it('should initialize the editor', () => {
            const editor = screen.getByTestId('tinymce-editor');
            expect(editor).not.toBeNull();
        });

        it('should pass the correct props to the Editor component', () => {
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
    });
});
