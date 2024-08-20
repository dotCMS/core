import { BlockProps } from '../../../models';

type CodeBlockProps = BlockProps & { language?: string };

export const CodeBlock = ({ language, children }: CodeBlockProps) => {
    return (
        <pre data-language={language}>
            <code>{children}</code>
        </pre>
    );
};

export const BlockQuote = ({ children }: BlockProps) => {
    return <blockquote>{children}</blockquote>;
};
