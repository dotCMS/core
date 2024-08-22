/* eslint-disable @typescript-eslint/no-explicit-any */
import { BlockQuote, CodeBlock } from './blocks/Code';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { Bold, Heading, Italic, Paragraph, Strike, TextBlock, Underline } from './blocks/Texts';

//TODO: Uncomment later
const Blocks: Record<string, any> = {
    paragraph: Paragraph,
    heading: Heading,
    text: TextBlock,
    bold: Bold,
    italic: Italic,
    strike: Strike,
    underline: Underline,
    bulletList: BulletList,
    orderedList: OrderedList,
    listItem: ListItem,
    blockquote: BlockQuote,
    codeBlock: CodeBlock,
    hardBreak: () => <br />,
    horizontalRule: () => <hr />
    // dotImage: DotCMSImage
};

interface BlockEditorRendererProps {
    blocks: any;
    customRenderers?: Record<string, React.FC>;
    className?: string;
    style?: React.CSSProperties;
}

const BlockEditorItem = ({ content, customRenderers }: { content: any; customRenderers: any }) => {
    if (!Array.isArray(content)) {
        console.log(content);
        
        return null;
    }

    return (
        // eslint-disable-next-line react/jsx-no-useless-fragment
        <>
            {content?.map((node: any, index: number) => {
                switch (node.type) {
                    case 'paragraph':
                        return (
                            <Paragraph key={index}>
                                <BlockEditorItem content={node} customRenderers={customRenderers} />
                            </Paragraph>
                        );

                    case 'heading':
                        return (
                            <Heading key={index} level={node.attrs?.level}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </Heading>
                        );

                    case 'text':
                        return <TextBlock key={index} {...node} />;

                    case 'bold':
                        return (
                            <Bold key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </Bold>
                        );

                    case 'italic':
                        return (
                            <Italic key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </Italic>
                        );

                    case 'strike':
                        return (
                            <Strike key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </Strike>
                        );

                    case 'underline':
                        return (
                            <Underline key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </Underline>
                        );

                    case 'bulletList':
                        return (
                            <BulletList key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </BulletList>
                        );

                    case 'orderedList':
                        return (
                            <OrderedList key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </OrderedList>
                        );

                    case 'listItem':
                        return (
                            <ListItem key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </ListItem>
                        );

                    case 'blockquote':
                        return (
                            <BlockQuote key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </BlockQuote>
                        );

                    case 'codeBlock':
                        console.log('codeBlock node: ', node);

                        return (
                            <CodeBlock key={index} language={node.attrs?.language}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
                            </CodeBlock>
                        );

                    case 'hardBreak':
                        return <br key={index} />;

                    case 'horizontalRule':
                        return <hr key={index} />;

                    case 'dotImage':
                        return <DotCMSImage alt="" key={index} {...node.attrs} />;

                    default:
                        return <div key={index}>Unknown Block Type: {node.type}</div>;
                }
            })}
        </>
    );
};

export const BlockEditorRenderer = (props: BlockEditorRendererProps) => {
    const { blocks, customRenderers } = props;

    return <BlockEditorItem content={blocks.content} customRenderers={customRenderers} />;
};
