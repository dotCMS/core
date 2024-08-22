/* eslint-disable @typescript-eslint/no-explicit-any */
import { BlockQuote, CodeBlock } from './blocks/Code';
import { DotContent } from './blocks/Contentlet';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { Bold, Heading, Italic, Paragraph, Strike, TextBlock, Underline } from './blocks/Texts';

//TODO: Move this to models later
interface Mark {
    type: string;
    attrs: Record<string, string>;
}

export interface ContentNode<T = Record<string, string>> {
    type: string;
    content: ContentNode[];
    attrs?: T;
    marks?: Mark[];
    text?: string;
}

//TODO: Use this to centralize the block types
// const Blocks: Record<string, any> = {
//     paragraph: Paragraph,
//     heading: Heading,
//     text: TextBlock,
//     bold: Bold,
//     italic: Italic,
//     strike: Strike,
//     underline: Underline,
//     bulletList: BulletList,
//     orderedList: OrderedList,
//     listItem: ListItem,
//     blockquote: BlockQuote,
//     codeBlock: CodeBlock,
//     hardBreak: () => <br />,
//     horizontalRule: () => <hr />,
//     dotImage: DotCMSImage
// };

type CustomRenderer = Record<string, React.FC<any>>;
interface BlockEditorRendererProps {
    blocks: { content: ContentNode[] };
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

const BlockEditorItem = ({
    content,
    customRenderers
}: {
    content: ContentNode[];
    customRenderers?: CustomRenderer;
}) => {
    return (
        // eslint-disable-next-line react/jsx-no-useless-fragment
        <>
            {content?.map((node: ContentNode, index: number) => {
                const CustomRendererComponent = customRenderers?.[node.type];
                if (CustomRendererComponent) {
                    return (
                        <CustomRendererComponent key={index} {...node.attrs} content={node.content}>
                            <BlockEditorItem
                                content={node.content}
                                customRenderers={customRenderers}
                            />
                        </CustomRendererComponent>
                    );
                }

                switch (node.type) {
                    case 'paragraph':
                        return (
                            <Paragraph key={index}>
                                <BlockEditorItem
                                    content={node.content}
                                    customRenderers={customRenderers}
                                />
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
                        return <DotCMSImage key={index} {...node.attrs} />;

                    case 'dotContent':
                        return (
                            <DotContent
                                key={index}
                                {...node.attrs}
                                customRenderers={customRenderers}
                            />
                        );

                    default:
                        return <div key={index}>Unknown Block Type: {node.type}</div>;
                }
            })}
        </>
    );
};

export const BlockEditorRenderer = ({
    blocks,
    customRenderers,
    className,
    style
}: BlockEditorRendererProps) => {
    return (
        <div className={className} style={style}>
            <BlockEditorItem content={blocks.content} customRenderers={customRenderers} />
        </div>
    );
};
