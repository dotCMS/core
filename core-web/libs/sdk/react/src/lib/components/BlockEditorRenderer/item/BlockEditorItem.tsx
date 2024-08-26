import { Blocks, ContentNode, CustomRenderer } from '../../../models/blocks.interface';
import { BlockQuote, CodeBlock } from '../blocks/Code';
import { DotContent } from '../blocks/Contentlet';
import { DotCMSImage } from '../blocks/Image';
import { BulletList, ListItem, OrderedList } from '../blocks/Lists';
import { TableRenderer } from '../blocks/Table';
import { Heading, Paragraph, TextBlock } from '../blocks/Texts';
import { DotCMSVideo } from '../blocks/Video';

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
    return content?.map((node: ContentNode) => {
        const CustomRendererComponent = customRenderers?.[node.type];
        if (CustomRendererComponent) {
            return (
                <CustomRendererComponent
                    key={crypto.randomUUID()}
                    {...node.attrs}
                    content={node.content}>
                    <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                </CustomRendererComponent>
            );
        }

        switch (node.type) {
            case Blocks.PARAGRAPH:
                return (
                    <Paragraph key={crypto.randomUUID()} {...node}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Paragraph>
                );

            case Blocks.HEADING:
                return (
                    <Heading key={crypto.randomUUID()} {...node}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </Heading>
                );

            case Blocks.TEXT:
                return <TextBlock key={crypto.randomUUID()} {...node} />;

            case Blocks.BULLET_LIST:
                return (
                    <BulletList key={crypto.randomUUID()}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BulletList>
                );

            case Blocks.ORDERED_LIST:
                return (
                    <OrderedList key={crypto.randomUUID()}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </OrderedList>
                );

            case Blocks.LIST_ITEM:
                return (
                    <ListItem key={crypto.randomUUID()}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </ListItem>
                );

            case Blocks.BLOCK_QUOTE:
                return (
                    <BlockQuote key={crypto.randomUUID()}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </BlockQuote>
                );

            case Blocks.CODE_BLOCK:
                return (
                    <CodeBlock key={crypto.randomUUID()} {...node}>
                        <BlockEditorItem content={node.content} customRenderers={customRenderers} />
                    </CodeBlock>
                );

            case Blocks.HARDBREAK:
                return <br key={crypto.randomUUID()} />;

            case Blocks.HORIZONTAL_RULE:
                return <hr key={crypto.randomUUID()} />;

            case Blocks.DOT_IMAGE:
                return <DotCMSImage key={crypto.randomUUID()} {...node} />;

            case Blocks.DOT_VIDEO:
                return <DotCMSVideo key={crypto.randomUUID()} {...node} />;

            case Blocks.TABLE:
                return (
                    <TableRenderer
                        key={crypto.randomUUID()}
                        content={node.content}
                        blockEditorItem={BlockEditorItem}
                    />
                );

            case Blocks.DOT_CONTENT:
                return (
                    <DotContent
                        key={crypto.randomUUID()}
                        {...node}
                        customRenderers={customRenderers}
                    />
                );

            default:
                return <div key={crypto.randomUUID()}>Unknown Block Type {node.type}</div>;
        }
    });
};
