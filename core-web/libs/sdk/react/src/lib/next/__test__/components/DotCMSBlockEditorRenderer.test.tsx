import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import { BlockEditorNode } from '@dotcms/types';
import * as blockValidator from '@dotcms/uve/internal';

import {
    DotCMSBlockEditorRenderer,
    CustomRendererProps
} from '../../components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer';
import * as isDevModeHook from '../../hooks/useIsDevMode';

describe('DotCMSBlockEditorRenderer', () => {
    const blocks: BlockEditorNode = {
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
    };

    beforeEach(() => {
        jest.spyOn(isDevModeHook, 'useIsDevMode').mockReturnValue(true);
        jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render the block content correctly', () => {
        const { getByText } = render(<DotCMSBlockEditorRenderer blocks={blocks} />);
        expect(getByText('Hello, World!')).toBeInTheDocument();
    });

    it('should render with custom renderers', () => {
        const customRenderers = {
            paragraph: ({ node }: CustomRendererProps) => {
                const text = node.content?.[0]?.text;

                return <p data-testid="custom-paragraph">{text}</p>;
            }
        };

        const { getByTestId } = render(
            <DotCMSBlockEditorRenderer blocks={blocks} customRenderers={customRenderers} />
        );

        expect(getByTestId('custom-paragraph')).toBeInTheDocument();
    });

    it('should apply className and style props to container', () => {
        const { container } = render(
            <DotCMSBlockEditorRenderer
                blocks={blocks}
                className="test-class"
                style={{ color: 'red' }}
            />
        );

        expect(container.firstChild).toHaveClass('test-class');
        expect(container.firstChild).toHaveStyle('color: red');
    });

    describe('Error Handling', () => {
        it('should show error message in dev mode when blocks object is not defined', () => {
            jest.spyOn(blockValidator, 'isValidBlocks').mockReturnValue({
                error: 'Error: Blocks object is not defined'
            });

            const { getByTestId } = render(
                <DotCMSBlockEditorRenderer blocks={null as unknown as BlockEditorNode} />
            );

            expect(getByTestId('invalid-blocks-message')).toHaveTextContent(
                'Error: Blocks object is not defined'
            );
            expect(console.error).toHaveBeenCalledWith('Error: Blocks object is not defined');
        });

        it('should show error message in dev mode when blocks object is invalid', () => {
            jest.spyOn(blockValidator, 'isValidBlocks').mockReturnValue({
                error: 'Error: Blocks must be an object, but received: string'
            });

            const { getByTestId } = render(
                <DotCMSBlockEditorRenderer
                    blocks={'Invalid blocks' as unknown as BlockEditorNode}
                />
            );

            expect(getByTestId('invalid-blocks-message')).toHaveTextContent(
                'Error: Blocks must be an object, but received: string'
            );
            expect(console.error).toHaveBeenCalledWith(
                'Error: Blocks must be an object, but received: string'
            );
        });

        it('should not render error message in production mode when blocks are invalid', () => {
            jest.spyOn(isDevModeHook, 'useIsDevMode').mockReturnValue(false);
            jest.spyOn(blockValidator, 'isValidBlocks').mockReturnValue({
                error: 'Error: Blocks content is empty'
            });

            const { container } = render(
                <DotCMSBlockEditorRenderer blocks={null as unknown as BlockEditorNode} />
            );

            expect(container.firstChild).toBeNull();
            expect(console.error).toHaveBeenCalledWith('Error: Blocks content is empty');
        });
    });

    describe('Block validation', () => {
        it('should validate blocks on mount', () => {
            const isValidBlocksSpy = jest
                .spyOn(blockValidator, 'isValidBlocks')
                .mockReturnValue({ error: null });

            render(<DotCMSBlockEditorRenderer blocks={blocks} />);

            expect(isValidBlocksSpy).toHaveBeenCalledWith(blocks);
        });

        it('should update block state when blocks change', () => {
            const isValidBlocksSpy = jest
                .spyOn(blockValidator, 'isValidBlocks')
                .mockReturnValue({ error: null });

            const { rerender } = render(<DotCMSBlockEditorRenderer blocks={blocks} />);

            const updatedBlocks: BlockEditorNode = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        attrs: {},
                        content: [
                            {
                                type: 'text',
                                text: 'Updated content'
                            }
                        ]
                    }
                ]
            };

            rerender(<DotCMSBlockEditorRenderer blocks={updatedBlocks} />);

            expect(isValidBlocksSpy).toHaveBeenCalledWith(updatedBlocks);
        });
    });
});
