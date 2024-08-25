import { BlockQuote, CodeBlock } from './blocks/Code';
import { Contentlet, DotContent } from './blocks/Contentlet';
import { DotCMSImage } from './blocks/Image';
import { BulletList, ListItem, OrderedList } from './blocks/Lists';
import { TableRenderer } from './blocks/Table';
import { Heading, Paragraph, TextBlock } from './blocks/Texts';
import { DotCMSVideo } from './blocks/Video';

import {
    Block,
    Blocks,
    ContentNode,
    CustomRenderer,
    DotAssetProps
} from '../../models/blocks.interface';

export interface BlockEditorRendererProps {
    blocks: Block;
    customRenderers?: CustomRenderer;
    className?: string;
    style?: React.CSSProperties;
}

/**
 * Renders a block editor item based on the provided content and custom renderers.
 *
 * @param content - The content nodes to render.
 * @param customRenderers - Optional custom renderers for specific node types.
 * @returns The rendered block editor item.
 */
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
            case Blocks.PARAGRAPH:
                return (
                    <Paragraph key={index} attrs={node.attrs}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Paragraph>
                );

            case Blocks.HEADING:
                return (
                    <Heading key={index} level={node.attrs?.level}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Heading>
                );

            case Blocks.TEXT:
                return <TextBlock key={index} {...node} />;

            case Blocks.BULLET_LIST:
                return (
                    <BulletList key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BulletList>
                );

            case Blocks.ORDERED_LIST:
                return (
                    <OrderedList key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </OrderedList>
                );

            case Blocks.LIST_ITEM:
                return (
                    <ListItem key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </ListItem>
                );

            case Blocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={index}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BlockQuote>
                );

            case Blocks.CODE_BLOCK:
                return (
                    <CodeBlock key={index} language={node.attrs?.language}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </CodeBlock>
                );

            case Blocks.HARDBREAK:
                return <br key={index} />;

            case Blocks.HORIZONTAL_RULE:
                return <hr key={index} />;

            case Blocks.DOT_IMAGE:
                return <DotCMSImage key={index} {...(node.attrs as DotAssetProps)} />;

            case Blocks.DOT_VIDEO:
                return <DotCMSVideo key={index} {...(node.attrs as DotAssetProps)} />;

            case Blocks.TABLE:
                return (
                    <TableRenderer
                        key={index}
                        content={node.content}
                        blockEditorItem={BlockEditorItem}
                    />
                );

            case Blocks.DOT_CONTENT:
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

/**
 * Renders a block editor with the specified blocks, custom renderers, className, and style.
 *
 * @param blocks - The blocks to be rendered in the editor.
 * @param customRenderers - Custom renderers for specific block types.
 * @param className - The CSS class name for the container element.
 * @param style - The inline styles for the container element.
 * @returns The rendered block editor.
 */
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
