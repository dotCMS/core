import { BlockQuote, CodeBlock } from './blocks/Code';
import { Contentlet, DotContent } from './blocks/Contentlet';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { TableRenderer } from './blocks/Table';
import { Heading, Paragraph, TextBlock } from './blocks/Texts';
import { DotCMSVideo } from './blocks/Video';

import { Block, ContentNode, CustomRenderer, DotAssetProps } from '../../models/blocks.interface';

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

export interface BlockEditorRendererProps {
    blocks: Block;
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

export const BlockEditorItem = ({
    content,
    customRenderers
}: {
    content: ContentNode[];
    customRenderers?: CustomRenderer;
}) => {
    return content?.map((node: ContentNode, index: number) => {
        const CustomRendererComponent = customRenderers?.[node.type];
        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent key={index} {...node.attrs} content={node.content}>
                    <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                </CustomRendererComponent>
            );
        }

        switch (node.type) {
            case 'paragraph':
                return (
                    <Paragraph key={index} attrs={node.attrs}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Paragraph>
                );

            case 'heading':
                return (
                    <Heading key={index} level={node.attrs?.level}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Heading>
                );

            case 'text':
                return <TextBlock key={index} {...node} />;

            case 'bulletList':
                return (
                    <BulletList key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BulletList>
                );

            case 'orderedList':
                return (
                    <OrderedList key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </OrderedList>
                );

            case 'listItem':
                return (
                    <ListItem key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </ListItem>
                );

            case 'blockquote':
                return (
                    <BlockQuote key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BlockQuote>
                );

            case 'codeBlock':
                return (
                    <CodeBlock key={index} language={node.attrs?.language}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </CodeBlock>
                );

            case 'hardBreak':
                return <br key={index} />;

            case 'horizontalRule':
                return <hr key={index} />;

            case 'dotImage':
                return <DotCMSImage key={index} {...(node.attrs as DotAssetProps)} />;

            case 'dotVideo':
                return <DotCMSVideo key={index} {...(node.attrs as DotAssetProps)} />;

            case 'table':
                return (
                    <TableRenderer
                        key={index}
                        content={node.content}
                        blockEditorItem={BlockEditorItem}
                    />
                );

            case 'dotContent':
                return (
                    <DotContent
                        key={index}
                        data={node.attrs?.data as Contentlet}
                        customRenderers={customRenderers}
                    />
                );

            default:
                return <div key={index}>Unknown Block Type {node.type}</div>;
        }
    });
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
