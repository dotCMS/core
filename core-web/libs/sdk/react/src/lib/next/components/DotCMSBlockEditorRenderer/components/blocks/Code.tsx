import { BlockEditorNode } from '@dotcms/types';

interface CodeBlockProps {
    node: BlockEditorNode;
    children: React.ReactNode;
}

/**
 * Renders a code block component.
 *
 * @param attrs - The attributes of the code block.
 * @param children - The content of the code block.
 * @returns The rendered code block component.
 */
export const CodeBlock = ({ node, children }: CodeBlockProps) => {
    const language = node?.attrs?.language || '';

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
export const BlockQuote = ({ children }: { children: React.ReactNode }) => {
    return <blockquote>{children}</blockquote>;
};
