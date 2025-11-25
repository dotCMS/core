import { BlockProps } from '../../../models/blocks.interface';
import { CodeBlockProps } from '../../../models/content-node.interface';

/**
 * Renders a code block component.
 *
 * @param attrs - The attributes of the code block.
 * @param children - The content of the code block.
 * @returns The rendered code block component.
 */
export const CodeBlock = ({ attrs, children }: CodeBlockProps) => {
    const language = attrs?.language || '';

    return (
        <pre data-language={language}>
            <code>{children}</code>
        </pre>
    );
};

/**
 * Renders a blockquote component.
 *
 * @param children - The content to be rendered inside the blockquote.
 * @returns The rendered blockquote component.
 */
export const BlockQuote = ({ children }: BlockProps) => {
    return <blockquote>{children}</blockquote>;
};
