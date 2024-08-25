import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import { BlockEditorRenderer } from './BlockEditorRenderer';

import { Block } from '../../models/blocks.interface';

describe('BlockEditorRenderer', () => {
    const blocks = {
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
});
