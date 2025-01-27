import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import * as client from '@dotcms/client';

import { BlockEditorRenderer } from './BlockEditorRenderer';

import { dotcmsContentletMock } from '../../mocks/mockPageContext';
import { Block } from '../../models/blocks.interface';

describe('BlockEditorRenderer', () => {
    const blocks = {
        type: 'doc',
        content: [
            {
                type: 'paragraph',
                attrs: {},
                content: [
                    {
                        type: 'text',
                        text: 'Hello, World!'
                    }
                ]
            }
        ]
    } as Block;

    it('should render the BlockEditorItem component', () => {
        const { getByText } = render(<BlockEditorRenderer blocks={blocks} />);
        expect(getByText('Hello, World!')).toBeInTheDocument();
    });

    it('should render the custom renderer component', () => {
        const customRenderers = {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            paragraph: ({ content }: { content: any }) => {
                const [{ text }] = content;

                return <p data-testid="custom-paragraph">{text}</p>;
            }
        };
        const { getByTestId } = render(
            <BlockEditorRenderer blocks={blocks} customRenderers={customRenderers} />
        );
        expect(getByTestId('custom-paragraph')).toBeInTheDocument();
    });

    it('should render the property className and style props', () => {
        const { container } = render(
            <BlockEditorRenderer blocks={blocks} className="test-class" style={{ color: 'red' }} />
        );
        expect(container.firstChild).toHaveClass('test-class');
        expect(container.firstChild).toHaveStyle('color: red');
    });

    describe('Error Handling', () => {
        it('should show error message when blocks object is not provided', () => {
            jest.spyOn(client, 'isInsideEditor').mockImplementation(() => true);
            const consoleSpy = jest.spyOn(console, 'error');

            const { getByTestId } = render(
                <BlockEditorRenderer blocks={undefined as unknown as Block} />
            );

            expect(getByTestId('invalid-blocks-message')).toHaveTextContent(
                'Error: Blocks object is not defined'
            );
            expect(consoleSpy).toHaveBeenCalledWith('Error: Blocks object is not defined');
        });

        it('should show error message when blocks object is not valid', () => {
            jest.spyOn(client, 'isInsideEditor').mockImplementation(() => true);
            const consoleSpy = jest.spyOn(console, 'error');

            const { getByTestId } = render(
                <BlockEditorRenderer blocks={'Testing' as unknown as Block} />
            );

            expect(getByTestId('invalid-blocks-message')).toHaveTextContent(
                'Error: Blocks must be an object, but received: string'
            );
            expect(consoleSpy).toHaveBeenCalledWith(
                'Error: Blocks must be an object, but received: string'
            );
        });

        it('should show error message when blocks object dont have a doc type', () => {
            jest.spyOn(client, 'isInsideEditor').mockImplementation(() => false);
            const consoleSpy = jest.spyOn(console, 'error');

            const { queryByTestId } = render(
                <BlockEditorRenderer blocks={{ content: [] } as unknown as Block} />
            );

            expect(queryByTestId('invalid-blocks-message')).not.toBeInTheDocument();
            expect(consoleSpy).toHaveBeenCalledWith('Error: Blocks must have a doc type');
        });

        it('should show error message when blocks content array is empty', () => {
            jest.spyOn(client, 'isInsideEditor').mockImplementation(() => false);
            const consoleSpy = jest.spyOn(console, 'error');

            const { queryByTestId } = render(
                <BlockEditorRenderer blocks={{ content: [], type: 'doc' } as unknown as Block} />
            );

            expect(queryByTestId('invalid-blocks-message')).not.toBeInTheDocument();
            expect(consoleSpy).toHaveBeenCalledWith('Error: Blocks content is empty');
        });
    });

    describe('when the editable prop is true', () => {
        beforeEach(() => {
            jest.spyOn(client, 'isInsideEditor').mockImplementation(() => true);
        });

        it("should receive the 'editable' prop and render the BlockEditorBlock component", () => {
            const { getByText } = render(
                <BlockEditorRenderer
                    blocks={blocks}
                    editable
                    contentlet={dotcmsContentletMock}
                    fieldName="fieldName"
                />
            );
            expect(getByText('Hello, World!')).toBeInTheDocument();
        });

        it('should call `initInlineEditing` when the component is clicked', () => {
            const spy = jest.spyOn(client, 'initInlineEditing');
            const { inode, languageId: language, contentType } = dotcmsContentletMock;
            const { getByTestId } = render(
                <BlockEditorRenderer
                    blocks={blocks}
                    editable
                    contentlet={dotcmsContentletMock}
                    fieldName="fieldName"
                />
            );
            const blockEditorContainer = getByTestId('dot-block-editor-container');
            blockEditorContainer.click();
            expect(blockEditorContainer).toHaveTextContent('Hello, World!');
            expect(spy).toHaveBeenCalledWith('BLOCK_EDITOR', {
                inode,
                language,
                contentType,
                content: blocks,
                fieldName: 'fieldName'
            });
        });
    });
});
