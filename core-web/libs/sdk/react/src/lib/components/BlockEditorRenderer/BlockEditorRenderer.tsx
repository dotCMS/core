import { BlockQuote, CodeBlock } from './blocks/Code';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { Bold, Heading, Italic, Paragraph, Strike, TextNode, Underline } from './blocks/Texts';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface ContentNode<T = Record<string, string>> {
    type: string;
    content: ContentNode[];
    attrs?: T;
    marks?: Mark[];
    text?: string;
}

interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export const BlockEditorRenderer = ({ content }: { content: ContentNode[] }) => {
    return (
        <>
            {content.map((node, index) => {
                switch (node.type) {
                    case 'paragraph':
                        return (
                            <Paragraph key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </Paragraph>
                        );

                    case 'heading':
                        return (
                            <Heading key={index} level={node.attrs?.level}>
                                <BlockEditorRenderer content={node.content} />
                            </Heading>
                        );

                    // case 'text':
                    //     return node.text;

                    case 'text':
                        return <TextNode key={index} {...node} />

                    case 'bold':
                        return (
                            <Bold key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </Bold>
                        );

                    case 'italic':
                        return (
                            <Italic key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </Italic>
                        );

                    case 'strike':
                        return (
                            <Strike key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </Strike>
                        );

                    case 'underline':
                        return (
                            <Underline key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </Underline>
                        );

                    case 'bulletList':
                        return (
                            <BulletList key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </BulletList>
                        );

                    case 'orderedList':
                        return (
                            <OrderedList key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </OrderedList>
                        );

                    case 'listItem':
                        return (
                            <ListItem key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </ListItem>
                        );

                    case 'blockquote':
                        return (
                            <BlockQuote key={index}>
                                <BlockEditorRenderer content={node.content} />
                            </BlockQuote>
                        );

                    case 'codeBlock':
                        return (
                            <CodeBlock key={index} language={node.attrs?.language}>
                                <BlockEditorRenderer content={node.content} />
                            </CodeBlock>
                        );

                    case 'hardBreak':
                        return <br key={index} />;

                    case 'horizontalRule':
                        return <hr key={index} />;

                    default:
                        console.log('Nothing: ', node);

                        return <div>Nothing</div>;
                }
            })}
        </>
    );
};
